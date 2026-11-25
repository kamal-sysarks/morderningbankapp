package com.modernbank.dto;

import java.math.BigDecimal;

public class TransactionResponse {
    private String referenceNumber;
    private BigDecimal newBalance;
    private String message;

    public TransactionResponse(String referenceNumber, BigDecimal newBalance, String message) {
        this.referenceNumber = referenceNumber;
        this.newBalance = newBalance;
        this.message = message;
    }

    public String getReferenceNumber() { return referenceNumber; }
    public BigDecimal getNewBalance() { return newBalance; }
    public String getMessage() { return message; }
}
