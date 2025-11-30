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
    private Label versionLabel;
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
        // set version label from manifest or pom properties
        String version = getAppVersion();
        if (version == null || version.isBlank()) version = "dev";
        if (versionLabel != null) versionLabel.setText("Version " + version);
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
                // First-run auto-login / prefill behavior
                try {
                    String autoLogin = ConfigUtil.get("firstRun.autoLogin");
                    String forceChange = ConfigUtil.get("firstRun.forceChange");
                    boolean doAuto = "true".equalsIgnoreCase(autoLogin);
                    boolean mustForce = "true".equalsIgnoreCase(forceChange);
                    if (doAuto) {
                        // attempt auto-login as admin (use stored admin password if provided in config)
                        Platform.runLater(() -> {
                            try {
                                if (usernameField != null) usernameField.setText("admin");
                                String storedPwd = ConfigUtil.get("firstRun.adminPassword");
                                if (storedPwd != null && !storedPwd.isBlank() && passwordField != null) passwordField.setText(storedPwd);
                                handleLogin();
                                if (authenticated && mustForce) {
                                    promptForcePasswordChange();
                                }
                            } catch (Exception ignore) {}
                        });
                    } else {
                        // prefills: if admin was created and no remembered username, prefill with 'admin'
                        String adminCreated = ConfigUtil.get("firstRun.adminCreated");
                        if (adminCreated != null && adminCreated.equalsIgnoreCase("true") && (usernameField.getText() == null || usernameField.getText().isBlank())) {
                            usernameField.setText("admin");
                        }
                    }
                } catch (Exception ignore) {}
            } catch (Exception ignore) {}
        });
    }

    private String getAppVersion() {
        try {
            String v = getClass().getPackage().getImplementationVersion();
            if (v != null && !v.isBlank()) return v;
            // fallback: read pom.properties from META-INF/maven/com.app/registre-comptable/pom.properties
            try (java.io.InputStream in = getClass().getResourceAsStream("/META-INF/maven/com.app/registre-comptable/pom.properties")) {
                if (in != null) {
                    java.util.Properties p = new java.util.Properties();
                    p.load(in);
                    String pv = p.getProperty("version");
                    if (pv != null && !pv.isBlank()) return pv;
                }
            } catch (Exception ignore) {}
            // Additional fallback: read a simple version resource bundled in classpath (helps when running from IDE module output)
            try (java.io.InputStream in2 = getClass().getResourceAsStream("/version.txt")) {
                if (in2 != null) {
                    try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(in2))) {
                        String line = r.readLine();
                        if (line != null && !line.isBlank()) return line.trim();
                    }
                }
            } catch (Exception ignore) {}
        } catch (Exception ignore) {}
        return null;
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
                // After successful login, enforce password-change if required by config
                try {
                    String forceChange = com.app.registre.util.ConfigUtil.get("firstRun.forceChange");
                    boolean mustForce = "true".equalsIgnoreCase(forceChange);
                    if (mustForce) {
                        promptForcePasswordChange();
                    }
                } catch (Exception ignore) {}
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

    /**
     * Prompt the current user to change their password when required on first run.
     */
    private void promptForcePasswordChange() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(12));

            javafx.scene.control.Label newLabel = new javafx.scene.control.Label("Nouveau mot de passe:");
            javafx.scene.control.PasswordField newPass = new javafx.scene.control.PasswordField();
            javafx.scene.control.Label confirmLabel = new javafx.scene.control.Label("Confirmer mot de passe:");
            javafx.scene.control.PasswordField confirmPass = new javafx.scene.control.PasswordField();

            grid.add(newLabel, 0, 0);
            grid.add(newPass, 1, 0);
            grid.add(confirmLabel, 0, 1);
            grid.add(confirmPass, 1, 1);

            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.initOwner(stage);
            dialog.setTitle("Changer le mot de passe");
            dialog.setHeaderText("Vous devez changer votre mot de passe à la première connexion.");
            dialog.getDialogPane().setContent(grid);
            javafx.scene.control.ButtonType save = new javafx.scene.control.ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

            javafx.scene.control.Button btnSave = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(save);
            btnSave.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                String a = newPass.getText();
                String b = confirmPass.getText();
                if (a == null || a.isBlank() || !a.equals(b)) {
                    ev.consume();
                    javafx.scene.control.Alert aerr = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Les mots de passe ne correspondent pas ou sont vides.", javafx.scene.control.ButtonType.OK);
                    aerr.initOwner(stage);
                    aerr.showAndWait();
                }
            });

            java.util.Optional<javafx.scene.control.ButtonType> res = dialog.showAndWait();
            if (res.isPresent() && res.get() == save) {
                try {
                    String currentUser = com.app.registre.Session.getCurrentUser();
                    if (currentUser != null) {
                        boolean ok = new UserDAO().updatePassword(currentUser, newPass.getText());
                        if (ok) {
                            javafx.scene.control.Alert info = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Mot de passe mis à jour.", javafx.scene.control.ButtonType.OK);
                            info.initOwner(stage);
                            info.showAndWait();
                            // clear firstRun.forceChange
                            com.app.registre.util.ConfigUtil.set("firstRun.forceChange", "false");
                        } else {
                            javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Impossible de mettre à jour le mot de passe.", javafx.scene.control.ButtonType.OK);
                            err.initOwner(stage);
                            err.showAndWait();
                        }
                    }
                } catch (Exception ex) { /* ignore */ }
            }
        } catch (Exception ignore) {}
    }

    public boolean isAuthenticated() { return authenticated; }
}
