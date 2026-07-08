package com.accountingai.controller;

import com.accountingai.AppServices;
import com.accountingai.util.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the login screen ({@code login-view.fxml}).
 *
 * <p>Validates the username/password against the seeded {@code users} table
 * (default credentials {@code admin} / {@code 1234}). On success it swaps the
 * current window's scene to the main application view.</p>
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    /**
     * Handles the "Login" button. Trims the inputs, checks they are not empty,
     * hashes the password and verifies it against the database. On success loads
     * the main view; on failure shows an inline error message.
     */
    @FXML
    void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        // Basic client-side validation.
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        // Hash the password and verify against the users table.
        String passwordHash = PasswordUtil.sha256(password);
        boolean ok = AppServices.get().userDao().verify(username, passwordHash);

        if (ok) {
            errorLabel.setText("");
            openMainView();
        } else {
            errorLabel.setText("Incorrect username or password.");
            passwordField.clear();
        }
    }

    /**
     * Loads {@code main-view.fxml} (900x600), applies the shared stylesheet and
     * swaps it onto the current stage, making the window resizable.
     */
    private void openMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/accountingai/main-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(
                    getClass().getResource("/com/accountingai/styles.css").toExternalForm());

            // The login button lives on the current stage; reuse it.
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setResizable(true);
            stage.setScene(scene);
            stage.setTitle("Accounting AI Financial Assistant");
            stage.centerOnScreen();
        } catch (Exception e) {
            // Surface any loading problem inline rather than crashing the app.
            errorLabel.setText("Unable to open the application: " + e.getMessage());
        }
    }
}
