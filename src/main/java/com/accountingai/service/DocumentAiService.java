package com.accountingai.service;

import com.accountingai.model.DocumentAiAnalysis;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * AI-style document analysis for imported financial PDFs.
 *
 * <p>Local deterministic analysis is the default. If configured with
 * {@code ACCOUNTING_AI_AI_PROVIDER=openai} and an API key, this service attempts
 * an OpenAI-compatible remote analysis and falls back to local analysis on any
 * error so imports keep working.</p>
 */
public class DocumentAiService {

    private static final String LOCAL_PROVIDER = "LOCAL";
    private static final String LOCAL_FALLBACK_PROVIDER = "LOCAL_FALLBACK";
    private static final String LOCAL_MODEL = "HEURISTIC_V2";
    private static final int MAX_REMOTE_TEXT_CHARS = 12000;

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

    private final DocumentAiConfig config;
    private final HttpClient httpClient;

    /**
     * Creates the service using runtime config from system properties/env vars.
     */
    public DocumentAiService() {
        this(new DocumentAiConfig());
    }

    /**
     * Creates the service with explicit config.
     *
     * @param config AI runtime config
     */
    public DocumentAiService(DocumentAiConfig config) {
        this(config, null);
    }

    /**
     * Creates the service with explicit config and HTTP client.
     *
     * @param config     AI runtime config
     * @param httpClient HTTP client for remote AI calls
     */
    public DocumentAiService(DocumentAiConfig config, HttpClient httpClient) {
        this.config = config == null ? new DocumentAiConfig() : config;
        this.httpClient = httpClient;
    }

    /**
     * Generates semantic metadata and a summary for extracted document text.
     *
     * @param text      raw extracted PDF text; may be null
     * @param statement parsed statement; may be null or partially populated
     * @return a non-null AI analysis result
     */
    public DocumentAiAnalysis analyze(String text, Statement statement) {
        String safeText = text == null ? "" : text;
        if (config.isRemoteAiEnabled()) {
            try {
                return analyzeWithRemoteAi(safeText, statement);
            } catch (Exception e) {
                DocumentAiAnalysis fallback = localAnalysis(safeText, statement);
                fallback.setProvider(LOCAL_FALLBACK_PROVIDER);
                fallback.setExtractedMetadata(appendMetadata(
                        fallback.getExtractedMetadata(),
                        "ai_fallback_reason=" + sanitizeMetadataValue(e.getMessage())));
                return fallback;
            }
        }
        return localAnalysis(safeText, statement);
    }

    private DocumentAiAnalysis localAnalysis(String safeText, Statement statement) {
        String documentType = classifyDocument(safeText, statement);
        String extractedMetadata = buildMetadata(safeText, statement, documentType);
        String summary = summarize(safeText, statement, documentType);
        return new DocumentAiAnalysis(
                documentType, extractedMetadata, summary, LocalDateTime.now(),
                LOCAL_PROVIDER, LOCAL_MODEL);
    }

    private DocumentAiAnalysis analyzeWithRemoteAi(String text, Statement statement) throws Exception {
        String prompt = buildRemotePrompt(text, statement);
        String responseBody = sendRemoteRequest(prompt);
        String content = extractMessageContent(responseBody);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI response did not contain message content.");
        }

        DocumentAiAnalysis parsed = parseRemoteContent(content);
        parsed.setAnalyzedAt(LocalDateTime.now());
        parsed.setProvider("OPENAI");
        parsed.setModel(config.getModel());

        DocumentAiAnalysis local = localAnalysis(text, statement);
        if (parsed.getDocumentType() == null || parsed.getDocumentType().isBlank()) {
            parsed.setDocumentType(local.getDocumentType());
        }
        if (parsed.getExtractedMetadata() == null || parsed.getExtractedMetadata().isBlank()) {
            parsed.setExtractedMetadata(local.getExtractedMetadata());
        }
        if (parsed.getSummary() == null || parsed.getSummary().isBlank()) {
            parsed.setSummary(local.getSummary());
        }
        return parsed;
    }

    private String sendRemoteRequest(String prompt) throws Exception {
        String body = "{"
                + "\"model\":\"" + jsonEscape(config.getModel()) + "\","
                + "\"temperature\":0.1,"
                + "\"response_format\":{\"type\":\"json_object\"},"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\""
                + jsonEscape("You extract accounting document metadata and summaries. "
                        + "Return strict JSON only.") + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}"
                + "]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getEndpoint()))
                .timeout(config.getTimeout())
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient client = httpClient == null ? HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .build() : httpClient;
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("AI request failed with status " + status);
        }
        return response.body();
    }

    private String buildRemotePrompt(String text, Statement statement) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze this accounting/financial document. Return JSON with exactly these fields:\n")
                .append("{\n")
                .append("  \"document_type\": \"BANK_STATEMENT | INVOICE | RECEIPT | FINANCIAL_DOCUMENT | DOCUMENT\",\n")
                .append("  \"extracted_metadata\": \"key=value lines for customer_name, account_number, period_start, period_end, transaction_count, totals, keywords, and notable financial facts\",\n")
                .append("  \"summary\": \"2-4 concise sentences describing the document and financial highlights\"\n")
                .append("}\n\n")
                .append("Rules:\n")
                .append("- Do not invent fields that are not supported by the text.\n")
                .append("- Keep extracted_metadata machine-readable as newline-separated key=value lines.\n")
                .append("- Use yyyy-MM-dd dates when possible.\n")
                .append("- Keep money values in USD-style decimal/currency format when possible.\n\n");

        if (statement != null) {
            sb.append("Parsed statement facts available from the backend:\n")
                    .append("period_start=").append(formatDate(statement.getPeriodStart())).append('\n')
                    .append("period_end=").append(formatDate(statement.getPeriodEnd())).append('\n')
                    .append("transaction_count=").append(transactionCount(statement)).append('\n')
                    .append("total_deposits=").append(formatMoneyOrNull(statement.getTotalDeposits())).append('\n')
                    .append("total_withdrawals=").append(formatMoneyOrNull(statement.getTotalWithdrawals())).append('\n')
                    .append("ending_balance=").append(formatMoneyOrNull(statement.getEndingBalance())).append("\n\n");
        }

        sb.append("Extracted PDF text:\n")
                .append(truncate(text, MAX_REMOTE_TEXT_CHARS));
        return sb.toString();
    }

    private DocumentAiAnalysis parseRemoteContent(String content) {
        String documentType = extractJsonStringField(content, "document_type");
        String metadata = extractJsonStringField(content, "extracted_metadata");
        String summary = extractJsonStringField(content, "summary");

        if (summary == null && metadata == null && documentType == null) {
            summary = content.trim();
        }
        return new DocumentAiAnalysis(documentType, metadata, summary, null, "OPENAI", config.getModel());
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

    private static String appendMetadata(String metadata, String extraLine) {
        if (extraLine == null || extraLine.isBlank()) {
            return metadata;
        }
        if (metadata == null || metadata.isBlank()) {
            return extraLine;
        }
        return metadata + "\n" + extraLine;
    }

    private static String sanitizeMetadataValue(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n[truncated]";
    }

    private static String extractMessageContent(String responseBody) {
        return extractJsonStringField(responseBody, "content");
    }

    private static String extractJsonStringField(String json, String fieldName) {
        if (json == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        String needle = "\"" + fieldName + "\"";
        int searchFrom = 0;
        while (searchFrom < json.length()) {
            int fieldAt = json.indexOf(needle, searchFrom);
            if (fieldAt < 0) {
                return null;
            }
            int colon = json.indexOf(':', fieldAt + needle.length());
            if (colon < 0) {
                return null;
            }
            int quote = nextNonWhitespace(json, colon + 1);
            if (quote >= 0 && quote < json.length() && json.charAt(quote) == '"') {
                return readJsonString(json, quote);
            }
            searchFrom = colon + 1;
        }
        return null;
    }

    private static int nextNonWhitespace(String value, int start) {
        for (int i = start; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String readJsonString(String json, int openingQuote) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = openingQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            sb.append("\\u");
                        }
                    }
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
