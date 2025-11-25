package com.app.registre.controller;

import com.app.registre.dao.OperationDAO;
import com.app.registre.model.Operation;
import com.app.registre.util.ExcelUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class MoisController {

    @FXML private ComboBox<String> moisComboBox;
    @FXML private Text moisSelectedText;
    @FXML private Text moisOperationsText;
    @FXML private Text moisMontantText;
    @FXML private Text moisMoyenneText;
    @FXML private TableView<Operation> moisOperationsTable;
    @FXML private TableView<?> natureTable;
    @FXML private TableColumn<Operation, String> colOp;
    @FXML private TableColumn<Operation, String> colNature;
    @FXML private TableColumn<Operation, String> colBudg;
    @FXML private TableColumn<Operation, Double> colMontant;
    @FXML private TableColumn<Operation, LocalDate> colDateEntree;
    @FXML private TableColumn<Operation, LocalDate> colDateVisa;
    @FXML private TableColumn<Operation, LocalDate> colDateRejet;
    @FXML private TableColumn<Operation, String> colMotifRejet;
    @FXML private TableColumn<Operation, LocalDate> colDateReponse;
    @FXML private TableColumn<Operation, String> colContenuReponse;
    @FXML private TableColumn<Operation, String> colDecision;
    @FXML private javafx.scene.control.MenuButton columnsMenu;

    private ObservableList<Operation> operations;
    private OperationDAO operationDAO;

    public void initialize() {
        operationDAO = new OperationDAO();
        setupMoisComboBox();
        setupColumns();
        moisOperationsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setupColumnsMenu();
        if (moisComboBox.getValue() == null && !moisComboBox.getItems().isEmpty()) {
            moisComboBox.setValue(moisComboBox.getItems().get(0));
        }
        loadMoisData();
    }

    private void setupMoisComboBox() {
        moisComboBox.getItems().addAll(
                "JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"
        );
    }

    private void setupColumns() {
        colOp.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("op"));
        colNature.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nature"));
        colBudg.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("budg"));
        colMontant.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("montant"));
        colDateEntree.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateEntree"));
        colDateVisa.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateVisa"));
        colDateRejet.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateRejet"));
        colMotifRejet.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("motifRejet"));
        colDateReponse.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateReponse"));
        colContenuReponse.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("contenuReponse"));
        colDecision.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("decision"));

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colDateEntree.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : df.format(item));
            }
        });
        colDateVisa.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : df.format(item));
            }
        });
        colDateRejet.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : df.format(item));
            }
        });
        colDateReponse.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : df.format(item));
            }
        });

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        colMontant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : nf.format(item));
            }
        });
    }

    private void setupColumnsMenu() {
        if (columnsMenu == null) return;
        columnsMenu.getItems().clear();
        java.util.List<TableColumn<?, ?>> cols = new java.util.ArrayList<>(moisOperationsTable.getColumns());
        for (TableColumn<?, ?> c : cols) {
            javafx.scene.control.CheckMenuItem item = new javafx.scene.control.CheckMenuItem(c.getText());
            item.setSelected(c.isVisible());
            item.selectedProperty().addListener((o, ov, nv) -> c.setVisible(nv));
            columnsMenu.getItems().add(item);
        }
    }

    @FXML
    private void loadMoisData() {
        String mois = moisComboBox.getValue();
        if (mois != null) {
            List<Operation> operationList = operationDAO.findByMois(mois);
            operations = FXCollections.observableArrayList(operationList);
            moisOperationsTable.setItems(operations);
            autoResizeColumns();

            updateMoisStatistics(mois);
        }
    }

    private void updateMoisStatistics(String mois) {
        moisSelectedText.setText(mois);

        int count = operations.size();
        double total = operationDAO.getTotalMontantByMois(mois);
        double moyenne = count > 0 ? total / count : 0;

        moisOperationsText.setText(String.valueOf(count));
        moisMontantText.setText(String.format("%,.2f", total));
        moisMoyenneText.setText(String.format("%,.2f", moyenne));
    }

    @FXML
    private void exportMoisToExcel() {
        String mois = moisComboBox.getValue();
        if (mois == null || operations == null || operations.isEmpty()) {
            showInfo("Veuillez sélectionner un mois avec des opérations");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le mois vers Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
        fileChooser.setInitialFileName("operations_" + mois + "_" + java.time.LocalDate.now() + ".xlsx");

        File file = fileChooser.showSaveDialog(moisOperationsTable.getScene().getWindow());
        if (file != null) {
            try {
                ExcelUtil.exportOperationsToExcel(operations, file.getAbsolutePath());
                showInfo("Export réussi !");
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("Erreur lors de l'export: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void autoResizeColumns() {
        for (TableColumn<?, ?> col : moisOperationsTable.getColumns()) {
            double max = computeTextWidth(col.getText());
            int rows = moisOperationsTable.getItems() != null ? moisOperationsTable.getItems().size() : 0;
            int sample = Math.min(rows, 200);
            for (int i = 0; i < sample; i++) {
                Object cell = col.getCellData(i);
                String s = cell == null ? "" : cell.toString();
                max = Math.max(max, computeTextWidth(s));
            }
            col.setPrefWidth(Math.max(60, max + 24));
        }
    }

    private double computeTextWidth(String text) {
        if (text == null) return 0;
        javafx.scene.text.Text t = new javafx.scene.text.Text(text);
        t.setStyle("-fx-font-size: 13px;");
        return t.getLayoutBounds().getWidth();
    }
}
