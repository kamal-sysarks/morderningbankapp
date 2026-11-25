package com.legacybank;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests against a real SQLite database file that is created from the scripts in
 * /database by DatabaseInitializer. A temporary file is used, so the application database is
 * never touched.
 */
public class DatabaseIntegrationTest {

    private File dbFile;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        dbFile = File.createTempFile("legacybank-test", ".db");
        System.setProperty("db.url", "jdbc:sqlite:" + dbFile.getAbsolutePath());
        DBConnection.reset();
        DatabaseInitializer.initializeIfNeeded();
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false);
    }

    @After
    public void tearDown() {
        DBConnection.rollback(conn);
        DBConnection.close(conn);
        System.clearProperty("db.url");
        DBConnection.reset();
        dbFile.delete();
    }

    private int count(String sql, String... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setString(i + 1, params[i]);
        }
        ResultSet rs = ps.executeQuery();
        rs.next();
        int n = rs.getInt(1);
        DBConnection.close(rs, ps);
        return n;
    }

    private BigDecimal balanceOf(String accountNumber) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT BALANCE FROM ACCOUNTS WHERE ACCOUNT_NUMBER = ?");
        ps.setString(1, accountNumber);
        ResultSet rs = ps.executeQuery();
        rs.next();
        BigDecimal b = BankUtil.getMoney(rs, "BALANCE");
        DBConnection.close(rs, ps);
        return b;
    }

    @Test
    public void adminEmployeeExists() throws SQLException {
        assertEquals(1, count("SELECT COUNT(*) FROM EMPLOYEES WHERE USERNAME = ? AND PASSWORD = ? AND STATUS = 'ACTIVE'",
                "admin", "admin123"));
    }

    @Test
    public void seedDataVolumes() throws SQLException {
        assertEquals(10, count("SELECT COUNT(*) FROM CUSTOMERS"));
        assertEquals(15, count("SELECT COUNT(*) FROM ACCOUNTS"));
        assertTrue(count("SELECT COUNT(*) FROM TRANSACTIONS") > 0);
    }

    @Test
    public void initializeIsIdempotent() throws Exception {
        conn.commit();
        DatabaseInitializer.initializeIfNeeded();
        assertEquals(10, count("SELECT COUNT(*) FROM CUSTOMERS"));
    }

    @Test
    public void accountsExposeExpectedSeedBalances() throws SQLException {
        assertEquals(new BigDecimal("15250.75"), balanceOf("1001000001"));
        assertEquals(new BigDecimal("560.35"), balanceOf("1001000010"));
    }

    @Test
    public void transferIsAtomicOnCommit() throws Exception {
        BigDecimal fromBefore = balanceOf("1001000001");
        BigDecimal toBefore = balanceOf("1001000003");
        BigDecimal amount = new BigDecimal("250.10");

        String ref = BankUtil.nextReference(conn, "TRF");
        BankUtil.updateBalance(conn, "1001000001", fromBefore.subtract(amount));
        BankUtil.updateBalance(conn, "1001000003", toBefore.add(amount));
        BankUtil.insertTransaction(conn, "1001000001", "TRANSFER_DEBIT", amount, "test", ref);
        BankUtil.insertTransaction(conn, "1001000003", "TRANSFER_CREDIT", amount, "test", ref);
        conn.commit();

        assertEquals(fromBefore.subtract(amount), balanceOf("1001000001"));
        assertEquals(toBefore.add(amount), balanceOf("1001000003"));
        assertEquals(2, count("SELECT COUNT(*) FROM TRANSACTIONS WHERE REFERENCE_NUMBER = ?", ref));
    }

    @Test
    public void transferRollsBackEverythingOnFailure() throws Exception {
        BigDecimal fromBefore = balanceOf("1001000001");
        BigDecimal toBefore = balanceOf("1001000003");
        int txnBefore = count("SELECT COUNT(*) FROM TRANSACTIONS");
        BigDecimal amount = new BigDecimal("100.00");
        try {
            String ref = BankUtil.nextReference(conn, "TRF");
            BankUtil.updateBalance(conn, "1001000001", fromBefore.subtract(amount));
            BankUtil.updateBalance(conn, "1001000003", toBefore.add(amount));
            BankUtil.insertTransaction(conn, "1001000001", "TRANSFER_DEBIT", amount, "test", ref);
            // credit leg fails: unknown account violates the foreign key
            BankUtil.insertTransaction(conn, "9999999999", "TRANSFER_CREDIT", amount, "test", ref);
            fail("expected foreign key violation");
        } catch (SQLException expected) {
            DBConnection.rollback(conn);
        }
        assertEquals(fromBefore, balanceOf("1001000001"));
        assertEquals(toBefore, balanceOf("1001000003"));
        assertEquals(txnBefore, count("SELECT COUNT(*) FROM TRANSACTIONS"));
    }

    @Test
    public void referenceNumbersAreUniqueAndIncreasing() throws Exception {
        String a = BankUtil.nextReference(conn, "DEP");
        String b = BankUtil.nextReference(conn, "DEP");
        assertEquals(13, a.length());
        assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void balanceCheckConstraintPreventsOverdraft() throws SQLException {
        try {
            BankUtil.updateBalance(conn, "1001000010", new BigDecimal("-0.01"));
            fail("expected check constraint violation");
        } catch (SQLException expected) {
            assertTrue(expected.getMessage().toUpperCase().contains("CHECK"));
        }
    }

    @Test
    public void historyQueryReturnsNewestFirst() throws Exception {
        PreparedStatement ps = conn.prepareStatement(
                "SELECT TRANSACTION_ID FROM TRANSACTIONS WHERE ACCOUNT_NUMBER = ? "
                + "ORDER BY TRANSACTION_DATE DESC, TRANSACTION_ID DESC");
        ps.setString(1, "1001000001");
        ResultSet rs = ps.executeQuery();
        assertTrue(rs.next());
        long first = rs.getLong(1);
        assertTrue(rs.next());
        assertTrue(first > rs.getLong(1)); // deposit (60 days old) is older than the withdrawal
        DBConnection.close(rs, ps);
    }
}
