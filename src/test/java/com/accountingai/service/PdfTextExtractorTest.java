package com.accountingai.service;

import com.accountingai.TestPdfs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PdfTextExtractor}.
 *
 * <p>Generates a sample PDF into a {@code @TempDir} with {@link TestPdfs}, then
 * confirms the extracted text contains the expected sample content and that
 * {@code isValidPdf} correctly distinguishes real PDFs from non-PDF files.</p>
 */
class PdfTextExtractorTest {

    @TempDir
    Path tmp;

    @Test
    void extractTextContainsSampleContent() throws IOException {
        Path pdf = tmp.resolve("sample.pdf");
        TestPdfs.writeSampleStatement(pdf);

        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(pdf);

        assertTrue(text.contains("John Smith"), "Extracted text should contain the customer name.");
        assertTrue(text.contains("Payroll Deposit"), "Extracted text should contain a transaction description.");
        assertTrue(text.contains("123456789"), "Extracted text should contain the account number.");
    }

    @Test
    void isValidPdfTrueForRealPdf() throws IOException {
        Path pdf = tmp.resolve("valid.pdf");
        TestPdfs.writeSampleStatement(pdf);

        PdfTextExtractor extractor = new PdfTextExtractor();
        assertTrue(extractor.isValidPdf(pdf), "A generated PDF should be recognized as valid.");
    }

    @Test
    void isValidPdfFalseForNonPdfFile() throws IOException {
        Path notPdf = tmp.resolve("notes.txt");
        Files.writeString(notPdf, "this is definitely not a pdf");

        PdfTextExtractor extractor = new PdfTextExtractor();
        assertFalse(extractor.isValidPdf(notPdf), "A plain text file must not be reported as a valid PDF.");
    }
}
