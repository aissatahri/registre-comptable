package com.app.registre.controller;

import com.app.registre.dao.OperationDAO;
import com.app.registre.dao.RecapDAO;
import com.app.registre.model.Operation;
import com.app.registre.util.ExcelUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.util.Callback;

public class RegistreController {

    @FXML private TableView<Operation> operationsTable;
    @FXML private TableColumn<Operation, String> colOp;
    @FXML private TableColumn<Operation, String> colOvCheq;
    @FXML private TableColumn<Operation, String> colImp;
    @FXML private TableColumn<Operation, String> colNature;
    @FXML private TableColumn<Operation, String> colBudg;
    @FXML private TableColumn<Operation, Double> colMontant;
    @FXML private TableColumn<Operation, java.time.LocalDate> colDateEntree;
    @FXML private TableColumn<Operation, java.time.LocalDate> colDateVisa;
    @FXML private TableColumn<Operation, java.time.LocalDate> colDateRejet;
    @FXML private TableColumn<Operation, String> colMotifRejet;
    @FXML private TableColumn<Operation, java.time.LocalDate> colDateReponse;
    @FXML private TableColumn<Operation, String> colContenuReponse;
    @FXML private TableColumn<Operation, String> colMois;
    @FXML private TableColumn<Operation, String> colDecision;
    @FXML private TableColumn<Operation, Void> actionsColumn;
    @FXML private TableColumn<Operation, Boolean> selectColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterMois;
    @FXML private ComboBox<String> filterNature;
    @FXML private javafx.scene.control.MenuButton columnsMenu;
    @FXML private Text totalOperationsText;
    @FXML private Text totalMontantText;
    @FXML private Text dossiersPayesText;
    @FXML private Text dossiersRejetesText;

    private ObservableList<Operation> operations;
    private OperationDAO operationDAO;
    private RecapDAO recapDAO;
    private CheckBox headerSelectAll;

    public void initialize() {
        try {
            operationDAO = new OperationDAO();
            recapDAO = new RecapDAO();

            colOp.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("op"));
            colOvCheq.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("ovCheq"));
            colImp.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("imp"));
            colNature.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nature"));
            colBudg.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("budg"));
            colMontant.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("montant"));
            colDateEntree.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateEntree"));
            colDateVisa.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateVisa"));
            colDateRejet.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateRejet"));
            colMotifRejet.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("motifRejet"));
            colDateReponse.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateReponse"));
            colContenuReponse.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("contenuReponse"));
            colMois.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("mois"));
            colDecision.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("decision"));
            selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
            selectColumn.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(selectColumn));
            selectColumn.setStyle("-fx-alignment: CENTER;");
            headerSelectAll = new CheckBox();
            headerSelectAll.setOnAction(e -> {
                boolean s = headerSelectAll.isSelected();
                ObservableList<Operation> items = operationsTable.getItems();
                if (items != null) {
                    for (Operation op : items) op.setSelected(s);
                }
            });
            selectColumn.setGraphic(headerSelectAll);
            operationsTable.setEditable(true);
            selectColumn.setEditable(true);
            operationsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            colDateEntree.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(java.time.LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : df.format(item));
                }
            });
            colDateVisa.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(java.time.LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : df.format(item));
                }
            });
            colDateRejet.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(java.time.LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : df.format(item));
                }
            });
            colDateReponse.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(java.time.LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : df.format(item));
                }
            });

            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            colMontant.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : nf.format(item));
                }
            });

            loadOperations();
            setupFilters();
            setupColumnsMenu();
            setupActionButtons();
            updateStatistics();
        } catch (Exception e) {
            showError("Erreur d'initialisation du registre: " + e.getMessage());
        }
    }

    /* --------------------------- TABLE BUTTONS --------------------------- */

    private void setupActionButtons() {
        Callback<TableColumn<Operation, Void>, TableCell<Operation, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Operation, Void> call(final TableColumn<Operation, Void> param) {
                return new TableCell<>() {

                    private final Button btnEdit = new Button("Modif");
                    private final Button btnDelete = new Button("Supp");

                    {
                        btnEdit.getStyleClass().addAll("btn-warning", "btn-icon");
                        btnDelete.getStyleClass().addAll("btn-danger", "btn-icon");
                        btnEdit.setText("✏");
                        btnDelete.setText("🗑");

                        btnEdit.setOnAction(event -> {
                            Operation op = getTableView().getItems().get(getIndex());
                            editOperation(op);
                        });

                        btnDelete.setOnAction(event -> {
                            Operation op = getTableView().getItems().get(getIndex());
                            deleteOperation(op);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(10, btnEdit, btnDelete);
                            setGraphic(box);
                        }
                    }
                };
            }
        };

        actionsColumn.setCellFactory(cellFactory);
    }

    /* --------------------------- LOAD & FILTER --------------------------- */

    private void loadOperations() {
        List<Operation> operationList = operationDAO.findAll();
        operations = FXCollections.observableArrayList(operationList);
        operationsTable.setItems(operations);
        operationsTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        autoResizeColumns();
        updateHeaderSelectAll();
    }

    private void setupFilters() {
        filterMois.getItems().addAll("JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE");

        filterNature.getItems().addAll("MARCHE V", "MARCHE NV", "BC", "REGIE", "CONVENTION", "ESD", "SUBVENTION");

        filterMois.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterNature.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    @FXML
    private void applyFilters() {
        String mois = filterMois.getValue();
        String nature = filterNature.getValue();
        String search = searchField.getText().toLowerCase();

        ObservableList<Operation> filtered = FXCollections.observableArrayList();

        for (Operation op : operations) {
            boolean matches = true;

            if (mois != null && !mois.equals(op.getMois())) matches = false;
            if (nature != null && !nature.equals(op.getNature())) matches = false;

            if (!search.isEmpty() && !containsSearch(op, search)) matches = false;

            if (matches) filtered.add(op);
        }

        operationsTable.setItems(filtered);
        updateStatistics();
        autoResizeColumns();
        updateHeaderSelectAll();
    }

    @FXML
    private void resetFilters() {
        filterMois.setValue(null);
        filterNature.setValue(null);
        searchField.clear();
        operationsTable.setItems(operations);
        updateStatistics();
        updateHeaderSelectAll();
    }

    private boolean containsSearch(Operation op, String search) {
        return (op.getOp() != null && op.getOp().toLowerCase().contains(search)) ||
                (op.getNature() != null && op.getNature().toLowerCase().contains(search)) ||
                (op.getImp() != null && op.getImp().toLowerCase().contains(search)) ||
                (op.getDecision() != null && op.getDecision().toLowerCase().contains(search)) ||
                (op.getOvCheq() != null && op.getOvCheq().toLowerCase().contains(search)) ||
                (op.getBudg() != null && op.getBudg().toLowerCase().contains(search)) ||
                (op.getMois() != null && op.getMois().toLowerCase().contains(search)) ||
                (op.getMotifRejet() != null && op.getMotifRejet().toLowerCase().contains(search)) ||
                (op.getContenuReponse() != null && op.getContenuReponse().toLowerCase().contains(search));
    }

    /* --------------------------- STATISTICS --------------------------- */

    private void updateStatistics() {
        int totalOps = operationsTable.getItems().size();
        double totalMontant = operationsTable.getItems().stream().mapToDouble(Operation::getMontant).sum();
        int payes = countDecision(operationsTable.getItems(), new String[]{"P", "ACCEPTE", "ACCEPTÉ"});
        int rejetes = countDecision(operationsTable.getItems(), new String[]{"R", "REFUSE", "REFUSÉ"});

        totalOperationsText.setText(String.valueOf(totalOps));
        totalMontantText.setText(String.format("%,.2f", totalMontant));
        dossiersPayesText.setText(String.valueOf(payes));
        dossiersRejetesText.setText(String.valueOf(rejetes));
    }

    private int countDecision(List<Operation> items, String[] tokens) {
        int c = 0;
        for (Operation op : items) {
            String d = op.getDecision();
            if (d == null) continue;
            String n = d.trim().toUpperCase();
            for (String t : tokens) {
                if (n.equals(t)) { c++; break; }
            }
        }
        return c;
    }

    /* --------------------------- CRUD OPERATIONS --------------------------- */

    @FXML
    private void showAddOperationDialog() {
        showOperationDialog(null, false);
    }

    public void editOperation(Operation selected) {
        if (selected != null) {
            showOperationDialog(selected, true);
        } else {
            showError("Veuillez sélectionner une opération à modifier");
        }
    }

    public void deleteOperation(Operation selected) {
        if (selected != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation de suppression");
            confirmAlert.setHeaderText("Supprimer l'opération " + selected.getOp() + " ?");
            confirmAlert.setContentText("Cette action est irréversible.");

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                operationDAO.delete(selected.getId());
                showInfo("Opération supprimée avec succès");
                loadOperations();
                notifyMenuStats();
            }
        } else {
            showError("Veuillez sélectionner une opération à supprimer");
        }
    }

    @FXML
    private void deleteAllOperations() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer toutes les opérations");
        confirm.setHeaderText("Supprimer toutes les opérations du registre ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            operationDAO.deleteAll();
            showInfo("Toutes les opérations ont été supprimées.");
            loadOperations();
            notifyMenuStats();
        }
    }

    @FXML
    private void deleteSelectedOperations() {
        List<Operation> toDelete = operationsTable.getItems().stream().filter(Operation::isSelected).toList();
        if (toDelete.isEmpty()) {
            showInfo("Aucune opération sélectionnée.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la sélection");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer " + toDelete.size() + " opérations sélectionnées ?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            for (Operation op : toDelete) {
                operationDAO.delete(op.getId());
            }
            showInfo(toDelete.size() + " opérations supprimées.");
            loadOperations();
            notifyMenuStats();
        }
    }

    private void showOperationDialog(Operation operation, boolean isEditMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/operation-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            OperationDialogController controller = loader.getController();
            controller.setOperation(operation, isEditMode);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(isEditMode ? "Modifier l'opération" : "Nouvelle opération");

            ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

            Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.setOnAction(event -> controller.handleSave());

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == saveButtonType) {
                Operation updatedOperation = controller.getOperation();
                if (updatedOperation != null) {
                    if (isEditMode) {
                        operationDAO.update(updatedOperation);
                        showInfo("Opération modifiée avec succès");
                    } else {
                        operationDAO.insert(updatedOperation);
                        showInfo("Opération ajoutée avec succès");
                    }
                    loadOperations();
                    autoResizeColumns();
                    notifyMenuStats();
                }
            }
        } catch (IOException e) {
            showError("Erreur lors du chargement du dialogue: " + e.getMessage());
        }
    }

    /* --------------------------- EXCEL --------------------------- */

    @FXML
    private void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter vers Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );
        fileChooser.setInitialFileName("operations_" + java.time.LocalDate.now() + ".xlsx");

        File file = fileChooser.showSaveDialog(operationsTable.getScene().getWindow());
        if (file != null) {
            try {
                List<Operation> operationsToExport = operationsTable.getItems();
                ExcelUtil.exportOperationsToExcel(operationsToExport, file.getAbsolutePath());
                showInfo("Export réussi !");
            } catch (IOException e) {
                showError("Erreur lors de l'export: " + e.getMessage());
            }
        }
    }

    @FXML
    private void importFromExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importer depuis Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );

        File file = fileChooser.showOpenDialog(operationsTable.getScene().getWindow());
        if (file != null) {
            try {
                List<Operation> importedOperations = ExcelUtil.importOperationsFromExcel(file.getAbsolutePath());

                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setHeaderText("Importer " + importedOperations.size() + " opérations ?");
                Optional<ButtonType> result = confirmAlert.showAndWait();

                if (result.isPresent() && result.get() == ButtonType.OK) {
                    int importedCount = 0;

                    for (Operation op : importedOperations) {
                        try {
                            operationDAO.insert(op);
                            importedCount++;
                        } catch (Exception e) {
                            System.err.println("Erreur import: " + e.getMessage());
                        }
                    }

                    showInfo("Importation terminée !\n" + importedCount + " opérations importées");
                    loadOperations();
                    autoResizeColumns();
                    notifyMenuStats();
                }
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("ne peut pas accéder au fichier") || msg.toLowerCase().contains("used by another process")) {
                    showError("Le fichier Excel semble être ouvert par Excel ou un autre programme. Fermez le classeur puis réessayez. Vous pouvez aussi copier le fichier dans un autre dossier (ex: Documents) avant l'import.");
                } else {
                    showError("Erreur lors de l'import: " + msg);
                }
            }
        }
    }

    /* --------------------------- ALERTS --------------------------- */

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void autoResizeColumns() {
        for (TableColumn<?, ?> col : operationsTable.getColumns()) {
            double max = computeTextWidth(col.getText());
            int rows = operationsTable.getItems() != null ? operationsTable.getItems().size() : 0;
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

    private void notifyMenuStats() {
        try {
            javafx.scene.Scene scene = operationsTable.getScene();
            if (scene == null) return;
            Text opsText = (Text) scene.lookup("#opsCountMenuText");
            Text recText = (Text) scene.lookup("#recettesMenuText");
            if (opsText == null && recText == null) return;
            RecapDAO recap = new RecapDAO();
            int ops = recap.getTotalOperations();
            double recettes = recap.getTotalRecettes();
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            if (opsText != null) opsText.setText(String.valueOf(ops));
            if (recText != null) recText.setText(nf.format(recettes));
        } catch (Exception ignored) {}
    }

    private void setupColumnsMenu() {
        if (columnsMenu == null) return;
        columnsMenu.getItems().clear();
        java.util.List<TableColumn<?, ?>> cols = new java.util.ArrayList<>(operationsTable.getColumns());
        for (TableColumn<?, ?> c : cols) {
            javafx.scene.control.CheckMenuItem item = new javafx.scene.control.CheckMenuItem(c.getText());
            item.setSelected(c.isVisible());
            item.selectedProperty().addListener((o, ov, nv) -> c.setVisible(nv));
            columnsMenu.getItems().add(item);
        }
    }

    private void updateHeaderSelectAll() {
        if (headerSelectAll == null) return;
        ObservableList<Operation> items = operationsTable.getItems();
        boolean all = items != null && !items.isEmpty() && items.stream().allMatch(Operation::isSelected);
        headerSelectAll.setSelected(all);
    }
}
