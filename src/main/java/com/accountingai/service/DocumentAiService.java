package com.accountingai.service;

import com.accountingai.model.DocumentAiAnalysis;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local AI-style document analysis for imported financial PDFs.
 *
 * <p>This service deliberately avoids network calls so imported financial data
 * stays on the user's machine. It combines the existing parsed statement with
 * lightweight natural-language heuristics to produce basic semantic metadata
 * and a concise document summary.</p>
 */
public class DocumentAiService {

    private static final Pattern CUSTOMER = Pattern.compile(
            "(?im)^\\s*customer(?:\\s+name)?\\s*:?\\s*(.+?)\\s*$");
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile(
            "(?im)^\\s*account\\s+number\\s*:?\\s*([\\w-]+)\\s*$");
    private static final Pattern DATE = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    private static final Set<String> STOP_WORDS = new HashSet<>(Set.of(
            "the", "and", "for", "with", "from", "this", "that", "your",
            "you", "are", "was", "were", "will", "have", "has", "had",
            "statement", "account", "number", "customer", "period", "total",
            "balance", "beginning", "ending", "transactions", "transaction"));

    /**
     * Generates semantic metadata and a summary for extracted document text.
     *
     * @param text      raw extracted PDF text; may be null
     * @param statement parsed statement; may be null or partially populated
     * @return a non-null AI analysis result
     */
    public DocumentAiAnalysis analyze(String text, Statement statement) {
        String safeText = text == null ? "" : text;
        String documentType = classifyDocument(safeText, statement);
        String extractedMetadata = buildMetadata(safeText, statement, documentType);
        String summary = summarize(safeText, statement, documentType);
        return new DocumentAiAnalysis(documentType, extractedMetadata, summary, LocalDateTime.now());
    }

    private String classifyDocument(String text, Statement statement) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("bank statement") || hasStatementShape(statement)
                || (lower.contains("ending balance") && lower.contains("transactions"))) {
            return "BANK_STATEMENT";
        }
        if (lower.contains("invoice")) {
            return "INVOICE";
        }
        if (lower.contains("receipt")) {
            return "RECEIPT";
        }
        if (lower.contains("balance") || lower.contains("payment") || lower.contains("deposit")) {
            return "FINANCIAL_DOCUMENT";
        }
        return "DOCUMENT";
    }

    private String buildMetadata(String text, Statement statement, String documentType) {
        List<String> entries = new ArrayList<>();
        entries.add("document_type=" + documentType);

        addIfPresent(entries, "customer_name", findFirst(CUSTOMER, text));
        addIfPresent(entries, "account_number", findFirst(ACCOUNT_NUMBER, text));

        if (statement != null) {
            addIfPresent(entries, "period_start", formatDate(statement.getPeriodStart()));
            addIfPresent(entries, "period_end", formatDate(statement.getPeriodEnd()));
            entries.add("transaction_count=" + transactionCount(statement));
            addIfPresent(entries, "total_deposits", formatMoneyOrNull(statement.getTotalDeposits()));
            addIfPresent(entries, "total_withdrawals", formatMoneyOrNull(statement.getTotalWithdrawals()));
            addIfPresent(entries, "ending_balance", formatMoneyOrNull(statement.getEndingBalance()));

            largestPositive(statement).ifPresent(t ->
                    entries.add("largest_inflow=" + t.getDescription() + " " + formatMoney(t.getAmount())));
            largestNegative(statement).ifPresent(t ->
                    entries.add("largest_outflow=" + t.getDescription() + " " + formatMoney(t.getAmount())));
        }

        addIfPresent(entries, "first_detected_date", firstDate(text));
        entries.add("line_count=" + countLines(text));
        entries.add("word_count=" + countWords(text));
        addIfPresent(entries, "keywords", keywords(text, 8));

        return String.join("\n", entries);
    }

    private String summarize(String text, Statement statement, String documentType) {
        if (hasStatementShape(statement)) {
            int count = transactionCount(statement);
            StringBuilder sb = new StringBuilder();
            sb.append(labelFor(documentType));

            String period = periodLabel(statement);
            if (!period.isBlank()) {
                sb.append(" for ").append(period);
            }

            sb.append(" contains ").append(count).append(count == 1 ? " transaction" : " transactions");

            String ending = formatMoneyOrNull(statement.getEndingBalance());
            String deposits = formatMoneyOrNull(statement.getTotalDeposits());
            String withdrawals = formatMoneyOrNull(statement.getTotalWithdrawals());
            if (ending != null || deposits != null || withdrawals != null) {
                sb.append(". ");
                if (ending != null) {
                    sb.append("Ending balance is ").append(ending);
                }
                if (deposits != null || withdrawals != null) {
                    if (ending != null) {
                        sb.append(" after ");
                    }
                    if (deposits != null) {
                        sb.append(deposits).append(" in deposits");
                    }
                    if (deposits != null && withdrawals != null) {
                        sb.append(" and ");
                    }
                    if (withdrawals != null) {
                        sb.append(withdrawals).append(" in withdrawals");
                    }
                }
            }

            largestPositive(statement).ifPresent(t ->
                    sb.append(". Largest inflow: ").append(t.getDescription())
                            .append(" (").append(formatMoney(t.getAmount())).append(")"));
            largestNegative(statement).ifPresent(t ->
                    sb.append(". Largest outflow: ").append(t.getDescription())
                            .append(" (").append(formatMoney(t.getAmount())).append(")"));
            sb.append('.');
            return sb.toString();
        }

        List<String> lines = significantLines(text, 3);
        if (!lines.isEmpty()) {
            return labelFor(documentType) + " summary: " + String.join(" ", lines);
        }
        return "No readable document text was available for summarization.";
    }

    private boolean hasStatementShape(Statement statement) {
        if (statement == null) {
            return false;
        }
        return statement.getPeriodStart() != null
                || statement.getPeriodEnd() != null
                || nonZero(statement.getBeginningBalance())
                || nonZero(statement.getTotalDeposits())
                || nonZero(statement.getTotalWithdrawals())
                || nonZero(statement.getEndingBalance())
                || transactionCount(statement) > 0;
    }

    private int transactionCount(Statement statement) {
        return statement == null || statement.getTransactions() == null
                ? 0
                : statement.getTransactions().size();
    }

    private Optional<Transaction> largestPositive(Statement statement) {
        if (statement == null || statement.getTransactions() == null) {
            return Optional.empty();
        }
        return statement.getTransactions().stream()
                .filter(t -> t.getAmount() != null && t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(Transaction::getAmount));
    }

    private Optional<Transaction> largestNegative(Statement statement) {
        if (statement == null || statement.getTransactions() == null) {
            return Optional.empty();
        }
        return statement.getTransactions().stream()
                .filter(t -> t.getAmount() != null && t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .min(Comparator.comparing(Transaction::getAmount));
    }

    private static boolean nonZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) != 0;
    }

    private static void addIfPresent(List<String> entries, String key, String value) {
        if (value != null && !value.isBlank()) {
            entries.add(key + "=" + value.trim());
        }
    }

    private static String findFirst(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String firstDate(String text) {
        Matcher matcher = DATE.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private static String periodLabel(Statement statement) {
        String start = formatDate(statement.getPeriodStart());
        String end = formatDate(statement.getPeriodEnd());
        if (start != null && end != null) {
            return start + " to " + end;
        }
        if (start != null) {
            return "starting " + start;
        }
        if (end != null) {
            return "ending " + end;
        }
        return "";
    }

    private static String formatMoneyOrNull(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return formatMoney(value);
    }

    private static String formatMoney(BigDecimal value) {
        return MONEY.format(value);
    }

    private static String labelFor(String documentType) {
        if ("BANK_STATEMENT".equals(documentType)) {
            return "Bank statement";
        }
        if ("FINANCIAL_DOCUMENT".equals(documentType)) {
            return "Financial document";
        }
        if ("INVOICE".equals(documentType)) {
            return "Invoice";
        }
        if ("RECEIPT".equals(documentType)) {
            return "Receipt";
        }
        return "Document";
    }

    private static int countLines(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.split("\\R").length;
    }

    private static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String token : text.split("[^A-Za-z0-9]+")) {
            if (!token.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static List<String> significantLines(String text, int limit) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (!line.isBlank()) {
                lines.add(line);
            }
            if (lines.size() >= limit) {
                break;
            }
        }
        return lines;
    }

    private static String keywords(String text, int limit) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Map<String, Integer> counts = new HashMap<>();
        for (String raw : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (raw.length() < 3 || STOP_WORDS.contains(raw) || raw.chars().allMatch(Character::isDigit)) {
                continue;
            }
            counts.merge(raw, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }
}
