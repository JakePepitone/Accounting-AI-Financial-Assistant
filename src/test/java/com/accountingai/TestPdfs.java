package com.accountingai;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime PDF fixture generator.
 *
 * <p>The tests never depend on a committed binary PDF. Instead they call
 * {@link #writeSampleStatement(Path)} (or {@link #writeEmptyPdf(Path)}) to
 * synthesize a text-based PDF into a JUnit {@code @TempDir} on the fly using
 * Apache PDFBox 3.x. The generated document contains the exact "John Smith"
 * sample text that {@code StatementParser} and the extraction/import tests
 * expect, so the round-trip (write PDF -> extract text -> parse) can be
 * verified end to end without any external files.</p>
 */
public final class TestPdfs {

    private TestPdfs() {
        // Static helper — not instantiable.
    }

    /**
     * Writes a one-page, text-based PDF containing the full John Smith sample
     * statement. The text is laid out one line per {@code showText} call so that
     * {@code PDFTextStripper} recovers each logical line.
     *
     * @param out destination path (parent directories are created if needed)
     * @throws IOException if the file cannot be written
     */
    public static void writeSampleStatement(Path out) throws IOException {
        // Every line that must appear in the extracted text, in visual order.
        List<String> lines = new ArrayList<>();
        lines.add("Bank Statement");
        lines.add("Customer: John Smith");
        lines.add("Account Number: 123456789");
        lines.add("Period: 2026-06-01 to 2026-06-30");
        lines.add("Beginning Balance: 5000.00");
        lines.add("Total Deposits: 2500.00");
        lines.add("Total Withdrawals: 1273.56");
        lines.add("Ending Balance: 6226.44");
        lines.add("Transactions:");
        lines.add("2026-06-01 Payroll Deposit 2500.00");
        lines.add("2026-06-03 Amazon Purchase -125.99");
        lines.add("2026-06-05 Utility Bill -85.12");
        lines.add("2026-06-10 Restaurant -62.45");
        lines.add("2026-06-15 Gas Station -48.00");
        lines.add("2026-06-20 Grocery Store -152.00");
        lines.add("2026-06-25 Internet Service -75.00");

        writeLines(out, lines);
    }

    /**
     * Writes a one-page blank PDF (a valid PDF whose page has no drawn text).
     *
     * @param out destination path (parent directories are created if needed)
     * @throws IOException if the file cannot be written
     */
    public static void writeEmptyPdf(Path out) throws IOException {
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(out.toFile());
        }
    }

    /**
     * Shared helper: draw the given lines of text onto a single page and save.
     */
    private static void writeLines(Path out, List<String> lines) throws IOException {
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            // PDFBox 3.x: fonts are created from the Standard14Fonts enum.
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 12);
                cs.setLeading(16f);
                // Start near the top-left of a Letter page.
                cs.newLineAtOffset(50, 720);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLine();
                }
                cs.endText();
            }
            doc.save(out.toFile());
        }
    }
}
