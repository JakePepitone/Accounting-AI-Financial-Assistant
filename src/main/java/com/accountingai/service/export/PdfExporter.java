package com.accountingai.service.export;

import com.accountingai.model.Account;
import com.accountingai.model.ExportData;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Exports a bank statement to a simple, readable PDF using Apache PDFBox 3.0.
 * <p>
 * The document has a title, the account and period details, a balance summary,
 * and then the list of transactions. Content is drawn line by line from the top
 * of each page; when the cursor reaches the bottom margin a new page is started
 * (basic pagination). All text uses the standard Helvetica font.
 */
class PdfExporter implements Exporter {

    /** Standard Helvetica font (PDFBox 3.0 uses the Standard14Fonts enum). */
    private static final PDType1Font FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    // Layout constants (PDF points; US Letter page is 612 x 792).
    private static final float MARGIN = 50f;
    private static final float TITLE_SIZE = 16f;
    private static final float BODY_SIZE = 11f;
    private static final float LINE_HEIGHT = 16f;
    private static final float PAGE_TOP = 742f;   // 792 - 50 margin
    private static final float PAGE_BOTTOM = 50f;

    /**
     * Renders the export data as PDF bytes.
     *
     * @param data the statement data to export (must not be null)
     * @return the generated PDF as bytes
     * @throws ExportException if the PDF cannot be produced
     */
    @Override
    public byte[] toBytes(ExportData data) throws ExportException {
        if (data == null) {
            throw new ExportException("Export data is null");
        }

        Account account = data.getAccount();
        Statement statement = data.getStatement();
        List<Transaction> transactions = data.getTransactions();

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Writer object tracks the current page, content stream and y-cursor.
            PageWriter writer = new PageWriter(document);

            // Title.
            writer.writeLine("Bank Statement", FONT_BOLD, TITLE_SIZE);
            writer.blankLine();

            // Account and period.
            if (account != null) {
                writer.writeLine("Customer: " + safe(account.getCustomerName()), FONT, BODY_SIZE);
                writer.writeLine("Account Number: " + safe(account.getAccountNumber()), FONT, BODY_SIZE);
            }
            if (statement != null) {
                writer.writeLine("Period: " + dateStr(statement.getPeriodStart())
                        + " to " + dateStr(statement.getPeriodEnd()), FONT, BODY_SIZE);
            }
            writer.blankLine();

            // Balance summary.
            if (statement != null) {
                writer.writeLine("Balance Summary", FONT_BOLD, BODY_SIZE);
                writer.writeLine("  Beginning Balance: " + money(statement.getBeginningBalance()), FONT, BODY_SIZE);
                writer.writeLine("  Total Deposits:    " + money(statement.getTotalDeposits()), FONT, BODY_SIZE);
                writer.writeLine("  Total Withdrawals: " + money(statement.getTotalWithdrawals()), FONT, BODY_SIZE);
                writer.writeLine("  Ending Balance:    " + money(statement.getEndingBalance()), FONT, BODY_SIZE);
                writer.blankLine();
            }

            // Transactions.
            writer.writeLine("Transactions", FONT_BOLD, BODY_SIZE);
            writer.writeLine(pad("Date", 12) + pad("Description", 32) + "Amount", FONT_BOLD, BODY_SIZE);
            if (transactions != null) {
                for (Transaction t : transactions) {
                    if (t == null) {
                        continue;
                    }
                    String line = pad(dateStr(t.getDate()), 12)
                            + pad(safe(t.getDescription()), 32)
                            + money(t.getAmount());
                    writer.writeLine(line, FONT, BODY_SIZE);
                }
            }

            // Finish up: close the last content stream and serialize.
            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExportException("Failed to write PDF export", e);
        }
    }

    /**
     * Small helper that owns the "cursor" while writing text top-to-bottom and
     * transparently starts a new page when the bottom margin is reached.
     */
    private static final class PageWriter {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        PageWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        /** Starts a fresh page and resets the vertical cursor to the top. */
        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage();
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_TOP;
        }

        /**
         * Writes a single line of text, advancing the cursor and paginating
         * automatically when needed.
         */
        void writeLine(String text, PDType1Font font, float size) throws IOException {
            if (y <= PAGE_BOTTOM) {
                newPage();
            }
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            // Guard against null and strip characters PDFBox cannot encode in WinAnsi.
            stream.showText(sanitize(text));
            stream.endText();
            y -= LINE_HEIGHT;
        }

        /** Advances the cursor by one line without drawing anything. */
        void blankLine() {
            y -= LINE_HEIGHT;
        }

        /** Closes the underlying content stream. */
        void close() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }

    /**
     * Replaces characters that the standard WinAnsi encoding cannot represent so
     * PDFBox never throws mid-export. Also converts null to empty string.
     */
    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Keep printable ASCII and common Latin-1; replace the rest with '?'.
            if (c >= 32 && c < 127) {
                sb.append(c);
            } else if (c >= 160 && c <= 255) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    /** Right-pads (or truncates) a string to a fixed column width. */
    private static String pad(String value, int width) {
        String v = value == null ? "" : value;
        if (v.length() >= width) {
            return v.substring(0, width - 1) + " ";
        }
        StringBuilder sb = new StringBuilder(v);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
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
