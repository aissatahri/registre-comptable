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
    @FXML private Button btnRegistre;
    @FXML private Button btnRecap;
    @FXML private Button btnMois;
    @FXML private Button btnDesignations;
    @FXML private Button btnInitialSolde;
    private boolean sidebarVisible = true;
    private final RecapDAO recapDAO = new RecapDAO();
    private final com.app.registre.dao.OperationDAO operationDAO = new com.app.registre.dao.OperationDAO();

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
    private void showDesignations() {
        loadView("/view/designations.fxml");
        refreshMenuStats();
        setActive(btnDesignations);
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
        } catch (Exception ignore) {}
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
