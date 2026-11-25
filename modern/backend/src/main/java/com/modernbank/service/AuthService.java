package com.modernbank.service;

import com.modernbank.dto.LoginRequest;
import com.modernbank.dto.LoginResponse;
import com.modernbank.exception.BankingException;
import com.modernbank.model.Employee;
import com.modernbank.repository.EmployeeRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-hours:8}")
    private int expirationHours;

    public AuthService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository
                .findByUsernameAndStatus(request.getUsername(), "ACTIVE")
                .orElseThrow(() -> new BankingException("Invalid username or password."));

        if (!passwordEncoder.matches(request.getPassword(), employee.getPassword())) {
            throw new BankingException("Invalid username or password.");
        }

        String token = generateToken(employee);
        return new LoginResponse(token, employee.getUsername(), employee.getEmployeeName());
    }

    private String generateToken(Employee employee) {
        try {
            long now = System.currentTimeMillis();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(employee.getUsername())
                    .claim("name", employee.getEmployeeName())
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + (long) expirationHours * 3600 * 1000))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(jwtSecret.getBytes()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT", e);
        }
    }
}
