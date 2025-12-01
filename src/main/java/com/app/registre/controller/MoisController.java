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
import com.app.registre.util.DialogUtils;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class MoisController {

    @FXML private ComboBox<String> moisComboBox;
    @FXML private ComboBox<String> anneeComboBox;
    @FXML private Text moisSelectedText;
    @FXML private Text moisOperationsText;
    @FXML private Text moisMontantText;
    @FXML private Text moisRecetteText;
    @FXML private Text moisSurRamText;
    @FXML private Text moisSurEngText;
    @FXML private Text moisDepenseText;
    @FXML private Text moisPrevSoldeText;
    @FXML private TableView<Operation> moisOperationsTable;
    @FXML private TableView<?> natureTable;
    // Columns aligned with the operation dialog / mois.fxml
    @FXML private TableColumn<Operation, String> colImp;
    @FXML private TableColumn<Operation, String> colDesignation;
    @FXML private TableColumn<Operation, String> colNature;
    @FXML private TableColumn<Operation, String> colN;
    @FXML private TableColumn<Operation, String> colBudg;
    @FXML private TableColumn<Operation, String> colExercice;
    @FXML private TableColumn<Operation, String> colBeneficiaire;
    @FXML private TableColumn<Operation, LocalDate> colDateEmission;
    @FXML private TableColumn<Operation, LocalDate> colDateVisa;
    @FXML private TableColumn<Operation, Integer> colOpOr;
    @FXML private TableColumn<Operation, String> colOvCheq;
    @FXML private TableColumn<Operation, Double> colRecette;
    @FXML private TableColumn<Operation, Double> colSurRam;
    @FXML private TableColumn<Operation, Double> colSurEng;
    @FXML private TableColumn<Operation, Double> colDepense;
    @FXML private TableColumn<Operation, Double> colMontant;
    @FXML private javafx.scene.control.MenuButton columnsMenu;

    private ObservableList<Operation> operations;
    private OperationDAO operationDAO;

    public void initialize() {
        operationDAO = new OperationDAO();
        setupMoisComboBox();
        setupAnneeComboBox();
        setupColumns();
        moisOperationsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        setupColumnsMenu();
        // Highlight rows that contain a `recette` value in yellow
        moisOperationsTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Operation> row = new javafx.scene.control.TableRow<>() {
                @Override
                protected void updateItem(Operation item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else {
                        Double recette = item.getRecette();
                        if (recette != null && Math.abs(recette) > 0.000001) {
                            setStyle("-fx-background-color: #fff59d;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            };
            return row;
        });
        // Default to current month
        if (moisComboBox.getValue() == null && !moisComboBox.getItems().isEmpty()) {
            try { moisComboBox.setValue(getCurrentMonth()); } catch (Exception ignored) { moisComboBox.setValue(moisComboBox.getItems().get(0)); }
        }
        loadMoisData();
    }

    /**
     * Programmatically open this controller for a given year and month (1-12).
     * This is intended for external callers that want to show the month view
     * preselected for a specific month/year, e.g. when clicking a recap card.
     */
    public void openFor(int year, int month) {
        try {
            // ensure year list is populated
            setupAnneeComboBox();
        } catch (Exception ignored) {}
        try {
            String ys = String.valueOf(year);
            if (anneeComboBox != null) {
                if (!anneeComboBox.getItems().contains(ys)) anneeComboBox.getItems().add(0, ys);
                anneeComboBox.setValue(ys);
            }
            if (moisComboBox != null && month >= 1 && month <= 12) {
                String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                        "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
                moisComboBox.setValue(months[month - 1]);
            }
            // load data after selection
            loadMoisData();
        } catch (Exception ignored) {}
    }

    private void setupMoisComboBox() {
        moisComboBox.getItems().addAll(
                "JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"
        );
    }

    private void setupAnneeComboBox() {
        if (anneeComboBox == null) return;
        java.util.Set<Integer> years = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
        java.util.List<Operation> all = operationDAO.findAll();
        for (Operation op : all) {
            if (op.getDateEmission() != null) years.add(op.getDateEmission().getYear());
        }
        if (years.isEmpty()) years.add(LocalDate.now().getYear());
        anneeComboBox.getItems().clear();
        for (Integer y : years) anneeComboBox.getItems().add(y.toString());
        if (anneeComboBox.getValue() == null && !anneeComboBox.getItems().isEmpty()) {
            anneeComboBox.setValue(anneeComboBox.getItems().get(0));
        }
    }

    private void setupColumns() {
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

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colDateEmission.setCellFactory(col -> new TableCell<>() {
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

        colN.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
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
        String annee = anneeComboBox != null ? anneeComboBox.getValue() : null;
        int filterYear = annee != null ? Integer.parseInt(annee) : LocalDate.now().getYear();
        if (mois != null) {
            // Load all operations and filter by month derived from dateEmission (preferred) or fallback to stored mois
            List<Operation> all = operationDAO.findAll();
            List<Operation> filtered = new java.util.ArrayList<>();
            for (Operation op : all) {
                // Only include operations that have a date_emission and match both month and year
                if (op.getDateEmission() != null) {
                    if (getMonthName(op.getDateEmission()).equals(mois) && op.getDateEmission().getYear() == filterYear) {
                        filtered.add(op);
                    }
                }
            }

            operations = FXCollections.observableArrayList(filtered);
            moisOperationsTable.setItems(operations);
            autoResizeColumns();

            updateMoisStatistics(mois);
        }
    }

    private void updateMoisStatistics(String mois) {
        moisSelectedText.setText(mois);
        int count = operations == null ? 0 : operations.size();
        double totalRecette = 0.0;
        double totalSurRam = 0.0;
        double totalSurEng = 0.0;
        double totalDepense = 0.0;
        Double dernierSolde = null;
        if (operations != null) {
            for (Operation op : operations) {
                if (op.getRecette() != null) totalRecette += op.getRecette();
                double sr = op.getSurRam() != null ? op.getSurRam() : 0.0;
                double se = op.getSurEng() != null ? op.getSurEng() : 0.0;
                totalSurRam += sr;
                totalSurEng += se;
                if (op.getDepense() != null) totalDepense += op.getDepense();
                if (op.getSolde() != null) dernierSolde = op.getSolde();
            }
        }
        double moyenne = count > 0 ? (totalRecette - totalDepense) / count : 0;

        moisOperationsText.setText(String.valueOf(count));
        moisRecetteText.setText(String.format("%,.2f", totalRecette));
        moisSurRamText.setText(String.format("%,.2f", totalSurRam));
        moisSurEngText.setText(String.format("%,.2f", totalSurEng));
        moisDepenseText.setText(String.format("%,.2f", totalDepense));
            // compute previous solde for the month: take the last record of (month-1) in the SAME YEAR
        try {
            int monthNum = monthNumberFromName(mois);
            int year = anneeComboBox != null && anneeComboBox.getValue() != null ? Integer.parseInt(anneeComboBox.getValue()) : LocalDate.now().getYear();

            // --- SOLDE PRECEDENT : dernier solde du mois précédent (même année) ---
            try {
                Double prev = 0.0;
                int prevMonth = monthNum - 1;
                
                // D'abord, trouver la date du solde initial pour ne pas remonter avant
                Operation globalInitial = operationDAO.findInitialOperation();
                Integer initialYear = null;
                Integer initialMonth = null;
                if (globalInitial != null && globalInitial.getDateEmission() != null) {
                    initialYear = globalInitial.getDateEmission().getYear();
                    initialMonth = globalInitial.getDateEmission().getMonthValue();
                } else if (globalInitial != null && globalInitial.getDateEntree() != null) {
                    initialYear = globalInitial.getDateEntree().getYear();
                    initialMonth = globalInitial.getDateEntree().getMonthValue();
                }
                
                // Chercher le dernier solde disponible en remontant les mois
                boolean found = false;
                if (prevMonth >= 1) {
                    // Remonter les mois jusqu'à trouver un solde (mais pas avant le solde initial)
                    for (int m = prevMonth; m >= 1; m--) {
                        // Ne pas remonter avant le mois du solde initial
                        if (initialYear != null && initialMonth != null) {
                            if (year < initialYear || (year == initialYear && m < initialMonth)) {
                                break; // On est avant le solde initial
                            }
                        }
                        prev = operationDAO.getLastMontantForMonthYear(year, m);
                        if (prev != null) {
                            found = true;
                            break;
                        }
                    }
                }
                
                if (!found && (initialYear == null || year > initialYear || (year == initialYear && prevMonth >= initialMonth))) {
                    // Pas trouvé dans l'année courante → chercher décembre année précédente et remonter
                    for (int m = 12; m >= 1; m--) {
                        // Ne pas remonter avant le mois du solde initial
                        if (initialYear != null && initialMonth != null) {
                            if ((year - 1) < initialYear || ((year - 1) == initialYear && m < initialMonth)) {
                                break;
                            }
                        }
                        prev = operationDAO.getLastMontantForMonthYear(year - 1, m);
                        if (prev != null) {
                            found = true;
                            break;
                        }
                    }
                }
                
                if (!found && globalInitial != null && globalInitial.getSolde() != null) {
                    // Utiliser le solde initial si on est au mois du solde initial ou après
                    if (initialYear != null && initialMonth != null) {
                        if (year > initialYear || (year == initialYear && monthNum >= initialMonth)) {
                            prev = globalInitial.getSolde();
                        } else {
                            prev = 0.0; // Avant le solde initial
                        }
                    } else {
                        prev = 0.0;
                    }
                } else if (!found) {
                    prev = 0.0;
                } else if (prev == null) {
                    prev = 0.0;
                }

                if (moisPrevSoldeText != null) {
                    moisPrevSoldeText.setText(String.format("%,.2f", prev));
                }

                // --- DERNIER SOLDE DU MOIS COURANT ---
                if (operations == null || operations.isEmpty()) {
                    // Aucun mouvement dans le mois → dernier solde = solde précédent
                    moisMontantText.setText(String.format("%,.2f", prev));
                } else {
                    // Il y a des opérations → on prend le solde de la dernière opération
                    Double curr = operationDAO.getLastMontantForMonthYear(year, monthNum);
                    moisMontantText.setText(String.format("%,.2f", curr == null ? prev : curr));
                }
            } catch (Exception ignored) {
                moisMontantText.setText(String.format("%,.2f", dernierSolde == null ? 0.0 : dernierSolde));
            }

        } catch (Exception ignored) {
            moisMontantText.setText(String.format("%,.2f", dernierSolde == null ? 0.0 : dernierSolde));
        }
        // moyenne not displayed in the FXML anymore

        // no previous-month solde displayed
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
                    // compute previous month's last solde using same logic as updateMoisStatistics
                    Double prev = 0.0;
                    try {
                        int monthNum = monthNumberFromName(mois);
                        int year = anneeComboBox != null && anneeComboBox.getValue() != null ? Integer.parseInt(anneeComboBox.getValue()) : LocalDate.now().getYear();
                        int prevMonth = monthNum - 1;
                        if (prevMonth >= 1) {
                            prev = operationDAO.getLastMontantForMonthYear(year, prevMonth);
                            if (prev == null) {
                                if (operationDAO.hasOperationsForMonthYear(year, prevMonth)) {
                                    operationDAO.recomputeAllSoldes();
                                    prev = operationDAO.getLastMontantForMonthYear(year, prevMonth);
                                    if (prev == null) prev = 0.0;
                                } else {
                                    prev = 0.0;
                                }
                            }
                        } else {
                            prev = 0.0;
                        }
                    } catch (Exception ex) {
                        prev = 0.0;
                    }
                    ExcelUtil.exportOperationsToExcel(operations, file.getAbsolutePath(), prev);
                    showInfo("Export réussi !");
                } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                DialogUtils.initOwner(alert, moisOperationsTable);
                alert.setHeaderText(null);
                alert.setContentText("Erreur lors de l'export: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogUtils.initOwner(alert, moisOperationsTable);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void autoResizeColumns() {
        for (TableColumn<?, ?> col : moisOperationsTable.getColumns()) {
            // Start with header text width (use bolder measurement to avoid truncation)
            double max = computeTextWidth(col.getText());
            int rows = moisOperationsTable.getItems() != null ? moisOperationsTable.getItems().size() : 0;
            int sample = Math.min(rows, 200);
            for (int i = 0; i < sample; i++) {
                Object cell = col.getCellData(i);
                String s = cell == null ? "" : cell.toString();
                max = Math.max(max, computeTextWidth(s));
            }
            // Add extra padding to leave room for sort icons and header padding
            col.setPrefWidth(Math.max(80, max + 40));
        }
    }

    private double computeTextWidth(String text) {
        if (text == null) return 0;
        javafx.scene.text.Text t = new javafx.scene.text.Text(text);
        // use a bolder measurement to account for header font weight
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
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

    private int monthNumberFromName(String mois) {
        if (mois == null) return LocalDate.now().getMonthValue();
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equalsIgnoreCase(mois.trim())) return i + 1;
        }
        return LocalDate.now().getMonthValue();
    }

    

}
