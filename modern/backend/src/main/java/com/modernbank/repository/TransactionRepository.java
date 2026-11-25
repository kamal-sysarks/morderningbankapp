package com.modernbank.repository;

import com.modernbank.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Transaction> mapper = (rs, row) -> {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getLong("TRANSACTION_ID"));
        t.setAccountNumber(rs.getString("ACCOUNT_NUMBER"));
        t.setTransactionType(rs.getString("TRANSACTION_TYPE"));
        t.setAmount(rs.getBigDecimal("AMOUNT").setScale(2, java.math.RoundingMode.HALF_UP));
        t.setDescription(rs.getString("DESCRIPTION"));
        String dateStr = rs.getString("TRANSACTION_DATE");
        if (dateStr != null) {
            t.setTransactionDate(LocalDateTime.parse(dateStr, FMT));
        }
        t.setReferenceNumber(rs.getString("REFERENCE_NUMBER"));
        return t;
    };

    public void insert(String accountNumber, String type, BigDecimal amount, String description, String reference) {
        jdbc.update(
                "INSERT INTO TRANSACTIONS (ACCOUNT_NUMBER, TRANSACTION_TYPE, AMOUNT, DESCRIPTION, TRANSACTION_DATE, REFERENCE_NUMBER) " +
                "VALUES (?, ?, ?, ?, datetime('now', 'localtime'), ?)",
                accountNumber, type, amount, description, reference);
    }

    public List<Transaction> findByAccountNumber(String accountNumber, int offset, int limit) {
        return jdbc.query(
                "SELECT TRANSACTION_ID, ACCOUNT_NUMBER, TRANSACTION_TYPE, AMOUNT, DESCRIPTION, TRANSACTION_DATE, REFERENCE_NUMBER " +
                "FROM TRANSACTIONS WHERE ACCOUNT_NUMBER = ? ORDER BY TRANSACTION_DATE DESC LIMIT ? OFFSET ?",
                mapper, accountNumber, limit, offset);
    }

    public long countByAccountNumber(String accountNumber) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM TRANSACTIONS WHERE ACCOUNT_NUMBER = ?",
                Long.class, accountNumber);
        return count != null ? count : 0L;
    }

    public long countToday() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM TRANSACTIONS WHERE DATE(TRANSACTION_DATE) = DATE('now', 'localtime')",
                Long.class);
        return count != null ? count : 0L;
    }

    /** Insert a row into REFERENCE_SEQUENCE and return the next ID (called within a transaction). */
    public long nextRefId() {
        jdbc.update("INSERT INTO REFERENCE_SEQUENCE DEFAULT VALUES");
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        return id != null ? id : 1L;
    }
}
