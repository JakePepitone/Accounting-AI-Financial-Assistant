package com.accountingai.controller;

import com.accountingai.AppServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isBlank()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        boolean ok = AppServices.get().authService().verifyLogin(username, password);

        if (ok) {
            errorLabel.setText("");
            openMainView();
        } else {
            errorLabel.setText("Incorrect username or password.");
            passwordField.clear();
        }
    }

    private void openMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/accountingai/main-view.fxml"));
            Parent root = loader.load();

            // pass the logged-in username to the dashboard
            MainController mainController = loader.getController();
            mainController.setUsername(usernameField.getText().trim());


            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/com/accountingai/styles.css").toExternalForm());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setResizable(true);
            stage.setScene(scene);
            stage.setTitle("Accounting AI Financial Assistant");
            stage.centerOnScreen();
        } catch (Exception e) {
            errorLabel.setText("Unable to open the application: " + e.getMessage());
        }
    }

    /**
     * Handles the "Create Account" link. Loads {@code register-view.fxml}
     * and swaps it onto the current stage.
     */
    @FXML
    void handleCreateAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/accountingai/register-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 500, 650);
            scene.getStylesheets().add(
                    getClass().getResource("/com/accountingai/styles.css").toExternalForm());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Accounting AI Financial Assistant - Create Account");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Unable to open the application: " + e.getMessage());
        }
    }
}
