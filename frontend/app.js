/**
 * ============================================================================
 * Frontend Controller: Library Book Lending & Usage Analysis System
 * Academic Year: 2026-2027 (Odd Semester) | Class: 25IT3A
 * Institution: Velalar College of Engineering and Technology, Erode - 12
 * Department: Department of Information Technology
 * Student: Abishek | Register No: 732925ITR004 | Scenario 4
 * ============================================================================
 */

// Global State
const state = {
    books: [],
    categories: [],
    students: [],
    transactions: [],
    overdueLoans: [],
    charts: {}
};

// Determine API Base URL (works seamlessly whether hosted on port 8080 or 3000)
const API_BASE = window.location.origin;

// ----------------------------------------------------------------------------
// Initialization & Navigation
// ----------------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initDates();
    loadDashboard();
    loadCategories();
});

function initNavigation() {
    const tabs = document.querySelectorAll('.nav-tabs .tab-item');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const tabId = tab.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-tabs .tab-item').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));

    const targetTab = document.querySelector(`.nav-tabs .tab-item[data-tab="${tabId}"]`);
    const targetContent = document.getElementById(`tab-${tabId}`);

    if (targetTab && targetContent) {
        targetTab.classList.add('active');
        targetContent.classList.add('active');
    }

    // Trigger tab-specific loaders
    switch (tabId) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'catalog':
            loadCatalog();
            break;
        case 'issue':
            prepareIssueTab();
            break;
        case 'return':
            prepareReturnTab();
            break;
        case 'overdue':
            loadOverdueLoans();
            break;
        case 'students':
            loadStudents();
            break;
        case 'analytics':
            loadAnalytics();
            break;
    }
}

function initDates() {
    const today = new Date().toISOString().split('T')[0];
    const issueDateInput = document.getElementById('issue-date');
    const returnDateInput = document.getElementById('return-date');

    if (issueDateInput) {
        issueDateInput.value = today;
        calculateDueDatePreview();
    }
    if (returnDateInput) {
        returnDateInput.value = today;
    }
}

// ----------------------------------------------------------------------------
// Toast Notifications
// ----------------------------------------------------------------------------
let toastTimer = null;
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    if (!toast) return;

    if (toastTimer) clearTimeout(toastTimer);

    toast.className = `toast toast-${type}`;
    let icon = 'circle-info';
    if (type === 'success') icon = 'circle-check';
    if (type === 'error') icon = 'triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid fa-${icon}"></i> <span>${message}</span>`;
    toast.classList.remove('hidden');

    toastTimer = setTimeout(() => {
        toast.classList.add('hidden');
    }, 4000);
}

// ----------------------------------------------------------------------------
// TAB 1: DASHBOARD
// ----------------------------------------------------------------------------
async function loadDashboard() {
    try {
        // Fetch stats & transactions in parallel
        const [statsRes, txRes] = await Promise.all([
            fetch(`${API_BASE}/api/stats`).catch(() => fetch(`${API_BASE}/api/analytics`)),
            fetch(`${API_BASE}/api/transactions`)
        ]);

        if (statsRes && statsRes.ok) {
            const stats = await statsRes.json();
            // Handle both /api/stats and /api/analytics summary schema
            const totalBooks = stats.totalBooks ?? stats.summary?.totalTitles ?? 0;
            const availableCopies = stats.availableCopies ?? stats.summary?.availableCopies ?? 0;
            const activeLoans = stats.activeLoans ?? stats.summary?.activeLoans ?? 0;
            const overdueLoans = stats.overdueLoans ?? stats.summary?.overdueLoans ?? 0;
            const totalFines = stats.totalFines ?? stats.summary?.fineCollected ?? 0;
            const totalStudents = stats.totalStudents ?? stats.summary?.totalStudents ?? 0;

            document.getElementById('stat-total-books').textContent = totalBooks;
            document.getElementById('stat-available-copies').textContent = `${availableCopies} Physical Copies Avail.`;
            document.getElementById('stat-active-loans').textContent = activeLoans;
            document.getElementById('stat-overdue-loans').textContent = overdueLoans;
            document.getElementById('stat-total-fines').textContent = `₹${parseFloat(totalFines).toFixed(2)}`;
            document.getElementById('stat-total-students').textContent = totalStudents;
        }

        if (txRes && txRes.ok) {
            const txs = await txRes.json();
            state.transactions = txs;
            renderRecentTransactions(txs.slice(0, 8));
        }
    } catch (err) {
        console.error('Failed to load dashboard:', err);
    }
}

function refreshDashboard() {
    showToast('Refreshing circulation data...', 'info');
    loadDashboard();
}

function renderRecentTransactions(transactions) {
    const tbody = document.getElementById('recent-transactions-body');
    const badge = document.getElementById('tx-badge-count');
    if (!tbody) return;

    if (badge) badge.textContent = `Latest ${transactions.length} records`;

    if (transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">No transactions recorded yet.</td></tr>';
        return;
    }

    tbody.innerHTML = transactions.map(t => {
        const isIssued = t.status === 'ISSUED';
        const statusBadge = isIssued 
            ? '<span class="badge badge-warning"><i class="fa-solid fa-clock"></i> Active Loan</span>' 
            : '<span class="badge badge-success"><i class="fa-solid fa-check"></i> Returned</span>';

        const fineText = t.fine_amount > 0 || t.fineAmount > 0 
            ? `<strong class="text-red">₹${parseFloat(t.fine_amount || t.fineAmount).toFixed(2)}</strong>` 
            : '<span class="text-muted">₹0.00</span>';

        return `
            <tr>
                <td><strong>#${t.transaction_id || t.transactionId}</strong></td>
                <td>
                    <strong>${t.student_name || t.studentName}</strong>
                    <div class="text-muted" style="font-size: 0.75rem;">${t.register_no || t.registerNo}</div>
                </td>
                <td>${t.book_title || t.bookTitle}</td>
                <td><span class="badge badge-info">${t.category_name || t.categoryName || 'General'}</span></td>
                <td>${t.issue_date || t.issueDate}</td>
                <td>${t.due_date || t.dueDate}</td>
                <td>${statusBadge}</td>
                <td>${fineText}</td>
            </tr>
        `;
    }).join('');
}

// ----------------------------------------------------------------------------
// TAB 2: BOOK CATALOG
// ----------------------------------------------------------------------------
async function loadCatalog() {
    try {
        const [booksRes, catRes] = await Promise.all([
            fetch(`${API_BASE}/api/books`),
            fetch(`${API_BASE}/api/categories`)
        ]);

        if (booksRes.ok) {
            state.books = await booksRes.json();
            renderBooksTable(state.books);
        }

        if (catRes.ok) {
            state.categories = await catRes.json();
            populateCategoryFilter(state.categories);
        }
    } catch (err) {
        showToast('Failed to load catalog: ' + err.message, 'error');
    }
}

function populateCategoryFilter(categories) {
    const filter = document.getElementById('category-filter');
    const modalCategory = document.getElementById('new-category');
    if (!filter) return;

    const currentVal = filter.value;
    filter.innerHTML = '<option value="">All Subject Categories</option>' +
        categories.map(c => `<option value="${c.category_id || c.categoryId}">${c.category_name || c.categoryName}</option>`).join('');
    filter.value = currentVal;

    if (modalCategory) {
        modalCategory.innerHTML = categories.map(c => 
            `<option value="${c.category_id || c.categoryId}">${c.category_name || c.categoryName}</option>`
        ).join('');
    }
}

function renderBooksTable(books) {
    const tbody = document.getElementById('books-table-body');
    if (!tbody) return;

    if (books.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">No books matched your filter criteria.</td></tr>';
        return;
    }

    tbody.innerHTML = books.map(b => {
        const id = b.book_id || b.bookId;
        const avail = b.available_copies ?? b.availableCopies ?? 0;
        const total = b.total_copies ?? b.totalCopies ?? 0;
        const isAvailable = avail > 0;

        const statusBadge = isAvailable
            ? `<span class="badge badge-success"><i class="fa-solid fa-check"></i> In Stock (${avail})</span>`
            : `<span class="badge badge-danger"><i class="fa-solid fa-xmark"></i> Out of Stock</span>`;

        const actionBtn = isAvailable
            ? `<button class="btn btn-primary btn-sm" onclick="quickIssueBook(${id})"><i class="fa-solid fa-arrow-up-from-bracket"></i> Issue</button>`
            : `<button class="btn btn-secondary btn-sm" disabled title="No physical copies available"><i class="fa-solid fa-ban"></i> Locked</button>`;

        return `
            <tr>
                <td>${id}</td>
                <td><code>${b.isbn}</code></td>
                <td><strong>${b.title}</strong></td>
                <td>${b.author_name || b.authorName || 'N/A'}</td>
                <td><span class="badge badge-info">${b.category_name || b.categoryName || 'General'}</span></td>
                <td>
                    <strong>${avail}</strong> / ${total}
                    <div style="background: #e2e8f0; height: 5px; border-radius: 3px; margin-top: 4px; overflow: hidden; width: 60px;">
                        <div style="background: ${isAvailable ? '#10b981' : '#ef4444'}; width: ${(avail/total)*100}%; height: 100%;"></div>
                    </div>
                </td>
                <td>${statusBadge}</td>
                <td>${actionBtn}</td>
            </tr>
        `;
    }).join('');
}

function filterBooks() {
    const search = (document.getElementById('catalog-search')?.value || '').toLowerCase();
    const categoryId = document.getElementById('category-filter')?.value;
    const availability = document.getElementById('availability-filter')?.value;

    const filtered = state.books.filter(b => {
        const matchesSearch = 
            (b.title && b.title.toLowerCase().includes(search)) ||
            (b.isbn && b.isbn.toLowerCase().includes(search)) ||
            (b.author_name && b.author_name.toLowerCase().includes(search)) ||
            (b.authorName && b.authorName.toLowerCase().includes(search));

        const catId = b.category_id || b.categoryId;
        const matchesCategory = !categoryId || catId.toString() === categoryId;

        const avail = b.available_copies ?? b.availableCopies ?? 0;
        let matchesAvailability = true;
        if (availability === 'available') matchesAvailability = avail > 0;
        if (availability === 'unavailable') matchesAvailability = avail === 0;

        return matchesSearch && matchesCategory && matchesAvailability;
    });

    renderBooksTable(filtered);
}

function quickIssueBook(bookId) {
    switchTab('issue');
    setTimeout(() => {
        const bookSelect = document.getElementById('issue-book-select');
        if (bookSelect) {
            bookSelect.value = bookId;
            onBookSelectChange();
        }
    }, 100);
}

// ----------------------------------------------------------------------------
// TAB 3: ISSUE BOOK (Enforcing Rules)
// ----------------------------------------------------------------------------
async function prepareIssueTab() {
    try {
        const [studentsRes, booksRes] = await Promise.all([
            fetch(`${API_BASE}/api/students`),
            fetch(`${API_BASE}/api/books`)
        ]);

        if (studentsRes.ok) {
            state.students = await studentsRes.json();
            populateStudentDropdown(state.students);
        }

        if (booksRes.ok) {
            state.books = await booksRes.json();
            populateBookDropdown(state.books);
        }

        calculateDueDatePreview();
    } catch (err) {
        showToast('Error loading issue form options: ' + err.message, 'error');
    }
}

function populateStudentDropdown(students) {
    const select = document.getElementById('issue-student-select');
    if (!select) return;

    select.innerHTML = '<option value="">-- Choose Student Borrower --</option>' +
        students.map(s => {
            const id = s.student_id || s.studentId;
            const active = s.active_loans ?? s.activeLoans ?? 0;
            const isBlocked = active >= 3;
            const suffix = isBlocked ? ' [⚠️ Max 3 Limit Reached]' : ` [Active: ${active}/3]`;
            return `<option value="${id}">${s.register_no || s.registerNo} - ${s.name} (${s.department})${suffix}</option>`;
        }).join('');
}

function populateBookDropdown(books) {
    const select = document.getElementById('issue-book-select');
    if (!select) return;

    select.innerHTML = '<option value="">-- Choose Book to Issue --</option>' +
        books.map(b => {
            const id = b.book_id || b.bookId;
            const avail = b.available_copies ?? b.availableCopies ?? 0;
            const suffix = avail <= 0 ? ' [❌ Out of Stock]' : ` [Avail: ${avail}]`;
            return `<option value="${id}">${b.title} - ${b.category_name || b.categoryName}${suffix}</option>`;
        }).join('');
}

function onStudentSelectChange() {
    const select = document.getElementById('issue-student-select');
    const statusHint = document.getElementById('student-borrow-status');
    const submitBtn = document.getElementById('btn-issue-submit');
    const studentId = parseInt(select.value, 10);

    if (!studentId) {
        statusHint.textContent = 'Select a student to inspect borrow limits.';
        statusHint.className = 'form-hint';
        return;
    }

    const student = state.students.find(s => (s.student_id || s.studentId) === studentId);
    if (!student) return;

    const active = student.active_loans ?? student.activeLoans ?? 0;
    if (active >= 3) {
        statusHint.innerHTML = `<span class="text-red"><strong>⚠️ Maximum Limit Reached:</strong> Student already holds ${active} books. Under Scenario 4 rules, a student cannot hold more than 3 books simultaneously.</span>`;
        if (submitBtn) submitBtn.disabled = true;
    } else {
        statusHint.innerHTML = `<span class="text-green"><strong>Eligible:</strong> Current active loans: ${active}/3 books. (${3 - active} remaining allowance).</span>`;
        if (submitBtn) submitBtn.disabled = false;
    }
}

function onBookSelectChange() {
    const select = document.getElementById('issue-book-select');
    const stockHint = document.getElementById('book-stock-status');
    const submitBtn = document.getElementById('btn-issue-submit');
    const bookId = parseInt(select.value, 10);

    if (!bookId) {
        stockHint.textContent = 'Select a book to inspect current availability.';
        stockHint.className = 'form-hint';
        return;
    }

    const book = state.books.find(b => (b.book_id || b.bookId) === bookId);
    if (!book) return;

    const avail = book.available_copies ?? book.availableCopies ?? 0;
    if (avail <= 0) {
        stockHint.innerHTML = `<span class="text-red"><strong>❌ Zero Copies Available:</strong> All copies are currently issued to students. This title cannot be issued.</span>`;
        if (submitBtn) submitBtn.disabled = true;
    } else {
        stockHint.innerHTML = `<span class="text-green"><strong>In Stock:</strong> ${avail} copies available out of ${book.total_copies || book.totalCopies} total.</span>`;
        if (submitBtn) submitBtn.disabled = false;
    }
}

function calculateDueDatePreview() {
    const issueDateStr = document.getElementById('issue-date')?.value;
    const previewInput = document.getElementById('due-date-preview');
    if (!issueDateStr || !previewInput) return;

    const issueDate = new Date(issueDateStr);
    issueDate.setDate(issueDate.getDate() + 14);
    previewInput.value = `${issueDate.toISOString().split('T')[0]} (14 Calendar Days)`;
}

async function submitIssueBook(event) {
    event.preventDefault();

    const studentId = document.getElementById('issue-student-select').value;
    const bookId = document.getElementById('issue-book-select').value;
    const issueDate = document.getElementById('issue-date').value;

    if (!studentId || !bookId) {
        showToast('Please select both a student and a book.', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/issue`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                student_id: parseInt(studentId, 10),
                studentId: parseInt(studentId, 10),
                book_id: parseInt(bookId, 10),
                bookId: parseInt(bookId, 10),
                issue_date: issueDate,
                issueDate: issueDate
            })
        });

        const data = await res.json();
        if (!res.ok || data.error) {
            throw new Error(data.error || 'Failed to issue book');
        }

        showToast(data.message || 'Book issued successfully!', 'success');

        // Render Issue Receipt Slip
        renderIssueReceipt(data.transaction);

        // Reset and refresh options
        document.getElementById('issue-book-form').reset();
        initDates();
        prepareIssueTab();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderIssueReceipt(tx) {
    const box = document.getElementById('issue-receipt-box');
    const content = document.getElementById('issue-receipt-content');
    if (!box || !content) return;

    box.classList.remove('hidden');
    content.innerHTML = `
        <div class="slip-details">
            <div class="slip-row"><span>Transaction ID:</span> <strong>#${tx.transaction_id || tx.transactionId}</strong></div>
            <div class="slip-row"><span>Student Borrower:</span> <strong>${tx.student_name || tx.studentName} (${tx.register_no || tx.registerNo})</strong></div>
            <div class="slip-row"><span>Book Title:</span> <strong>${tx.book_title || tx.bookTitle}</strong></div>
            <div class="slip-row"><span>Issue Date:</span> <strong>${tx.issue_date || tx.issueDate}</strong></div>
            <div class="slip-row"><span>Mandatory Due Date:</span> <strong class="text-blue">${tx.due_date || tx.dueDate}</strong></div>
            <div class="slip-row"><span>Overdue Rule:</span> <span>₹2.00 / day penalty applied after due date</span></div>
        </div>
    `;
}

// ----------------------------------------------------------------------------
// TAB 4: RETURN BOOK (Calculating Fines)
// ----------------------------------------------------------------------------
async function prepareReturnTab() {
    try {
        const res = await fetch(`${API_BASE}/api/transactions`);
        if (res.ok) {
            const allTxs = await res.json();
            const activeTxs = allTxs.filter(t => t.status === 'ISSUED');
            state.activeTransactions = activeTxs;
            populateReturnDropdown(activeTxs);
        }
    } catch (err) {
        showToast('Error loading active loans: ' + err.message, 'error');
    }
}

function populateReturnDropdown(activeTxs) {
    const select = document.getElementById('return-tx-select');
    if (!select) return;

    if (activeTxs.length === 0) {
        select.innerHTML = '<option value="">No currently active loans to return</option>';
        return;
    }

    select.innerHTML = '<option value="">-- Choose Active Loan --</option>' +
        activeTxs.map(t => {
            const txId = t.transaction_id || t.transactionId;
            const regNo = t.register_no || t.registerNo;
            const sName = t.student_name || t.studentName;
            const title = t.book_title || t.bookTitle;
            const due = t.due_date || t.dueDate;
            return `<option value="${txId}">#${txId}: ${title} - Borrowed by ${sName} (${regNo}) | Due: ${due}</option>`;
        }).join('');
}

function onReturnTxChange() {
    const txId = parseInt(document.getElementById('return-tx-select').value, 10);
    const dueDateInput = document.getElementById('return-due-date-preview');

    if (!txId) {
        if (dueDateInput) dueDateInput.value = '-';
        resetFinePreview();
        return;
    }

    const tx = state.activeTransactions.find(t => (t.transaction_id || t.transactionId) === txId);
    if (!tx) return;

    const due = tx.due_date || tx.dueDate;
    if (dueDateInput) dueDateInput.value = due;

    recalcReturnFinePreview();
}

function recalcReturnFinePreview() {
    const txId = parseInt(document.getElementById('return-tx-select')?.value, 10);
    const returnDateStr = document.getElementById('return-date')?.value;
    const banner = document.getElementById('fine-preview-banner');
    const amountEl = document.getElementById('preview-fine-amount');
    const detailsEl = document.getElementById('preview-fine-details');

    if (!txId || !returnDateStr || !state.activeTransactions) {
        resetFinePreview();
        return;
    }

    const tx = state.activeTransactions.find(t => (t.transaction_id || t.transactionId) === txId);
    if (!tx) return;

    const due = new Date(tx.due_date || tx.dueDate);
    const ret = new Date(returnDateStr);
    const diff = (ret.getTime() - due.getTime()) / (1000 * 60 * 60 * 24);
    const overdueDays = Math.max(0, Math.round(diff));
    const fineAmount = overdueDays * 2.0;

    if (overdueDays > 0) {
        banner.className = 'fine-banner banner-fine';
        amountEl.textContent = `₹${fineAmount.toFixed(2)}`;
        detailsEl.innerHTML = `<strong>${overdueDays} days overdue</strong> (Due: ${tx.due_date || tx.dueDate} vs Return: ${returnDateStr}) at ₹2.00 / day penalty.`;
    } else {
        banner.className = 'fine-banner banner-clear';
        amountEl.textContent = '₹0.00';
        detailsEl.textContent = `Returned within 14-day limit. No overdue penalty applicable.`;
    }
}

function resetFinePreview() {
    const banner = document.getElementById('fine-preview-banner');
    const amountEl = document.getElementById('preview-fine-amount');
    const detailsEl = document.getElementById('preview-fine-details');
    if (banner) banner.className = 'fine-banner banner-neutral';
    if (amountEl) amountEl.textContent = '₹0.00';
    if (detailsEl) detailsEl.textContent = 'Select an active transaction to calculate fine.';
}

async function submitReturnBook(event) {
    event.preventDefault();

    const txId = document.getElementById('return-tx-select').value;
    const returnDate = document.getElementById('return-date').value;

    if (!txId) {
        showToast('Please select a transaction to return.', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/return`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                transaction_id: parseInt(txId, 10),
                transactionId: parseInt(txId, 10),
                return_date: returnDate,
                returnDate: returnDate
            })
        });

        const data = await res.json();
        if (!res.ok || data.error) {
            throw new Error(data.error || 'Failed to process return');
        }

        showToast(data.message || 'Book returned successfully!', 'success');

        // Render Return Receipt
        renderReturnReceipt(data);

        // Reset and refresh active transactions
        document.getElementById('return-book-form').reset();
        initDates();
        prepareReturnTab();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderReturnReceipt(data) {
    const box = document.getElementById('return-receipt-box');
    const content = document.getElementById('return-receipt-content');
    if (!box || !content) return;

    box.classList.remove('hidden');
    const fineText = data.fine_amount > 0 || data.fineAmount > 0
        ? `<strong class="text-red">₹${parseFloat(data.fine_amount || data.fineAmount).toFixed(2)} (${data.overdue_days || data.overdueDays} days late)</strong>`
        : '<strong class="text-green">₹0.00 (On Time)</strong>';

    content.innerHTML = `
        <div class="slip-details">
            <div class="slip-row"><span>Transaction ID:</span> <strong>#${data.transaction_id || data.transactionId}</strong></div>
            <div class="slip-row"><span>Book Title:</span> <strong>${data.book_title || data.bookTitle || 'Returned Copy'}</strong></div>
            <div class="slip-row"><span>Student Borrower:</span> <strong>${data.student_name || data.studentName || '-'}</strong></div>
            <div class="slip-row"><span>Return Date:</span> <strong>${data.return_date || data.returnDate}</strong></div>
            <div class="slip-row"><span>Overdue Fine Collected:</span> ${fineText}</div>
            <div class="slip-row"><span>Inventory Status:</span> <span class="text-green"><i class="fa-solid fa-check"></i> 1 copy restored to shelf</span></div>
        </div>
    `;
}

// ----------------------------------------------------------------------------
// TAB 5: OVERDUE TRACKER
// ----------------------------------------------------------------------------
async function loadOverdueLoans() {
    const tbody = document.getElementById('overdue-table-body');
    if (!tbody) return;

    try {
        let res = await fetch(`${API_BASE}/api/overdue`);
        if (!res.ok) res = await fetch(`${API_BASE}/api/reports/overdue`);
        if (!res.ok) throw new Error('Failed to fetch overdue records');

        const overdues = await res.json();
        state.overdueLoans = overdues;

        if (overdues.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center text-green" style="padding: 24px;">🎉 Excellent! There are currently no overdue loans in the system.</td></tr>';
            return;
        }

        tbody.innerHTML = overdues.map(t => {
            const txId = t.transaction_id || t.transactionId;
            const days = t.overdue_days ?? t.overdueDays ?? 0;
            const fine = t.calculated_fine ?? t.fineAmount ?? (days * 2.0);

            return `
                <tr>
                    <td><strong>#${txId}</strong></td>
                    <td><code>${t.register_no || t.registerNo}</code></td>
                    <td><strong>${t.student_name || t.studentName}</strong></td>
                    <td>${t.book_title || t.bookTitle}</td>
                    <td><span class="badge badge-info">${t.category_name || t.categoryName || 'General'}</span></td>
                    <td class="text-red"><strong>${t.due_date || t.dueDate}</strong></td>
                    <td><span class="badge badge-danger">${days} Days Late</span></td>
                    <td><strong class="text-red">₹${parseFloat(fine).toFixed(2)}</strong></td>
                    <td>
                        <button class="btn btn-success btn-sm" onclick="quickReturnOverdue(${txId})">
                            <i class="fa-solid fa-undo"></i> Return
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-red">Error: ${err.message}</td></tr>`;
    }
}

function quickReturnOverdue(txId) {
    switchTab('return');
    setTimeout(() => {
        const select = document.getElementById('return-tx-select');
        if (select) {
            select.value = txId;
            onReturnTxChange();
        }
    }, 150);
}

// ----------------------------------------------------------------------------
// TAB 6: STUDENTS
// ----------------------------------------------------------------------------
async function loadStudents() {
    const tbody = document.getElementById('students-table-body');
    if (!tbody) return;

    try {
        const res = await fetch(`${API_BASE}/api/students`);
        if (!res.ok) throw new Error('Failed to load students');

        const students = await res.json();
        state.students = students;

        if (students.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">No students registered yet.</td></tr>';
            return;
        }

        tbody.innerHTML = students.map(s => {
            const id = s.student_id || s.studentId;
            const regNo = s.register_no || s.registerNo;
            const active = s.active_loans ?? s.activeLoans ?? 0;
            const isLimit = active >= 3;

            const badge = isLimit
                ? `<span class="badge badge-danger"><i class="fa-solid fa-triangle-exclamation"></i> 3/3 (Max Reached)</span>`
                : `<span class="badge badge-success">${active}/3 Books Active</span>`;

            return `
                <tr>
                    <td>${id}</td>
                    <td><code>${regNo}</code></td>
                    <td><strong>${s.name}</strong></td>
                    <td>${s.department}</td>
                    <td>Year ${s.year}</td>
                    <td><a href="mailto:${s.email}" style="color: var(--primary); text-decoration: none;">${s.email}</a></td>
                    <td>${badge}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm" onclick="viewStudentHistory(${id}, '${s.name}', '${regNo}')">
                            <i class="fa-solid fa-history"></i> History
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-red">Error: ${err.message}</td></tr>`;
    }
}

async function viewStudentHistory(studentId, name, regNo) {
    const modal = document.getElementById('student-history-modal');
    const title = document.getElementById('modal-student-title');
    const info = document.getElementById('modal-student-info');
    const tbody = document.getElementById('student-history-tbody');

    if (!modal || !tbody) return;

    title.innerHTML = `<i class="fa-solid fa-clock-rotate-left"></i> Borrowing Ledger: <strong>${name}</strong>`;
    info.innerHTML = `Register Number: <strong>${regNo}</strong> | Student ID: <strong>#${studentId}</strong>`;
    tbody.innerHTML = '<tr><td colspan="8" class="text-center">Loading student borrowing ledger...</td></tr>';
    modal.classList.remove('hidden');

    try {
        let res = await fetch(`${API_BASE}/api/students/${studentId}/history`);
        if (!res.ok) res = await fetch(`${API_BASE}/api/reports/history?studentId=${studentId}`);

        if (!res.ok) throw new Error('Failed to load borrowing history');

        const history = await res.json();
        if (history.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">This student has no past or active borrowings on record.</td></tr>';
            return;
        }

        tbody.innerHTML = history.map(t => {
            const isIssued = t.status === 'ISSUED';
            const statusBadge = isIssued
                ? '<span class="badge badge-warning">Active Loan</span>'
                : '<span class="badge badge-success">Returned</span>';

            const fineText = t.fine_amount > 0 || t.fineAmount > 0
                ? `<strong class="text-red">₹${parseFloat(t.fine_amount || t.fineAmount).toFixed(2)}</strong>`
                : '<span class="text-muted">₹0.00</span>';

            return `
                <tr>
                    <td>#${t.transaction_id || t.transactionId}</td>
                    <td><strong>${t.book_title || t.bookTitle}</strong></td>
                    <td><span class="badge badge-info">${t.category_name || t.categoryName || 'General'}</span></td>
                    <td>${t.issue_date || t.issueDate}</td>
                    <td>${t.due_date || t.dueDate}</td>
                    <td>${t.return_date || t.returnDate || '<span class="text-muted">Not Returned</span>'}</td>
                    <td>${statusBadge}</td>
                    <td>${fineText}</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-red">Error: ${err.message}</td></tr>`;
    }
}

function closeStudentHistoryModal() {
    const modal = document.getElementById('student-history-modal');
    if (modal) modal.classList.add('hidden');
}

// ----------------------------------------------------------------------------
// TAB 7: USAGE ANALYTICS & VISUALIZATIONS (Data Science 25ITC31)
// ----------------------------------------------------------------------------
async function loadAnalytics() {
    refreshPythonReportImages();
    try {
        const res = await fetch(`${API_BASE}/api/analytics`);
        if (!res.ok) throw new Error('Failed to load analytics aggregation');

        const data = await res.json();
        renderAnalyticsCharts(data);
    } catch (err) {
        showToast('Error loading analytics: ' + err.message, 'error');
    }
}

function refreshPythonReportImages() {
    const cacheKey = Date.now();
    const reports = [
        ['python-chart-category', 'category_distribution.png'],
        ['python-chart-monthly', 'monthly_borrowing_trends.png'],
        ['python-chart-books', 'top_borrowed_books.png'],
        ['python-chart-fines', 'overdue_and_fine_analysis.png']
    ];

    reports.forEach(([elementId, filename]) => {
        const image = document.getElementById(elementId);
        if (image) image.src = `${API_BASE}/visualizations/${filename}?v=${cacheKey}`;
    });
}

async function runPythonAnalytics() {
    const button = document.getElementById('btn-run-python-analytics');
    const status = document.getElementById('python-analytics-status');
    if (button) button.disabled = true;
    if (status) status.textContent = 'Running Python analysis against the shared SQLite database…';

    try {
        const response = await fetch(`${API_BASE}/api/analytics/python/run`, { method: 'POST' });
        const result = await response.json();
        if (!response.ok || result.error) {
            throw new Error(result.error || 'Python analytics could not be completed.');
        }

        refreshPythonReportImages();
        if (status) status.textContent = result.message || 'Python reports refreshed from SQLite.';
        showToast('Python analysis completed; reports refreshed.', 'success');
    } catch (err) {
        if (status) status.textContent = err.message;
        showToast('Python analytics failed. Check the report status for details.', 'error');
    } finally {
        if (button) button.disabled = false;
    }
}

function renderAnalyticsCharts(data) {
    if (typeof Chart === 'undefined') {
        console.warn('Chart.js library is not loaded');
        return;
    }

    const chartColors = [
        '#2563eb', '#10b981', '#f59e0b', '#ef4444', 
        '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16'
    ];

    // 1. Donut: Category Distribution
    const catData = data.categoryDist || data.byCategory || [];
    const catLabels = catData.map(c => c.category_name || c.category);
    const catCounts = catData.map(c => c.borrow_count ?? c.count ?? 0);

    destroyChart('categoryDonutChart');
    const ctx1 = document.getElementById('categoryDonutChart')?.getContext('2d');
    if (ctx1) {
        state.charts['categoryDonutChart'] = new Chart(ctx1, {
            type: 'doughnut',
            data: {
                labels: catLabels,
                datasets: [{
                    data: catCounts,
                    backgroundColor: chartColors,
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'right' }
                }
            }
        });
    }

    // 2. Bar: Monthly Trends
    const monthData = data.monthlyTrends || data.monthlyTrend || [];
    const monthLabels = monthData.map(m => m.month);
    const monthIssues = monthData.map(m => m.total_loans ?? m.issues ?? 0);
    const monthOverdues = monthData.map(m => m.overdue_loans ?? m.returns ?? 0);

    destroyChart('monthlyTrendChart');
    const ctx2 = document.getElementById('monthlyTrendChart')?.getContext('2d');
    if (ctx2) {
        state.charts['monthlyTrendChart'] = new Chart(ctx2, {
            type: 'bar',
            data: {
                labels: monthLabels,
                datasets: [
                    {
                        label: 'Total Issued Loans',
                        data: monthIssues,
                        backgroundColor: '#2563eb',
                        borderRadius: 4
                    },
                    {
                        label: 'Overdue / Returned',
                        data: monthOverdues,
                        backgroundColor: '#f59e0b',
                        borderRadius: 4
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, ticks: { stepSize: 1 } }
                }
            }
        });
    }

    // 3. Horizontal Bar: Top Books
    const topBooks = data.topBooks || [];
    const bookLabels = topBooks.map(b => b.title.length > 28 ? b.title.substring(0, 26) + '..' : b.title);
    const bookCounts = topBooks.map(b => b.borrow_count ?? b.borrowCount ?? 0);

    destroyChart('topBooksChart');
    const ctx3 = document.getElementById('topBooksChart')?.getContext('2d');
    if (ctx3) {
        state.charts['topBooksChart'] = new Chart(ctx3, {
            type: 'bar',
            data: {
                labels: bookLabels,
                datasets: [{
                    label: 'Borrow Frequency',
                    data: bookCounts,
                    backgroundColor: '#10b981',
                    borderRadius: 4
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: { beginAtZero: true, ticks: { stepSize: 1 } }
                }
            }
        });
    }

    // 4. Bar: Fines Collected by Category
    const finesData = data.fineByCategory || data.byCategory || [];
    const fineLabels = finesData.map(f => f.category_name || f.category);
    const fineAmounts = finesData.map(f => f.total_fine ?? f.totalFine ?? (f.count * 2.5));

    destroyChart('finesCategoryChart');
    const ctx4 = document.getElementById('finesCategoryChart')?.getContext('2d');
    if (ctx4) {
        state.charts['finesCategoryChart'] = new Chart(ctx4, {
            type: 'bar',
            data: {
                labels: fineLabels,
                datasets: [{
                    label: 'Fines Collected (₹)',
                    data: fineAmounts,
                    backgroundColor: '#8b5cf6',
                    borderRadius: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: value => '₹' + value
                        }
                    }
                }
            }
        });
    }
}

function destroyChart(chartId) {
    if (state.charts[chartId]) {
        state.charts[chartId].destroy();
        delete state.charts[chartId];
    }
}

// ----------------------------------------------------------------------------
// MODAL HANDLERS (Add Student, Add Book)
// ----------------------------------------------------------------------------
function openAddStudentModal() {
    const modal = document.getElementById('add-student-modal');
    if (modal) modal.classList.remove('hidden');
}

function closeAddStudentModal() {
    const modal = document.getElementById('add-student-modal');
    if (modal) modal.classList.add('hidden');
}

async function submitAddStudent(event) {
    event.preventDefault();

    const regNo = document.getElementById('new-reg-no').value.trim();
    const name = document.getElementById('new-name').value.trim();
    const department = document.getElementById('new-dept').value.trim();
    const year = parseInt(document.getElementById('new-year').value, 10);
    const email = document.getElementById('new-email').value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/students`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                register_no: regNo,
                registerNo: regNo,
                name,
                department,
                year,
                email
            })
        });

        const data = await res.json();
        if (!res.ok || data.error) {
            throw new Error(data.error || 'Failed to register student');
        }

        showToast(`Student ${name} (${regNo}) registered successfully!`, 'success');
        closeAddStudentModal();
        document.getElementById('add-student-form').reset();
        loadStudents();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openAddBookModal() {
    const modal = document.getElementById('add-book-modal');
    if (modal) {
        populateCategoryFilter(state.categories);
        modal.classList.remove('hidden');
    }
}

function closeAddBookModal() {
    const modal = document.getElementById('add-book-modal');
    if (modal) modal.classList.add('hidden');
}

async function submitAddBook(event) {
    event.preventDefault();

    const isbn = document.getElementById('new-isbn').value.trim();
    const title = document.getElementById('new-title').value.trim();
    const categoryId = parseInt(document.getElementById('new-category').value, 10);
    const authorId = parseInt(document.getElementById('new-author').value, 10);
    const totalCopies = parseInt(document.getElementById('new-copies').value, 10);

    try {
        const res = await fetch(`${API_BASE}/api/books`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                isbn,
                title,
                category_id: categoryId,
                categoryId: categoryId,
                author_id: authorId,
                authorId: authorId,
                total_copies: totalCopies,
                totalCopies: totalCopies
            })
        });

        const data = await res.json();
        if (!res.ok || data.error) {
            throw new Error(data.error || 'Failed to add book');
        }

        showToast(`Book '${title}' added to catalog!`, 'success');
        closeAddBookModal();
        document.getElementById('add-book-form').reset();
        loadCatalog();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadCategories() {
    try {
        const res = await fetch(`${API_BASE}/api/categories`);
        if (res.ok) {
            state.categories = await res.json();
            populateCategoryFilter(state.categories);
        }
    } catch (err) {
        console.warn('Could not load categories:', err);
    }
}
