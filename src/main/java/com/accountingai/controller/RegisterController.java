package com.accountingai.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import com.accountingai.db.DatabaseManager;
import com.accountingai.db.dao.UserDao;
import com.accountingai.model.User;
import com.accountingai.util.PasswordUtil;

public class RegisterController {

    private final UserDao userDao = new UserDao(new DatabaseManager());

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
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String username = newUsernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = newPasswordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            registerErrorLabel.setText("Please fill in all fields.");
            return;
        }

        if (!isValidEmail(email)) {
            registerErrorLabel.setText("Please enter a valid email address.");
            return;
        }

        if (!password.equals(confirm)) {
            registerErrorLabel.setText("Passwords do not match.");
            confirmPasswordField.clear();
            return;
        }

        String passwordHash = PasswordUtil.sha256(password);

        User user = new User(
                firstName,
                lastName,
                username,
                email,
                passwordHash
        );

        int userId = userDao.insert(user);
        if (userId > 0) {
            registerErrorLabel.setText("Account created! )");
        }
        else {
            registerErrorLabel.setText("Unable to create account.");
        }
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".") && email.indexOf("@") < email.lastIndexOf(".");
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