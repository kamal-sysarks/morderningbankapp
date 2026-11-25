package com.modernbank.service;

import com.modernbank.dto.CustomerWithAccounts;
import com.modernbank.dto.DashboardSummary;
import com.modernbank.exception.ResourceNotFoundException;
import com.modernbank.model.Account;
import com.modernbank.model.Customer;
import com.modernbank.repository.AccountRepository;
import com.modernbank.repository.CustomerRepository;
import com.modernbank.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public CustomerService(CustomerRepository customerRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public CustomerWithAccounts getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("No customer found."));
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return new CustomerWithAccounts(customer, accounts);
    }

    public CustomerWithAccounts getCustomerByAccountNumber(String accountNumber) {
        Customer customer = customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No customer found."));
        List<Account> accounts = accountRepository.findByCustomerId(customer.getCustomerId());
        return new CustomerWithAccounts(customer, accounts);
    }

    public Account getAccountDetails(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account " + accountNumber + " does not exist."));
    }

    public DashboardSummary getDashboardSummary() {
        long totalCustomers = customerRepository.countAll();
        long activeAccounts = accountRepository.countActive();
        long todayTransactions = transactionRepository.countToday();
        return new DashboardSummary(totalCustomers, activeAccounts, todayTransactions);
    }
}
