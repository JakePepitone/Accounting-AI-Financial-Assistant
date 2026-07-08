package com.accountingai.controller;

import com.accountingai.AppServices;
import com.accountingai.model.Account;
import com.accountingai.model.DocumentMetadata;
import com.accountingai.model.ExportData;
import com.accountingai.model.ImportResult;
import com.accountingai.model.SearchResult;
import com.accountingai.model.Statement;
import com.accountingai.model.Transaction;
import com.accountingai.service.BatchProgressListener;
import com.accountingai.model.BatchResult;
import com.accountingai.service.export.ExportFormat;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the main application view ({@code main-view.fxml}).
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Upload a single PDF, parse it and persist accounts/statements/transactions.</li>
 *   <li>Batch-import a folder of PDFs on a background thread.</li>
 *   <li>Preview the selected document (first-page image + extracted text).</li>
 *   <li>Search the current document text and the transactions table.</li>
 *   <li>Export the selected statement to PDF / CSV / Excel.</li>
 *   <li>Open the settings dialog.</li>
 * </ul>
 * All handlers referenced from the FXML are declared here so FXMLLoader can wire
 * them at load time.</p>
 */
public class MainController {

    // --- FXML widgets (ids must match main-view.fxml) --------------------
    @FXML
    private ListView<DocumentMetadata> documentListView;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> searchResultsView;

    @FXML
    private ImageView previewImageView;

    @FXML
    private TextArea previewTextArea;

    @FXML
    private Label statusLabel;

    // --- State -----------------------------------------------------------
    private AppServices services;

    /** Text of the currently previewed document, used by the search feature. */
    private String currentDocumentText = "";

    /**
     * Called automatically by FXMLLoader after the FXML fields are injected.
     * Wires up the service container, configures the document list rendering and
     * loads the initial list of imported documents.
     */
    @FXML
    public void initialize() {
        services = AppServices.get();

        // Show the file name (not the default toString) for each document.
        documentListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DocumentMetadata item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFileName());
            }
        });

        refreshDocumentList();

        // Preview whichever document the user selects.
        documentListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldDoc, newDoc) -> showPreview(newDoc));
    }

    // ---------------------------------------------------------------------
    // Upload / import
    // ---------------------------------------------------------------------

    /**
     * Handles the "Upload PDF" button: lets the user pick a single PDF, imports
     * it, persists the parsed data and refreshes the document list.
     */
    @FXML
    void handleUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a PDF statement");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return; // user cancelled
        }

        try {
            ImportResult result = services.importService().importPdf(file.toPath());
            if (result.isSuccess()) {
                persistImport(result);
                refreshDocumentList();
                statusLabel.setText("Imported: " + file.getName());
            } else {
                statusLabel.setText("Import failed: " + result.getMessage());
                showAlert(Alert.AlertType.ERROR, "Import failed",
                        result.getMessage() == null ? "Unknown error" : result.getMessage());
            }
        } catch (Exception e) {
            statusLabel.setText("Import error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Import error", String.valueOf(e.getMessage()));
        }
    }

    /**
     * Persists a successful import: finds-or-creates the account, inserts the
     * statement (linked to the account), the transactions (linked to the
     * statement) and finally the document metadata (linked to the statement).
     *
     * @param result the successful import result to store
     */
    private void persistImport(ImportResult result) {
        Statement statement = result.getStatement();
        DocumentMetadata metadata = result.getMetadata();

        // 1) Account: parse produced one on the statement's owner; fall back to a
        //    placeholder so the foreign keys stay valid even for sparse PDFs.
        Account parsed = extractAccount(result);
        int accountId = services.accountDao().findOrCreate(parsed);

        // 2) Statement.
        int statementId = 0;
        if (statement != null) {
            statement.setAccountId(accountId);
            statementId = services.statementDao().insert(statement);

            // 3) Transactions (link each to the new statement id).
            List<Transaction> txns = statement.getTransactions();
            if (txns != null && !txns.isEmpty()) {
                for (Transaction t : txns) {
                    t.setStatementId(statementId);
                }
                services.transactionDao().insertBatch(txns);
            }
        }

        // 4) Document metadata (link to the statement if we created one).
        if (metadata != null) {
            if (statementId > 0) {
                metadata.setStatementId(statementId);
            }
            services.documentDao().insert(metadata);
        }
    }

    /**
     * Extracts an {@link Account} to persist. The import service does not attach
     * an account to the {@link ImportResult}, so we re-parse the stored document
     * text; if that yields nothing usable we fall back to a placeholder derived
     * from the file name so the database foreign keys remain valid.
     *
     * @param result the import result
     * @return a non-null account with at least a customer name and number
     */
    private Account extractAccount(ImportResult result) {
        Account account = null;
        try {
            String storedPath = result.getStoredFilePath();
            if (storedPath != null) {
                String text = services.previewService().extractText(new File(storedPath));
                account = parseAccountSafely(text);
            }
        } catch (Exception ignored) {
            // fall through to the placeholder below
        }

        if (account == null) {
            account = new Account();
        }
        // Ensure required fields are populated so UNIQUE(account_number) holds.
        if (account.getCustomerName() == null || account.getCustomerName().isBlank()) {
            account.setCustomerName("Unknown Customer");
        }
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
            // Derive a stable-ish number from the document so re-imports match.
            DocumentMetadata md = result.getMetadata();
            String base = md != null && md.getFileName() != null ? md.getFileName() : "unknown";
            account.setAccountNumber("AUTO-" + Integer.toHexString(base.hashCode()));
        }
        return account;
    }

    /**
     * Parses an {@link Account} from raw text via the statement parser, guarding
     * against nulls. Returns null if nothing could be parsed.
     */
    private Account parseAccountSafely(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new com.accountingai.service.StatementParser().parseAccount(text);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Handles "Import Folder": lets the user multi-select PDFs and processes them
     * on a background {@link Task} so the UI stays responsive. Progress updates
     * the status label; a summary alert is shown when finished.
     */
    @FXML
    void handleImportFolder() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select PDF statements");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        List<File> files = chooser.showOpenMultipleDialog(window());
        if (files == null || files.isEmpty()) {
            return;
        }

        List<Path> paths = new ArrayList<>();
        for (File f : files) {
            paths.add(f.toPath());
        }

        // Background task: run the batch processor off the JavaFX thread.
        Task<BatchResult> task = new Task<>() {
            @Override
            protected BatchResult call() {
                BatchProgressListener listener = (completed, total, item) ->
                        Platform.runLater(() -> statusLabel.setText(
                                "Importing " + completed + " of " + total + ": "
                                        + item.fileName() + (item.success() ? " OK" : " FAILED")));
                return services.batchProcessor().processFiles(paths, listener);
            }
        };

        task.setOnSucceeded(evt -> {
            BatchResult r = task.getValue();
            // Persist each successfully imported file. The batch processor only
            // reports pass/fail, so re-import here to store the parsed data.
            int stored = 0;
            for (Path p : paths) {
                try {
                    ImportResult ir = services.importService().importPdf(p);
                    if (ir.isSuccess()) {
                        persistImport(ir);
                        stored++;
                    }
                } catch (Exception ignored) {
                    // already reflected in the batch result counts
                }
            }
            refreshDocumentList();
            statusLabel.setText(r.successCount() + " succeeded, " + r.failureCount() + " failed");
            showAlert(Alert.AlertType.INFORMATION, "Batch import complete",
                    r.successCount() + " succeeded, " + r.failureCount() + " failed"
                            + " (stored " + stored + ").");
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            statusLabel.setText("Batch import error: " + (ex == null ? "unknown" : ex.getMessage()));
            showAlert(Alert.AlertType.ERROR, "Batch import error",
                    ex == null ? "Unknown error" : String.valueOf(ex.getMessage()));
        });

        Thread thread = new Thread(task, "batch-import");
        thread.setDaemon(true);
        thread.start();
    }

    // ---------------------------------------------------------------------
    // Export
    // ---------------------------------------------------------------------

    /** Exports the selected statement as a PDF. */
    @FXML
    void handleExportPdf() {
        export(ExportFormat.PDF);
    }

    /** Exports the selected statement as CSV. */
    @FXML
    void handleExportCsv() {
        export(ExportFormat.CSV);
    }

    /** Exports the selected statement as an Excel workbook. */
    @FXML
    void handleExportXlsx() {
        export(ExportFormat.XLSX);
    }

    /**
     * Shared export helper: builds {@link ExportData} from the selected document,
     * prompts for a save location and writes the file in the given format.
     *
     * @param format the export format
     */
    private void export(ExportFormat format) {
        DocumentMetadata selected = documentListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a document to export first.");
            return;
        }

        ExportData data = buildExportData(selected);
        if (data == null) {
            statusLabel.setText("No statement data available for the selected document.");
            showAlert(Alert.AlertType.WARNING, "Nothing to export",
                    "The selected document has no linked statement to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export as " + format.getLabel());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                format.getLabel(), "*." + format.getExtension()));
        chooser.setInitialFileName("statement." + format.getExtension());

        File target = chooser.showSaveDialog(window());
        if (target == null) {
            return;
        }

        try {
            Path written = services.exportService().export(data, format, target.toPath());
            statusLabel.setText("Exported to " + written);
            showAlert(Alert.AlertType.INFORMATION, "Export complete",
                    "Saved to:\n" + written);
        } catch (Exception e) {
            statusLabel.setText("Export failed: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Export failed", String.valueOf(e.getMessage()));
        }
    }

    /**
     * Assembles {@link ExportData} for a document by loading its statement, the
     * statement's transactions and the owning account from the database.
     *
     * @param doc the selected document metadata
     * @return export data, or {@code null} if the document has no statement
     */
    private ExportData buildExportData(DocumentMetadata doc) {
        Integer statementId = doc.getStatementId();
        if (statementId == null) {
            return null;
        }

        Optional<Statement> statementOpt = services.statementDao().findById(statementId);
        if (statementOpt.isEmpty()) {
            return null;
        }
        Statement statement = statementOpt.get();

        List<Transaction> txns = services.transactionDao().findByStatementId(statementId);
        statement.setTransactions(txns);

        Account account = services.accountDao().findById(statement.getAccountId()).orElse(null);
        return new ExportData(account, statement, txns);
    }

    // ---------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------

    /**
     * Handles the "Search" button: searches the current document's text and the
     * transactions table for the query, then lists formatted hits.
     */
    @FXML
    void handleSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        searchResultsView.getItems().clear();

        if (query.isEmpty()) {
            statusLabel.setText("Enter a search term.");
            return;
        }

        List<SearchResult> hits = new ArrayList<>();
        // 1) In-document text search (only if a document is previewed).
        hits.addAll(services.searchService().searchInText(currentDocumentText, query));
        // 2) Transactions across the whole database.
        hits.addAll(services.searchService().searchTransactions(query));

        List<String> formatted = new ArrayList<>();
        for (SearchResult r : hits) {
            formatted.add(formatHit(r));
        }
        searchResultsView.getItems().setAll(formatted);
        statusLabel.setText(hits.size() + " result" + (hits.size() == 1 ? "" : "s") + " for \"" + query + "\"");
    }

    /**
     * Formats a {@link SearchResult} for display in the results list.
     */
    private String formatHit(SearchResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(r.source()).append("] ");
        if (r.lineNumber() > 0) {
            sb.append("L").append(r.lineNumber()).append(": ");
        }
        if (r.label() != null && !r.label().isBlank()) {
            sb.append(r.label());
            if (r.context() != null && !r.context().isBlank()) {
                sb.append(" — ");
            }
        }
        if (r.context() != null) {
            sb.append(r.context());
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------

    /**
     * Opens the settings dialog as a modal window and waits for it to close.
     */
    @FXML
    void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/accountingai/settings-dialog.fxml"));
            Parent root = loader.load();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(window());
            dialog.setTitle("Settings");

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/com/accountingai/styles.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            statusLabel.setText("Unable to open settings: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Settings error", String.valueOf(e.getMessage()));
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Reloads the document list from the database (newest first).
     */
    private void refreshDocumentList() {
        documentListView.getItems().setAll(services.documentDao().findAll());
    }

    /**
     * Renders a preview of the given document: first-page image and extracted
     * text. Any failure is reported on the status label rather than thrown.
     *
     * @param doc the selected document metadata (may be {@code null})
     */
    private void showPreview(DocumentMetadata doc) {
        if (doc == null) {
            return;
        }
        File file = doc.getFilePath() == null ? null : new File(doc.getFilePath());
        if (file == null || !file.exists()) {
            statusLabel.setText("File not found: "
                    + (doc.getFilePath() == null ? doc.getFileName() : doc.getFilePath()));
            previewTextArea.setText("");
            previewImageView.setImage(null);
            currentDocumentText = "";
            return;
        }

        try {
            Image image = services.previewService().renderFirstPage(file);
            previewImageView.setImage(image);

            currentDocumentText = services.previewService().extractText(file);
            previewTextArea.setText(currentDocumentText);
            statusLabel.setText("Previewing: " + doc.getFileName());
        } catch (Exception e) {
            statusLabel.setText("Preview failed: " + e.getMessage());
        }
    }

    /**
     * Shows a simple alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(window());
        alert.showAndWait();
    }

    /**
     * Returns the window that owns this view (for dialog parenting), or null.
     */
    private Window window() {
        return documentListView.getScene() == null ? null : documentListView.getScene().getWindow();
    }
}
