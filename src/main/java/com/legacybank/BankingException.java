package com.legacybank;

/**
 * Checked exception used for business-rule failures (insufficient funds,
 * inactive account, invalid input, ...). Its message is shown to the employee.
 */
public class BankingException extends Exception {

    private static final long serialVersionUID = 1L;

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
