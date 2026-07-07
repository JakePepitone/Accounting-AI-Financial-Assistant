package com.accountingai.service;

import com.accountingai.TestPdfs;
import com.accountingai.model.DocumentMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MetadataExtractor}.
 *
 * <p>Generates a sample PDF and checks the extracted {@link DocumentMetadata}:
 * a positive page count, a non-null file name, a plausible file size, and the
 * default "IMPORTED" status with a null statement id.</p>
 */
class MetadataExtractorTest {

    @TempDir
    Path tmp;

    @Test
    void extractPopulatesCoreMetadata() throws IOException {
        Path pdf = tmp.resolve("meta_sample.pdf");
        TestPdfs.writeSampleStatement(pdf);

        MetadataExtractor extractor = new MetadataExtractor();
        DocumentMetadata meta = extractor.extract(pdf.toFile());

        assertNotNull(meta, "Metadata should never be null.");
        assertTrue(meta.getPageCount() >= 1, "A one-page PDF should report at least one page.");
        assertEquals("meta_sample.pdf", meta.getFileName(), "File name should be set from the file.");
        assertTrue(meta.getFileSizeBytes() > 0, "File size should be positive.");
        assertNotNull(meta.getUploadedAt(), "uploadedAt should be set to the import time.");
    }

    @Test
    void extractSetsImportedStatusAndNullStatementId() throws IOException {
        Path pdf = tmp.resolve("meta_status.pdf");
        TestPdfs.writeSampleStatement(pdf);

        MetadataExtractor extractor = new MetadataExtractor();
        DocumentMetadata meta = extractor.extract(pdf.toFile());

        assertEquals("IMPORTED", meta.getStatus(), "Default status should be IMPORTED.");
        assertTrue(meta.getStatementId() == null, "A freshly-extracted document has no linked statement yet.");
        assertNotNull(meta.getFilePath(), "File path should be recorded (absolute).");
    }
}
