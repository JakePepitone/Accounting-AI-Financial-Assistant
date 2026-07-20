package com.accountingai.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * <p>The parser is deliberately forgiving: it NEVER throws. When a field is
 * missing or malformed it simply falls back to a sensible default (null dates,
 * {@link BigDecimal#ZERO} balances, empty transaction list) and returns whatever
 * it managed to recover.</p>
 */
public class StatementParser {

    private static final String MONTH_NAME =
            "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|"
                    + "Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|"
                    + "Nov(?:ember)?|Dec(?:ember)?)";
    private static final String DATE_TOKEN =
            "(?:\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}/\\d{1,2}/\\d{2,4}|"
                    + MONTH_NAME + "\\s+\\d{1,2},\\s+\\d{4})";
    private static final String MONEY_TOKEN =
            "(?:\\(?\\s*-?\\$?\\s*\\d[\\d,]*(?:\\.\\d{2})?\\s*\\)?|-?\\$?\\s*\\d[\\d,]*(?:\\.\\d{2})?)";

    private static final Pattern DATE = Pattern.compile(DATE_TOKEN, Pattern.CASE_INSENSITIVE);

    // Balance labels -> each captures a signed decimal number.
    private static final Pattern BEGINNING =
            Pattern.compile("(?i)beginning\\s+balance\\s*:?\\s*(" + MONEY_TOKEN + ")");
    private static final Pattern DEPOSITS =
            Pattern.compile("(?i)total\\s+deposits\\s*:?\\s*(" + MONEY_TOKEN + ")");
    private static final Pattern WITHDRAWALS =
            Pattern.compile("(?i)total\\s+withdrawals\\s*:?\\s*(" + MONEY_TOKEN + ")");
    private static final Pattern ENDING =
            Pattern.compile("(?i)ending\\s+balance\\s*:?\\s*(" + MONEY_TOKEN + ")");

    // A transaction line: <date> <description...> <amount at end of line>.
    // e.g. "2026-06-03 Amazon Purchase -125.99"
    private static final Pattern TXN_LINE = Pattern.compile(
            "^\\s*(" + DATE_TOKEN + ")\\s+(.+?)\\s+(" + MONEY_TOKEN + ")\\s*$",
            Pattern.CASE_INSENSITIVE);

    // Account labels.
    private static final Pattern CUSTOMER =
            Pattern.compile("(?i)customer(?:\\s+name)?\\s*:?\\s*(.+)");
    private static final Pattern ACCOUNT_NUMBER =
            Pattern.compile("(?i)account\\s+number\\s*:?\\s*([\\w-]+)");

    private static final DateTimeFormatter MONTH_DAY_YEAR =
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US);
    private static final DateTimeFormatter MONTH_DAY_YEAR_FULL =
            DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);

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

        parsePeriod(text, statement);

        // --- Balances ---
        statement.setBeginningBalance(findMoney(BEGINNING, text));
        statement.setTotalDeposits(findMoney(DEPOSITS, text));
        statement.setTotalWithdrawals(findMoney(WITHDRAWALS, text).abs());
        statement.setEndingBalance(findMoney(ENDING, text));

        // --- Transactions (scan line by line) ---
        List<Transaction> transactions = new ArrayList<>();
        String[] lines = text.split("\\R");
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
        String trimmed = raw.trim();
        boolean parenthesesNegative = trimmed.startsWith("(") && trimmed.endsWith(")");
        String cleaned = trimmed.replace("$", "")
                .replace(",", "")
                .replace("(", "")
                .replace(")", "")
                .replace(" ", "")
                .trim();
        if (cleaned.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal parsed = new BigDecimal(cleaned);
            return parenthesesNegative && parsed.compareTo(BigDecimal.ZERO) > 0
                    ? parsed.negate()
                    : parsed;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void parsePeriod(String text, Statement statement) {
        for (String line : text.split("\\R")) {
            if (line.toLowerCase(Locale.ROOT).contains("period")) {
                List<LocalDate> dates = findDates(line);
                if (dates.size() >= 2) {
                    statement.setPeriodStart(dates.get(0));
                    statement.setPeriodEnd(dates.get(1));
                    return;
                }
            }
        }

        List<LocalDate> dates = findDates(text);
        if (dates.size() >= 2) {
            statement.setPeriodStart(dates.get(0));
            statement.setPeriodEnd(dates.get(1));
        }
    }

    private List<LocalDate> findDates(String text) {
        List<LocalDate> dates = new ArrayList<>();
        Matcher matcher = DATE.matcher(text);
        while (matcher.find()) {
            LocalDate date = parseDateOrNull(matcher.group());
            if (date != null) {
                dates.add(date);
            }
        }
        return dates;
    }

    /** Parses a common statement date format, returning null (rather than throwing) on failure. */
    private LocalDate parseDateOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replaceAll("\\s+", " ");
        Matcher shortYear = Pattern.compile("^(\\d{1,2}/\\d{1,2}/)(\\d{2})$").matcher(value);
        if (shortYear.matches()) {
            value = shortYear.group(1) + "20" + shortYear.group(2);
        }
        for (DateTimeFormatter formatter : dateFormattersFor(value)) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next known format.
            }
        }
        return null;
    }

    private List<DateTimeFormatter> dateFormattersFor(String value) {
        List<DateTimeFormatter> formatters = new ArrayList<>();
        formatters.add(DateTimeFormatter.ISO_LOCAL_DATE);
        formatters.add(DateTimeFormatter.ofPattern("uuuu/M/d"));

        formatters.add(DateTimeFormatter.ofPattern("M/d/uuuu"));
        formatters.add(MONTH_DAY_YEAR);
        formatters.add(MONTH_DAY_YEAR_FULL);
        return formatters;
    }
}
