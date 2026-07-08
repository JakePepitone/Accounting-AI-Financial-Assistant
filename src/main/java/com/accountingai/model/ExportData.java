package com.accountingai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A bundle of everything needed to export a single statement.
 * <p>
 * Groups together the owning {@link Account}, the {@link Statement}, and its
 * {@link Transaction} list so that exporters (CSV/XLSX/PDF) have all the data in
 * one immutable-ish object. Constructed with the all-args constructor.
 */
public class ExportData {

    /** The account the statement belongs to. */
    private final Account account;

    /** The statement being exported. */
    private final Statement statement;

    /** The transactions on the statement. */
    private final List<Transaction> transactions;

    /**
     * All-args constructor.
     *
     * @param account      the owning account
     * @param statement    the statement to export
     * @param transactions the transactions (null becomes an empty list)
     */
    public ExportData(Account account, Statement statement, List<Transaction> transactions) {
        this.account = account;
        this.statement = statement;
        // Defensive copy-ish: never store null so exporters can iterate safely.
        this.transactions = (transactions != null) ? transactions : new ArrayList<>();
    }

    public Account getAccount() {
        return account;
    }

    public Statement getStatement() {
        return statement;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        return "ExportData{" +
                "account=" + account +
                ", statement=" + statement +
                ", transactions=" + transactions.size() + " items" +
                '}';
    }
}
