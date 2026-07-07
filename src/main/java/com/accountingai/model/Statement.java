package com.accountingai.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bank statement for a single billing period.
 * <p>
 * Maps to the {@code statements} table: (statement_id, account_id, period_start,
 * period_end, beginning_balance, total_deposits, total_withdrawals,
 * ending_balance). The {@link #transactions} list is a convenience container and
 * is NOT a database column; it is populated by service/DAO code when needed.
 */
public class Statement {

    /** Primary key (statement_id). 0 means "not yet persisted". */
    private int id;

    /** Foreign key to the owning account. */
    private int accountId;

    /** First day of the statement period. */
    private LocalDate periodStart;

    /** Last day of the statement period. */
    private LocalDate periodEnd;

    /** Balance at the start of the period. */
    private BigDecimal beginningBalance;

    /** Sum of all deposits in the period. */
    private BigDecimal totalDeposits;

    /** Sum of all withdrawals in the period (stored as a positive magnitude). */
    private BigDecimal totalWithdrawals;

    /** Balance at the end of the period. */
    private BigDecimal endingBalance;

    /**
     * Convenience list of transactions for this statement.
     * Not a DB column; initialized to an empty list to avoid NPEs.
     */
    private List<Transaction> transactions = new ArrayList<>();

    /** No-arg constructor required for framework/database mapping. */
    public Statement() {
    }

    /**
     * All-args constructor.
     *
     * @param id               the statement id (0 if not persisted)
     * @param accountId        the owning account id
     * @param periodStart      the period start date
     * @param periodEnd        the period end date
     * @param beginningBalance the beginning balance
     * @param totalDeposits    the total deposits
     * @param totalWithdrawals the total withdrawals
     * @param endingBalance    the ending balance
     * @param transactions     the transactions (null becomes an empty list)
     */
    public Statement(int id, int accountId, LocalDate periodStart, LocalDate periodEnd,
                     BigDecimal beginningBalance, BigDecimal totalDeposits,
                     BigDecimal totalWithdrawals, BigDecimal endingBalance,
                     List<Transaction> transactions) {
        this.id = id;
        this.accountId = accountId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.beginningBalance = beginningBalance;
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.endingBalance = endingBalance;
        // Defensive: never leave the list null.
        this.transactions = (transactions != null) ? transactions : new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getBeginningBalance() {
        return beginningBalance;
    }

    public void setBeginningBalance(BigDecimal beginningBalance) {
        this.beginningBalance = beginningBalance;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(BigDecimal totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(BigDecimal totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public BigDecimal getEndingBalance() {
        return endingBalance;
    }

    public void setEndingBalance(BigDecimal endingBalance) {
        this.endingBalance = endingBalance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        // Defensive: never store null.
        this.transactions = (transactions != null) ? transactions : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Statement{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", beginningBalance=" + beginningBalance +
                ", totalDeposits=" + totalDeposits +
                ", totalWithdrawals=" + totalWithdrawals +
                ", endingBalance=" + endingBalance +
                ", transactions=" + (transactions == null ? 0 : transactions.size()) + " items" +
                '}';
    }
}
