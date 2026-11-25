package com.modernbank.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import com.modernbank.exception.BankingException;

class TransactionServiceValidationTest {

    @Test
    void validateAmount_valid() {
        BigDecimal result = TransactionService.validateAmount(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void validateAmount_zero_throws() {
        assertThrows(BankingException.class, () -> TransactionService.validateAmount(BigDecimal.ZERO));
    }

    @Test
    void validateAmount_negative_throws() {
        assertThrows(BankingException.class, () -> TransactionService.validateAmount(new BigDecimal("-5.00")));
    }

    @Test
    void validateAmount_tooManyDecimals_throws() {
        assertThrows(BankingException.class, () -> TransactionService.validateAmount(new BigDecimal("10.001")));
    }

    @Test
    void validateAmount_exceedsMax_throws() {
        assertThrows(BankingException.class, () -> TransactionService.validateAmount(new BigDecimal("10000001.00")));
    }

    @Test
    void validateAmount_exactMax_ok() {
        assertDoesNotThrow(() -> TransactionService.validateAmount(new BigDecimal("10000000.00")));
    }

    @Test
    void validateAccountNumber_valid() {
        assertEquals("1001000001", TransactionService.validateAccountNumber("1001000001"));
    }

    @Test
    void validateAccountNumber_blank_throws() {
        assertThrows(BankingException.class, () -> TransactionService.validateAccountNumber("  "));
    }

    @Test
    void validateAccountNumber_tooLong_throws() {
        assertThrows(BankingException.class, () -> TransactionService.validateAccountNumber("123456789012345678901"));
    }

    @Test
    void validateAccountNumber_specialChars_throws() {
        assertThrows(BankingException.class, () -> TransactionService.validateAccountNumber("ACC-001"));
    }
}
