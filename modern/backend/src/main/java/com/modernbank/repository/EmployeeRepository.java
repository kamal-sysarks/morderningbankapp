package com.modernbank.repository;

import com.modernbank.model.Employee;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbc;

    public EmployeeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Employee> mapper = (rs, row) -> {
        Employee e = new Employee();
        e.setEmployeeId(rs.getLong("EMPLOYEE_ID"));
        e.setUsername(rs.getString("USERNAME"));
        e.setPassword(rs.getString("PASSWORD"));
        e.setEmployeeName(rs.getString("EMPLOYEE_NAME"));
        e.setStatus(rs.getString("STATUS"));
        return e;
    };

    public Optional<Employee> findByUsernameAndStatus(String username, String status) {
        var rows = jdbc.query(
                "SELECT EMPLOYEE_ID, USERNAME, PASSWORD, EMPLOYEE_NAME, STATUS " +
                "FROM EMPLOYEES WHERE USERNAME = ? AND STATUS = ?",
                mapper, username, status);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
