package com.modernbank.dto;

import java.math.BigDecimal;

public class TransferResponse {
    private String referenceNumber;
    private BigDecimal fromBalance;
    private BigDecimal toBalance;
    private String message;

    public TransferResponse(String referenceNumber, BigDecimal fromBalance, BigDecimal toBalance, String message) {
        this.referenceNumber = referenceNumber;
        this.fromBalance = fromBalance;
        this.toBalance = toBalance;
        this.message = message;
    }

    public String getReferenceNumber() { return referenceNumber; }
    public BigDecimal getFromBalance() { return fromBalance; }
    public BigDecimal getToBalance() { return toBalance; }
    public String getMessage() { return message; }
}
