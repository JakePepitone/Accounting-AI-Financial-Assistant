package com.accountingai.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.accountingai.model.DocumentMetadata;
import com.accountingai.model.ImportResult;
import com.accountingai.model.Statement;
import com.accountingai.util.AppPaths;
import com.accountingai.util.FileFormatValidator;

/**
 * Orchestrates the end-to-end import of a single PDF bank statement:
 * <ol>
 *   <li>validate that the file is a real, non-empty PDF,</li>
 *   <li>copy it into the application's import store,</li>
 *   <li>extract its text and parse it into a {@link Statement},</li>
 *   <li>extract its {@link DocumentMetadata},</li>
 *   <li>return a success/failure {@link ImportResult}.</li>
 * </ol>
 *
 * <p>Collaborators are injected via the constructor so this service is easy to
 * unit-test, and {@link #importPdf(Path)} is intentionally NOT final so tests
 * can subclass and return canned results.</p>
 */
public class PdfImportService {

    private final PdfTextExtractor extractor;
    private final StatementParser parser;
    private final MetadataExtractor metadataExtractor;
    private final Path importStore;

    /**
     * Full constructor with all collaborators injected.
     *
     * @param extractor         PDF text extractor
     * @param parser            statement text parser
     * @param metadataExtractor PDF metadata extractor
     * @param importStore       directory into which imported files are copied
     */
    public PdfImportService(PdfTextExtractor extractor,
                            StatementParser parser,
                            MetadataExtractor metadataExtractor,
                            Path importStore) {
        this.extractor = extractor;
        this.parser = parser;
        this.metadataExtractor = metadataExtractor;
        this.importStore = importStore;
    }

    /**
     * Convenience constructor that wires up default collaborators and uses the
     * standard application import-store directory.
     */
    public PdfImportService() {
        this(new PdfTextExtractor(),
             new StatementParser(),
             new MetadataExtractor(),
             AppPaths.importStoreDir());
    }

    /**
     * Imports one PDF file.
     *
     * @param pdf path to the source PDF
     * @return {@link ImportResult#ok} on success, or {@link ImportResult#fail}
     *         with a human-readable message on any validation or processing error
     */
    public ImportResult importPdf(Path pdf) {
        try {
            // 1) Validate. A null message means "valid".
            File source = pdf.toFile();
            String validationError = FileFormatValidator.validate(source);
            if (validationError != null) {
                return ImportResult.fail(validationError);
            }

            // 2) Copy into the import store (overwrite if it already exists).
            Files.createDirectories(importStore);
            Path stored = importStore.resolve(source.getName());
            Files.copy(pdf, stored, StandardCopyOption.REPLACE_EXISTING);

            // 3) Extract text and parse it into a Statement.
            String text = extractor.extractText(stored);
            Statement statement = parser.parseStatement(text);

            // 4) Extract document metadata from the stored copy.
            DocumentMetadata metadata = metadataExtractor.extract(stored.toFile());

            // 5) Success.
            return ImportResult.ok(stored.toString(), statement, metadata);
        } catch (Exception e) {
            // Any failure becomes a graceful, message-bearing failure result.
            String message = (e.getMessage() != null) ? e.getMessage() : e.toString();
            return ImportResult.fail(message);
        }
    }
}
