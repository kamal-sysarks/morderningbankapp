package com.legacybank;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;

/**
 * Static helper methods shared by the screens.
 */
public class BankUtil {

    /** Largest single deposit / withdrawal / transfer accepted by the application. */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000.00");

    /** Parses and validates a monetary amount typed by an employee. */
    public static BigDecimal parseAmount(String text) throws BankingException {
        if (text == null || text.trim().length() == 0) {
            throw new BankingException("Please enter an amount.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new BankingException("Amount must be a valid number.");
        }
        if (amount.signum() <= 0) {
            throw new BankingException("Amount must be greater than zero.");
        }
        if (amount.scale() > 2 && amount.stripTrailingZeros().scale() > 2) {
            throw new BankingException("Amount cannot have more than two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BankingException("Amount cannot exceed " + formatMoney(MAX_AMOUNT) + " per transaction.");
        }
        return amount.setScale(2, BigDecimal.ROUND_UNNECESSARY);
    }

    /** Validates and trims an account number (1 to 20 letters/digits). */
    public static String checkAccountNumber(String text, String fieldName) throws BankingException {
        if (text == null || text.trim().length() == 0) {
            throw new BankingException("Please enter the " + fieldName + ".");
        }
        String value = text.trim();
        if (!value.matches("[A-Za-z0-9]{1,20}")) {
            throw new BankingException("The " + fieldName + " may only contain letters and digits (max 20).");
        }
        return value;
    }

    public static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return new DecimalFormat("#,##0.00").format(value);
    }

    /**
     * Reads a money column. SQLite stores NUMERIC values as 64-bit floating point when they have
     * decimals, so the value is rounded back to exactly two decimal places.
     */
    public static BigDecimal getMoney(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        if (value == null) {
            return new BigDecimal("0.00");
        }
        return value.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Returns the next reference number such as WDR0000000007. SQLite has no sequences, so a row is
     * inserted into REFERENCE_SEQUENCE and its generated key is used (it is part of the caller's
     * transaction).
     */
    public static String nextReference(Connection conn, String prefix) throws SQLException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            st.executeUpdate("INSERT INTO REFERENCE_SEQUENCE DEFAULT VALUES");
            rs = st.executeQuery("SELECT last_insert_rowid()");
            rs.next();
            long id = rs.getLong(1);
            String digits = String.valueOf(id);
            StringBuffer sb = new StringBuffer(prefix);
            for (int i = digits.length(); i < 10; i++) {
                sb.append('0');
            }
            return sb.append(digits).toString();
        } finally {
            DBConnection.close(rs, st);
        }
    }

    /** Inserts one row into TRANSACTIONS using the caller's connection / transaction. */
    public static void insertTransaction(Connection conn, String accountNumber, String type,
            BigDecimal amount, String description, String reference) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(
                    "INSERT INTO TRANSACTIONS (ACCOUNT_NUMBER, TRANSACTION_TYPE, AMOUNT, "
                    + "DESCRIPTION, TRANSACTION_DATE, REFERENCE_NUMBER) "
                    + "VALUES (?, ?, ?, ?, datetime('now', 'localtime'), ?)");
            ps.setString(1, accountNumber);
            ps.setString(2, type);
            ps.setBigDecimal(3, amount);
            ps.setString(4, description);
            ps.setString(5, reference);
            ps.executeUpdate();
        } finally {
            DBConnection.close(ps);
        }
    }

    /** Updates an account balance using the caller's connection / transaction. */
    public static void updateBalance(Connection conn, String accountNumber, BigDecimal newBalance)
            throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("UPDATE ACCOUNTS SET BALANCE = ? WHERE ACCOUNT_NUMBER = ?");
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountNumber);
            ps.executeUpdate();
        } finally {
            DBConnection.close(ps);
        }
    }
}
