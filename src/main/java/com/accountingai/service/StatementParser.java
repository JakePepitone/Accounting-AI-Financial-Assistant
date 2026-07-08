package com.accountingai.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.accountingai.model.Account;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

/**
 * Parses the plain text of a bank statement (as produced by
 * {@link PdfTextExtractor}) into structured {@link Statement} and
 * {@link Account} objects.
 *
 * <p>Scope is intentionally limited to the "John Smith" sample statement format
 * used throughout the capstone. The parser is deliberately forgiving: it NEVER
 * throws. When a field is missing or malformed it simply falls back to a
 * sensible default (null dates, {@link BigDecimal#ZERO} balances, empty
 * transaction list) and returns whatever it managed to recover.</p>
 */
public class StatementParser {

    // A single ISO date like 2026-06-01.
    private static final Pattern DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    // "Period: 2026-06-01 to 2026-06-30" (labels are case-insensitive, "to"/"-" both allowed).
    private static final Pattern PERIOD = Pattern.compile(
            "(?i)period\\s*:?\\s*(\\d{4}-\\d{2}-\\d{2})\\s*(?:to|-|through)\\s*(\\d{4}-\\d{2}-\\d{2})");

    // Balance labels -> each captures a signed decimal number.
    private static final Pattern BEGINNING =
            Pattern.compile("(?i)beginning\\s+balance\\s*:?\\s*\\$?\\s*(-?\\d[\\d,]*\\.?\\d*)");
    private static final Pattern DEPOSITS =
            Pattern.compile("(?i)total\\s+deposits\\s*:?\\s*\\$?\\s*(-?\\d[\\d,]*\\.?\\d*)");
    private static final Pattern WITHDRAWALS =
            Pattern.compile("(?i)total\\s+withdrawals\\s*:?\\s*\\$?\\s*(-?\\d[\\d,]*\\.?\\d*)");
    private static final Pattern ENDING =
            Pattern.compile("(?i)ending\\s+balance\\s*:?\\s*\\$?\\s*(-?\\d[\\d,]*\\.?\\d*)");

    // A transaction line: <date> <description...> <amount at end of line>.
    // e.g. "2026-06-03 Amazon Purchase -125.99"
    private static final Pattern TXN_LINE = Pattern.compile(
            "^\\s*(\\d{4}-\\d{2}-\\d{2})\\s+(.+?)\\s+(-?\\$?\\d[\\d,]*\\.\\d{2})\\s*$");

    // Account labels.
    private static final Pattern CUSTOMER =
            Pattern.compile("(?i)customer(?:\\s+name)?\\s*:?\\s*(.+)");
    private static final Pattern ACCOUNT_NUMBER =
            Pattern.compile("(?i)account\\s+number\\s*:?\\s*([\\w-]+)");

    /**
     * Parses balances, the statement period, and all transaction lines out of
     * the given statement text.
     *
     * @param text raw statement text (may be null)
     * @return a populated {@link Statement}; missing pieces use defaults
     */
    public Statement parseStatement(String text) {
        Statement statement = new Statement();
        // Start with safe defaults so callers never see nulls where they expect numbers.
        statement.setBeginningBalance(BigDecimal.ZERO);
        statement.setTotalDeposits(BigDecimal.ZERO);
        statement.setTotalWithdrawals(BigDecimal.ZERO);
        statement.setEndingBalance(BigDecimal.ZERO);
        statement.setTransactions(new ArrayList<>());

        if (text == null || text.isBlank()) {
            return statement;
        }

        // --- Period dates ---
        Matcher periodMatcher = PERIOD.matcher(text);
        if (periodMatcher.find()) {
            statement.setPeriodStart(parseDateOrNull(periodMatcher.group(1)));
            statement.setPeriodEnd(parseDateOrNull(periodMatcher.group(2)));
        }

        // --- Balances ---
        statement.setBeginningBalance(findMoney(BEGINNING, text));
        statement.setTotalDeposits(findMoney(DEPOSITS, text));
        statement.setTotalWithdrawals(findMoney(WITHDRAWALS, text));
        statement.setEndingBalance(findMoney(ENDING, text));

        // --- Transactions (scan line by line) ---
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            Matcher m = TXN_LINE.matcher(line);
            if (m.matches()) {
                Transaction t = new Transaction();
                t.setDate(parseDateOrNull(m.group(1)));
                t.setDescription(m.group(2).trim());
                t.setAmount(parseMoney(m.group(3)));
                transactions.add(t);
            }
        }
        statement.setTransactions(transactions);

        return statement;
    }

    /**
     * Parses the customer name and account number from statement text.
     *
     * @param text raw statement text (may be null)
     * @return an {@link Account}; missing fields default to empty strings
     */
    public Account parseAccount(String text) {
        Account account = new Account();
        account.setCustomerName("");
        account.setAccountNumber("");

        if (text == null || text.isBlank()) {
            return account;
        }

        Matcher customerMatcher = CUSTOMER.matcher(text);
        if (customerMatcher.find()) {
            // Only keep the name portion up to the end of the line.
            String name = customerMatcher.group(1).trim();
            int newline = name.indexOf('\n');
            if (newline >= 0) {
                name = name.substring(0, newline).trim();
            }
            account.setCustomerName(name);
        }

        Matcher numberMatcher = ACCOUNT_NUMBER.matcher(text);
        if (numberMatcher.find()) {
            account.setAccountNumber(numberMatcher.group(1).trim());
        }

        return account;
    }

    // ---------------------------------------------------------------------
    // Small private helpers
    // ---------------------------------------------------------------------

    /** Runs a money regex over the text and returns the first match, or ZERO. */
    private BigDecimal findMoney(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return parseMoney(m.group(1));
        }
        return BigDecimal.ZERO;
    }

    /** Converts a captured money string (may include $ and commas) to BigDecimal; ZERO on failure. */
    private BigDecimal parseMoney(String raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        String cleaned = raw.replace("$", "").replace(",", "").trim();
        if (cleaned.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** Parses an ISO date, returning null (rather than throwing) on failure. */
    private LocalDate parseDateOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        // Guard: make sure it actually looks like a date before parsing.
        Matcher m = DATE.matcher(raw);
        if (!m.find()) {
            return null;
        }
        try {
            return LocalDate.parse(m.group(1));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
