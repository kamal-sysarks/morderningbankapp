package com.modernbank.dto;

import com.modernbank.model.Account;
import com.modernbank.model.Customer;
import java.util.List;

public class CustomerWithAccounts {
    private Customer customer;
    private List<Account> accounts;

    public CustomerWithAccounts(Customer customer, List<Account> accounts) {
        this.customer = customer;
        this.accounts = accounts;
    }

    public Customer getCustomer() { return customer; }
    public List<Account> getAccounts() { return accounts; }
}
