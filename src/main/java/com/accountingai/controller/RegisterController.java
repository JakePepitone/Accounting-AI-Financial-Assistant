package com.accountingai.controller;

import com.accountingai.AppServices;
import com.accountingai.service.AuthService.RegistrationResult;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;


    @FXML
    private TextField newUsernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label registerErrorLabel;

    @FXML
    private void handleRegister() {

        // Combine first and last name into a full name string
        String firstName = text(firstNameField).trim();
        String lastName = text(lastNameField).trim();
        String fullName = (firstName + " " + lastName).trim();

        RegistrationResult result = AppServices.get().authService().register(
                fullName,
                text(newUsernameField),
                text(emailField),
                text(newPasswordField),
                text(confirmPasswordField));

        registerErrorLabel.setText(result.message());
        if (result.success()) {
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else if ("Passwords do not match.".equals(result.message())) {
            confirmPasswordField.clear();
        }
    }

    private String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/accountingai/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 500, 650);
            scene.getStylesheets().add(
                    getClass().getResource("/com/accountingai/styles.css").toExternalForm());

            Stage stage = (Stage) newUsernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Accounting AI Financial Assistant");
            stage.centerOnScreen();
        } catch (Exception e) {
            registerErrorLabel.setText("Unable to return to login: " + e.getMessage());
        }
    }
}
