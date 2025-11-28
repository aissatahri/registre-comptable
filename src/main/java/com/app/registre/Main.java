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
            // Before loading the main UI, ensure there is at least one user and show login dialog
            log.info("Initialisation des utilisateurs et affichage de la fenêtre de connexion...");
            com.app.registre.dao.UserDAO userDAO = new com.app.registre.dao.UserDAO();
            if (userDAO.getUserCount() == 0) {
                // create default admin/admin - user should change this later
                userDAO.createUser("admin", "admin");
                log.info("Compte administrateur par défaut créé: admin / admin (changez le mot de passe)");
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
            loginStage.setScene(loginScene);
            // Apply stylesheet so login looks consistent
            String cssLogin = Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm();
            loginScene.getStylesheets().add(cssLogin);
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

            String css = Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm();
            scene.getStylesheets().add(css);
            log.info("CSS chargé");

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
