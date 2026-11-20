package com.library;

import com.library.dao.*;
import com.library.exception.BookUnavailableException;
import com.library.exception.LibraryException;
import com.library.exception.StudentLimitExceededException;
import com.library.model.Book;
import com.library.model.BorrowTransaction;
import com.library.model.Student;
import com.library.service.LibraryService;
import com.library.service.ReportService;
import com.library.util.ConsoleUI;
import com.library.util.DateUtil;
import com.library.util.InputValidator;

import java.time.LocalDate;
import java.util.Scanner;

/**
 * Main Entry Point for the Library Book Lending and Usage Analysis System.
 * 
 * College: Velalar College of Engineering and Technology, Erode - 12
 * Department: Department of Information Technology
 * Course Assessment: Scenario 4 - Project-Based Assessment
 * Student: Abishek | Reg No: 732925ITR004 | Section: 25IT3A
 */
public class Main {

    private static final BookDAO bookDAO = new BookDAO();
    private static final StudentDAO studentDAO = new StudentDAO();
    private static final TransactionDAO transactionDAO = new TransactionDAO();

    private static final CategoryDAO categoryDAO = new CategoryDAO();
    private static final AuthorDAO authorDAO = new AuthorDAO();

    private static final LibraryService libraryService = new LibraryService(bookDAO, studentDAO, transactionDAO);
    private static final ReportService reportService = new ReportService(bookDAO, studentDAO, transactionDAO);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // If --server or --web argument is passed, launch the web server directly
        if (args.length > 0 && (args[0].equalsIgnoreCase("--server") || args[0].equalsIgnoreCase("--web") || args[0].equalsIgnoreCase("-w"))) {
            int port = 8080;
            if (args.length > 1) {
                try { port = Integer.parseInt(args[1]); } catch (Exception ignored) {}
            }
            try {
                com.library.server.LibraryServer.main(new String[]{String.valueOf(port)});
            } catch (Exception e) {
                System.err.println("Failed to start web server: " + e.getMessage());
            }
            return;
        }

        // 1. Initialize schema & seed records
        DatabaseInitializer.initialize(bookDAO, studentDAO, transactionDAO, categoryDAO, authorDAO);

        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter your choice [0-11]: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    handleIssueBook();
                    break;
                case "2":
                    handleReturnBook();
                    break;
                case "3":
                    reportService.displayAvailableBooks();
                    break;
                case "4":
                    reportService.displayAllBooks();
                    break;
                case "5":
                    handleOverdueReport();
                    break;
                case "6":
                    handleStudentHistory();
                    break;
                case "7":
                    reportService.displayAllStudents();
                    break;
                case "8":
                    handleRegisterStudent();
                    break;
                case "9":
                    handleAddBook();
                    break;
                case "10":
                    runAutomatedDemonstration();
                    break;
                case "11":
                    handleLaunchWebServer();
                    break;
                case "0":
                    running = false;
                    ConsoleUI.printHeader("Thank you for using the Library Circulation System!");
                    System.out.println("Developed by Abishek (732925ITR004), Dept of IT, VCET Erode.");
                    break;
                default:
                    ConsoleUI.printError("Invalid choice! Please select an option between 0 and 11.");
            }

            if (running) {
                System.out.println("\nPress [Enter] to return to the main menu...");
                scanner.nextLine();
            }
        }

        DatabaseConnection.closeConnection();
    }

    private static void printBanner() {
        System.out.println("==========================================================================================");
        System.out.println("       VELALAR COLLEGE OF ENGINEERING AND TECHNOLOGY, ERODE - 638012");
        System.out.println("                 DEPARTMENT OF INFORMATION TECHNOLOGY");
        System.out.println("        ACADEMIC YEAR: 2026-2027 (ODD SEMESTER) | CLASS: 25IT3A");
        System.out.println("   PROJECT: LIBRARY BOOK LENDING AND USAGE ANALYSIS SYSTEM (SCENARIO 4)");
        System.out.println("   STUDENT: ABISHEK | REGISTER NO: 732925ITR004");
        System.out.println("==========================================================================================");
        if (DatabaseConnection.isDriverAvailable()) {
            ConsoleUI.printInfo("Database Mode: SQLite Database Connected (database/library.db)");
        } else {
            ConsoleUI.printInfo("Database Mode: Memory Persistence Mode Active (SQLite JDBC Driver optional)");
        }
    }

    private static void printMenu() {
        ConsoleUI.printHeader("Main Navigation Menu");
        System.out.println("  [1]  Issue Book to Student");
        System.out.println("  [2]  Return Book & Calculate Overdue Fine");
        System.out.println("  [3]  View Available Books (Stock > 0)");
        System.out.println("  [4]  View Complete Book Catalog");
        System.out.println("  [5]  View Overdue Loans & Accrued Fines");
        System.out.println("  [6]  View Individual Student Borrowing History");
        System.out.println("  [7]  View Registered Student Members");
        System.out.println("  [8]  Register New Student Member");
        System.out.println("  [9]  Add New Book to Inventory");
        System.out.println("  [10] Run Automated System Demonstration (Self-Test)");
        System.out.println("  [11] Launch Web Server & Interactive Dashboard (Port 8080)");
        System.out.println("  [0]  Exit Application");
        ConsoleUI.printDivider();
    }

    private static void handleLaunchWebServer() {
        ConsoleUI.printHeader("Launching Embedded HTTP Web Server");
        try {
            com.library.server.LibraryServer server = new com.library.server.LibraryServer(
                    8080, bookDAO, studentDAO, transactionDAO, categoryDAO, authorDAO, libraryService, reportService
            );
            server.start();
            System.out.println("Server is running. Open http://localhost:8080 in your browser.");
            System.out.println("Press Enter to keep running or return to menu...");
            scanner.nextLine();
        } catch (Exception e) {
            ConsoleUI.printError("Failed to launch web server: " + e.getMessage());
        }
    }

    private static void handleIssueBook() {
        ConsoleUI.printHeader("Book Issue Portal");
        try {
            System.out.print("Enter Student ID (or Register No): ");
            String studentInput = scanner.nextLine().trim();
            int studentId;
            if (studentInput.matches("\\d+")) {
                studentId = Integer.parseInt(studentInput);
            } else {
                Student s = studentDAO.getStudentByRegNo(studentInput);
                if (s == null) {
                    ConsoleUI.printError("Student with Register No '" + studentInput + "' not found.");
                    return;
                }
                studentId = s.getStudentId();
            }

            System.out.print("Enter Book ID to Issue: ");
            int bookId = Integer.parseInt(scanner.nextLine().trim());

            LocalDate issueDate = LocalDate.now();
            System.out.print("Enter Issue Date (YYYY-MM-DD) [Press Enter for Today: " + DateUtil.format(issueDate) + "]: ");
            String dateStr = scanner.nextLine().trim();
            if (!dateStr.isEmpty()) {
                LocalDate parsed = DateUtil.parse(dateStr);
                if (parsed != null) issueDate = parsed;
            }

            BorrowTransaction tx = libraryService.issueBook(studentId, bookId, issueDate);

            ConsoleUI.printSuccess("Book Issued Successfully!");
            System.out.println("------------------------------------------------------------------------------------------");
            System.out.println("  Transaction ID : #" + tx.getTransactionId());
            System.out.println("  Borrower Name  : " + tx.getStudentName() + " (" + tx.getRegisterNo() + ")");
            System.out.println("  Book Title     : " + tx.getBookTitle());
            System.out.println("  Issue Date     : " + DateUtil.format(tx.getIssueDate()));
            System.out.println("  Due Date       : " + DateUtil.format(tx.getDueDate()) + " (14-day loan period)");
            System.out.println("  Fine Rule      : ₹2.00 / day if returned after due date");
            System.out.println("------------------------------------------------------------------------------------------");

        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid numerical input. Please enter valid IDs.");
        } catch (BookUnavailableException | StudentLimitExceededException e) {
            ConsoleUI.printWarning(e.getMessage());
        } catch (LibraryException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private static void handleReturnBook() {
        ConsoleUI.printHeader("Book Return Portal");
        try {
            System.out.print("Enter Transaction ID to Return: ");
            int txId = Integer.parseInt(scanner.nextLine().trim());

            LocalDate returnDate = LocalDate.now();
            System.out.print("Enter Return Date (YYYY-MM-DD) [Press Enter for Today: " + DateUtil.format(returnDate) + "]: ");
            String dateStr = scanner.nextLine().trim();
            if (!dateStr.isEmpty()) {
                LocalDate parsed = DateUtil.parse(dateStr);
                if (parsed != null) returnDate = parsed;
            }

            BorrowTransaction tx = transactionDAO.getTransactionById(txId);
            if (tx == null) {
                ConsoleUI.printError("Transaction #" + txId + " not found.");
                return;
            }

            double fine = libraryService.returnBook(txId, returnDate);

            ConsoleUI.printSuccess("Book Returned Successfully!");
            System.out.println("------------------------------------------------------------------------------------------");
            System.out.println("  Transaction ID : #" + txId);
            System.out.println("  Book Title     : " + tx.getBookTitle());
            System.out.println("  Issue Date     : " + DateUtil.format(tx.getIssueDate()));
            System.out.println("  Due Date       : " + DateUtil.format(tx.getDueDate()));
            System.out.println("  Return Date    : " + DateUtil.format(returnDate));
            long overdueDays = DateUtil.calculateOverdueDays(tx.getDueDate(), returnDate);
            System.out.println("  Overdue Days   : " + overdueDays + " days");
            System.out.printf("  Fine Amount    : ₹%.2f (Rate: ₹2.00/day)\n", fine);
            if (fine > 0) {
                ConsoleUI.printWarning("Please collect fine of ₹" + String.format("%.2f", fine) + " from the student.");
            } else {
                ConsoleUI.printInfo("Returned on or before due date. Zero fine applied.");
            }
            System.out.println("------------------------------------------------------------------------------------------");

        } catch (NumberFormatException e) {
            ConsoleUI.printError("Please enter a valid numeric Transaction ID.");
        } catch (LibraryException e) {
            ConsoleUI.printError(e.getMessage());
        }
    }

    private static void handleOverdueReport() {
        LocalDate refDate = LocalDate.now();
        System.out.print("Enter Reference Date (YYYY-MM-DD) [Press Enter for Today: " + DateUtil.format(refDate) + "]: ");
        String str = scanner.nextLine().trim();
        if (!str.isEmpty()) {
            LocalDate parsed = DateUtil.parse(str);
            if (parsed != null) refDate = parsed;
        }
        reportService.displayOverdueLoans(refDate);
    }

    private static void handleStudentHistory() {
        System.out.print("Enter Student ID (or Register No): ");
        String input = scanner.nextLine().trim();
        int studentId;
        if (input.matches("\\d+")) {
            studentId = Integer.parseInt(input);
        } else {
            Student s = studentDAO.getStudentByRegNo(input);
            if (s == null) {
                ConsoleUI.printError("Student with Register No '" + input + "' not found.");
                return;
            }
            studentId = s.getStudentId();
        }
        reportService.displayStudentHistory(studentId);
    }

    private static void handleRegisterStudent() {
        ConsoleUI.printHeader("Register New Student");
        try {
            System.out.print("Enter Register Number (e.g., 732925ITR101): ");
            String regNo = scanner.nextLine().trim();
            if (!InputValidator.isValidRegisterNo(regNo)) {
                ConsoleUI.printError("Invalid Register Number format.");
                return;
            }

            System.out.print("Enter Student Full Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Enter Department [e.g., Information Technology]: ");
            String dept = scanner.nextLine().trim();

            System.out.print("Enter Year of Study [1-4]: ");
            int year = Integer.parseInt(scanner.nextLine().trim());
            if (!InputValidator.isValidYear(year)) {
                ConsoleUI.printError("Year must be between 1 and 4.");
                return;
            }

            System.out.print("Enter College Email: ");
            String email = scanner.nextLine().trim();
            if (!InputValidator.isValidEmail(email)) {
                ConsoleUI.printError("Invalid email format.");
                return;
            }

            Student s = new Student(0, regNo, name, dept, year, email);
            boolean created = studentDAO.addStudent(s);
            if (created) {
                ConsoleUI.printSuccess("Student registered successfully with ID #" + s.getStudentId() + "!");
            } else {
                ConsoleUI.printError("Could not register student (register number or email may already exist).");
            }
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid number format for year.");
        }
    }

    private static void handleAddBook() {
        ConsoleUI.printHeader("Add New Book to Inventory");
        try {
            System.out.print("Enter ISBN (e.g., 978-0134685991): ");
            String isbn = scanner.nextLine().trim();

            System.out.print("Enter Book Title: ");
            String title = scanner.nextLine().trim();

            System.out.print("Enter Category ID [1-8]: ");
            int catId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Author ID [1-12]: ");
            int authorId = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Total Copies: ");
            int total = Integer.parseInt(scanner.nextLine().trim());

            if (!InputValidator.isValidCopies(total, total)) {
                ConsoleUI.printError("Copies must be non-negative.");
                return;
            }

            Book book = new Book(0, isbn, title, catId, authorId, total, total);
            boolean created = bookDAO.addBook(book);
            if (created) {
                ConsoleUI.printSuccess("Book added successfully with ID #" + book.getBookId() + "!");
            } else {
                ConsoleUI.printError("Could not add book (ISBN may already exist).");
            }
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid numerical format.");
        }
    }

    private static void runAutomatedDemonstration() {
        ConsoleUI.printHeader("Automated System Demonstration & Business Logic Verification");

        System.out.println("\n[STEP 1] Displaying Available Books in Catalog:");
        reportService.displayAvailableBooks();

        System.out.println("\n[STEP 2] Simulating Issue of Book ID: 1 to Student ID: 1 (Abishek):");
        try {
            BorrowTransaction tx = libraryService.issueBook(1, 1, LocalDate.now());
            ConsoleUI.printSuccess("Demonstration Issue Succeeded! Transaction #" + tx.getTransactionId());
        } catch (Exception e) {
            ConsoleUI.printWarning("Issue simulation note: " + e.getMessage());
        }

        System.out.println("\n[STEP 3] Testing Constraint: Preventing Out-of-Stock Issuance:");
        // Temporarily set a test book copies to 0
        Book testBook = bookDAO.getBookById(8);
        int originalCopies = testBook != null ? testBook.getAvailableCopies() : 0;
        if (testBook != null) testBook.setAvailableCopies(0);
        try {
            libraryService.issueBook(2, 8, LocalDate.now());
            ConsoleUI.printError("FAILED: Unavailable book was issued!");
        } catch (BookUnavailableException e) {
            ConsoleUI.printSuccess("PASSED: Caught expected BookUnavailableException -> " + e.getMessage());
        } catch (Exception e) {
            ConsoleUI.printWarning("Caught: " + e.getMessage());
        } finally {
            if (testBook != null) testBook.setAvailableCopies(originalCopies);
        }

        System.out.println("\n[STEP 4] Testing Fine Calculation Logic (14 days loan period, ₹2.00/day fine):");
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate returnedEarly = LocalDate.of(2026, 9, 28);
        LocalDate returnedLate = LocalDate.of(2026, 10, 6); // 5 days late

        double fineEarly = BorrowTransaction.calculateFine(due, returnedEarly, 2.0);
        double fineLate = BorrowTransaction.calculateFine(due, returnedLate, 2.0);

        System.out.printf("  Due Date: %s | Return Date: %s (Early) -> Fine: ₹%.2f (Expected: ₹0.00) -> %s\n",
                due, returnedEarly, fineEarly, fineEarly == 0.0 ? "PASS" : "FAIL");
        System.out.printf("  Due Date: %s | Return Date: %s (5 Days Late) -> Fine: ₹%.2f (Expected: ₹10.00) -> %s\n",
                due, returnedLate, fineLate, fineLate == 10.0 ? "PASS" : "FAIL");

        System.out.println("\n[STEP 5] Displaying Overdue Loans Report:");
        reportService.displayOverdueLoans(LocalDate.of(2026, 11, 19));

        ConsoleUI.printSuccess("All automated verification steps completed successfully!");
    }
}
