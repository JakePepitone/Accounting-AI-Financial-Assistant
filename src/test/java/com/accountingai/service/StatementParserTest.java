package com.accountingai.service;

import com.accountingai.model.Account;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link StatementParser}.
 *
 * <p>Uses a hard-coded sample statement string (same content that
 * {@code TestPdfs.writeSampleStatement} embeds in a PDF) so parsing can be tested
 * without the PDF round-trip. Verifies period dates, the four balances, the seven
 * transactions, and account name/number extraction. The parser must never throw.</p>
 */
class StatementParserTest {

    /** The canonical sample statement text, matching the John Smith fixture. */
    private static final String SAMPLE_TEXT =
            "Bank Statement\n"
            + "Customer: John Smith\n"
            + "Account Number: 123456789\n"
            + "Period: 2026-06-01 to 2026-06-30\n"
            + "Beginning Balance: 5000.00\n"
            + "Total Deposits: 2500.00\n"
            + "Total Withdrawals: 1273.56\n"
            + "Ending Balance: 6226.44\n"
            + "Transactions:\n"
            + "2026-06-01 Payroll Deposit 2500.00\n"
            + "2026-06-03 Amazon Purchase -125.99\n"
            + "2026-06-05 Utility Bill -85.12\n"
            + "2026-06-10 Restaurant -62.45\n"
            + "2026-06-15 Gas Station -48.00\n"
            + "2026-06-20 Grocery Store -152.00\n"
            + "2026-06-25 Internet Service -75.00\n";

    @Test
    void parsesPeriodDates() {
        StatementParser parser = new StatementParser();
        Statement s = parser.parseStatement(SAMPLE_TEXT);
        assertNotNull(s);
        assertEquals(LocalDate.parse("2026-06-01"), s.getPeriodStart());
        assertEquals(LocalDate.parse("2026-06-30"), s.getPeriodEnd());
    }

    @Test
    void parsesEndingBalance() {
        StatementParser parser = new StatementParser();
        Statement s = parser.parseStatement(SAMPLE_TEXT);
        assertEquals(0, new BigDecimal("6226.44").compareTo(s.getEndingBalance()),
                "Ending balance should parse to 6226.44");
    }

    @Test
    void parsesAllSevenTransactionsIncludingSignedAmounts() {
        StatementParser parser = new StatementParser();
        Statement s = parser.parseStatement(SAMPLE_TEXT);

        List<Transaction> txns = s.getTransactions();
        assertEquals(7, txns.size(), "Should parse exactly 7 transaction lines.");

        Transaction amazon = txns.stream()
                .filter(t -> t.getDescription().contains("Amazon"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Amazon transaction not parsed"));
        assertEquals(0, new BigDecimal("-125.99").compareTo(amazon.getAmount()));

        Transaction payroll = txns.stream()
                .filter(t -> t.getDescription().contains("Payroll"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Payroll transaction not parsed"));
        assertEquals(0, new BigDecimal("2500.00").compareTo(payroll.getAmount()));
    }

    @Test
    void parsesAccountNameAndNumber() {
        StatementParser parser = new StatementParser();
        Account a = parser.parseAccount(SAMPLE_TEXT);
        assertNotNull(a);
        assertEquals("John Smith", a.getCustomerName());
        assertEquals("123456789", a.getAccountNumber());
    }

    @Test
    void neverThrowsOnGarbageInput() {
        StatementParser parser = new StatementParser();
        // Empty / null / nonsense must return a partial (non-null) result, not throw.
        Statement empty = parser.parseStatement("");
        assertNotNull(empty);
        assertTrue(empty.getTransactions().isEmpty());

        Statement nullResult = parser.parseStatement(null);
        assertNotNull(nullResult);

        Account garbageAccount = parser.parseAccount("no labels here at all");
        assertNotNull(garbageAccount);
        assertNotNull(garbageAccount.getCustomerName(), "Name defaults to empty string, not null.");
        assertNotNull(garbageAccount.getAccountNumber());
    }
}
