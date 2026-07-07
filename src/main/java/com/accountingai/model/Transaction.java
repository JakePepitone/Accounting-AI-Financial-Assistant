package com.accountingai.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single line item on a bank statement.
 * <p>
 * Maps to the {@code transactions} table: (transaction_id, statement_id,
 * transaction_date, description, amount). Positive amounts are deposits,
 * negative amounts are withdrawals.
 */
public class Transaction {

    /** Primary key (transaction_id). 0 means "not yet persisted". */
    private int id;

    /** Foreign key to the owning statement. */
    private int statementId;

    /** The date the transaction posted. */
    private LocalDate date;

    /** A short description of the transaction (e.g. "Amazon Purchase"). */
    private String description;

    /** The signed amount (deposits positive, withdrawals negative). */
    private BigDecimal amount;

    /** No-arg constructor required for framework/database mapping. */
    public Transaction() {
    }

    /**
     * All-args constructor.
     *
     * @param id          the transaction id (0 if not persisted)
     * @param statementId the owning statement id
     * @param date        the transaction date
     * @param description the transaction description
     * @param amount      the signed amount
     */
    public Transaction(int id, int statementId, LocalDate date, String description, BigDecimal amount) {
        this.id = id;
        this.statementId = statementId;
        this.date = date;
        this.description = description;
        this.amount = amount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatementId() {
        return statementId;
    }

    public void setStatementId(int statementId) {
        this.statementId = statementId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", statementId=" + statementId +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", amount=" + amount +
                '}';
    }
}
