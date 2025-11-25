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
import java.util.Objects;

public class MenuController {

    @FXML private StackPane contentArea;
    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Text opsCountMenuText;
    @FXML private Text recettesMenuText;
    @FXML private Button btnRegistre;
    @FXML private Button btnPaiement;
    @FXML private Button btnRecap;
    @FXML private Button btnMois;
    private boolean sidebarVisible = true;
    private final RecapDAO recapDAO = new RecapDAO();

    @FXML
    private void showRegistre() {
        loadView("/view/registre.fxml");
        refreshMenuStats();
        setActive(btnRegistre);
    }

    @FXML
    private void showPaiement() {
        loadView("/view/paiement.fxml");
        refreshMenuStats();
        setActive(btnPaiement);
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
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            if (opsCountMenuText != null) opsCountMenuText.setText(String.valueOf(ops));
            if (recettesMenuText != null) recettesMenuText.setText(nf.format(recettes));
        } catch (Exception ignore) {}
    }

    private void setActive(Button active) {
        if (active == null) return;
        java.util.List<Button> all = java.util.Arrays.asList(btnRegistre, btnPaiement, btnRecap, btnMois);
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().remove("active");
        }
        active.getStyleClass().add("active");
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
