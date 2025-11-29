package com.app.registre.controller;

import com.app.registre.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.app.registre.util.ConfigUtil;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private VBox root;
    @FXML
    private Label errorLabel;
    @FXML
    private Button btnLogin;
    @FXML
    private Button btnCancel;
    @FXML
    private CheckBox rememberCheck;
    @FXML
    private Hyperlink forgotLink;

    private boolean authenticated = false;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        errorLabel.setText("");
        // Some projects use different fx:id names (e.g. 'user'/'password').
        // Resolve actual controls after the scene is attached.
        Platform.runLater(() -> {
            try {
                // attempt to resolve alternate IDs if injections are null
                if (usernameField == null && root != null) {
                    Node n = root.lookup("#user");
                    if (n instanceof TextField) usernameField = (TextField) n;
                }
                if (passwordField == null && root != null) {
                    Node n = root.lookup("#password");
                    if (n instanceof PasswordField) passwordField = (PasswordField) n;
                }

                // Prefill and remember logic
                String remembered = ConfigUtil.get("remember.username");
                if (usernameField != null) {
                    if (remembered != null && !remembered.isBlank()) {
                        usernameField.setText(remembered);
                        if (rememberCheck != null) rememberCheck.setSelected(true);
                    }
                    // Do NOT prefill the username on first launch; leave empty unless remembered.
                }
                // Do NOT prefill the password field on first launch for security/privacy.
                // Keep passwordField empty so the user must enter their password.
                if (btnLogin != null) btnLogin.setDefaultButton(true);
                if (forgotLink != null) forgotLink.setOnAction(evt -> {
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "La fonctionnalité 'Mot de passe oublié' doit être configurée (envoyer token par email).", javafx.scene.control.ButtonType.OK);
                    a.initOwner((usernameField != null && usernameField.getScene() != null) ? usernameField.getScene().getWindow() : null);
                    a.showAndWait();
                });
            } catch (Exception ignore) {}
        });
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
            // persist remembered username
            if (rememberCheck != null && rememberCheck.isSelected()) {
                ConfigUtil.set("remember.username", user.trim());
            } else {
                ConfigUtil.remove("remember.username");
            }
            authenticated = true;
            // store the current user in session and also cache displayName to avoid extra DB lookup
            String uname = user.trim();
            com.app.registre.Session.setCurrentUser(uname);
            try {
                com.app.registre.model.User uu = new com.app.registre.dao.UserDAO().findByUsername(uname);
                if (uu != null && uu.getDisplayName() != null && !uu.getDisplayName().isBlank()) {
                    com.app.registre.Session.setDisplayName(uu.getDisplayName());
                } else {
                    com.app.registre.Session.setDisplayName(uname);
                }
            } catch (Exception ignore) {
                com.app.registre.Session.setDisplayName(uname);
            }
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
