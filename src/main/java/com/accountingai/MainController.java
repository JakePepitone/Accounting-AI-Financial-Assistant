package com.accountingai;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.File;
import java.util.List;

public class MainController {

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<String> documentListView;

    private final ObservableList<String> uploadedDocuments = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        statusLabel.setText("Ready");
        documentListView.setItems(uploadedDocuments);
    }

    @FXML
    private void handleUpload(javafx.event.ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Financial Document(s)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            statusLabel.setText("Upload cancelled.");
            return;
        }

        int addedCount = 0;
        for (File file : selectedFiles) {
            if (isValidPdf(file)) {
                uploadedDocuments.add(file.getName());
                addedCount++;
            } else {
                statusLabel.setText("Invalid file skipped: " + file.getName());
            }
        }

        if (addedCount > 0) {
            statusLabel.setText(addedCount + " document(s) uploaded successfully.");
        }
    }

    private boolean isValidPdf(File file) {
        return file != null
                && file.getName().toLowerCase().endsWith(".pdf")
                && file.exists();
    }
}