package com.legacybank;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

/**
 * Unit tests for the input validation helpers (no database required).
 */
public class BankUtilTest {

    @Test
    public void parseAmountAcceptsValidValues() throws Exception {
        assertEquals(new BigDecimal("100.00"), BankUtil.parseAmount("100"));
        assertEquals(new BigDecimal("12.50"), BankUtil.parseAmount(" 12.5 "));
        assertEquals(new BigDecimal("0.01"), BankUtil.parseAmount("0.01"));
    }

    @Test
    public void parseAmountRejectsZeroNegativeAndGarbage() {
        String[] bad = { "0", "-5", "abc", "", "  ", null, "1.234", "99999999999" };
        for (int i = 0; i < bad.length; i++) {
            try {
                BankUtil.parseAmount(bad[i]);
                fail("Expected BankingException for: " + bad[i]);
            } catch (BankingException expected) {
                assertTrue(expected.getMessage().length() > 0);
            }
        }
    }

    @Test
    public void checkAccountNumber() throws Exception {
        assertEquals("1001000001", BankUtil.checkAccountNumber(" 1001000001 ", "account"));
        try {
            BankUtil.checkAccountNumber("10-01", "account");
            fail("expected exception");
        } catch (BankingException expected) {
            // ok
        }
        try {
            BankUtil.checkAccountNumber("", "account");
            fail("expected exception");
        } catch (BankingException expected) {
            // ok
        }
    }

    @Test
    public void formatMoney() {
        assertEquals("1,234.50", BankUtil.formatMoney(new BigDecimal("1234.5")));
        assertEquals("", BankUtil.formatMoney(null));
    }
}
