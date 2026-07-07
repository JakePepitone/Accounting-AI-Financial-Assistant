package com.accountingai.service.export;

import com.accountingai.model.Account;
import com.accountingai.model.ExportData;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Exports a bank statement to a real Excel workbook (.xlsx) using Apache POI.
 * <p>
 * The workbook contains two sheets:
 * <ul>
 *   <li><b>Statement</b> — labelled summary rows (customer, account, period, balances).</li>
 *   <li><b>Transactions</b> — a bold header row {@code Date/Description/Amount} followed
 *       by one row per transaction, with the amount stored as a numeric cell.</li>
 * </ul>
 */
class XlsxExporter implements Exporter {

    /**
     * Renders the export data as .xlsx bytes.
     *
     * @param data the statement data to export (must not be null)
     * @return the workbook serialized to bytes
     * @throws ExportException if the workbook cannot be written
     */
    @Override
    public byte[] toBytes(ExportData data) throws ExportException {
        if (data == null) {
            throw new ExportException("Export data is null");
        }

        Account account = data.getAccount();
        Statement statement = data.getStatement();
        List<Transaction> transactions = data.getTransactions();

        // try-with-resources guarantees the workbook is closed even on error.
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle boldStyle = createBoldStyle(workbook);

            writeSummarySheet(workbook, boldStyle, account, statement);
            writeTransactionsSheet(workbook, boldStyle, transactions);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to write XLSX export", e);
        }
    }

    /**
     * Builds the "Statement" summary sheet with label/value rows.
     */
    private void writeSummarySheet(Workbook workbook, CellStyle boldStyle,
                                   Account account, Statement statement) {
        Sheet sheet = workbook.createSheet("Statement");
        int r = 0;

        // Title row (bold).
        Row title = sheet.createRow(r++);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Statement Summary");
        titleCell.setCellStyle(boldStyle);

        labelledRow(sheet, r++, boldStyle, "Customer",
                account == null ? "" : safe(account.getCustomerName()));
        labelledRow(sheet, r++, boldStyle, "Account Number",
                account == null ? "" : safe(account.getAccountNumber()));

        if (statement != null) {
            labelledRow(sheet, r++, boldStyle, "Period Start", dateStr(statement.getPeriodStart()));
            labelledRow(sheet, r++, boldStyle, "Period End", dateStr(statement.getPeriodEnd()));
            labelledRow(sheet, r++, boldStyle, "Beginning Balance", money(statement.getBeginningBalance()));
            labelledRow(sheet, r++, boldStyle, "Total Deposits", money(statement.getTotalDeposits()));
            labelledRow(sheet, r++, boldStyle, "Total Withdrawals", money(statement.getTotalWithdrawals()));
            labelledRow(sheet, r++, boldStyle, "Ending Balance", money(statement.getEndingBalance()));
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Builds the "Transactions" sheet with a bold header and one row per transaction.
     */
    private void writeTransactionsSheet(Workbook workbook, CellStyle boldStyle,
                                        List<Transaction> transactions) {
        Sheet sheet = workbook.createSheet("Transactions");

        // Header row (bold).
        Row header = sheet.createRow(0);
        String[] headers = {"Date", "Description", "Amount"};
        for (int c = 0; c < headers.length; c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(boldStyle);
        }

        int rowIdx = 1;
        if (transactions != null) {
            for (Transaction t : transactions) {
                if (t == null) {
                    continue;
                }
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dateStr(t.getDate()));
                row.createCell(1).setCellValue(safe(t.getDescription()));

                // Store the amount as a numeric cell when possible, else blank.
                Cell amountCell = row.createCell(2);
                BigDecimal amount = t.getAmount();
                if (amount != null) {
                    amountCell.setCellValue(amount.doubleValue());
                }
            }
        }

        for (int c = 0; c < headers.length; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    /**
     * Writes a two-column "label / value" row, with the label in bold.
     */
    private void labelledRow(Sheet sheet, int rowIndex, CellStyle boldStyle,
                             String label, String value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(boldStyle);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }

    /**
     * Creates a reusable bold cell style.
     */
    private CellStyle createBoldStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    /** Null-safe string helper. */
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /** ISO date text, or empty if null. */
    private static String dateStr(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    /** Plain money text, or empty if null. */
    private static String money(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }
}
