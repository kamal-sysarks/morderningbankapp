# Legacy Banking Transaction Management System

A deliberately **traditional, monolithic** banking desktop application used by branch employees
to manage customer accounts and cash transactions. It is the starting point for an AI-assisted
legacy-modernization workshop (later: React + Spring Boot).

**Do not modernize this code at this stage** - the legacy shape is the point.

## Technology

| Item | Choice |
|------|--------|
| Language | Java 8 source/target level (`maven.compiler.release=8`; builds on JDK 8+) |
| UI | Swing / AWT (JFrame, JPanel, JTable, JComboBox, JMenuBar, JOptionPane ...) |
| Database | SQLite 3 (single file `legacybank.db`, embedded, no server) |
| Data access | Plain JDBC, `org.xerial:sqlite-jdbc` driver |
| Build | Maven |
| Tests | JUnit 4 |

No Spring, no REST, no ORM, no dependency injection.

## Quick start

Prerequisites: JDK 8+ and Maven 3.6+. Nothing else - no database server and no Docker.

```bash
# Build and test (creates target/legacy-banking-app.jar)
mvn clean package

# Run - creates and seeds ./legacybank.db on first start
java -jar target/legacy-banking-app.jar
#   or:  mvn exec:java
```

Log in with **`admin` / `admin123`** (demo credentials stored in the `EMPLOYEES` table).

To reset all data, stop the application and delete `legacybank.db`.

### Maven commands

| Command | What it does |
|---------|--------------|
| `mvn clean compile` | Compile |
| `mvn test` | Unit and database integration tests (each test uses a temporary SQLite file) |
| `mvn clean package` | Compile, test, build the runnable `target/legacy-banking-app.jar` |
| `mvn exec:java` | Launch the app from Maven |

## Database configuration

`src/main/resources/db.properties` (only `DBConnection` reads it):

```properties
db.driver=org.sqlite.JDBC
db.url=jdbc:sqlite:legacybank.db
```

Override with `-Ddb.url=jdbc:sqlite:/path/to/bank.db` or `-Ddb.config=/path/to/file.properties`.
Scripts, schema and manual setup with the `sqlite3` tool: [database/README.md](database/README.md).

## Features / screens

| Screen | Class | What it does |
|--------|-------|--------------|
| Login | `LoginFrame` | Checks username/password against `EMPLOYEES` |
| Main Dashboard | `MainFrame` | Menu bar and buttons to open the screens |
| Customer Search | `CustomerSearchFrame` | Search by customer ID or account number; shows customer details and all accounts (type, status, balance) |
| Account Details | `AccountDetailsFrame` | One account with owner info; shortcuts to deposit/withdraw/history |
| Deposit | `DepositFrame` | Validates (exists, active, positive amount), updates balance, records transaction |
| Withdrawal | `WithdrawalFrame` | Validates (exists, active, sufficient funds), updates balance, records transaction |
| Fund Transfer | `TransferFrame` | Debit + credit + two transaction rows in a single JDBC transaction; rollback on any failure |
| Transaction History | `TransactionHistoryFrame` | By account number, optional type filter, newest first |

Business rules: positive amounts only (max 2 decimals), account must exist and be `ACTIVE`,
withdrawals/transfers cannot exceed the balance, source and destination of a transfer must differ.
Each operation runs in one `BEGIN IMMEDIATE` transaction, so two open application windows cannot
overdraw an account. A `CHECK (BALANCE >= 0)` constraint backs this up in the database.

## Architecture (intentionally legacy)

```
+---------------------------------------------------------------+
|  Single Java process (Swing desktop client)                   |
|                                                               |
|  LoginFrame -> MainFrame -> Customer/Account/Deposit/         |
|                             Withdrawal/Transfer/History Frames|
|        |  (ActionListener = UI + business logic + SQL)        |
|        v                                                      |
|  DBConnection (static JDBC helper)   BankUtil / AppSession    |
|  DatabaseInitializer (schema + seed on first start)           |
+---------------------------|-----------------------------------+
                            | JDBC (sqlite-jdbc, in-process)
                            v
                  legacybank.db  (SQLite file)
          EMPLOYEES  CUSTOMERS  ACCOUNTS  TRANSACTIONS
                  REFERENCE_SEQUENCE
```

Legacy characteristics that are preserved on purpose:

* One monolithic desktop application, `com.legacybank` flat package, no layering.
* Swing event handlers contain validation, business rules and embedded SQL strings.
* Direct JDBC (`Connection`/`PreparedStatement`/`ResultSet`), a new connection per user action,
  manual commit/rollback, manual resource closing in `finally`.
* Static global state (`AppSession`) and static helpers (`DBConnection`, `BankUtil`).
* Logging with `System.out.println`; checked exceptions; Java-5/6-style code (anonymous listeners, raw loops).
* Plain-text demo passwords, no authentication integrations.

Do not confuse "legacy style" with unsafe: money is `BigDecimal`/`NUMERIC(15,2)`, all SQL uses bind
variables, and every financial operation is a single atomic transaction.

### Database dependencies (migration checklist)

* JDBC driver `org.sqlite.JDBC`, URL format `jdbc:sqlite:<file>`; connection settings in `db.properties`.
* SQLite SQL used in Java: `IFNULL`, `datetime('now','localtime')`, `substr`, `last_insert_rowid()`,
  `INSERT ... DEFAULT VALUES`, `sqlite_master`; no sequences or stored procedures.
* Dates are stored as text; `NUMERIC(15,2)` is stored as floating point and rounded in Java (`BankUtil.getMoney`).
* SQL is embedded in: `LoginFrame`, `CustomerSearchFrame`, `AccountDetailsFrame`, `DepositFrame`,
  `WithdrawalFrame`, `TransferFrame`, `TransactionHistoryFrame`, `BankUtil`, `DatabaseInitializer`.

## Project layout

```
legacy-banking-app/
  pom.xml
  README.md
  database/            sqlite-schema.sql, seed-data.sql, README.md
  src/main/java/com/legacybank/
      BankingApplication, LoginFrame, MainFrame, CustomerSearchFrame, AccountDetailsFrame,
      DepositFrame, WithdrawalFrame, TransferFrame, TransactionHistoryFrame,
      DBConnection, DatabaseInitializer, BankUtil, AppSession, BankingException
  src/main/resources/db.properties
  src/test/java/com/legacybank/
      BankUtilTest (pure unit tests)
      DatabaseIntegrationTest (temporary SQLite database built from the scripts)
```

## Demo walkthrough

1. Log in as `admin` / `admin123`.
2. Customer Search -> `1001` -> two accounts; select one -> Account Details.
3. Deposit `500` to `1001000002`; withdraw `100000` from `1001000010` (rejected: insufficient funds).
4. Transfer `250` from `1001000001` to `1001000003`; transfer to `1001000008` (rejected: frozen).
5. Transaction History for `1001000001`, filter by type.
6. Restart the application: balances and history are still there (they live in `legacybank.db`).

## Demo credentials

`admin` / `admin123` - workshop use only.
