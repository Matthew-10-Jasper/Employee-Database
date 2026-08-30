-- Manager Portal SQLite database
-- The Java application creates manager_portal.db automatically and executes this file.

CREATE TABLE IF NOT EXISTS managers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS employees (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    department TEXT NOT NULL,
    status TEXT NOT NULL,
    task TEXT NOT NULL,
    phone TEXT,
    salary REAL,
    address TEXT
);

CREATE TABLE IF NOT EXISTS daily_activity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id INTEGER NOT NULL,
    work_date TEXT NOT NULL,
    hours REAL NOT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id)
);

INSERT OR IGNORE INTO managers (username, password, role)
VALUES ('manager', 'admin123', 'MANAGER');

INSERT OR IGNORE INTO managers (username, password, role)
VALUES ('priya.lead', 'leadpass1', 'ENGINEERING_LEAD');

INSERT OR IGNORE INTO employees
    (id, name, department, status, task, phone, salary, address)
VALUES
    (1, 'Arun Kumar', 'Engineering', 'Actively Working',
     'Building employee dashboard', '9876543210', 65000,
     'Tirunelveli, Tamil Nadu');

INSERT OR IGNORE INTO employees
    (id, name, department, status, task, phone, salary, address)
VALUES
    (2, 'Priya Sharma', 'Engineering', 'In Meeting',
     'Sprint planning meeting', '9876501234', 72000,
     'Chennai, Tamil Nadu');

INSERT OR IGNORE INTO employees
    (id, name, department, status, task, phone, salary, address)
VALUES
    (3, 'Rahul Das', 'HR', 'Idle',
     'Reviewing employee records', '9123456780', 58000,
     'Coimbatore, Tamil Nadu');

INSERT OR IGNORE INTO employees
    (id, name, department, status, task, phone, salary, address)
VALUES
    (4, 'Meena Ravi', 'Finance', 'On Leave',
     'On approved leave', '9988776655', 61000,
     'Madurai, Tamil Nadu');

INSERT OR IGNORE INTO employees
    (id, name, department, status, task, phone, salary, address)
VALUES
    (5, 'Vikram Raj', 'Engineering', 'Actively Working',
     'Developing API', '9000000001', 68000,
     'Nagercoil, Tamil Nadu');

INSERT OR IGNORE INTO daily_activity (employee_id, work_date, hours)
SELECT 1, date('now'), 6.5
WHERE NOT EXISTS (
    SELECT 1 FROM daily_activity WHERE employee_id = 1 AND work_date = date('now')
);

INSERT OR IGNORE INTO daily_activity (employee_id, work_date, hours)
SELECT 2, date('now'), 5.0
WHERE NOT EXISTS (
    SELECT 1 FROM daily_activity WHERE employee_id = 2 AND work_date = date('now')
);

INSERT OR IGNORE INTO daily_activity (employee_id, work_date, hours)
SELECT 3, date('now'), 4.5
WHERE NOT EXISTS (
    SELECT 1 FROM daily_activity WHERE employee_id = 3 AND work_date = date('now')
);

INSERT OR IGNORE INTO daily_activity (employee_id, work_date, hours)
SELECT 4, date('now'), 0.0
WHERE NOT EXISTS (
    SELECT 1 FROM daily_activity WHERE employee_id = 4 AND work_date = date('now')
);
