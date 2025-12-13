package com.app.registre.controller;

import com.app.registre.dao.OperationDAO;
import com.app.registre.model.Operation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.io.File;
import java.io.IOException;
import com.app.registre.util.ExcelUtil;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class RechercheController {

    @FXML private TextField artField;
    @FXML private TextField parField;
    @FXML private TextField ligField;
    @FXML private javafx.scene.control.ComboBox<String> moisBox;
    @FXML private javafx.scene.control.ComboBox<Integer> anneeBox;
    @FXML private javafx.scene.control.Button btnSearch;
    @FXML private TableView<Operation> table;
    @FXML private javafx.scene.control.Label resultCountLabel;
    @FXML private TableColumn<Operation, Integer> colId;
    @FXML private TableColumn<Operation, Integer> colArt;
    @FXML private TableColumn<Operation, Integer> colPar;
    @FXML private TableColumn<Operation, Integer> colLig;
    @FXML private TableColumn<Operation, String> colImp;
    @FXML private TableColumn<Operation, String> colDesignation;
    @FXML private TableColumn<Operation, String> colNature;
    @FXML private TableColumn<Operation, Integer> colN;
    @FXML private TableColumn<Operation, String> colBudg;
    @FXML private TableColumn<Operation, String> colExercice;
    @FXML private TableColumn<Operation, String> colBeneficiaire;
    @FXML private TableColumn<Operation, String> colDateEmission;
    @FXML private TableColumn<Operation, Integer> colOpOr;
    @FXML private TableColumn<Operation, Integer> colOvCheq;
    @FXML private TableColumn<Operation, Double> colRecette;
    @FXML private TableColumn<Operation, Double> colSurRam;
    @FXML private TableColumn<Operation, Double> colSurEng;
    @FXML private TableColumn<Operation, Double> colDepense;
    @FXML private TableColumn<Operation, Double> colMontant;

    private OperationDAO operationDAO = new OperationDAO();
    private final ObservableList<Operation> data = FXCollections.observableArrayList();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private PauseTransition debounce;
    private final String[] MONTHS = {"","JANVIER","FEVRIER","MARS","AVRIL","MAI","JUIN","JUILLET","AOUT","SEPTEMBRE","OCTOBRE","NOVEMBRE","DECEMBRE"};

    private String monthToFrench(LocalDate d) {
        if (d == null) return "";
        int m = d.getMonthValue();
        if (m < 1 || m >= MONTHS.length) return "";
        return MONTHS[m];
    }

    @FXML
    private void initialize() {
        // columns in the same order as main register view
        colArt.setCellValueFactory(new PropertyValueFactory<>("art"));
        colPar.setCellValueFactory(new PropertyValueFactory<>("par"));
        colLig.setCellValueFactory(new PropertyValueFactory<>("lig"));
        colImp.setCellValueFactory(new PropertyValueFactory<>("imp"));
        colDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));
        colNature.setCellValueFactory(new PropertyValueFactory<>("nature"));
        colN.setCellValueFactory(new PropertyValueFactory<>("n"));
        colBudg.setCellValueFactory(new PropertyValueFactory<>("budg"));
        colExercice.setCellValueFactory(new PropertyValueFactory<>("exercice"));
        colBeneficiaire.setCellValueFactory(new PropertyValueFactory<>("beneficiaire"));
        colDateEmission.setCellValueFactory(cell -> {
            Operation o = cell.getValue();
            String d = o.getDateEmission() != null ? o.getDateEmission().format(df) : "";
            return new javafx.beans.property.SimpleStringProperty(d);
        });
        colOpOr.setCellValueFactory(new PropertyValueFactory<>("opOr"));
        colOvCheq.setCellValueFactory(new PropertyValueFactory<>("ovCheq"));
        colRecette.setCellValueFactory(new PropertyValueFactory<>("recette"));
        colSurRam.setCellValueFactory(new PropertyValueFactory<>("surRam"));
        colSurEng.setCellValueFactory(new PropertyValueFactory<>("surEng"));
        colDepense.setCellValueFactory(new PropertyValueFactory<>("depense"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("solde"));

        table.setItems(data);
        // populate month/year selectors
        moisBox.getItems().addAll(java.util.Arrays.asList(MONTHS).subList(1, MONTHS.length));
        moisBox.getItems().add(0, "");
        moisBox.setValue("");
        // Populate year combo with years present in the operations (descending)
        try {
            java.util.List<Operation> allOps = operationDAO.findAll();
            java.util.Set<Integer> years = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
            for (Operation o : allOps) {
                if (o == null) continue;
                if (o.getDateEmission() != null) {
                    years.add(o.getDateEmission().getYear());
                } else if (o.getExercice() != null) {
                    try { years.add(Integer.parseInt(o.getExercice())); } catch (NumberFormatException ignore) {}
                }
            }
            if (years.isEmpty()) {
                int currentYear = java.time.LocalDate.now().getYear();
                for (int y = currentYear; y >= currentYear - 10; y--) years.add(y);
            }
            anneeBox.getItems().addAll(years);
            anneeBox.setValue(null);
        } catch (Exception ex) {
            int currentYear = java.time.LocalDate.now().getYear();
            for (int y = currentYear; y >= currentYear - 10; y--) anneeBox.getItems().add(y);
            anneeBox.setValue(null);
        }
        // debounce to avoid firing a DB query on every keystroke
        debounce = new PauseTransition(Duration.millis(300));
        debounce.setOnFinished(e -> runSearchAsync());

        artField.textProperty().addListener((obs, oldV, newV) -> debounce.playFromStart());
        parField.textProperty().addListener((obs, oldV, newV) -> debounce.playFromStart());
        ligField.textProperty().addListener((obs, oldV, newV) -> debounce.playFromStart());
        moisBox.valueProperty().addListener((obs, oldV, newV) -> debounce.playFromStart());
        anneeBox.valueProperty().addListener((obs, oldV, newV) -> debounce.playFromStart());

        // initial load
        runSearchAsync();
    }

    @FXML
    private void onSearch() {
        debounce.stop();
        runSearchAsync();
    }

    @FXML
    private void onReset() {
        artField.clear(); parField.clear(); ligField.clear();
        debounce.stop();
        runSearchAsync();
    }

    private void runSearchAsync() {
        final Integer art = parseIntOrNull(artField.getText());
        final Integer par = parseIntOrNull(parField.getText());
        final Integer lig = parseIntOrNull(ligField.getText());
        final String mois = (moisBox.getValue() != null && !moisBox.getValue().isBlank()) ? moisBox.getValue() : null;
        final Integer annee = anneeBox.getValue();

        Task<List<Operation>> task = new Task<>() {
            @Override
            protected List<Operation> call() throws Exception {
                return operationDAO.findByArtParLig(art, par, lig);
            }
        };

        task.setOnSucceeded(evt -> {
            List<Operation> ops = task.getValue();
            if (mois != null || annee != null) {
                java.util.List<Operation> filtered = new java.util.ArrayList<>();
                for (Operation o : ops) {
                    boolean keep = true;
                    if (mois != null && !mois.isBlank()) {
                        String moisOp = o.getMois() != null ? o.getMois() : "";
                        String moisFromDate = monthToFrench(o.getDateEmission());
                        if (!mois.equalsIgnoreCase(moisOp) && !mois.equalsIgnoreCase(moisFromDate)) {
                            keep = false;
                        }
                    }
                    if (annee != null) {
                        java.time.LocalDate d = o.getDateEmission();
                        if (d == null || d.getYear() != annee) keep = false;
                    }
                    if (keep) filtered.add(o);
                }
                data.setAll(filtered);
                if (resultCountLabel != null) resultCountLabel.setText(filtered.size() + " opérations");
            } else {
                data.setAll(ops);
                if (resultCountLabel != null) resultCountLabel.setText(ops.size() + " opérations");
            }
        });

        task.setOnFailed(evt -> {
            // silently ignore or log; keep UI responsive
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task, "recherche-search-thread");
        t.setDaemon(true);
        t.start();
    }

    private Integer parseIntOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    @FXML
    private void onExportExcel() {
        // Export the currently filtered results
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter vers Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
        String defaultName = "registre_export_" + java.time.LocalDate.now() + ".xlsx";
        fileChooser.setInitialFileName(defaultName);
        File file = fileChooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        try {
            java.util.List<Operation> toExport = new java.util.ArrayList<>(data);
            ExcelUtil.exportOperationsToExcel(toExport, file.getAbsolutePath());
            Alert info = new Alert(Alert.AlertType.INFORMATION, "Exportation terminée: " + file.getAbsolutePath());
            info.showAndWait();
        } catch (IOException e) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'export: " + e.getMessage());
            err.showAndWait();
        }
    }
}
