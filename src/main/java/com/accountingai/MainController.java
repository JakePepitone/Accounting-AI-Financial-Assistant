package com.accountingai;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class MainController {

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<String> documentListView;

    // initialize the dashboard
    @FXML
    public void initialize() {
        statusLabel.setText("Welcome! Upload feature coming soon.");
    }
}
