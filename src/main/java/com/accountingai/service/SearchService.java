package com.accountingai.service;

import java.util.ArrayList;
import java.util.List;

import com.accountingai.db.dao.TransactionDao;
import com.accountingai.model.SearchResult;
import com.accountingai.model.Transaction;

/**
 * Provides two kinds of search used by the main UI:
 * <ul>
 *   <li>{@link #searchInText(String, String)} — a case-insensitive, per-line
 *       search over the currently previewed document text, and</li>
 *   <li>{@link #searchTransactions(String)} — a database search over stored
 *       transaction descriptions (delegates to {@link TransactionDao}).</li>
 * </ul>
 *
 * <p>Both methods are null/blank tolerant and always return a (possibly empty)
 * list rather than {@code null}.</p>
 */
public class SearchService {

    private final TransactionDao transactionDao;

    /**
     * @param transactionDao DAO used for {@link #searchTransactions(String)};
     *                       may be null if only text search is needed
     */
    public SearchService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /**
     * Searches the given text line-by-line for a case-insensitive substring
     * match of {@code query}.
     *
     * @param text  the document text to search (may be null)
     * @param query the search term; blank/null yields an empty result list
     * @return one {@link SearchResult} per matching line (source {@code "TEXT"})
     */
    public List<SearchResult> searchInText(String text, String query) {
        List<SearchResult> results = new ArrayList<>();
        if (text == null || query == null || query.isBlank()) {
            return results;
        }

        String needle = query.toLowerCase();
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.toLowerCase().contains(needle)) {
                // lineNumber is 1-based for human readability; context is the whole line.
                results.add(new SearchResult("TEXT", query, i + 1, line.trim()));
            }
        }
        return results;
    }

    /**
     * Searches stored transactions whose description contains {@code query}.
     *
     * @param query the search term; blank/null yields an empty result list
     * @return one {@link SearchResult} per matching transaction
     *         (source {@code "TRANSACTION"})
     */
    public List<SearchResult> searchTransactions(String query) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.isBlank() || transactionDao == null) {
            return results;
        }

        List<Transaction> matches = transactionDao.searchByDescription(query);
        if (matches == null) {
            return results;
        }

        for (Transaction t : matches) {
            // Build a readable label/context from the transaction fields.
            String label = (t.getDescription() != null) ? t.getDescription() : "";
            String context = String.format("%s  %s  %s",
                    (t.getDate() != null ? t.getDate().toString() : ""),
                    label,
                    (t.getAmount() != null ? t.getAmount().toPlainString() : ""));
            // No meaningful line number for a DB row -> use the transaction id.
            results.add(new SearchResult("TRANSACTION", label, t.getId(), context.trim()));
        }
        return results;
    }
}
