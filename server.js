/**
 * ============================================================================
 * Backend Server: Library Book Lending and Usage Analysis System
 * Academic Year: 2026-2027 (Odd Semester) | Class: 25IT3A
 * Institution: Velalar College of Engineering and Technology, Erode - 12
 * Department: Department of Information Technology
 * Student: Abishek (Register No: 732925ITR004)
 * Scenario 4: Library Book Lending and Usage Analysis System
 * ============================================================================
 * 
 * Features:
 * - Pure Node.js (Zero external npm dependencies)
 * - Uses native node:sqlite (DatabaseSync) connected to database/library.db
 * - Full REST API for Circulation (Issue, Return, Fines, Catalog, History)
 * - Real-time business rule validation (Max 3 books, Stock checks, ₹2/day fine)
 * - Serves responsive Web Frontend dashboard
 */

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');
const { DatabaseSync } = require('node:sqlite');

const PORT = process.env.PORT || 8080;
const DB_PATH = path.join(__dirname, 'database', 'library.db');
const FRONTEND_DIR = path.join(__dirname, 'frontend');

// Ensure DB exists
if (!fs.existsSync(DB_PATH)) {
    const schemaPath = path.join(__dirname, 'database', 'schema.sql');
    const seedPath = path.join(__dirname, 'database', 'seed_data.sql');
    fs.mkdirSync(path.dirname(DB_PATH), { recursive: true });
    const tempDb = new DatabaseSync(DB_PATH);
    if (fs.existsSync(schemaPath)) tempDb.exec(fs.readFileSync(schemaPath, 'utf8'));
    if (fs.existsSync(seedPath)) tempDb.exec(fs.readFileSync(seedPath, 'utf8'));
    tempDb.close();
}

const db = new DatabaseSync(DB_PATH);
db.exec("PRAGMA foreign_keys = ON;");

// Utility to parse JSON request bodies
function parseBody(req) {
    return new Promise((resolve, reject) => {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                resolve(body ? JSON.parse(body) : {});
            } catch (err) {
                reject(err);
            }
        });
        req.on('error', reject);
    });
}

// Utility to send JSON response
function sendJson(res, statusCode, data) {
    res.writeHead(statusCode, {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
    });
    res.end(JSON.stringify(data));
}

// Calculate days between two ISO dates (YYYY-MM-DD)
function daysBetween(dateStr1, dateStr2) {
    const d1 = new Date(dateStr1);
    const d2 = new Date(dateStr2);
    const diff = (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24);
    return Math.round(diff);
}

// Add days to ISO date (YYYY-MM-DD)
function addDays(dateStr, days) {
    const d = new Date(dateStr);
    d.setDate(d.getDate() + days);
    return d.toISOString().split('T')[0];
}

function executePython(command, args) {
    return new Promise((resolve, reject) => {
        const child = spawn(command, args, { cwd: __dirname, windowsHide: true });
        let output = '';
        child.stdout.setEncoding('utf8');
        child.stderr.setEncoding('utf8');
        child.stdout.on('data', chunk => output += chunk);
        child.stderr.on('data', chunk => output += chunk);
        child.on('error', reject);
        child.on('close', code => {
            if (code === 0) resolve(output.trim());
            else reject(new Error(output.trim() || `Python analytics exited with code ${code}.`));
        });
    });
}

async function runPythonAnalytics() {
    const script = path.join(__dirname, 'python_analytics', 'analysis.py');
    const commands = [];
    if (process.platform === 'win32' && process.env.LOCALAPPDATA) {
        const installRoot = path.join(process.env.LOCALAPPDATA, 'Programs', 'Python');
        try {
            const installations = fs.readdirSync(installRoot, { withFileTypes: true })
                .filter(entry => entry.isDirectory() && /^Python\d+$/.test(entry.name))
                .sort((a, b) => b.name.localeCompare(a.name, undefined, { numeric: true }));
            for (const installation of installations) {
                commands.push([path.join(installRoot, installation.name, 'python.exe'), [script]]);
            }
        } catch (err) {
            if (err.code !== 'ENOENT') throw err;
        }
    }
    commands.push(...(process.platform === 'win32'
        ? [['py', ['-3', script]], ['python', [script]]]
        : [['python3', [script]], ['python', [script]]]));
    let launchError;

    for (const [command, args] of commands) {
        try {
            return await executePython(command, args);
        } catch (err) {
            if (err.code !== 'ENOENT') throw err;
            launchError = err;
        }
    }

    throw new Error(`Python was not found. Install Python and python_analytics/requirements.txt. ${launchError?.message || ''}`);
}

const server = http.createServer(async (req, res) => {
    // Enable CORS preflight
    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type'
        });
        return res.end();
    }

    const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
    const pathname = parsedUrl.pathname;
    const method = req.method;

    // ------------------------------------------------------------------------
    // API ROUTES
    // ------------------------------------------------------------------------

    // 1. Dashboard Statistics
    if (pathname === '/api/stats' && method === 'GET') {
        try {
            const totalBooks = db.prepare('SELECT count(*) as val FROM books').get().val;
            const availableCopies = db.prepare('SELECT sum(available_copies) as val FROM books').get().val || 0;
            const totalStudents = db.prepare('SELECT count(*) as val FROM students').get().val;
            const activeLoans = db.prepare("SELECT count(*) as val FROM transactions WHERE status='ISSUED'").get().val;
            const overdueLoans = db.prepare("SELECT count(*) as val FROM transactions WHERE status='ISSUED' AND date(due_date) < date('now')").get().val;
            const totalFines = db.prepare("SELECT sum(fine_amount) as val FROM transactions").get().val || 0;

            return sendJson(res, 200, {
                totalBooks,
                availableCopies,
                totalStudents,
                activeLoans,
                overdueLoans,
                totalFines
            });
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 2. Books Catalog
    if (pathname === '/api/books' && method === 'GET') {
        try {
            const query = `
                SELECT b.book_id, b.isbn, b.title, b.category_id, b.author_id,
                       b.total_copies, b.available_copies,
                       c.category_name, a.author_name
                FROM books b
                LEFT JOIN categories c ON b.category_id = c.category_id
                LEFT JOIN authors a ON b.author_id = a.author_id
                ORDER BY b.book_id ASC;
            `;
            const books = db.prepare(query).all();
            return sendJson(res, 200, books);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 3. Categories List
    if (pathname === '/api/categories' && method === 'GET') {
        try {
            const categories = db.prepare('SELECT * FROM categories ORDER BY category_id ASC;').all();
            return sendJson(res, 200, categories);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 4. Students List
    if (pathname === '/api/students' && method === 'GET') {
        try {
            const query = `
                SELECT s.student_id, s.register_no, s.name, s.department, s.year, s.email,
                       (SELECT count(*) FROM transactions t WHERE t.student_id = s.student_id AND t.status = 'ISSUED') as active_loans
                FROM students s
                ORDER BY s.student_id ASC;
            `;
            const students = db.prepare(query).all();
            return sendJson(res, 200, students);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 5. Transactions List
    if (pathname === '/api/transactions' && method === 'GET') {
        try {
            const query = `
                SELECT t.transaction_id, t.issue_date, t.due_date, t.return_date, t.status, t.fine_amount,
                       s.student_id, s.register_no, s.name AS student_name, s.department,
                       b.book_id, b.title AS book_title, c.category_name
                FROM transactions t
                JOIN students s ON t.student_id = s.student_id
                JOIN books b ON t.book_id = b.book_id
                JOIN categories c ON b.category_id = c.category_id
                ORDER BY t.transaction_id DESC;
            `;
            const txs = db.prepare(query).all();
            return sendJson(res, 200, txs);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 6. Overdue Loans Report (Dynamic fine calculation at ₹2.00/day)
    if (pathname === '/api/overdue' && method === 'GET') {
        try {
            const today = new Date().toISOString().split('T')[0];
            const query = `
                SELECT t.transaction_id, t.issue_date, t.due_date,
                       s.student_id, s.register_no, s.name AS student_name, s.department,
                       b.book_id, b.title AS book_title, c.category_name
                FROM transactions t
                JOIN students s ON t.student_id = s.student_id
                JOIN books b ON t.book_id = b.book_id
                JOIN categories c ON b.category_id = c.category_id
                WHERE t.status = 'ISSUED' AND date(t.due_date) < date(?)
                ORDER BY t.due_date ASC;
            `;
            const overdues = db.prepare(query).all(today).map(tx => {
                const overdueDays = Math.max(0, daysBetween(tx.due_date, today));
                const fine = overdueDays * 2.0;
                return {
                    ...tx,
                    overdue_days: overdueDays,
                    calculated_fine: fine
                };
            });
            return sendJson(res, 200, overdues);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 7. Student History
    if (pathname.startsWith('/api/students/') && pathname.endsWith('/history') && method === 'GET') {
        try {
            const parts = pathname.split('/');
            const studentId = parseInt(parts[3], 10);
            const query = `
                SELECT t.transaction_id, t.issue_date, t.due_date, t.return_date, t.status, t.fine_amount,
                       b.book_id, b.title AS book_title, c.category_name, a.author_name
                FROM transactions t
                JOIN books b ON t.book_id = b.book_id
                JOIN categories c ON b.category_id = c.category_id
                JOIN authors a ON b.author_id = a.author_id
                WHERE t.student_id = ?
                ORDER BY t.issue_date DESC;
            `;
            const history = db.prepare(query).all(studentId);
            return sendJson(res, 200, history);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 8. Issue Book (Enforces all FR2 and FR3 Business Rules)
    if (pathname === '/api/issue' && method === 'POST') {
        try {
            const body = await parseBody(req);
            const studentId = parseInt(body.student_id, 10);
            const bookId = parseInt(body.book_id, 10);
            const issueDate = body.issue_date || new Date().toISOString().split('T')[0];

            if (!studentId || !bookId) {
                return sendJson(res, 400, { error: 'Both student_id and book_id are required.' });
            }

            // 1. Verify student exists
            const student = db.prepare('SELECT * FROM students WHERE student_id = ?').get(studentId);
            if (!student) {
                return sendJson(res, 404, { error: `Student with ID #${studentId} not found.` });
            }

            // 2. Check active loans limit (FR3: Max 3 active books)
            const activeCount = db.prepare("SELECT count(*) as val FROM transactions WHERE student_id = ? AND status = 'ISSUED'").get(studentId).val;
            if (activeCount >= 3) {
                return sendJson(res, 400, {
                    error: `Student ${student.name} (${student.register_no}) has already borrowed 3 books (Maximum limit reached). Return a book before borrowing again.`
                });
            }

            // 3. Prevent duplicate active issuance of the exact same title
            const duplicateLoan = db.prepare("SELECT transaction_id FROM transactions WHERE student_id = ? AND book_id = ? AND status = 'ISSUED'").get(studentId, bookId);
            if (duplicateLoan) {
                return sendJson(res, 400, {
                    error: `Student already holds an unreturned copy of this book (Transaction #${duplicateLoan.transaction_id}).`
                });
            }

            // 4. Verify book exists and check stock (FR3: Prevent issuing unavailable copies)
            const book = db.prepare('SELECT * FROM books WHERE book_id = ?').get(bookId);
            if (!book) {
                return sendJson(res, 404, { error: `Book with ID #${bookId} not found.` });
            }
            if (book.available_copies <= 0) {
                return sendJson(res, 400, {
                    error: `Book '${book.title}' is currently OUT OF STOCK (0 available copies).`
                });
            }

            // 5. Execute Transaction (Decrement stock & insert transaction)
            const dueDate = addDays(issueDate, 14); // 14-day loan period

            db.exec('BEGIN TRANSACTION;');
            db.prepare('UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ?;').run(bookId);
            const insertStmt = db.prepare(`
                INSERT INTO transactions (student_id, book_id, issue_date, due_date, return_date, fine_amount, status)
                VALUES (?, ?, ?, ?, NULL, 0.0, 'ISSUED');
            `);
            const info = insertStmt.run(studentId, bookId, issueDate, dueDate);
            db.exec('COMMIT;');

            return sendJson(res, 201, {
                success: true,
                message: `Book '${book.title}' successfully issued to ${student.name}!`,
                transaction: {
                    transaction_id: info.lastInsertRowid,
                    student_id: studentId,
                    student_name: student.name,
                    register_no: student.register_no,
                    book_id: bookId,
                    book_title: book.title,
                    issue_date: issueDate,
                    due_date: dueDate,
                    fine_rule: '₹2.00 / day if returned after due date'
                }
            });
        } catch (err) {
            db.exec('ROLLBACK;');
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 9. Return Book & Calculate Fine (FR4 & FR5)
    if (pathname === '/api/return' && method === 'POST') {
        try {
            const body = await parseBody(req);
            const transactionId = parseInt(body.transaction_id, 10);
            const returnDate = body.return_date || new Date().toISOString().split('T')[0];

            if (!transactionId) {
                return sendJson(res, 400, { error: 'transaction_id is required.' });
            }

            const tx = db.prepare(`
                SELECT t.*, b.title AS book_title, s.name AS student_name, s.register_no
                FROM transactions t
                JOIN books b ON t.book_id = b.book_id
                JOIN students s ON t.student_id = s.student_id
                WHERE t.transaction_id = ?;
            `).get(transactionId);

            if (!tx) {
                return sendJson(res, 404, { error: `Transaction #${transactionId} not found.` });
            }

            if (tx.status !== 'ISSUED') {
                return sendJson(res, 400, { error: `Transaction #${transactionId} has already been returned.` });
            }

            // Calculate overdue days and fine (₹2.00 per day after due date)
            const overdueDays = Math.max(0, daysBetween(tx.due_date, returnDate));
            const fineAmount = overdueDays * 2.0;

            db.exec('BEGIN TRANSACTION;');
            db.prepare(`
                UPDATE transactions
                SET status = 'RETURNED', return_date = ?, fine_amount = ?
                WHERE transaction_id = ?;
            `).run(returnDate, fineAmount, transactionId);

            db.prepare('UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?;').run(tx.book_id);
            db.exec('COMMIT;');

            return sendJson(res, 200, {
                success: true,
                message: `Book '${tx.book_title}' returned successfully!`,
                transaction_id: transactionId,
                book_title: tx.book_title,
                student_name: tx.student_name,
                issue_date: tx.issue_date,
                due_date: tx.due_date,
                return_date: returnDate,
                overdue_days: overdueDays,
                fine_amount: fineAmount
            });
        } catch (err) {
            db.exec('ROLLBACK;');
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 10. Register Student
    if (pathname === '/api/students' && method === 'POST') {
        try {
            const body = await parseBody(req);
            const { register_no, name, department, year, email } = body;
            if (!register_no || !name || !department || !year || !email) {
                return sendJson(res, 400, { error: 'All fields (register_no, name, department, year, email) are required.' });
            }

            const stmt = db.prepare(`
                INSERT INTO students (register_no, name, department, year, email)
                VALUES (?, ?, ?, ?, ?);
            `);
            const info = stmt.run(register_no.trim().toUpperCase(), name.trim(), department.trim(), parseInt(year, 10), email.trim().toLowerCase());
            return sendJson(res, 201, { success: true, student_id: info.lastInsertRowid, message: 'Student registered successfully!' });
        } catch (err) {
            return sendJson(res, 400, { error: 'Registration failed: ' + err.message });
        }
    }

    // 11. Add New Book
    if (pathname === '/api/books' && method === 'POST') {
        try {
            const body = await parseBody(req);
            const { isbn, title, category_id, author_id, total_copies } = body;
            if (!isbn || !title || !category_id || !author_id || !total_copies) {
                return sendJson(res, 400, { error: 'All book details are required.' });
            }
            const copies = parseInt(total_copies, 10);
            const stmt = db.prepare(`
                INSERT INTO books (isbn, title, category_id, author_id, total_copies, available_copies)
                VALUES (?, ?, ?, ?, ?, ?);
            `);
            const info = stmt.run(isbn.trim(), title.trim(), parseInt(category_id, 10), parseInt(author_id, 10), copies, copies);
            return sendJson(res, 201, { success: true, book_id: info.lastInsertRowid, message: 'Book added to catalog successfully!' });
        } catch (err) {
            return sendJson(res, 400, { error: 'Failed to add book: ' + err.message });
        }
    }

    // 12. Analytics Data for Interactive Charts
    if (pathname === '/api/analytics' && method === 'GET') {
        try {
            // Category distribution
            const categoryDist = db.prepare(`
                SELECT c.category_name, count(t.transaction_id) as borrow_count
                FROM categories c
                JOIN books b ON c.category_id = b.category_id
                JOIN transactions t ON b.book_id = t.book_id
                GROUP BY c.category_id, c.category_name
                ORDER BY borrow_count DESC;
            `).all();

            // Monthly trends
            const monthlyTrends = db.prepare(`
                SELECT strftime('%Y-%m', t.issue_date) as month,
                       count(t.transaction_id) as total_loans,
                       sum(CASE WHEN t.fine_amount > 0 THEN 1 ELSE 0 END) as overdue_loans,
                       sum(t.fine_amount) as total_fines
                FROM transactions t
                GROUP BY strftime('%Y-%m', t.issue_date)
                ORDER BY month ASC;
            `).all();

            // Top books
            const topBooks = db.prepare(`
                SELECT b.title, count(t.transaction_id) as borrow_count
                FROM books b
                JOIN transactions t ON b.book_id = t.book_id
                GROUP BY b.book_id, b.title
                ORDER BY borrow_count DESC
                LIMIT 8;
            `).all();

            // Fine by category
            const fineByCategory = db.prepare(`
                SELECT c.category_name, sum(t.fine_amount) as total_fine
                FROM categories c
                JOIN books b ON c.category_id = b.category_id
                JOIN transactions t ON b.book_id = t.book_id
                WHERE t.fine_amount > 0
                GROUP BY c.category_id, c.category_name
                ORDER BY total_fine DESC;
            `).all();

            return sendJson(res, 200, {
                categoryDist,
                monthlyTrends,
                topBooks,
                fineByCategory
            });
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // Generate the Python EDA reports from the shared SQLite database.
    if (pathname === '/api/analytics/python/run' && method === 'POST') {
        try {
            const output = await runPythonAnalytics();
            return sendJson(res, 200, {
                success: true,
                message: 'Python analytics completed. The charts have been refreshed.',
                output
            });
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // 13. Export Transaction Data for Python Analysis (Requirement 7)
    if (pathname === '/api/export/transactions' && method === 'GET') {
        try {
            const query = `
                SELECT 
                    t.transaction_id,
                    t.issue_date,
                    t.due_date,
                    t.return_date,
                    t.status,
                    t.fine_amount,
                    s.student_id,
                    s.register_no,
                    s.name AS student_name,
                    s.department,
                    s.year,
                    b.book_id,
                    b.isbn,
                    b.title AS book_title,
                    c.category_name,
                    a.author_name
                FROM transactions t
                JOIN students s ON t.student_id = s.student_id
                JOIN books b ON t.book_id = b.book_id
                JOIN categories c ON b.category_id = c.category_id
                JOIN authors a ON b.author_id = a.author_id
                ORDER BY t.transaction_id ASC;
            `;
            const rows = db.prepare(query).all();
            return sendJson(res, 200, rows);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    if (pathname === '/api/export/transactions.csv' && method === 'GET') {
        try {
            const query = `
                SELECT 
                    t.transaction_id,
                    t.issue_date,
                    t.due_date,
                    COALESCE(t.return_date, '') AS return_date,
                    t.status,
                    t.fine_amount,
                    s.student_id,
                    s.register_no,
                    s.name AS student_name,
                    s.department,
                    s.year,
                    b.book_id,
                    b.isbn,
                    b.title AS book_title,
                    c.category_name,
                    a.author_name
                FROM transactions t
                JOIN students s ON t.student_id = s.student_id
                JOIN books b ON t.book_id = b.book_id
                JOIN categories c ON b.category_id = c.category_id
                JOIN authors a ON b.author_id = a.author_id
                ORDER BY t.transaction_id ASC;
            `;
            const rows = db.prepare(query).all();
            res.writeHead(200, {
                'Content-Type': 'text/csv; charset=utf-8',
                'Content-Disposition': 'attachment; filename="library_circulation_transactions.csv"',
                'Access-Control-Allow-Origin': '*'
            });
            let csv = 'transaction_id,issue_date,due_date,return_date,status,fine_amount,student_id,register_no,student_name,department,year,book_id,isbn,book_title,category_name,author_name\n';
            for (const r of rows) {
                csv += `${r.transaction_id},${r.issue_date},${r.due_date},${r.return_date},${r.status},${r.fine_amount},${r.student_id},"${r.register_no}","${r.student_name.replace(/"/g, '""')}",${r.department},${r.year},${r.book_id},"${r.isbn}","${r.book_title.replace(/"/g, '""')}","${r.category_name}","${r.author_name}"\n`;
            }
            return res.end(csv);
        } catch (err) {
            return sendJson(res, 500, { error: err.message });
        }
    }

    // Serve Python Visualizations
    if (pathname.startsWith('/visualizations/')) {
        const imgName = path.basename(pathname);
        const imgPath = path.join(__dirname, 'python_analytics', 'visualizations', imgName);
        if (fs.existsSync(imgPath)) {
            res.writeHead(200, {
                'Content-Type': 'image/png',
                'Access-Control-Allow-Origin': '*'
            });
            return fs.createReadStream(imgPath).pipe(res);
        }
    }

    // ------------------------------------------------------------------------
    // STATIC FRONTEND ASSETS
    // ------------------------------------------------------------------------
    let filePath = path.join(FRONTEND_DIR, pathname === '/' ? 'index.html' : pathname);

    // Security check: prevent directory traversal
    if (!filePath.startsWith(FRONTEND_DIR)) {
        res.writeHead(403);
        return res.end('Access Denied');
    }

    fs.stat(filePath, (err, stats) => {
        if (err || !stats.isFile()) {
            // If asset not found, serve index.html for SPA routing
            filePath = path.join(FRONTEND_DIR, 'index.html');
        }

        const ext = path.extname(filePath).toLowerCase();
        const contentTypes = {
            '.html': 'text/html; charset=utf-8',
            '.css': 'text/css; charset=utf-8',
            '.js': 'application/javascript; charset=utf-8',
            '.json': 'application/json; charset=utf-8',
            '.png': 'image/png',
            '.jpg': 'image/jpeg',
            '.svg': 'image/svg+xml',
            '.ico': 'image/x-icon'
        };

        const contentType = contentTypes[ext] || 'text/plain';
        fs.readFile(filePath, (readErr, content) => {
            if (readErr) {
                res.writeHead(500);
                return res.end('Internal Server Error');
            }
            res.writeHead(200, { 'Content-Type': contentType });
            res.end(content);
        });
    });
});

server.listen(PORT, () => {
    console.log('========================================================================');
    console.log(' VELALAR COLLEGE OF ENGINEERING AND TECHNOLOGY, ERODE - 12');
    console.log(' DEPARTMENT OF INFORMATION TECHNOLOGY');
    console.log(' LIBRARY BOOK LENDING & USAGE ANALYSIS SYSTEM (SCENARIO 4)');
    console.log(' Student: Abishek | Reg No: 732925ITR004 | Section: 25IT3A');
    console.log('========================================================================');
    console.log(`[SUCCESS] Backend & Frontend connected successfully!`);
    console.log(`[LIVE] Server running at: http://localhost:${PORT}`);
    console.log('========================================================================');
});
