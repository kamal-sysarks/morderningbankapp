# Database Setup - SQLite

The application uses **SQLite**, an embedded database. There is no server, Docker image or
account to set up. The database is one file, `legacybank.db`.

| File | Purpose |
|------|---------|
| `sqlite-schema.sql` | Tables, primary/foreign keys, check constraints, indexes, reference-number table (drops existing app tables first) |
| `seed-data.sql`     | 1 employee, 10 customers, 15 accounts, sample transactions |

The same two files are packaged into the jar and used by the application itself.

## 1. Automatic setup (normal case)

On start, `DatabaseInitializer` looks for the `EMPLOYEES` table in the database file. If it is
missing (first run, or the file was deleted), it runs `sqlite-schema.sql` then `seed-data.sql`.
Nothing else is needed:

```bash
java -jar target/legacy-banking-app.jar     # creates ./legacybank.db on first start
```

**Reset the demo data:** stop the application, delete `legacybank.db`, start it again.

## 2. Manual setup (optional)

Use this if you want to create or inspect the database with the `sqlite3` command-line tool.

```bash
cd legacy-banking-app
rm -f legacybank.db
sqlite3 legacybank.db < database/sqlite-schema.sql
sqlite3 legacybank.db < database/seed-data.sql
```

Verify:

```bash
sqlite3 legacybank.db "SELECT COUNT(*) FROM CUSTOMERS; SELECT COUNT(*) FROM ACCOUNTS; SELECT USERNAME, EMPLOYEE_NAME FROM EMPLOYEES;"
```

Expected: 10 customers, 15 accounts, employee `admin`.

## 3. Connection configuration

The application reads `src/main/resources/db.properties`:

```properties
db.driver=org.sqlite.JDBC
db.url=jdbc:sqlite:legacybank.db
db.username=
db.password=
```

A relative path is resolved against the directory the application is started from. Override
without rebuilding, per property or with an external file:

```bash
java -Ddb.url=jdbc:sqlite:/var/data/bank.db -jar target/legacy-banking-app.jar
java -Ddb.config=/etc/legacybank/db.properties -jar target/legacy-banking-app.jar
```

`DBConnection` opens every connection with `foreign_keys=true`, `busy_timeout=10000` and
`transaction_mode=IMMEDIATE` (see "Concurrency" below).

## 4. Schema overview

```
EMPLOYEES (EMPLOYEE_ID PK, USERNAME UK, PASSWORD, EMPLOYEE_NAME, STATUS)
CUSTOMERS (CUSTOMER_ID PK, CUSTOMER_NAME, EMAIL, PHONE, ADDRESS, CREATED_DATE)
ACCOUNTS  (ACCOUNT_ID PK, ACCOUNT_NUMBER UK, CUSTOMER_ID FK -> CUSTOMERS, ACCOUNT_TYPE,
           BALANCE NUMERIC(15,2) >= 0, STATUS, CREATED_DATE)
TRANSACTIONS (TRANSACTION_ID PK, ACCOUNT_NUMBER FK -> ACCOUNTS.ACCOUNT_NUMBER,
           TRANSACTION_TYPE, AMOUNT NUMERIC(15,2) > 0, DESCRIPTION, TRANSACTION_DATE,
           REFERENCE_NUMBER)
REFERENCE_SEQUENCE (REF_ID PK AUTOINCREMENT)   -- hands out reference numbers
```

* IDs are `INTEGER PRIMARY KEY AUTOINCREMENT`. The seed data uses customer IDs 1001-1010, account IDs 5001-5015 and transaction IDs from 90001.
* Dates are stored as text `YYYY-MM-DD HH:MM:SS` (local time), written with `datetime('now', 'localtime')`.
* Indexes: unique index on `ACCOUNTS.ACCOUNT_NUMBER`, `IDX_TXN_ACCOUNT_NUMBER` (account number + date) for history queries, `IDX_TXN_REFERENCE`, `IDX_ACCOUNTS_CUSTOMER`.
* Transaction types: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_DEBIT`, `TRANSFER_CREDIT` (both legs of a transfer share one `REFERENCE_NUMBER`).
* Account types: `SAVINGS`, `CHECKING`, `FIXED_DEPOSIT`. Account statuses: `ACTIVE`, `FROZEN`, `CLOSED`.

### Money handling

SQLite stores decimal `NUMERIC` values as 64-bit floating point. To stay exact, the Java code
always uses `BigDecimal`, rounds values read from the database to two decimals
(`BankUtil.getMoney`), and limits amounts to two decimals and 10,000,000.00 per transaction.

### Concurrency

SQLite allows one writer at a time. `transaction_mode=IMMEDIATE` makes `setAutoCommit(false)` start
a `BEGIN IMMEDIATE` transaction, so the write lock is taken before the balance is read. Two tellers
(two application windows) therefore cannot both pass the "sufficient funds" check and overdraw an
account; the second waits (up to 10 seconds) for the first to commit or roll back.
The `CHECK (BALANCE >= 0)` constraint is a second line of defence.

## 5. Where SQL lives

There are no stored procedures. All business logic and SQL is in the Java classes, as in a typical
legacy two-tier application:

| Operation | Class |
|-----------|-------|
| Login | `LoginFrame` |
| Customer / account lookup | `CustomerSearchFrame`, `AccountDetailsFrame` |
| Deposit | `DepositFrame` (+ helpers in `BankUtil`) |
| Withdrawal | `WithdrawalFrame` (+ helpers in `BankUtil`) |
| Fund transfer (single transaction) | `TransferFrame` (+ helpers in `BankUtil`) |
| History | `TransactionHistoryFrame` |
| Schema / seed loading | `DatabaseInitializer` |

SQLite-specific constructs used: `INTEGER PRIMARY KEY AUTOINCREMENT`, `datetime('now','localtime')`,
`IFNULL`, `substr`, `last_insert_rowid()`, `PRAGMA foreign_keys`, `sqlite_master`, `INSERT ... DEFAULT VALUES`.

## Demo credentials (workshop only)

| Username | Password |
|----------|----------|
| `admin`  | `admin123` |

Passwords are stored in plain text in `EMPLOYEES.PASSWORD` on purpose. This is a demo system:
do not reuse these credentials anywhere real.

## Sample data

* Customers: IDs 1001-1010.
* Accounts: `1001000001` ... `1001000015`.
* `1001000008` is `FROZEN` and `1001000013` is `CLOSED` - use them to try the "inactive account" validation.
* `1001000010` has a small balance (560.35) - use it to try "insufficient funds".
