package com.modernbank.dto;

public class DashboardSummary {
    private long totalCustomers;
    private long activeAccounts;
    private long todayTransactions;

    public DashboardSummary(long totalCustomers, long activeAccounts, long todayTransactions) {
        this.totalCustomers = totalCustomers;
        this.activeAccounts = activeAccounts;
        this.todayTransactions = todayTransactions;
    }

    public long getTotalCustomers() { return totalCustomers; }
    public long getActiveAccounts() { return activeAccounts; }
    public long getTodayTransactions() { return todayTransactions; }
}
