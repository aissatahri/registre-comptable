package com.app.registre.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import com.app.registre.dao.RecapDAO;
import java.io.PrintWriter;
import java.io.StringWriter;
import com.app.registre.util.DialogUtils;
import java.util.Objects;

public class MenuController {

    @FXML private StackPane contentArea;
    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Text opsCountMenuText;
    @FXML private Text recettesMenuText;
    @FXML private Text lastSoldeMenuText;
    @FXML private javafx.scene.control.MenuButton userMenu;
    @FXML private Button btnRegistre;
    @FXML private Button btnRecap;
    @FXML private Button btnMois;
    @FXML private Button btnDesignations;
    @FXML private Button btnInitialSolde;
    @FXML private Button btnUserManagement;
    @FXML private Button btnChangeDb;
    private boolean sidebarVisible = true;
    private RecapDAO recapDAO = new RecapDAO();
    private com.app.registre.dao.OperationDAO operationDAO = new com.app.registre.dao.OperationDAO();

    @FXML
    private void showRegistre() {
        loadView("/view/registre.fxml");
        refreshMenuStats();
        setActive(btnRegistre);
    }

    @FXML
    private void showEditInitialSolde() {
        try {
            com.app.registre.model.Operation initial = operationDAO.findInitialOperation();

            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
            DialogUtils.initOwner(dialog, rootPane);
            dialog.setTitle("Modifier le solde initial");
            dialog.setHeaderText("Modifier ou définir le solde initial du registre");

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            javafx.scene.control.Label amountLabel = new javafx.scene.control.Label("Solde :");
            javafx.scene.control.TextField amountField = new javafx.scene.control.TextField(initial != null && initial.getSolde() != null ? String.format("%.2f", initial.getSolde()) : "0.00");
            amountField.setPromptText("0.00");

            javafx.scene.control.Label dateLabel = new javafx.scene.control.Label("Date :");
            javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(initial != null && initial.getDateEmission() != null ? initial.getDateEmission() : java.time.LocalDate.now());

            grid.add(amountLabel, 0, 0);
            grid.add(amountField, 1, 0);
            grid.add(dateLabel, 0, 1);
            grid.add(datePicker, 1, 1);

            dialog.getDialogPane().setContent(grid);

            javafx.scene.control.ButtonType save = new javafx.scene.control.ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

            javafx.scene.control.Button btnSave = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(save);
            btnSave.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                String txt = amountField.getText();
                try {
                    if (txt == null || txt.isBlank()) txt = "0";
                    Double.parseDouble(txt.replace(',', '.'));
                } catch (Exception ex) {
                    ev.consume();
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Montant invalide. Entrez un nombre.", javafx.scene.control.ButtonType.OK);
                    DialogUtils.initOwner(a, rootPane);
                    a.showAndWait();
                }
            });

            java.util.Optional<javafx.scene.control.ButtonType> res = dialog.showAndWait();
            if (res.isPresent() && res.get() == save) {
                double amount = 0.0;
                try { amount = Double.parseDouble(amountField.getText().replace(',', '.')); } catch (Exception ex) { amount = 0.0; }
                java.time.LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : java.time.LocalDate.now();

                if (initial == null) {
                    initial = new com.app.registre.model.Operation();
                    initial.setDesignation("Solde initial");
                    initial.setDateEntree(date);
                    initial.setDateEmission(date);
                    String[] months = {"JANVIER","FEVRIER","MARS","AVRIL","MAI","JUIN","JUILLET","AOUT","SEPTEMBRE","OCTOBRE","NOVEMBRE","DECEMBRE"};
                    initial.setMois(months[date.getMonthValue() - 1]);
                    initial.setMontant(amount);
                    operationDAO.insert(initial);
                } else {
                    initial.setMontant(amount);
                    initial.setDateEmission(date);
                    String[] months = {"JANVIER","FEVRIER","MARS","AVRIL","MAI","JUIN","JUILLET","AOUT","SEPTEMBRE","OCTOBRE","NOVEMBRE","DECEMBRE"};
                    initial.setMois(months[date.getMonthValue() - 1]);
                    operationDAO.update(initial);
                }

                // Recompute soldes for all operations after changing initial balance
                operationDAO.recomputeAllSoldes();

                javafx.scene.control.Alert info = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Solde initial mis à jour.", javafx.scene.control.ButtonType.OK);
                DialogUtils.initOwner(info, rootPane);
                info.showAndWait();
                refreshMenuStats();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'édition du solde initial", e);
        }
    }

    @FXML
    private void showPaiement() {
        // Removed: Paiements view has been deleted
    }

    @FXML
    private void showRecap() {
        loadView("/view/recap.fxml");
        refreshMenuStats();
        setActive(btnRecap);
    }

    @FXML
    private void toggleSidebar() {
        double w = sidebar.getWidth() > 0 ? sidebar.getWidth() : sidebar.getPrefWidth();
        if (sidebarVisible) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(220), sidebar);
            tt.setFromX(0);
            tt.setToX(-w);
            tt.setOnFinished(e -> rootPane.setLeft(null));
            tt.play();
            sidebarVisible = false;
        } else {
            rootPane.setLeft(sidebar);
            sidebar.setTranslateX(-w);
            TranslateTransition tt = new TranslateTransition(Duration.millis(220), sidebar);
            tt.setFromX(-w);
            tt.setToX(0);
            tt.play();
            sidebarVisible = true;
        }
    }

    @FXML
    private void initialize() {
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.isControlDown() && e.getCode() == KeyCode.B) {
                        toggleSidebar();
                        e.consume();
                    }
                });
                refreshMenuStats();
                // no theme applied programmatically
                // show current user in the top-right if available; prefer displayName from Session when present
                try {
                    String u = com.app.registre.Session.getCurrentUser();
                    String dn = com.app.registre.Session.getDisplayName();
                    if (userMenu != null) {
                        String text = "";
                        if (dn != null && !dn.isBlank()) {
                            text = dn;
                        } else if (u != null) {
                            text = u;
                        }
                        userMenu.setText(text);
                        setupUserMenuItems();
                    }
                } catch (Exception ignore) {}
            }
        });
    }

    @FXML
    private void showMois() {
        loadView("/view/mois.fxml");
        refreshMenuStats();
        setActive(btnMois);
    }

    @FXML
    private void showUserManagement() {
        try {
            loadView("/view/user_management.fxml");
            setActive(null);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture de la gestion des utilisateurs", e);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Déconnexion ?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
            DialogUtils.initOwner(confirm, rootPane);
            confirm.setTitle("Déconnexion");
            java.util.Optional<javafx.scene.control.ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == javafx.scene.control.ButtonType.YES) {
                // clear session and show login dialog
                com.app.registre.Session.clear();
                FXMLLoader loginLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/view/login.fxml")));
                Parent loginRoot = loginLoader.load();
                javafx.scene.Scene loginScene = new javafx.scene.Scene(loginRoot);
                javafx.stage.Stage loginStage = new javafx.stage.Stage();
                loginStage.initOwner(rootPane.getScene() != null ? rootPane.getScene().getWindow() : null);
                loginStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
                loginStage.setTitle("Connexion");
                if (rootPane.getScene() != null && rootPane.getScene().getStylesheets() != null) loginScene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
                loginStage.setScene(loginScene);
                loginStage.showAndWait();
                com.app.registre.controller.LoginController ctrl = loginLoader.getController();
                if (ctrl == null || !ctrl.isAuthenticated()) {
                    // user did not authenticate again: exit
                    javafx.application.Platform.exit();
                } else {
                    // authenticated: refresh UI
                    refreshMenuStats();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la déconnexion", e);
        }
    }

    @FXML
    private void showDesignations() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/view/designations.fxml")));
            Parent view = loader.load();

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initOwner(rootPane.getScene() != null ? rootPane.getScene().getWindow() : null);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setTitle("Designations");

            javafx.scene.Scene scene = new javafx.scene.Scene(view);
            // copy application's stylesheets if available
            if (rootPane.getScene() != null && rootPane.getScene().getStylesheets() != null) {
                scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            }

            dialog.setScene(scene);
            dialog.showAndWait();

            refreshMenuStats();
            setActive(btnDesignations);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture de la fenêtre Designations", e);
        }
    }

    @FXML
    private void handleChangeDatabase() {
        try {
            javafx.scene.control.ButtonType create = new javafx.scene.control.ButtonType("Créer une nouvelle", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType open = new javafx.scene.control.ButtonType("Ouvrir existante", javafx.scene.control.ButtonBar.ButtonData.OTHER);
            javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

            javafx.scene.control.Alert choice = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Choisissez une action:", create, open, cancel);
            DialogUtils.initOwner(choice, rootPane);
            choice.setTitle("Changer de base");
            java.util.Optional<javafx.scene.control.ButtonType> opt = choice.showAndWait();
            if (opt.isEmpty() || opt.get() == cancel) return;

            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier SQLite (*.db)", "*.db"));
            String localApp = System.getenv("LOCALAPPDATA");
            if (localApp == null || localApp.isBlank()) localApp = System.getProperty("user.home");
            java.io.File init = new java.io.File(localApp);
            if (init.exists()) fc.setInitialDirectory(init);

            java.io.File chosen = null;
            if (opt.get() == create) {
                fc.setInitialFileName("registre.db");
                chosen = fc.showSaveDialog(rootPane.getScene() != null ? rootPane.getScene().getWindow() : null);
            } else if (opt.get() == open) {
                chosen = fc.showOpenDialog(rootPane.getScene() != null ? rootPane.getScene().getWindow() : null);
            }

            if (chosen == null) return;

            String abs = chosen.getAbsolutePath();

            // Remember choice dialog
            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> rememberDialog = new javafx.scene.control.Dialog<>();
            rememberDialog.initOwner(rootPane.getScene() != null ? rootPane.getScene().getWindow() : null);
            rememberDialog.setTitle("Se souvenir");
            javafx.scene.control.CheckBox remember = new javafx.scene.control.CheckBox("Se souvenir de ce choix (utiliser à chaque démarrage)");
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(new javafx.scene.control.Label("Souhaitez-vous mémoriser cet emplacement ?"), remember);
            box.setSpacing(10);
            rememberDialog.getDialogPane().setContent(box);
            javafx.scene.control.ButtonType yes = new javafx.scene.control.ButtonType("Oui", javafx.scene.control.ButtonBar.ButtonData.YES);
            javafx.scene.control.ButtonType no = new javafx.scene.control.ButtonType("Non", javafx.scene.control.ButtonBar.ButtonData.NO);
            rememberDialog.getDialogPane().getButtonTypes().addAll(yes, no);
            java.util.Optional<javafx.scene.control.ButtonType> r = rememberDialog.showAndWait();
            boolean rememberChoice = r.isPresent() && r.get() == yes && remember.isSelected();

            if (rememberChoice) {
                try {
                    java.nio.file.Path cfgDir = java.nio.file.Paths.get(localApp, "RegistreComptable");
                    java.nio.file.Path cfgFile = cfgDir.resolve("config.properties");
                    if (!java.nio.file.Files.exists(cfgDir)) java.nio.file.Files.createDirectories(cfgDir);
                    java.util.Properties p = new java.util.Properties();
                    p.setProperty("db.path", abs);
                    try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(cfgFile)) { p.store(out, "RegistreComptable configuration"); }
                } catch (Exception ex) { /* ignore */ }
            }

            // Confirm hot-reload (close secondary windows)
            javafx.scene.control.Alert warn = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "L'application va fermer les fenêtres secondaires et recharger la base maintenant. Certaines vues ouvertes peuvent être fermées. Continuer ?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
            DialogUtils.initOwner(warn, rootPane);
            warn.setTitle("Recharger la base");
            java.util.Optional<javafx.scene.control.ButtonType> confirm = warn.showAndWait();
            if (confirm.isEmpty() || confirm.get() != javafx.scene.control.ButtonType.YES) return;

            // Close all windows except main
            javafx.stage.Window mainWindow = rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
            for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                if (w == null) continue;
                if (w != mainWindow) {
                    try { w.hide(); } catch (Exception ignore) {}
                }
            }

            // Apply new DB and reinitialize
            com.app.registre.dao.Database.setDbUrl("jdbc:sqlite:" + abs);
            com.app.registre.dao.Database.getInstance();
            this.recapDAO = new RecapDAO();
            this.operationDAO = new com.app.registre.dao.OperationDAO();
            refreshMenuStats();
            try { showRegistre(); } catch (Exception ex) { /* ignore */ }

            javafx.scene.control.Alert info = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Chemin enregistré et base rechargée.", javafx.scene.control.ButtonType.OK);
            DialogUtils.initOwner(info, rootPane);
            info.setTitle("Base modifiée");
            info.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du changement de base", e);
        }
    }

    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlFile)));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = "Erreur lors du chargement de la vue: " + e.toString();
            showError(msg, e);
        }
    }

    private void refreshMenuStats() {
        try {
            int ops = recapDAO.getTotalOperations();
            double recettes = recapDAO.getTotalRecettes();
            double dernierSolde = recapDAO.getDernierSolde();
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            if (opsCountMenuText != null) opsCountMenuText.setText(String.valueOf(ops));
            if (recettesMenuText != null) recettesMenuText.setText(nf.format(recettes));
            if (lastSoldeMenuText != null) lastSoldeMenuText.setText(nf.format(dernierSolde));
            // update displayed username/displayName from Session (avoid DB lookup when possible)
            try {
                String dn = com.app.registre.Session.getDisplayName();
                String u = com.app.registre.Session.getCurrentUser();
                if (userMenu != null) {
                    String text = "";
                    if (dn != null && !dn.isBlank()) text = dn;
                    else if (u != null) text = u;
                    userMenu.setText(text);
                }
            } catch (Exception ignore) {}
        } catch (Exception ignore) {}
    }

    // create/populate the user menu items (profile, settings, logout)
    private void setupUserMenuItems() {
        try {
            if (userMenu == null) return;
            userMenu.getItems().clear();
            javafx.scene.control.MenuItem profile = new javafx.scene.control.MenuItem("Profil");
            profile.setOnAction(e -> showProfileDialog());
            // accessibility handled on the menu button and tooltip

            javafx.scene.control.MenuItem settings = new javafx.scene.control.MenuItem("Paramètres");
            settings.setOnAction(e -> showSettingsDialog());
            // accessibility handled on the menu button and tooltip

            javafx.scene.control.MenuItem logout = new javafx.scene.control.MenuItem("Déconnexion");
            logout.setOnAction(e -> {
                // reuse existing logout handler
                handleLogout();
            });
            // accessibility handled on the menu button and tooltip

            // accessibility: tooltip and accessible text for the menu button (use Session displayName)
            try {
                String u = com.app.registre.Session.getCurrentUser();
                String dn = com.app.registre.Session.getDisplayName();
                String label = "Compte utilisateur";
                if (dn != null && !dn.isBlank()) label = "Connecté en tant que " + dn;
                else if (u != null) label = "Connecté en tant que " + u;
                javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(label);
                javafx.scene.control.Tooltip.install(userMenu, tip);
                userMenu.setAccessibleText(label);
            } catch (Exception ignore) {}

            userMenu.getItems().addAll(profile, settings, new javafx.scene.control.SeparatorMenuItem(), logout);
        } catch (Exception ignore) {}
    }

    private void showProfileDialog() {
        try {
            String current = com.app.registre.Session.getCurrentUser();
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(12));

            javafx.scene.control.Label userLabel = new javafx.scene.control.Label("Nom d'utilisateur:");
            javafx.scene.control.TextField userField = new javafx.scene.control.TextField(current != null ? current : "");
            userField.setDisable(true); // username not editable here

            javafx.scene.control.Label displayLabel = new javafx.scene.control.Label("Nom affiché:");
            // Load existing profile from DB if available
            com.app.registre.dao.UserDAO udao = new com.app.registre.dao.UserDAO();
            com.app.registre.model.User curUser = null;
            try {
                String currentUser = com.app.registre.Session.getCurrentUser();
                if (currentUser != null) curUser = udao.findByUsername(currentUser);
            } catch (Exception ignore) {}

            javafx.scene.control.TextField displayField = new javafx.scene.control.TextField(curUser != null && curUser.getDisplayName() != null ? curUser.getDisplayName() : "");

            javafx.scene.control.Label emailLabel = new javafx.scene.control.Label("Email:");
            javafx.scene.control.TextField emailField = new javafx.scene.control.TextField(curUser != null && curUser.getEmail() != null ? curUser.getEmail() : "");

            grid.add(userLabel, 0, 0);
            grid.add(userField, 1, 0);
            grid.add(displayLabel, 0, 1);
            grid.add(displayField, 1, 1);
            grid.add(emailLabel, 0, 2);
            grid.add(emailField, 1, 2);

            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
            DialogUtils.initOwner(dialog, rootPane);
            dialog.setTitle("Profil");
            dialog.getDialogPane().setContent(grid);
            javafx.scene.control.ButtonType save = new javafx.scene.control.ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

            java.util.Optional<javafx.scene.control.ButtonType> res = dialog.showAndWait();
            if (res.isPresent() && res.get() == save) {
                try {
                    String currentUser = com.app.registre.Session.getCurrentUser();
                    if (currentUser != null) {
                        boolean ok = new com.app.registre.dao.UserDAO().updateProfile(currentUser, displayField.getText(), emailField.getText());
                        if (ok) {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Profil mis à jour.", javafx.scene.control.ButtonType.OK);
                            DialogUtils.initOwner(a, rootPane);
                            a.showAndWait();
                        } else {
                            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Impossible d'enregistrer le profil.", javafx.scene.control.ButtonType.OK);
                            DialogUtils.initOwner(a, rootPane);
                            a.showAndWait();
                        }
                    }
                } catch (Exception ex) { /* ignore */ }
                // refresh displayed name
                refreshMenuStats();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture du profil", e);
        }
    }

    private void showSettingsDialog() {
        try {
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(12));

            javafx.scene.control.Label rememberLabel = new javafx.scene.control.Label("Se souvenir du nom d'utilisateur:");
            javafx.scene.control.CheckBox rememberBox = new javafx.scene.control.CheckBox();
            String remembered = com.app.registre.util.ConfigUtil.get("remember.username");
            rememberBox.setSelected(remembered != null && !remembered.isBlank());

            grid.add(rememberLabel, 0, 0);
            grid.add(rememberBox, 1, 0);

            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
            DialogUtils.initOwner(dialog, rootPane);
            dialog.setTitle("Paramètres");
            dialog.getDialogPane().setContent(grid);
            javafx.scene.control.ButtonType save = new javafx.scene.control.ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

            java.util.Optional<javafx.scene.control.ButtonType> res = dialog.showAndWait();
            if (res.isPresent() && res.get() == save) {
                try {
                    if (rememberBox.isSelected()) {
                        // keep current username if present
                        String u = com.app.registre.Session.getCurrentUser();
                        if (u != null) com.app.registre.util.ConfigUtil.set("remember.username", u);
                    } else {
                        com.app.registre.util.ConfigUtil.remove("remember.username");
                    }
                    // theme option removed: no theme persisted or applied
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Paramètres enregistrés.", javafx.scene.control.ButtonType.OK);
                    DialogUtils.initOwner(a, rootPane);
                    a.showAndWait();
                } catch (Exception ex) { /* ignore */ }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture des paramètres", e);
        }
    }

    private void setActive(Button active) {
        if (active == null) return;
        java.util.List<Button> all = java.util.Arrays.asList(btnRegistre, btnRecap, btnMois, btnInitialSolde);
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove("active");
        }
        active.getStyleClass().add("active");
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogUtils.initOwner(alert, rootPane);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Create expandable Exception area with full stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setExpanded(true);

        alert.showAndWait();
    }
}
