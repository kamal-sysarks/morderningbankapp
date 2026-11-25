package com.modernbank.repository;

import com.modernbank.model.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CustomerRepository {

    private final JdbcTemplate jdbc;

    public CustomerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Customer> mapper = (rs, row) -> {
        Customer c = new Customer();
        c.setCustomerId(rs.getLong("CUSTOMER_ID"));
        c.setCustomerName(rs.getString("CUSTOMER_NAME"));
        c.setEmail(rs.getString("EMAIL"));
        c.setPhone(rs.getString("PHONE"));
        c.setAddress(rs.getString("ADDRESS"));
        return c;
    };

    public Optional<Customer> findById(Long customerId) {
        var rows = jdbc.query(
                "SELECT CUSTOMER_ID, CUSTOMER_NAME, EMAIL, PHONE, ADDRESS FROM CUSTOMERS WHERE CUSTOMER_ID = ?",
                mapper, customerId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<Customer> findByAccountNumber(String accountNumber) {
        var rows = jdbc.query(
                "SELECT C.CUSTOMER_ID, C.CUSTOMER_NAME, C.EMAIL, C.PHONE, C.ADDRESS " +
                "FROM CUSTOMERS C, ACCOUNTS A " +
                "WHERE C.CUSTOMER_ID = A.CUSTOMER_ID AND A.ACCOUNT_NUMBER = ?",
                mapper, accountNumber);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long countAll() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM CUSTOMERS", Long.class);
        return count != null ? count : 0L;
    }
}
