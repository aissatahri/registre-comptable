package com.app.registre.controller;

import com.app.registre.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    private Button btnLogin;
    @FXML
    private Button btnCancel;

    private boolean authenticated = false;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        errorLabel.setText("");
        try {
            // Prefill with admin/admin to ease initial login if default account exists
            if (usernameField != null && (usernameField.getText() == null || usernameField.getText().isBlank())) usernameField.setText("admin");
            if (passwordField != null && (passwordField.getText() == null || passwordField.getText().isBlank())) passwordField.setText("admin");
            if (btnLogin != null) btnLogin.setDefaultButton(true);
        } catch (Exception ignore) {}
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        if (user == null || user.isBlank()) {
            errorLabel.setText("Nom d'utilisateur requis");
            return;
        }
        if (pass == null) pass = "";
        boolean ok = userDAO.validateUser(user.trim(), pass);
        if (ok) {
            authenticated = true;
            // store the current user in session
            com.app.registre.Session.setCurrentUser(user.trim());
            close();
        } else {
            errorLabel.setText("Identifiants invalides");
        }
    }

    @FXML
    private void handleCancel() {
        authenticated = false;
        close();
    }

    private void close() {
        Stage s = (Stage) usernameField.getScene().getWindow();
        s.close();
    }

    public boolean isAuthenticated() { return authenticated; }
}
