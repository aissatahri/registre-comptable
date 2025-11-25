package com.app.registre.controller;

import com.app.registre.dao.PaiementDAO;
import com.app.registre.model.Paiement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.util.List;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.util.Callback;
import java.util.Optional;

public class PaiementController {

    @FXML private TableView<Paiement> paiementsTable;
    @FXML private TableColumn<Paiement, String> colAnnee;
    @FXML private TableColumn<Paiement, String> colType;
    @FXML private TableColumn<Paiement, String> colCategorie;
    @FXML private TableColumn<Paiement, Double> colMontant;
    @FXML private ComboBox<String> anneeComboBox;
    @FXML private Text totalInvText;
    @FXML private Text totalExpText;
    @FXML private Text totalGeneralText;
    @FXML private TableColumn<Paiement, Void> actionsColumn;
    @FXML private MenuButton columnsMenu;

    private ObservableList<Paiement> paiements;
    private PaiementDAO paiementDAO;

    public void initialize() {
        paiementDAO = new PaiementDAO();

        // Configuration des colonnes
        colAnnee.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("annee"));
        colType.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        colCategorie.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("categorie"));
        colMontant.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("montant"));

        setupActionButtons();

        // allow multiple selection for bulk delete
        paiementsTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        loadPaiements();
        setupAnneeFilter();
        updateTotals();
        setupColumnsMenu();
    }

    @FXML
    private void deleteSelected() {
        ObservableList<Paiement> selected = paiementsTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showInfo("Aucun paiement sélectionné.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer les paiements sélectionnés");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer " + selected.size() + " paiements ?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            for (Paiement p : List.copyOf(selected)) {
                paiementDAO.delete(p.getId());
            }
            showInfo(selected.size() + " paiements supprimés.");
            refreshData();
            notifyMenuStats();
        }
    }

    private void setupActionButtons() {
        Callback<TableColumn<Paiement, Void>, TableCell<Paiement, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Paiement, Void> call(final TableColumn<Paiement, Void> param) {
                return new TableCell<>() {

                    private final Button btnEdit = new Button("Éditer");
                    private final Button btnDelete = new Button("Suppr");

                    {
                        btnEdit.getStyleClass().addAll("btn-warning", "btn-icon");
                        btnDelete.getStyleClass().addAll("btn-danger", "btn-icon");
                        // use simple unicode icons in button text
                        btnEdit.setText("✏");
                        btnDelete.setText("🗑");

                        btnEdit.setOnAction(event -> {
                            Paiement p = getTableView().getItems().get(getIndex());
                            editPaiement(p);
                        });

                        btnDelete.setOnAction(event -> {
                            Paiement p = getTableView().getItems().get(getIndex());
                            deletePaiement(p);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(8, btnEdit, btnDelete);
                            setGraphic(box);
                        }
                    }
                };
            }
        };

        actionsColumn.setCellFactory(cellFactory);
    }

    private void editPaiement(Paiement p) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/paiement-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            PaiementDialogController controller = loader.getController();
            controller.setPaiement(p, true);

            Dialog<javafx.scene.control.ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Modifier Paiement");

            javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancelButtonType = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

            javafx.scene.control.Button saveButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(saveButtonType);
            // disable save until form is valid
            saveButton.disableProperty().bind(controller.validProperty().not());
            saveButton.setOnAction(event -> {
                boolean ok = controller.handleSave();
                if (!ok) event.consume();
            });

            Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == saveButtonType) {
                if (controller.getPaiement() != null) {
                    paiementDAO.update(controller.getPaiement());
                    showInfo("Paiement modifié.");
                    refreshData();
                    notifyMenuStats();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Erreur lors de l'édition du paiement: " + e.getMessage());
        }
    }

    private void deletePaiement(Paiement p) {
        if (p == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer le paiement ?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            paiementDAO.delete(p.getId());
            showInfo("Paiement supprimé.");
            refreshData();
            notifyMenuStats();
        }
    }

    private void loadPaiements() {
        List<Paiement> paiementList = paiementDAO.findAll();
        paiements = FXCollections.observableArrayList(paiementList);
        paiementsTable.setItems(paiements);
    }

    private void setupAnneeFilter() {
        anneeComboBox.getItems().addAll("2011", "2012", "2013", "2014", "2015",
                "2016", "2017", "2018", "2019", "2020", "2021", "2022");
        anneeComboBox.setValue("2022");
    }

    private void updateTotals() {
        String annee = anneeComboBox.getValue();
        if (annee != null) {
            double totalInv = paiementDAO.getTotalByAnneeAndType(annee, "INV");
            double totalExp = paiementDAO.getTotalByAnneeAndType(annee, "EXP");
            double totalGeneral = totalInv + totalExp;

            totalInvText.setText(String.format("%,.2f", totalInv));
            totalExpText.setText(String.format("%,.2f", totalExp));
            totalGeneralText.setText(String.format("%,.2f", totalGeneral));
        }
    }

    @FXML
    private void refreshData() {
        loadPaiements();
        updateTotals();
    }

    @FXML
    private void showAddPaiementDialog() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/paiement-dialog.fxml"));
            DialogPane dialogPane = loader.load();

            PaiementDialogController controller = loader.getController();

            Dialog<javafx.scene.control.ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Nouveau Paiement");

            javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Enregistrer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType cancelButtonType = new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

            javafx.scene.control.Button saveButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(saveButtonType);
            // disable save until form is valid
            saveButton.disableProperty().bind(controller.validProperty().not());
            saveButton.setOnAction(event -> {
                boolean ok = controller.handleSave();
                if (!ok) event.consume();
            });

            java.util.Optional<javafx.scene.control.ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == saveButtonType) {
                if (controller.getPaiement() != null) {
                    paiementDAO.insert(controller.getPaiement());
                    showInfo("Paiement enregistré.");
                    refreshData();
                    notifyMenuStats();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Erreur lors de l'ouverture du dialogue: " + e.getMessage());
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupColumnsMenu() {
        if (columnsMenu == null) return;
        columnsMenu.getItems().clear();
        java.util.List<TableColumn<?, ?>> cols = new java.util.ArrayList<>(paiementsTable.getColumns());
        for (TableColumn<?, ?> c : cols) {
            CheckMenuItem item = new CheckMenuItem(c.getText());
            item.setSelected(c.isVisible());
            item.selectedProperty().addListener((o, ov, nv) -> c.setVisible(nv));
            columnsMenu.getItems().add(item);
        }
    }

    private void notifyMenuStats() {
        try {
            javafx.scene.Scene scene = paiementsTable.getScene();
            if (scene == null) return;
            Text recText = (Text) scene.lookup("#recettesMenuText");
            if (recText == null) return;
            double recettes = paiementDAO.getTotalRecettesAll();
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(2);
            recText.setText(nf.format(recettes));
        } catch (Exception ignored) {}
    }
}
