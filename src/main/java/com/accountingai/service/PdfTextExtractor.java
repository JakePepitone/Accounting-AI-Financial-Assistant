package com.accountingai.service;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extracts plain text from PDF files using Apache PDFBox 3.x.
 *
 * <p>This is a thin, single-responsibility wrapper around PDFBox so the rest of
 * the application never has to touch PDFBox directly. It uses the 3.x
 * {@link Loader#loadPDF(java.io.File)} API (NOT the removed 2.x
 * {@code PDDocument.load}).</p>
 */
public class PdfTextExtractor {

    /**
     * Reads the given PDF and returns all of its text content.
     *
     * @param pdf path to the PDF file on disk
     * @return the extracted text (may be empty for image-only PDFs)
     * @throws IOException if the file cannot be opened or parsed
     */
    public String extractText(Path pdf) throws IOException {
        // try-with-resources guarantees the document is always closed.
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Sort by position so lines come out in reading order.
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    /**
     * Cheap validity check: tries to load the PDF and reports whether it worked.
     *
     * @param pdf path to test
     * @return {@code true} if PDFBox could open the file, {@code false} otherwise
     */
    public boolean isValidPdf(Path pdf) {
        if (pdf == null) {
            return false;
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            // Opening successfully is enough to consider it a valid PDF.
            return true;
        } catch (Exception e) {
            // Any failure (missing file, corrupt bytes, not a PDF) => invalid.
            return false;
        }
    }
}
