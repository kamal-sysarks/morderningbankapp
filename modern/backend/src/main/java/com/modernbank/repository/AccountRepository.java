package com.modernbank.repository;

import com.modernbank.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Account> mapper = (rs, row) -> {
        Account a = new Account();
        a.setAccountId(rs.getLong("ACCOUNT_ID"));
        a.setAccountNumber(rs.getString("ACCOUNT_NUMBER"));
        a.setCustomerId(rs.getLong("CUSTOMER_ID"));
        a.setAccountType(rs.getString("ACCOUNT_TYPE"));
        a.setBalance(rs.getBigDecimal("BALANCE").setScale(2, java.math.RoundingMode.HALF_UP));
        a.setStatus(rs.getString("STATUS"));
        return a;
    };

    public Optional<Account> findByAccountNumber(String accountNumber) {
        var rows = jdbc.query(
                "SELECT ACCOUNT_ID, ACCOUNT_NUMBER, CUSTOMER_ID, ACCOUNT_TYPE, " +
                "IFNULL(BALANCE, 0) AS BALANCE, STATUS FROM ACCOUNTS WHERE ACCOUNT_NUMBER = ?",
                mapper, accountNumber);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Account> findByCustomerId(Long customerId) {
        return jdbc.query(
                "SELECT ACCOUNT_ID, ACCOUNT_NUMBER, CUSTOMER_ID, ACCOUNT_TYPE, " +
                "IFNULL(BALANCE, 0) AS BALANCE, STATUS FROM ACCOUNTS " +
                "WHERE CUSTOMER_ID = ? ORDER BY ACCOUNT_NUMBER",
                mapper, customerId);
    }

    public long countActive() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM ACCOUNTS WHERE STATUS = 'ACTIVE'", Long.class);
        return count != null ? count : 0L;
    }

    public void updateBalance(String accountNumber, java.math.BigDecimal newBalance) {
        jdbc.update("UPDATE ACCOUNTS SET BALANCE = ? WHERE ACCOUNT_NUMBER = ?", newBalance, accountNumber);
    }
}
