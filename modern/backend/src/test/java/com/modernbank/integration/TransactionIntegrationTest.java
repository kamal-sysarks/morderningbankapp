package com.modernbank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernbank.dto.TransactionRequest;
import com.modernbank.dto.TransferRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("DELETE FROM TRANSACTIONS");
        jdbc.update("DELETE FROM REFERENCE_SEQUENCE");
        jdbc.update("DELETE FROM ACCOUNTS");
        jdbc.update("DELETE FROM CUSTOMERS");

        jdbc.update("INSERT INTO CUSTOMERS (CUSTOMER_ID, CUSTOMER_NAME, EMAIL, PHONE, ADDRESS) VALUES (1001, 'John Anderson', 'j@example.com', '555-0101', 'Springfield')");
        jdbc.update("INSERT INTO ACCOUNTS (ACCOUNT_NUMBER, CUSTOMER_ID, ACCOUNT_TYPE, BALANCE, STATUS) VALUES ('1001000001', 1001, 'SAVINGS', 1000.00, 'ACTIVE')");
        jdbc.update("INSERT INTO ACCOUNTS (ACCOUNT_NUMBER, CUSTOMER_ID, ACCOUNT_TYPE, BALANCE, STATUS) VALUES ('1001000002', 1001, 'CHECKING', 500.00, 'ACTIVE')");
        jdbc.update("INSERT INTO ACCOUNTS (ACCOUNT_NUMBER, CUSTOMER_ID, ACCOUNT_TYPE, BALANCE, STATUS) VALUES ('1001000008', 1001, 'SAVINGS', 200.00, 'FROZEN')");

        token = generateToken("admin");
    }

    private String generateToken(String username) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .claim("name", "Branch Administrator")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(jwtSecret.getBytes()));
        return jwt.serialize();
    }

    @Test
    void deposit_success() throws Exception {
        TransactionRequest req = new TransactionRequest();
        req.setAccountNumber("1001000001");
        req.setAmount(new BigDecimal("250.00"));
        req.setDescription("Test deposit");

        mockMvc.perform(post("/api/transactions/deposit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber").value(org.hamcrest.Matchers.startsWith("DEP")))
                .andExpect(jsonPath("$.newBalance").value(1250.00));
    }

    @Test
    void deposit_frozenAccount_returns400() throws Exception {
        TransactionRequest req = new TransactionRequest();
        req.setAccountNumber("1001000008");
        req.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/transactions/deposit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdraw_insufficientFunds_returns400() throws Exception {
        TransactionRequest req = new TransactionRequest();
        req.setAccountNumber("1001000001");
        req.setAmount(new BigDecimal("9999.00"));

        mockMvc.perform(post("/api/transactions/withdraw")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient")));
    }

    @Test
    void transfer_success() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setFromAccount("1001000001");
        req.setToAccount("1001000002");
        req.setAmount(new BigDecimal("200.00"));

        mockMvc.perform(post("/api/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber").value(org.hamcrest.Matchers.startsWith("TRF")))
                .andExpect(jsonPath("$.fromBalance").value(800.00))
                .andExpect(jsonPath("$.toBalance").value(700.00));
    }

    @Test
    void transfer_sameAccount_returns400() throws Exception {
        TransferRequest req = new TransferRequest();
        req.setFromAccount("1001000001");
        req.setToAccount("1001000001");
        req.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transactionHistory_returnsPaged() throws Exception {
        mockMvc.perform(get("/api/accounts/1001000001/transactions?page=0&size=10")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void customerById_found() throws Exception {
        mockMvc.perform(get("/api/customers/1001")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.customerName").value("John Anderson"));
    }

    @Test
    void customerById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/customers/9999")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void accountDetails_found() throws Exception {
        mockMvc.perform(get("/api/accounts/1001000001")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1001000001"));
    }

    @Test
    void dashboardSummary_ok() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").isNumber())
                .andExpect(jsonPath("$.activeAccounts").isNumber());
    }
}
