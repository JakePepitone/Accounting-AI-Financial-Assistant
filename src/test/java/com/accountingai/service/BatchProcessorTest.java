package com.accountingai.service;

import com.accountingai.model.BatchResult;
import com.accountingai.model.BatchItemResult;
import com.accountingai.model.ImportResult;
import com.accountingai.model.Statement;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BatchProcessor}.
 *
 * <p>Rather than depend on real PDFs, the test subclasses {@link PdfImportService}
 * and overrides {@code importPdf} to return a canned success/failure decided by
 * the file name. This isolates the batch orchestration logic: success/failure
 * counts, continue-on-error, and the per-item progress callback.</p>
 */
class BatchProcessorTest {

    /**
     * A stub import service: any file whose name contains "good" imports
     * successfully; anything else fails. Files named "boom" throw, exercising the
     * catch-and-continue path.
     */
    private static class StubImportService extends PdfImportService {
        StubImportService() {
            // Reuse the convenience no-arg constructor; its collaborators are never
            // touched because importPdf is fully overridden below.
            super();
        }

        @Override
        public ImportResult importPdf(Path pdf) {
            String name = pdf.getFileName().toString();
            if (name.contains("boom")) {
                throw new RuntimeException("simulated crash for " + name);
            }
            if (name.contains("good")) {
                Statement s = new Statement();
                return ImportResult.ok("/stored/" + name, s, null);
            }
            return ImportResult.fail("bad file: " + name);
        }
    }

    @Test
    void countsSuccessesAndFailures() {
        BatchProcessor processor = new BatchProcessor(new StubImportService());

        List<Path> files = new ArrayList<>();
        files.add(Path.of("good1.pdf"));
        files.add(Path.of("bad1.pdf"));
        files.add(Path.of("good2.pdf"));

        BatchResult result = processor.processFiles(files, BatchProgressListener.NOOP);

        assertEquals(3, result.total());
        assertEquals(2, result.successCount(), "Two 'good' files should succeed.");
        assertEquals(1, result.failureCount(), "One 'bad' file should fail.");
        BatchItemResult first = result.getItems().get(0);
        assertTrue(first.importResult() != null && first.importResult().isSuccess(),
                "Successful batch items should carry the full import result.");
    }

    @Test
    void continuesAfterAThrowingImport() {
        BatchProcessor processor = new BatchProcessor(new StubImportService());

        List<Path> files = new ArrayList<>();
        files.add(Path.of("good1.pdf"));
        files.add(Path.of("boom.pdf"));   // throws — must be caught and recorded as failure
        files.add(Path.of("good2.pdf"));

        BatchResult result = processor.processFiles(files, BatchProgressListener.NOOP);

        assertEquals(3, result.total(), "All files should be processed despite the crash.");
        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount(), "The crashing file is counted as a failure.");
    }

    @Test
    void listenerIsCalledOncePerFile() {
        BatchProcessor processor = new BatchProcessor(new StubImportService());

        List<Path> files = new ArrayList<>();
        files.add(Path.of("good1.pdf"));
        files.add(Path.of("good2.pdf"));
        files.add(Path.of("bad1.pdf"));

        AtomicInteger callbacks = new AtomicInteger(0);
        BatchProgressListener listener = (completed, total, item) -> {
            callbacks.incrementAndGet();
            assertEquals(3, total, "Total should be the file count on every callback.");
            assertTrue(completed >= 1 && completed <= 3, "Completed count should be in range.");
        };

        processor.processFiles(files, listener);
        assertEquals(3, callbacks.get(), "Listener should fire exactly once per file.");
    }

    @Test
    void nullListenerIsTreatedAsNoop() {
        BatchProcessor processor = new BatchProcessor(new StubImportService());

        List<Path> files = new ArrayList<>();
        files.add(Path.of("good1.pdf"));

        // Passing null must not throw (BatchProcessor falls back to NOOP).
        BatchResult result = processor.processFiles(files, null);
        assertEquals(1, result.successCount());
    }
}
