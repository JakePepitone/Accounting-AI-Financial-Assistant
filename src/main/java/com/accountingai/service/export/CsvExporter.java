package com.accountingai.service.export;

import com.accountingai.model.Account;
import com.accountingai.model.ExportData;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Exports a bank statement to CSV using Apache Commons CSV.
 * <p>
 * The output has two logical sections:
 * <ol>
 *   <li>A small labelled summary (customer, account, period, balances).</li>
 *   <li>A transactions table with a {@code Date,Description,Amount} header and one
 *       row per transaction.</li>
 * </ol>
 * Everything is written into an in-memory {@link StringWriter} and returned as
 * UTF-8 bytes.
 */
class CsvExporter implements Exporter {

    /**
     * Renders the export data as CSV bytes.
     *
     * @param data the statement data to export (must not be null)
     * @return UTF-8 encoded CSV content
     * @throws ExportException if the CSV cannot be written
     */
    @Override
    public byte[] toBytes(ExportData data) throws ExportException {
        if (data == null) {
            throw new ExportException("Export data is null");
        }

        // Pull out the pieces defensively; any of them could be null.
        Account account = data.getAccount();
        Statement statement = data.getStatement();
        List<Transaction> transactions = data.getTransactions();

        StringWriter writer = new StringWriter();

        // CSVFormat.DEFAULT quotes fields containing commas/quotes automatically.
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

            // ---- Summary section (label/value rows) ----
            printer.printRecord("Statement Summary");
            printer.printRecord("Customer", safe(account == null ? null : account.getCustomerName()));
            printer.printRecord("Account Number", safe(account == null ? null : account.getAccountNumber()));

            if (statement != null) {
                printer.printRecord("Period Start", dateStr(statement.getPeriodStart()));
                printer.printRecord("Period End", dateStr(statement.getPeriodEnd()));
                printer.printRecord("Beginning Balance", money(statement.getBeginningBalance()));
                printer.printRecord("Total Deposits", money(statement.getTotalDeposits()));
                printer.printRecord("Total Withdrawals", money(statement.getTotalWithdrawals()));
                printer.printRecord("Ending Balance", money(statement.getEndingBalance()));
            }

            // Blank spacer row between the summary and the table.
            printer.printRecord();

            // ---- Transactions table ----
            printer.printRecord("Date", "Description", "Amount");
            if (transactions != null) {
                for (Transaction t : transactions) {
                    if (t == null) {
                        continue;
                    }
                    printer.printRecord(
                            dateStr(t.getDate()),
                            safe(t.getDescription()),
                            money(t.getAmount()));
                }
            }

            printer.flush();
        } catch (IOException e) {
            // Wrap the low level I/O failure in our export-specific type.
            throw new ExportException("Failed to write CSV export", e);
        }

        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Null-safe string helper.
     *
     * @param value possibly null string
     * @return the value or an empty string if null
     */
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * Formats a date as its ISO string, or empty if null.
     *
     * @param date possibly null date
     * @return ISO-8601 date text or empty string
     */
    private static String dateStr(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    /**
     * Formats a monetary value as plain text, or empty if null.
     *
     * @param amount possibly null amount
     * @return the amount's string form or empty string
     */
    private static String money(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }
}
