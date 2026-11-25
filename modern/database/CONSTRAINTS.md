# Database Constraints — Legacy Banking App

## Overview

All constraints below are enforced at the **SQLite database layer** in `database/sqlite-schema.sql`.

## Table: EMPLOYEES

| Constraint | Column | Rule |
|---|---|---|
| PRIMARY KEY | `EMPLOYEE_ID` | Auto-increment integer |
| UNIQUE | `USERNAME` | No duplicate usernames |
| CHECK | `STATUS` | Must be `ACTIVE` or `INACTIVE` |
| NOT NULL | `USERNAME`, `PASSWORD`, `EMPLOYEE_NAME`, `STATUS` | Required fields |

## Table: CUSTOMERS

| Constraint | Column | Rule |
|---|---|---|
| PRIMARY KEY | `CUSTOMER_ID` | Auto-increment integer |
| NOT NULL | `CUSTOMER_NAME`, `CREATED_DATE` | Required fields |

## Table: ACCOUNTS

| Constraint | Column | Rule |
|---|---|---|
| PRIMARY KEY | `ACCOUNT_ID` | Auto-increment integer |
| UNIQUE | `ACCOUNT_NUMBER` | No duplicate account numbers |
| FOREIGN KEY | `CUSTOMER_ID` → `CUSTOMERS(CUSTOMER_ID)` | Referential integrity |
| CHECK | `ACCOUNT_TYPE` | Must be `SAVINGS`, `CHECKING`, or `FIXED_DEPOSIT` |
| CHECK | `BALANCE` | `BALANCE >= 0` — no negative balances at DB level |
| CHECK | `STATUS` | Must be `ACTIVE`, `FROZEN`, or `CLOSED` |
| NOT NULL | All columns | Required fields |
| INDEX | `IDX_ACCOUNTS_CUSTOMER` | On `CUSTOMER_ID` for fast lookup |

## Table: TRANSACTIONS

| Constraint | Column | Rule |
|---|---|---|
| PRIMARY KEY | `TRANSACTION_ID` | Auto-increment integer |
| FOREIGN KEY | `ACCOUNT_NUMBER` → `ACCOUNTS(ACCOUNT_NUMBER)` | Referential integrity |
| CHECK | `TRANSACTION_TYPE` | Must be `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_DEBIT`, or `TRANSFER_CREDIT` |
| CHECK | `AMOUNT` | `AMOUNT > 0` — no zero or negative transaction amounts |
| NOT NULL | All columns except `DESCRIPTION` | Required fields |
| INDEX | `IDX_TXN_ACCOUNT_NUMBER` | On `(ACCOUNT_NUMBER, TRANSACTION_DATE)` for history queries |
| INDEX | `IDX_TXN_REFERENCE` | On `REFERENCE_NUMBER` for reference lookups |

## Table: REFERENCE_SEQUENCE

| Constraint | Column | Rule |
|---|---|---|
| PRIMARY KEY | `REF_ID` | Auto-increment integer — acts as a sequence generator |

## Critical: Foreign Key Enforcement

SQLite does **not** enforce foreign keys by default. The Spring Boot datasource must enable them per connection:

```properties
spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON
```

This is configured in `modern/backend/src/main/resources/application.properties`.

## Application-Layer Business Rules (enforced in service code, not DB)

These rules go beyond what the DB enforces:

| Rule | Source |
|---|---|
| Amount > 0, ≤ 10,000,000.00, max 2 decimal places | `BankUtil.parseAmount()` |
| Account number: alphanumeric, 1–20 chars | `BankUtil.checkAccountNumber()` |
| No overdraft on withdrawal/transfer | `WithdrawalFrame` / `TransferFrame` |
| Source ≠ destination on transfer | `TransferFrame` |
| Account must be `ACTIVE` for all transactions | All transaction frames |
| Atomic transactions with `BEGIN IMMEDIATE` | All transaction frames |
