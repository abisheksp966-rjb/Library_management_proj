package com.library.server;

import com.library.dao.*;
import com.library.exception.BookUnavailableException;
import com.library.exception.LibraryException;
import com.library.exception.RecordNotFoundException;
import com.library.exception.StudentLimitExceededException;
import com.library.model.*;
import com.library.service.LibraryService;
import com.library.service.ReportService;
import com.library.util.DateUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Embedded HTTP REST & Static Web Server for Library Circulation System.
 * Uses com.sun.net.httpserver.HttpServer (built into Java Standard Library).
 * 
 * College: Velalar College of Engineering and Technology, Erode - 638012
 * Student: Abishek (Register No: 732925ITR004)
 */
public class LibraryServer {

    private final int port;
    private final BookDAO bookDAO;
    private final StudentDAO studentDAO;
    private final TransactionDAO transactionDAO;
    private final CategoryDAO categoryDAO;
    private final AuthorDAO authorDAO;
    private final LibraryService libraryService;
    private final ReportService reportService;
    private HttpServer server;

    public LibraryServer(int port, BookDAO bookDAO, StudentDAO studentDAO, TransactionDAO transactionDAO,
                         CategoryDAO categoryDAO, AuthorDAO authorDAO,
                         LibraryService libraryService, ReportService reportService) {
        this.port = port;
        this.bookDAO = bookDAO;
        this.studentDAO = studentDAO;
        this.transactionDAO = transactionDAO;
        this.categoryDAO = categoryDAO;
        this.authorDAO = authorDAO;
        this.libraryService = libraryService;
        this.reportService = reportService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        // API routes
        server.createContext("/api/health", new HealthHandler());
        // Keep the frontend's shared API contract available on the Java server too.
        server.createContext("/api/stats", new AnalyticsHandler());
        server.createContext("/api/overdue", new OverdueReportsHandler());
        server.createContext("/api/categories", new CategoriesHandler());
        server.createContext("/api/authors", new AuthorsHandler());
        server.createContext("/api/books", new BooksHandler());
        server.createContext("/api/students", new StudentsHandler());
        server.createContext("/api/students/", new StudentHistoryHandler());
        server.createContext("/api/transactions", new TransactionsHandler());
        server.createContext("/api/issue", new IssueHandler());
        server.createContext("/api/return", new ReturnHandler());
        server.createContext("/api/reports/available", new AvailableReportsHandler());
        server.createContext("/api/reports/overdue", new OverdueReportsHandler());
        server.createContext("/api/reports/history", new StudentHistoryHandler());
        server.createContext("/api/analytics", new AnalyticsHandler());
        server.createContext("/api/analytics/python/run", new PythonAnalyticsHandler());
        server.createContext("/api/export/transactions", new ExportHandler());

        // Visualizations route
        server.createContext("/visualizations", new VisualizationsHandler());

        // Static files fallback (Frontend UI)
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("==========================================================================");
        System.out.println("🚀 Library Circulation & Analysis Web Server LIVE at: http://localhost:" + port);
        System.out.println("   Frontend UI: http://localhost:" + port + "/");
        System.out.println("   REST API:    http://localhost:" + port + "/api/health");
        System.out.println("   Python Data: http://localhost:" + port + "/api/export/transactions");
        System.out.println("==========================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods for HTTP & JSON
    // -------------------------------------------------------------------------

    private static void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = "{\"success\":false,\"error\":\"" + escapeJson(message) + "\"}";
        sendJsonResponse(exchange, statusCode, json);
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.trim().isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name());
                String val = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name()) : "";
                map.put(key, val);
            } catch (Exception ignored) {}
        }
        return map;
    }

    private static Map<String, String> parseJsonFields(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String clean = json.trim();
        if (clean.startsWith("{")) clean = clean.substring(1);
        if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);

        // Simple JSON property scanner
        boolean inQuotes = false;
        StringBuilder token = new StringBuilder();
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '"' && (i == 0 || clean.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                pairs.add(token.toString());
                token.setLength(0);
            } else {
                token.append(c);
            }
        }
        if (token.length() > 0) pairs.add(token.toString());

        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String val = kv[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String json = "{\"status\":\"UP\",\"message\":\"Library Circulation and Usage Analysis System API\"," +
                    "\"institution\":\"Velalar College of Engineering and Technology\",\"student\":\"Abishek\",\"regNo\":\"732925ITR004\"}";
            sendJsonResponse(exchange, 200, json);
        }
    }

    private class CategoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Category> list = categoryDAO.getAllCategories();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Category c = list.get(i);
                    sb.append(String.format("{\"categoryId\":%d,\"categoryName\":\"%s\",\"description\":\"%s\"}",
                            c.getCategoryId(), escapeJson(c.getCategoryName()), escapeJson(c.getDescription())));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> fields = parseJsonFields(body);
                String name = fields.get("categoryName");
                String desc = fields.getOrDefault("description", "");
                if (name == null || name.trim().isEmpty()) {
                    sendError(exchange, 400, "categoryName is required.");
                    return;
                }
                Category c = new Category(0, name.trim(), desc.trim());
                int id = categoryDAO.addCategory(c);
                sendJsonResponse(exchange, 201, String.format("{\"success\":true,\"categoryId\":%d,\"categoryName\":\"%s\"}",
                        id, escapeJson(name)));
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private class AuthorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Author> list = authorDAO.getAllAuthors();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Author a = list.get(i);
                    sb.append(String.format("{\"authorId\":%d,\"authorName\":\"%s\",\"email\":\"%s\"}",
                            a.getAuthorId(), escapeJson(a.getAuthorName()), escapeJson(a.getEmail())));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> fields = parseJsonFields(body);
                String name = fields.get("authorName");
                String email = fields.getOrDefault("email", "");
                if (name == null || name.trim().isEmpty()) {
                    sendError(exchange, 400, "authorName is required.");
                    return;
                }
                Author a = new Author(0, name.trim(), email.trim());
                int id = authorDAO.addAuthor(a);
                sendJsonResponse(exchange, 201, String.format("{\"success\":true,\"authorId\":%d,\"authorName\":\"%s\"}",
                        id, escapeJson(name)));
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private class BooksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Book> list = bookDAO.getAllBooks();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Book b = list.get(i);
                    sb.append(String.format(Locale.US,
                            "{\"bookId\":%d,\"isbn\":\"%s\",\"title\":\"%s\",\"categoryId\":%d,\"categoryName\":\"%s\"," +
                            "\"authorId\":%d,\"authorName\":\"%s\",\"totalCopies\":%d,\"availableCopies\":%d,\"isAvailable\":%b}",
                            b.getBookId(), escapeJson(b.getIsbn()), escapeJson(b.getTitle()),
                            b.getCategoryId(), escapeJson(b.getCategoryName()),
                            b.getAuthorId(), escapeJson(b.getAuthorName()),
                            b.getTotalCopies(), b.getAvailableCopies(), b.isAvailable()));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> fields = parseJsonFields(body);
                String isbn = fields.get("isbn");
                String title = fields.get("title");
                String catIdStr = fields.get("categoryId");
                String authIdStr = fields.get("authorId");
                String copiesStr = fields.get("totalCopies");

                if (isbn == null || title == null || catIdStr == null || authIdStr == null || copiesStr == null) {
                    sendError(exchange, 400, "Missing required fields: isbn, title, categoryId, authorId, totalCopies");
                    return;
                }

                try {
                    int catId = Integer.parseInt(catIdStr.trim());
                    int authId = Integer.parseInt(authIdStr.trim());
                    int totalCopies = Integer.parseInt(copiesStr.trim());
                    if (totalCopies <= 0) {
                        sendError(exchange, 400, "Total copies must be greater than zero.");
                        return;
                    }

                    Book book = new Book(0, isbn.trim(), title.trim(), catId, authId, totalCopies, totalCopies);
                    boolean ok = bookDAO.addBook(book);
                    if (ok) {
                        sendJsonResponse(exchange, 201, String.format(Locale.US,
                                "{\"success\":true,\"bookId\":%d,\"isbn\":\"%s\",\"title\":\"%s\",\"totalCopies\":%d}",
                                book.getBookId(), escapeJson(book.getIsbn()), escapeJson(book.getTitle()), totalCopies));
                    } else {
                        sendError(exchange, 500, "Failed to persist book to database.");
                    }
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "Numeric formatting error in categoryId, authorId, or totalCopies.");
                }
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Student> list = studentDAO.getAllStudents();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Student s = list.get(i);
                    int active = transactionDAO.getActiveCountForStudent(s.getStudentId());
                    boolean limitReached = active >= LibraryService.MAX_ACTIVE_LOANS_PER_STUDENT;
                    sb.append(String.format(Locale.US,
                            "{\"studentId\":%d,\"registerNo\":\"%s\",\"name\":\"%s\",\"department\":\"%s\"," +
                            "\"year\":%d,\"email\":\"%s\",\"activeLoans\":%d,\"maxLoans\":%d,\"limitReached\":%b}",
                            s.getStudentId(), escapeJson(s.getRegisterNo()), escapeJson(s.getName()),
                            escapeJson(s.getDepartment()), s.getYear(), escapeJson(s.getEmail()),
                            active, LibraryService.MAX_ACTIVE_LOANS_PER_STUDENT, limitReached));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendJsonResponse(exchange, 200, sb.toString());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> fields = parseJsonFields(body);
                String regNo = fields.get("registerNo");
                String name = fields.get("name");
                String dept = fields.get("department");
                String yrStr = fields.get("year");
                String email = fields.get("email");

                if (regNo == null || name == null || dept == null || yrStr == null || email == null) {
                    sendError(exchange, 400, "Missing required student attributes.");
                    return;
                }

                try {
                    int yr = Integer.parseInt(yrStr.trim());
                    Student s = new Student(0, regNo.trim(), name.trim(), dept.trim(), yr, email.trim());
                    boolean ok = studentDAO.addStudent(s);
                    if (ok) {
                        sendJsonResponse(exchange, 201, String.format(Locale.US,
                                "{\"success\":true,\"studentId\":%d,\"registerNo\":\"%s\",\"name\":\"%s\"}",
                                s.getStudentId(), escapeJson(s.getRegisterNo()), escapeJson(s.getName())));
                    } else {
                        sendError(exchange, 500, "Failed to register student.");
                    }
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "Invalid academic year.");
                }
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<BorrowTransaction> list = transactionDAO.getAllTransactions();
                sendJsonResponse(exchange, 200, formatTransactionsJson(list));
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private class IssueHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> fields = parseJsonFields(body);
                String sIdStr = fields.get("studentId");
                String bIdStr = fields.get("bookId");
                String issueDateStr = fields.get("issueDate");

                if (sIdStr == null || bIdStr == null) {
                    sendError(exchange, 400, "studentId and bookId are required.");
                    return;
                }

                try {
                    int studentId = Integer.parseInt(sIdStr.trim());
                    int bookId = Integer.parseInt(bIdStr.trim());
                    LocalDate issueDate = (issueDateStr != null && !issueDateStr.trim().isEmpty())
                            ? DateUtil.parse(issueDateStr.trim()) : LocalDate.now();

                    BorrowTransaction tx = libraryService.issueBook(studentId, bookId, issueDate);
                    String resp = String.format(Locale.US,
                            "{\"success\":true,\"message\":\"Book issued successfully.\"," +
                            "\"transaction\":{\"transactionId\":%d,\"studentId\":%d,\"registerNo\":\"%s\",\"studentName\":\"%s\"," +
                            "\"bookId\":%d,\"bookTitle\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\",\"status\":\"%s\"}}",
                            tx.getTransactionId(), tx.getStudentId(), escapeJson(tx.getRegisterNo()), escapeJson(tx.getStudentName()),
                            tx.getBookId(), escapeJson(tx.getBookTitle()), DateUtil.format(tx.getIssueDate()),
                            DateUtil.format(tx.getDueDate()), tx.getStatus());
                    sendJsonResponse(exchange, 200, resp);
                } catch (StudentLimitExceededException e) {
                    sendError(exchange, 400, e.getMessage());
                } catch (BookUnavailableException e) {
                    sendError(exchange, 400, e.getMessage());
                } catch (LibraryException e) {
                    sendError(exchange, 400, e.getMessage());
                } catch (Exception e) {
                    sendError(exchange, 500, "Internal server error: " + e.getMessage());
                }
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private class ReturnHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> fields = parseJsonFields(body);
                String txIdStr = fields.get("transactionId");
                String sIdStr = fields.get("studentId");
                String bIdStr = fields.get("bookId");
                String returnDateStr = fields.get("returnDate");

                LocalDate returnDate = (returnDateStr != null && !returnDateStr.trim().isEmpty())
                        ? DateUtil.parse(returnDateStr.trim()) : LocalDate.now();

                try {
                    double fine;
                    int txId = 0;
                    if (txIdStr != null && !txIdStr.trim().isEmpty()) {
                        txId = Integer.parseInt(txIdStr.trim());
                        fine = libraryService.returnBook(txId, returnDate);
                    } else if (sIdStr != null && bIdStr != null) {
                        int sId = Integer.parseInt(sIdStr.trim());
                        int bId = Integer.parseInt(bIdStr.trim());
                        BorrowTransaction active = transactionDAO.getActiveTransaction(sId, bId);
                        if (active == null) {
                            sendError(exchange, 404, "No active borrowing found for Student " + sId + " and Book " + bId);
                            return;
                        }
                        txId = active.getTransactionId();
                        fine = libraryService.returnBook(txId, returnDate);
                    } else {
                        sendError(exchange, 400, "Provide either transactionId OR (studentId and bookId).");
                        return;
                    }

                    BorrowTransaction updated = transactionDAO.getTransactionById(txId);
                    long overdueDays = 0;
                    if (updated != null && returnDate.isAfter(updated.getDueDate())) {
                        overdueDays = ChronoUnit.DAYS.between(updated.getDueDate(), returnDate);
                    }

                    String resp = String.format(Locale.US,
                            "{\"success\":true,\"message\":\"Book returned successfully.\"," +
                            "\"transactionId\":%d,\"returnDate\":\"%s\",\"overdueDays\":%d,\"fineAmount\":%.2f}",
                            txId, DateUtil.format(returnDate), overdueDays, fine);
                    sendJsonResponse(exchange, 200, resp);
                } catch (LibraryException e) {
                    sendError(exchange, 400, e.getMessage());
                } catch (Exception e) {
                    sendError(exchange, 500, "Internal error during book return: " + e.getMessage());
                }
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    private class AvailableReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            List<Book> list = bookDAO.getAvailableBooks();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Book b = list.get(i);
                sb.append(String.format(Locale.US,
                        "{\"bookId\":%d,\"isbn\":\"%s\",\"title\":\"%s\",\"categoryName\":\"%s\",\"authorName\":\"%s\"," +
                        "\"availableCopies\":%d,\"totalCopies\":%d}",
                        b.getBookId(), escapeJson(b.getIsbn()), escapeJson(b.getTitle()),
                        escapeJson(b.getCategoryName()), escapeJson(b.getAuthorName()),
                        b.getAvailableCopies(), b.getTotalCopies()));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class OverdueReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String refStr = params.get("date");
            LocalDate refDate = (refStr != null) ? DateUtil.parse(refStr) : LocalDate.now();

            List<BorrowTransaction> overdue = transactionDAO.getOverdueTransactions(refDate);
            StringBuilder sb = new StringBuilder("[");
            double totalFine = 0.0;
            for (int i = 0; i < overdue.size(); i++) {
                BorrowTransaction t = overdue.get(i);
                long days = DateUtil.calculateOverdueDays(t.getDueDate(), refDate);
                double fine = days * DateUtil.FINE_RATE_PER_DAY;
                totalFine += fine;
                sb.append(String.format(Locale.US,
                        "{\"transactionId\":%d,\"studentId\":%d,\"registerNo\":\"%s\",\"studentName\":\"%s\"," +
                        "\"bookId\":%d,\"bookTitle\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\"," +
                        "\"overdueDays\":%d,\"fineRate\":%.2f,\"fineAmount\":%.2f}",
                        t.getTransactionId(), t.getStudentId(), escapeJson(t.getRegisterNo()), escapeJson(t.getStudentName()),
                        t.getBookId(), escapeJson(t.getBookTitle()), DateUtil.format(t.getIssueDate()),
                        DateUtil.format(t.getDueDate()), days, DateUtil.FINE_RATE_PER_DAY, fine));
                if (i < overdue.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    private class StudentHistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String sIdStr = params.get("studentId");
            if (sIdStr == null) {
                String[] pathParts = exchange.getRequestURI().getPath().split("/");
                if (pathParts.length == 5 && "history".equals(pathParts[4])) {
                    sIdStr = pathParts[3];
                }
            }
            if (sIdStr == null) {
                sendError(exchange, 400, "Query parameter studentId is required.");
                return;
            }
            try {
                int sId = Integer.parseInt(sIdStr.trim());
                List<BorrowTransaction> history = transactionDAO.getStudentHistory(sId);
                sendJsonResponse(exchange, 200, formatTransactionsJson(history));
            } catch (NumberFormatException e) {
                sendError(exchange, 400, "Invalid studentId.");
            }
        }
    }

    private class PythonAnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
            String script = new File("python_analytics", "analysis.py").getPath();
            List<String[]> runners = new ArrayList<>();
            if (isWindows) {
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null) {
                    File installRoot = new File(localAppData, "Programs/Python");
                    File[] installations = installRoot.listFiles(file -> file.isDirectory()
                            && file.getName().matches("Python\\d+"));
                    if (installations != null) {
                        Arrays.sort(installations, (a, b) -> b.getName().compareToIgnoreCase(a.getName()));
                        for (File installation : installations) {
                            runners.add(new String[]{new File(installation, "python.exe").getAbsolutePath()});
                        }
                    }
                }
                runners.add(new String[]{"py", "-3"});
                runners.add(new String[]{"python"});
            } else {
                runners.add(new String[]{"python3"});
                runners.add(new String[]{"python"});
            }
            IOException launchError = null;

            for (String[] runner : runners) {
                List<String> command = new ArrayList<>(Arrays.asList(runner));
                command.add(script);
                try {
                    Process process = new ProcessBuilder(command)
                            .directory(new File("."))
                            .redirectErrorStream(true)
                            .start();
                    process.getOutputStream().close();
                    String output;
                    try (InputStream input = process.getInputStream()) {
                        output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        sendError(exchange, 500, output.trim().isEmpty()
                                ? "Python analytics exited with code " + exitCode + "."
                                : output.trim());
                        return;
                    }

                    String response = "{\"success\":true,\"message\":\"Python analytics completed. The charts have been refreshed.\",\"output\":\""
                            + escapeJson(output.trim()) + "\"}";
                    sendJsonResponse(exchange, 200, response);
                    return;
                } catch (IOException e) {
                    launchError = e;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sendError(exchange, 500, "Python analytics was interrupted.");
                    return;
                }
            }

            sendError(exchange, 500, "Python was not found. Install Python and python_analytics/requirements.txt."
                    + (launchError == null ? "" : " " + launchError.getMessage()));
        }
    }

    private class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;

            List<Book> books = bookDAO.getAllBooks();
            List<Student> students = studentDAO.getAllStudents();
            List<BorrowTransaction> transactions = transactionDAO.getAllTransactions();

            int totalTitles = books.size();
            int totalCopies = books.stream().mapToInt(Book::getTotalCopies).sum();
            int availableCopies = books.stream().mapToInt(Book::getAvailableCopies).sum();
            int issuedCopies = totalCopies - availableCopies;
            int totalStudents = students.size();

            long activeLoans = transactions.stream().filter(t -> "ISSUED".equalsIgnoreCase(t.getStatus())).count();
            LocalDate today = LocalDate.now();
            long overdueLoans = transactions.stream()
                    .filter(t -> "ISSUED".equalsIgnoreCase(t.getStatus()) && t.getDueDate().isBefore(today))
                    .count();

            double totalFineCollected = transactions.stream()
                    .filter(t -> "RETURNED".equalsIgnoreCase(t.getStatus()))
                    .mapToDouble(BorrowTransaction::getFineAmount)
                    .sum();

            double pendingFine = transactions.stream()
                    .filter(t -> "ISSUED".equalsIgnoreCase(t.getStatus()) && t.getDueDate().isBefore(today))
                    .mapToDouble(t -> DateUtil.calculateOverdueDays(t.getDueDate(), today) * DateUtil.FINE_RATE_PER_DAY)
                    .sum();

            // Category breakdown of all borrowings
            Map<String, Integer> catCounts = new HashMap<>();
            Map<Integer, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getBookId, b -> b, (a, b) -> a));
            for (BorrowTransaction t : transactions) {
                Book b = bookMap.get(t.getBookId());
                String catName = (b != null && b.getCategoryName() != null) ? b.getCategoryName() : "General";
                catCounts.put(catName, catCounts.getOrDefault(catName, 0) + 1);
            }

            StringBuilder catSb = new StringBuilder("[");
            int totalTrans = transactions.isEmpty() ? 1 : transactions.size();
            List<Map.Entry<String, Integer>> catList = new ArrayList<>(catCounts.entrySet());
            catList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            for (int i = 0; i < catList.size(); i++) {
                Map.Entry<String, Integer> entry = catList.get(i);
                double pct = (entry.getValue() * 100.0) / totalTrans;
                catSb.append(String.format(Locale.US, "{\"category\":\"%s\",\"count\":%d,\"percentage\":%.1f}",
                        escapeJson(entry.getKey()), entry.getValue(), pct));
                if (i < catList.size() - 1) catSb.append(",");
            }
            catSb.append("]");

            // Monthly Trend (Jan - Nov 2026)
            Map<String, int[]> monthlyMap = new TreeMap<>();
            for (BorrowTransaction t : transactions) {
                String m = t.getIssueDate().toString().substring(0, 7); // YYYY-MM
                monthlyMap.putIfAbsent(m, new int[]{0, 0});
                monthlyMap.get(m)[0]++; // issues
                if ("RETURNED".equalsIgnoreCase(t.getStatus()) && t.getReturnDate() != null) {
                    String rm = t.getReturnDate().toString().substring(0, 7);
                    monthlyMap.putIfAbsent(rm, new int[]{0, 0});
                    monthlyMap.get(rm)[1]++; // returns
                }
            }

            StringBuilder monthSb = new StringBuilder("[");
            List<String> months = new ArrayList<>(monthlyMap.keySet());
            Collections.sort(months);
            for (int i = 0; i < months.size(); i++) {
                String m = months.get(i);
                int[] counts = monthlyMap.get(m);
                monthSb.append(String.format(Locale.US, "{\"month\":\"%s\",\"issues\":%d,\"returns\":%d}",
                        m, counts[0], counts[1]));
                if (i < months.size() - 1) monthSb.append(",");
            }
            monthSb.append("]");

            // Top Borrowed Books
            Map<Integer, Integer> bookBorrowCounts = new HashMap<>();
            for (BorrowTransaction t : transactions) {
                bookBorrowCounts.put(t.getBookId(), bookBorrowCounts.getOrDefault(t.getBookId(), 0) + 1);
            }
            List<Map.Entry<Integer, Integer>> topList = new ArrayList<>(bookBorrowCounts.entrySet());
            topList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            StringBuilder topSb = new StringBuilder("[");
            int limit = Math.min(topList.size(), 8);
            for (int i = 0; i < limit; i++) {
                Map.Entry<Integer, Integer> entry = topList.get(i);
                Book b = bookMap.get(entry.getKey());
                String title = (b != null) ? b.getTitle() : "Book #" + entry.getKey();
                String cat = (b != null && b.getCategoryName() != null) ? b.getCategoryName() : "General";
                topSb.append(String.format(Locale.US,
                        "{\"bookId\":%d,\"title\":\"%s\",\"category\":\"%s\",\"borrowCount\":%d}",
                        entry.getKey(), escapeJson(title), escapeJson(cat), entry.getValue()));
                if (i < limit - 1) topSb.append(",");
            }
            topSb.append("]");

            Map<String, Double> fineByCategory = new HashMap<>();
            for (BorrowTransaction t : transactions) {
                if (t.getFineAmount() > 0) {
                    Book b = bookMap.get(t.getBookId());
                    String category = (b != null && b.getCategoryName() != null)
                            ? b.getCategoryName() : "General";
                    fineByCategory.put(category, fineByCategory.getOrDefault(category, 0.0) + t.getFineAmount());
                }
            }
            StringBuilder fineSb = new StringBuilder("[");
            List<Map.Entry<String, Double>> fineList = new ArrayList<>(fineByCategory.entrySet());
            fineList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            for (int i = 0; i < fineList.size(); i++) {
                Map.Entry<String, Double> entry = fineList.get(i);
                fineSb.append(String.format(Locale.US, "{\"category\":\"%s\",\"totalFine\":%.2f}",
                        escapeJson(entry.getKey()), entry.getValue()));
                if (i < fineList.size() - 1) fineSb.append(",");
            }
            fineSb.append("]");

            String json = String.format(Locale.US,
                    "{" +
                    "\"summary\":{" +
                    "\"totalTitles\":%d,\"totalCopies\":%d,\"availableCopies\":%d,\"issuedCopies\":%d," +
                    "\"totalStudents\":%d,\"activeLoans\":%d,\"overdueLoans\":%d," +
                    "\"fineCollected\":%.2f,\"pendingFine\":%.2f" +
                    "}," +
                    "\"byCategory\":%s," +
                    "\"monthlyTrend\":%s," +
                    "\"topBooks\":%s," +
                    "\"fineByCategory\":%s" +
                    "}",
                    totalTitles, totalCopies, availableCopies, issuedCopies,
                    totalStudents, activeLoans, overdueLoans,
                    totalFineCollected, pendingFine,
                    catSb.toString(), monthSb.toString(), topSb.toString(), fineSb.toString());

            sendJsonResponse(exchange, 200, json);
        }
    }

    private class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            List<BorrowTransaction> transactions = transactionDAO.getAllTransactions();
            String path = exchange.getRequestURI().getPath();

            if (path.endsWith(".csv")) {
                setCorsHeaders(exchange);
                exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"library_circulation_transactions.csv\"");
                StringBuilder csv = new StringBuilder();
                csv.append("transaction_id,student_id,register_no,student_name,book_id,book_title,issue_date,due_date,return_date,status,fine_amount\n");
                for (BorrowTransaction t : transactions) {
                    csv.append(String.format(Locale.US, "%d,%d,\"%s\",\"%s\",%d,\"%s\",%s,%s,%s,%s,%.2f\n",
                            t.getTransactionId(), t.getStudentId(),
                            t.getRegisterNo() != null ? t.getRegisterNo() : "",
                            t.getStudentName() != null ? t.getStudentName().replace("\"", "\"\"") : "",
                            t.getBookId(),
                            t.getBookTitle() != null ? t.getBookTitle().replace("\"", "\"\"") : "",
                            DateUtil.format(t.getIssueDate()),
                            DateUtil.format(t.getDueDate()),
                            t.getReturnDate() != null ? DateUtil.format(t.getReturnDate()) : "",
                            t.getStatus(),
                            t.getFineAmount()));
                }
                byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                sendJsonResponse(exchange, 200, formatTransactionsJson(transactions));
            }
        }
    }

    private class VisualizationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String path = exchange.getRequestURI().getPath(); // e.g. /visualizations/category_distribution.png
            String filename = path.replace("/visualizations/", "").replace("/visualizations", "");
            File file = new File("python_analytics/visualizations", filename);
            if (!file.exists() || file.isDirectory()) {
                sendError(exchange, 404, "Chart image not found: " + filename);
                return;
            }

            setCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Map to frontend folder
            File file = new File("frontend", path.substring(1));
            if (!file.exists() || file.isDirectory()) {
                // SPA fallback to index.html
                file = new File("frontend", "index.html");
            }

            if (!file.exists()) {
                sendError(exchange, 404, "File not found: " + path);
                return;
            }

            String mime = getMimeType(file.getName());
            setCorsHeaders(exchange);
            exchange.getResponseHeaders().set("Content-Type", mime);
            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String getMimeType(String name) {
            if (name.endsWith(".html")) return "text/html; charset=UTF-8";
            if (name.endsWith(".css")) return "text/css; charset=UTF-8";
            if (name.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (name.endsWith(".json")) return "application/json; charset=UTF-8";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            if (name.endsWith(".svg")) return "image/svg+xml";
            if (name.endsWith(".ico")) return "image/x-icon";
            return "text/plain; charset=UTF-8";
        }
    }

    private String formatTransactionsJson(List<BorrowTransaction> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            BorrowTransaction t = list.get(i);
            String retDate = t.getReturnDate() != null ? "\"" + DateUtil.format(t.getReturnDate()) + "\"" : "null";
            sb.append(String.format(Locale.US,
                    "{\"transactionId\":%d,\"studentId\":%d,\"registerNo\":\"%s\",\"studentName\":\"%s\"," +
                    "\"bookId\":%d,\"bookTitle\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\"," +
                    "\"returnDate\":%s,\"fineAmount\":%.2f,\"status\":\"%s\"}",
                    t.getTransactionId(), t.getStudentId(), escapeJson(t.getRegisterNo()), escapeJson(t.getStudentName()),
                    t.getBookId(), escapeJson(t.getBookTitle()), DateUtil.format(t.getIssueDate()),
                    DateUtil.format(t.getDueDate()), retDate, t.getFineAmount(), t.getStatus()));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (Exception ignored) {}
        }

        BookDAO bookDAO = new BookDAO();
        StudentDAO studentDAO = new StudentDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        CategoryDAO categoryDAO = new CategoryDAO();
        AuthorDAO authorDAO = new AuthorDAO();

        DatabaseInitializer.initialize(bookDAO, studentDAO, transactionDAO, categoryDAO, authorDAO);

        LibraryService libraryService = new LibraryService(bookDAO, studentDAO, transactionDAO);
        ReportService reportService = new ReportService(bookDAO, studentDAO, transactionDAO);

        LibraryServer server = new LibraryServer(port, bookDAO, studentDAO, transactionDAO, categoryDAO, authorDAO, libraryService, reportService);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
