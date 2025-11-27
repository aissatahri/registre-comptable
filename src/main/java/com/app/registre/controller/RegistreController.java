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
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javafx.util.Callback;

public class RegistreController {

    @FXML private TableView<Operation> operationsTable;
    @FXML private TableColumn<Operation, Integer> colOvCheq;
    @FXML private TableColumn<Operation, String> colImp;
    @FXML private TableColumn<Operation, String> colDesignation;
    @FXML private TableColumn<Operation, String> colN;
    @FXML private TableColumn<Operation, String> colNature;
    @FXML private TableColumn<Operation, String> colBudg;
    @FXML private TableColumn<Operation, String> colExercice;
    @FXML private TableColumn<Operation, String> colBeneficiaire;
    @FXML private TableColumn<Operation, Double> colMontant;
    @FXML private TableColumn<Operation, java.time.LocalDate> colDateEmission;
    @FXML private TableColumn<Operation, java.time.LocalDate> colDateVisa;
    @FXML private TableColumn<Operation, Integer> colOpOr;
    @FXML private TableColumn<Operation, Double> colRecette;
    @FXML private TableColumn<Operation, Double> colSurRam;
    @FXML private TableColumn<Operation, Double> colSurEng;
    @FXML private TableColumn<Operation, Double> colDepense;
    @FXML private TableColumn<Operation, Void> actionsColumn;
    @FXML private TableColumn<Operation, Boolean> selectColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterMois;
    @FXML private ComboBox<String> filterNature;
    @FXML private javafx.scene.control.MenuButton columnsMenu;
    @FXML private Text totalOperationsText;
    @FXML private Text totalMontantText;

    private ObservableList<Operation> operations;
    private OperationDAO operationDAO;
    private RecapDAO recapDAO;
    private CheckBox headerSelectAll;

    public void initialize() {
        try {
            operationDAO = new OperationDAO();
            recapDAO = new RecapDAO();

            // Map Table columns to Operation properties (ordered like the operation dialog)
            colImp.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("imp"));
            colDesignation.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("designation"));
            colNature.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nature"));
            colN.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("n"));
            colBudg.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("budg"));
            colExercice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("exercice"));
            colBeneficiaire.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("beneficiaire"));
            colDateEmission.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateEmission"));
            colDateVisa.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateVisa"));
            colOpOr.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("opOr"));
            colOvCheq.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("ovCheq"));
            colRecette.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("recette"));
            colSurRam.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("surRam"));
            colSurEng.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("surEng"));
            colDepense.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("depense"));
            colMontant.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("montant"));
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
            colDateEmission.setCellFactory(col -> new TableCell<>() {
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

            colN.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : item);
                }
            });

            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            // Format numeric columns (recette, surRam, surEng, depense, montant)
            javafx.util.Callback<TableColumn<Operation, Double>, TableCell<Operation, Double>> doubleCellFactory = col -> new TableCell<>() {
                @Override protected void updateItem(Double item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "" : nf.format(item));
                }
            };

            colRecette.setCellFactory(doubleCellFactory);
            colSurRam.setCellFactory(doubleCellFactory);
            colSurEng.setCellFactory(doubleCellFactory);
            colDepense.setCellFactory(doubleCellFactory);
            colMontant.setCellFactory(doubleCellFactory);

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
        // Recompute cumulative solde so that each row's solde = previous_solde + recette - depense
        double prevSolde = 0.0;
        int startIndex = 0;
        // If an explicit "Solde initial" record exists, use it as starting balance and begin after it
        for (int i = 0; i < operationList.size(); i++) {
            Operation op = operationList.get(i);
            if (op.getDesignation() != null && "Solde initial".equalsIgnoreCase(op.getDesignation().trim())) {
                Double s = op.getSolde();
                prevSolde = s != null ? s : 0.0;
                startIndex = i + 1;
                break;
            }
        }

        for (int i = startIndex; i < operationList.size(); i++) {
            Operation op = operationList.get(i);
            double recette = op.getRecette() != null ? op.getRecette() : 0.0;
            double depense = op.getDepense() != null ? op.getDepense() : 0.0;
            double computed = prevSolde + recette - depense;
            // Only update model & DB when different to avoid unnecessary writes
            Double existing = op.getSolde();
            if (existing == null || Math.abs(existing - computed) > 0.0001) {
                op.setSolde(computed);
                try {
                    operationDAO.update(op);
                } catch (Exception ignored) {}
            }
            prevSolde = computed;
        }

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

        // Default to the current month on view load
        try { filterMois.setValue(getCurrentMonth()); } catch (Exception ignored) {}

        filterMois.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        filterNature.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    @FXML
    private void applyFilters() {
        String mois = filterMois.getValue();
        String nature = filterNature.getValue();
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();

        ObservableList<Operation> filtered = FXCollections.observableArrayList();

        for (Operation op : operations) {
            boolean matches = true;
            // Determine month from dateEmission first (preferred), otherwise fall back to stored mois
            String opMonth = null;
            if (op.getDateEmission() != null) {
                opMonth = getMonthName(op.getDateEmission());
            } else if (op.getMois() != null) {
                opMonth = op.getMois();
            }
            if (mois != null && (opMonth == null || !mois.equals(opMonth))) matches = false;
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
        return (op.getDesignation() != null && op.getDesignation().toLowerCase().contains(search)) ||
            (op.getNature() != null && op.getNature().toLowerCase().contains(search)) ||
            (op.getImp() != null && op.getImp().toLowerCase().contains(search)) ||
            ((op.getOvCheqType() != null && op.getOvCheqType().toLowerCase().contains(search)) || (op.getOvCheq() != null && String.valueOf(op.getOvCheq()).contains(search))) ||
            (op.getBudg() != null && op.getBudg().toLowerCase().contains(search)) ||
            (op.getExercice() != null && op.getExercice().toLowerCase().contains(search)) ||
            (op.getBeneficiaire() != null && op.getBeneficiaire().toLowerCase().contains(search));
    }

    /* --------------------------- STATISTICS --------------------------- */

    private void updateStatistics() {
        int totalOps = operationsTable.getItems().size();
        // Instead of total montant, show the latest solde (dernier solde)
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        // Compute last solde from currently visible operations (post-filters)
        double displaySolde = 0.0;
        ObservableList<Operation> items = operationsTable.getItems();
        if (items != null && !items.isEmpty()) {
            Operation best = null;
            java.time.LocalDate bestDate = null;
            for (Operation op : items) {
                java.time.LocalDate d = op.getDateEmission() != null ? op.getDateEmission() : op.getDateVisa();
                if (d != null) {
                    if (best == null || bestDate == null || d.isAfter(bestDate) || (d.equals(bestDate) && op.getId() > best.getId())) {
                        best = op;
                        bestDate = d;
                    }
                } else {
                    // keep as fallback if no dated operations found yet
                    if (best == null) best = op;
                }
            }
            if (best != null && best.getSolde() != null) displaySolde = best.getSolde();
        }
        int payes = countDecision(operationsTable.getItems(), new String[]{"P", "ACCEPTE", "ACCEPTÉ"});
        int rejetes = countDecision(operationsTable.getItems(), new String[]{"R", "REFUSE", "REFUSÉ"});

        totalOperationsText.setText(String.valueOf(totalOps));
        totalMontantText.setText(nf.format(displaySolde));
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
            initAlertOwner(confirmAlert);
            confirmAlert.setTitle("Confirmation de suppression");
            confirmAlert.setHeaderText("Supprimer l'opération sélectionnée ?");
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
        initAlertOwner(confirm);
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
        initAlertOwner(confirm);
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
            // Attach dialog to owner window and make it window-modal
            Window owner = getOwnerWindow();
            if (owner != null) {
                dialog.initOwner(owner);
                dialog.initModality(Modality.WINDOW_MODAL);
            }
            dialog.setTitle(isEditMode ? "Modifier l'opération" : "Nouvelle opération");

            ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

            Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
            // Prevent the dialog from closing if validation fails inside the controller.
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                controller.handleSave();
                if (!controller.isSaved()) {
                    // consume the event to prevent the dialog from closing when validation failed
                    event.consume();
                } else {
                    // ensure the dialog result is set so showAndWait returns
                    dialog.setResult(saveButtonType);
                }
            });

            Optional<ButtonType> result = dialog.showAndWait();

            // Some dialog closing paths may not return the exact ButtonType (custom handlers may close the stage).
            // Rely on the controller's operation instance: controller.handleSave() creates/sets the operation;
            // controller.handleCancel() sets it to null. If an Operation is present, persist it.
            Operation updatedOperation = controller.getOperation();
            if (updatedOperation == null) {
                System.out.println("[RegistreController] No operation returned from dialog (operation is null)");
            } else {
                System.out.println("[RegistreController] Operation returned from dialog: imp=" + updatedOperation.getImp() + " designation=" + updatedOperation.getDesignation() + " solde=" + updatedOperation.getMontant());
            }

            if (updatedOperation != null) {
                if (isEditMode) {
                    operationDAO.update(updatedOperation);
                    // If the updated operation is the initial balance, recompute all soldes
                    if (updatedOperation.getDesignation() != null && "Solde initial".equalsIgnoreCase(updatedOperation.getDesignation().trim())) {
                        try { operationDAO.recomputeAllSoldes(); } catch (Exception ignored) {}
                    }
                    showInfo("Opération modifiée avec succès");
                } else {
                    operationDAO.insert(updatedOperation);
                    if (updatedOperation.getDesignation() != null && "Solde initial".equalsIgnoreCase(updatedOperation.getDesignation().trim())) {
                        try { operationDAO.recomputeAllSoldes(); } catch (Exception ignored) {}
                    }
                    showInfo("Opération ajoutée avec succès");
                }
                loadOperations();
                autoResizeColumns();
                notifyMenuStats();
            }
        } catch (Exception e) {
            // Print full stacktrace to console for debugging the FXML/controller initialization
            e.printStackTrace();
            String msg = e.toString();
            showError("Erreur lors du chargement du dialogue: " + msg);
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
        initAlertOwner(alert);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        initAlertOwner(alert);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Window getOwnerWindow() {
        if (operationsTable != null && operationsTable.getScene() != null) {
            return operationsTable.getScene().getWindow();
        }
        return null;
    }

    private void initAlertOwner(Alert alert) {
        Window owner = getOwnerWindow();
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
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
            // Add extra padding to ensure header text is fully visible and set minWidth
            double padded = Math.max(80, max + 48);
            col.setMinWidth(padded);
            col.setPrefWidth(padded);
        }
    }

    private double computeTextWidth(String text) {
        if (text == null) return 0;
        javafx.scene.text.Text t = new javafx.scene.text.Text(text);
        t.setStyle("-fx-font-size: 13px;");
        return t.getLayoutBounds().getWidth();
    }

    private String getCurrentMonth() {
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        return months[LocalDate.now().getMonthValue() - 1];
    }

    private String getMonthName(java.time.LocalDate d) {
        if (d == null) return null;
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        return months[d.getMonthValue() - 1];
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
