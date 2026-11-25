package com.modernbank.controller;

import com.modernbank.dto.CustomerWithAccounts;
import com.modernbank.dto.DashboardSummary;
import com.modernbank.model.Account;
import com.modernbank.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> dashboardSummary() {
        return ResponseEntity.ok(customerService.getDashboardSummary());
    }

    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CustomerWithAccounts> getCustomerById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @GetMapping("/customers/by-account/{accountNumber}")
    public ResponseEntity<CustomerWithAccounts> getCustomerByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(customerService.getCustomerByAccountNumber(accountNumber));
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<Account> getAccountDetails(@PathVariable String accountNumber) {
        return ResponseEntity.ok(customerService.getAccountDetails(accountNumber));
    }
}
