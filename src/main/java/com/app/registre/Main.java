package com.app.registre;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Before loading the main UI, ask the user where to store/load the database
            chooseDatabaseLocation(primaryStage);

            // After DB URL is set, ensure there is at least one user and show login dialog
            log.info("Initialisation des utilisateurs et affichage de la fenêtre de connexion...");
            com.app.registre.dao.UserDAO userDAO = new com.app.registre.dao.UserDAO();
            if (userDAO.getUserCount() == 0) {
                // create default admin/admin - user should change this later
                userDAO.createUser("admin", "admin");
                log.info("Compte administrateur par défaut créé: admin / admin (changez le mot de passe)");
                // Persist first-run flags so UI and installer can detect this state
                try {
                    String localApp2 = System.getenv("LOCALAPPDATA");
                    if (localApp2 == null || localApp2.isBlank()) localApp2 = System.getProperty("user.home");
                    java.nio.file.Path cfgDir2 = java.nio.file.Paths.get(localApp2, "RegistreComptable");
                    java.nio.file.Path cfgFile2 = cfgDir2.resolve("config.properties");
                    if (!java.nio.file.Files.exists(cfgDir2)) java.nio.file.Files.createDirectories(cfgDir2);
                    java.util.Properties props = new java.util.Properties();
                    if (java.nio.file.Files.exists(cfgFile2)) {
                        try (java.io.InputStream in = java.nio.file.Files.newInputStream(cfgFile2)) { props.load(in); }
                    }
                    props.setProperty("firstRun.adminCreated", "true");
                    props.setProperty("firstRun.forceChange", "true");
                    props.setProperty("firstRun.autoLogin", "false");
                    try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(cfgFile2)) { props.store(out, "RegistreComptable configuration"); }
                } catch (Exception ex) {
                    log.warn("Unable to persist first-run flags: {}", ex.getMessage());
                }
            }

            // Load login dialog
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent loginRoot = loginLoader.load();
            Scene loginScene = new Scene(loginRoot);
            Stage loginStage = new Stage();
            loginStage.initOwner(primaryStage);
            loginStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            loginStage.setResizable(false);
            loginStage.setTitle("Connexion");
            // Set the same application icon on the login dialog
            try {
                Image loginIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png")));
                loginStage.getIcons().add(loginIcon);
            } catch (Exception ex) {
                log.warn("Unable to load login icon: {}", ex.getMessage());
            }
            loginStage.setScene(loginScene);
            // Apply stylesheet so login looks consistent (guard against missing resource)
            java.net.URL cssLoginUrl = getClass().getResource("/style.css");
            if (loginScene != null && cssLoginUrl != null) {
                try {
                    loginScene.getStylesheets().add(cssLoginUrl.toExternalForm());
                } catch (Exception ex) {
                    log.warn("Unable to apply login stylesheet: {}", ex.getMessage());
                }
            }
            // Show and wait for authentication
            loginStage.showAndWait();
            com.app.registre.controller.LoginController loginController = loginLoader.getController();
            if (loginController == null || !loginController.isAuthenticated()) {
                log.info("Utilisateur non authentifié: arrêt de l'application");
                javafx.application.Platform.exit();
                return;
            }

            log.info("Chargement du FXML principal...");
            // Méthode plus explicite pour charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/menu.fxml"));
            Parent root = loader.load();

            log.info("FXML chargé avec succès");

            Scene scene = new Scene(root, 1200, 800);

            java.net.URL cssUrl = getClass().getResource("/style.css");
            if (scene != null && cssUrl != null) {
                try {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    log.info("CSS chargé");
                } catch (Exception ex) {
                    log.warn("Unable to apply main stylesheet: {}", ex.getMessage());
                }
            } else {
                log.warn("Main stylesheet not found or scene is null");
            }

            primaryStage.setTitle("Système de Gestion Comptable - Registre");

            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png")));
            primaryStage.getIcons().add(icon);
            log.info("Icône chargée");

            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            // After showing the main window, ensure that an initial balance is set when DB is empty
            ensureInitialBalance(primaryStage);

            log.info("Application démarrée avec succès");

        } catch (Exception e) {
            log.error("Erreur lors du démarrage: {}", e.getMessage(), e);

            // Fallback: afficher une erreur
            showErrorScreen(primaryStage, "Erreur: " + e.getMessage());
        }
    }

    /**
     * Show a small dialog at startup to let the user choose to create a new DB file,
     * open an existing one, or use the default location.
     */
    private void chooseDatabaseLocation(Stage owner) {
        try {
            // Check for saved config in %LOCALAPPDATA%\RegistreComptable\config.properties
            String localApp = System.getenv("LOCALAPPDATA");
            if (localApp == null || localApp.isBlank()) localApp = System.getProperty("user.home");
            java.nio.file.Path cfgDir = java.nio.file.Paths.get(localApp, "RegistreComptable");
            java.nio.file.Path cfgFile = cfgDir.resolve("config.properties");
            if (java.nio.file.Files.exists(cfgFile)) {
                try (java.io.InputStream in = java.nio.file.Files.newInputStream(cfgFile)) {
                    java.util.Properties p = new java.util.Properties();
                    p.load(in);
                    String saved = p.getProperty("db.path");
                    if (saved != null && !saved.isBlank()) {
                        // Trim whitespace and remove surrounding quotes if present
                        saved = saved.trim();
                        if ((saved.startsWith("\"") && saved.endsWith("\"")) || (saved.startsWith("'") && saved.endsWith("'"))) {
                            saved = saved.substring(1, saved.length() - 1);
                        }

                        // Support both forms: either the stored value is a plain file path
                        // (e.g. C:\Users\\...\\registre.db) or a full JDBC URL
                        // If the installer wrote a full JDBC URL, migrate it to store only the path
                        if (saved.startsWith("jdbc:sqlite:")) {
                            String extracted = saved.substring("jdbc:sqlite:".length());
                            try {
                                // overwrite stored config with the plain path for future runs
                                saveConfig(cfgDir, cfgFile, extracted);
                                saved = extracted;
                            } catch (Exception ex) {
                                log.warn("Unable to migrate stored JDBC URL to plain path: {}", ex.getMessage());
                            }
                        }

                        String url = saved.startsWith("jdbc:sqlite:") ? saved : "jdbc:sqlite:" + saved;
                        com.app.registre.dao.Database.setDbUrl(url);

                        // Validate the connection — if it fails, remove the bad config and continue
                        try {
                            java.sql.Connection testConn = com.app.registre.dao.Database.getInstance().getConnection();
                            if (testConn == null || testConn.isClosed()) throw new Exception("Unable to open DB connection");
                            // connection OK: skip the chooser dialog
                            return;
                        } catch (Exception ex) {
                            log.warn("Saved db.path appears invalid ({}): {}.", saved, ex.getMessage());

                            // Ask the user if they want to choose a different DB now
                            javafx.application.Platform.runLater(() -> {
                                javafx.scene.control.ButtonType choose = new javafx.scene.control.ButtonType("Choisir une base", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                                javafx.scene.control.ButtonType ignore = new javafx.scene.control.ButtonType("Ignorer", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                                        "Le chemin enregistré vers la base de données est invalide. Voulez-vous choisir un autre fichier de base de données ?",
                                        choose, ignore);
                                // Ne pas définir owner si le stage n'a pas encore de scène
                                if (owner != null && owner.getScene() != null) {
                                    a.initOwner(owner);
                                }
                                a.setTitle("Base de données invalide");
                                java.util.Optional<javafx.scene.control.ButtonType> r = a.showAndWait();
                                if (r.isPresent() && r.get() == choose) {
                                    javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                                    fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier SQLite (*.db)", "*.db"));
                                    java.io.File initial = new java.io.File(System.getenv("LOCALAPPDATA") != null ? System.getenv("LOCALAPPDATA") : System.getProperty("user.home"));
                                    if (initial.exists()) fc.setInitialDirectory(initial);
                                    java.io.File chosen = fc.showOpenDialog(owner);
                                    if (chosen != null) {
                                        String abs = chosen.getAbsolutePath();
                                        String newUrl = "jdbc:sqlite:" + abs;
                                        com.app.registre.dao.Database.setDbUrl(newUrl);
                                        // persist the chosen path
                                        try { saveConfig(cfgDir, cfgFile, abs); } catch (Exception saveEx) { log.warn("Unable to save chosen DB path: {}", saveEx.getMessage()); }
                                    }
                                } else {
                                    // user ignored: remove the bad config so chooser will appear below
                                    try { java.nio.file.Files.deleteIfExists(cfgFile); } catch (Exception delEx) { log.warn("Could not delete bad config file: {}", delEx.getMessage()); }
                                }
                            });
                            // fall through to show chooser dialog
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Impossible de lire le fichier de config, continuer: {}", ex.getMessage());
                }
            }
            javafx.scene.control.ButtonType create = new javafx.scene.control.ButtonType("Créer une nouvelle", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType open = new javafx.scene.control.ButtonType("Ouvrir existante", javafx.scene.control.ButtonBar.ButtonData.OTHER);
            javafx.scene.control.ButtonType useDefault = new javafx.scene.control.ButtonType("Utiliser emplacement par défaut", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "Choisissez où stocker ou charger la base de données.", create, open, useDefault);
            // Ne pas définir owner si le stage n'a pas encore de scène
            if (owner != null && owner.getScene() != null) {
                alert.initOwner(owner);
            }
            alert.setTitle("Emplacement de la base de données");
            alert.setHeaderText("Sélectionnez une option");

            // Add a remember checkbox to the dialog content area BEFORE showing it
            javafx.scene.control.CheckBox remember = new javafx.scene.control.CheckBox("Se souvenir de ce choix");
            // place checkbox under the alert content
            javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox();
            contentBox.setSpacing(10);
            contentBox.getChildren().addAll(new javafx.scene.control.Label("Choisissez où stocker ou charger la base de données."), remember);
            alert.getDialogPane().setContent(contentBox);

            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichier SQLite (*.db)", "*.db"));
            java.io.File initial = new java.io.File(localApp);
            if (initial.exists()) fc.setInitialDirectory(initial);

            java.util.Optional<javafx.scene.control.ButtonType> opt = alert.showAndWait();
            if (opt.isPresent()) {
                if (opt.get() == create) {
                    fc.setInitialFileName("registre.db");
                    java.io.File chosen = fc.showSaveDialog(owner);
                    if (chosen != null) {
                        String abs = chosen.getAbsolutePath();
                        String url = "jdbc:sqlite:" + abs;
                        com.app.registre.dao.Database.setDbUrl(url);
                        if (remember.isSelected()) saveConfig(cfgDir, cfgFile, abs);
                    }
                } else if (opt.get() == open) {
                    java.io.File chosen = fc.showOpenDialog(owner);
                    if (chosen != null) {
                        String abs = chosen.getAbsolutePath();
                        String url = "jdbc:sqlite:" + abs;
                        com.app.registre.dao.Database.setDbUrl(url);
                        if (remember.isSelected()) saveConfig(cfgDir, cfgFile, abs);
                    }
                } else {
                    // use default: create default path under %LOCALAPPDATA%\RegistreComptable\registre.db
                    try {
                        java.nio.file.Path defaultDb = cfgDir.resolve("registre.db");
                        String abs = defaultDb.toAbsolutePath().toString();
                        String url = "jdbc:sqlite:" + abs;
                        com.app.registre.dao.Database.setDbUrl(url);
                        if (remember.isSelected()) saveConfig(cfgDir, cfgFile, abs);
                    } catch (Exception ex) {
                        log.warn("Unable to set default DB path: {}", ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Erreur lors du choix de l'emplacement DB, utilisation de la valeur par défaut: {}", e.getMessage());
        }
    }

    private void saveConfig(java.nio.file.Path cfgDir, java.nio.file.Path cfgFile, String absolutePath) {
        try {
            if (!java.nio.file.Files.exists(cfgDir)) java.nio.file.Files.createDirectories(cfgDir);
            java.util.Properties p = new java.util.Properties();
            p.setProperty("db.path", absolutePath);
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(cfgFile)) {
                p.store(out, "RegistreComptable configuration");
            }
        } catch (Exception ex) {
            log.warn("Impossible d'enregistrer la configuration: {}", ex.getMessage());
        }
    }

    private void ensureInitialBalance(Stage owner) {
        try {
            com.app.registre.dao.OperationDAO dao = new com.app.registre.dao.OperationDAO();
            int count = dao.countOperations();
            if (count > 0) return; // already have operations

            // Build dialog to ask for initial solde and date
            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.setTitle("Solde initial");
            dialog.setHeaderText("Aucun enregistrement trouvé. Veuillez saisir le solde initial.");

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            javafx.scene.control.Label amountLabel = new javafx.scene.control.Label("Solde :");
            javafx.scene.control.TextField amountField = new javafx.scene.control.TextField("0.00");
            amountField.setPromptText("0.00");

            javafx.scene.control.Label dateLabel = new javafx.scene.control.Label("Date :");
            javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker(java.time.LocalDate.now());

            grid.add(amountLabel, 0, 0);
            grid.add(amountField, 1, 0);
            grid.add(dateLabel, 0, 1);
            grid.add(datePicker, 1, 1);

            dialog.getDialogPane().setContent(grid);

            javafx.scene.control.ButtonType save = new javafx.scene.control.ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancel = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(save, cancel);

            // validate amount input
            javafx.scene.control.Button btnSave = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(save);
            btnSave.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
                String txt = amountField.getText();
                try {
                    if (txt == null || txt.isBlank()) txt = "0";
                    Double.parseDouble(txt.replace(',', '.'));
                    // ok
                } catch (Exception ex) {
                    ev.consume();
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Montant invalide. Entrez un nombre.", javafx.scene.control.ButtonType.OK);
                    a.initOwner(owner);
                    a.showAndWait();
                }
            });

            java.util.Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();
            double amount = 0.0;
            java.time.LocalDate date = java.time.LocalDate.now();
            if (result.isPresent() && result.get() == save) {
                try {
                    amount = Double.parseDouble(amountField.getText().replace(',', '.'));
                } catch (Exception ex) { amount = 0.0; }
                date = datePicker.getValue() != null ? datePicker.getValue() : java.time.LocalDate.now();
            } else {
                // user cancelled: create initial zero solde with today's date
                amount = 0.0;
                date = java.time.LocalDate.now();
            }

            // insert initial operation
            com.app.registre.model.Operation op = new com.app.registre.model.Operation();
            op.setDesignation("Solde initial");
            op.setMontant(amount);
            op.setDateEntree(date);
            // Ensure the initial balance has a date_emission so date-based lookups include it
            op.setDateEmission(date);
            // set mois based on date
            String[] months = {"JANVIER","FEVRIER","MARS","AVRIL","MAI","JUIN","JUILLET","AOUT","SEPTEMBRE","OCTOBRE","NOVEMBRE","DECEMBRE"};
            op.setMois(months[date.getMonthValue()-1]);

            dao.insert(op);

            javafx.scene.control.Alert info = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Solde initial enregistré: " + String.format("%.2f", amount), javafx.scene.control.ButtonType.OK);
            info.initOwner(owner);
            info.showAndWait();

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du solde initial: {}", e.getMessage(), e);
        }
    }

    private void showErrorScreen(Stage primaryStage, String message) {
        try {
            javafx.scene.control.Label label = new javafx.scene.control.Label(message);
            label.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-padding: 20px;");

            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(label);
            root.setAlignment(javafx.geometry.Pos.CENTER);

            Scene scene = new Scene(root, 600, 400);
            primaryStage.setTitle("Erreur - Registre Comptable");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception ex) {
            System.err.println("Erreur critique: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        log.info("Lancement de l'application...");
        // Petit helper de débogage: si la propriété système 'inspectSchema' est true,
        // exécuter l'inspecteur de schéma et quitter sans démarrer l'UI.
        if (Boolean.getBoolean("inspectSchema")) {
            com.app.registre.tools.SchemaInspector.main(args);
            return;
        }
        // Helper: run a simple test inserter when 'runInsertTest' property is true
        if (Boolean.getBoolean("runInsertTest")) {
            com.app.registre.tools.TestInsertOperation.main(args);
            return;
        }
        launch(args);
    }
}
