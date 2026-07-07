package com.accountingai.model;

/**
 * Represents a bank customer account.
 * <p>
 * Maps to the {@code accounts} table: (account_id, customer_name, account_number).
 * Plain POJO with no framework dependencies.
 */
public class Account {

    /** Primary key (account_id). 0 means "not yet persisted". */
    private int id;

    /** The customer's full name (e.g. "John Smith"). */
    private String customerName;

    /** The bank account number (unique). */
    private String accountNumber;

    /** No-arg constructor required for framework/database mapping. */
    public Account() {
    }

    /**
     * All-args constructor.
     *
     * @param id            the account id (0 if not persisted)
     * @param customerName  the customer's name
     * @param accountNumber the account number
     */
    public Account(int id, String customerName, String accountNumber) {
        this.id = id;
        this.customerName = customerName;
        this.accountNumber = accountNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", customerName='" + customerName + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                '}';
    }
}
