package com.accountingai;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainController {

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<String> documentListView;

    @FXML
    private Button uploadButton;

    @FXML
    protected void onUploadButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Financial Document");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            documentListView.getItems().add(selectedFile.getName());
            statusLabel.setText("Uploaded: " + selectedFile.getName());
        } else {
            statusLabel.setText("No file selected.");
        }
    }
}
