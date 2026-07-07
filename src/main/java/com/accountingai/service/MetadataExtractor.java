package com.accountingai.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import com.accountingai.model.DocumentMetadata;

/**
 * Reads document-level metadata (title, author, page count, size, etc.) from a
 * PDF file and packages it into a {@link DocumentMetadata} object for storage.
 *
 * <p>Uses the PDFBox 3.x {@link Loader#loadPDF(File)} API. All embedded metadata
 * reads are null-safe because many real-world PDFs omit title/author.</p>
 */
public class MetadataExtractor {

    /**
     * Extracts metadata from the given PDF file.
     *
     * @param file the PDF file (must exist)
     * @return a populated {@link DocumentMetadata} with {@code status = "IMPORTED"}
     *         and {@code statementId = null}
     * @throws IOException if the file cannot be opened or parsed
     */
    public DocumentMetadata extract(File file) throws IOException {
        DocumentMetadata metadata = new DocumentMetadata();

        // File-system level facts we can set without opening the PDF.
        metadata.setFileName(file.getName());
        metadata.setFilePath(file.getAbsolutePath());
        metadata.setFileSizeBytes(file.length());
        metadata.setUploadedAt(LocalDateTime.now());
        metadata.setStatementId(null);
        metadata.setStatus("IMPORTED");

        // PDF-internal facts require loading the document.
        try (PDDocument doc = Loader.loadPDF(file)) {
            metadata.setPageCount(doc.getNumberOfPages());

            PDDocumentInformation info = doc.getDocumentInformation();
            if (info != null) {
                // Title/Author are frequently missing -> guard against null.
                metadata.setTitle(info.getTitle());
                metadata.setAuthor(info.getAuthor());
            }
        }

        return metadata;
    }
}
