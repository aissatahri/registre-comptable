package com.app.registre.controller;

import com.app.registre.model.Operation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class OperationDialogController implements Initializable {

    @FXML private Text dialogTitle;
    @FXML private TextField opField;
    @FXML private TextField ovCheqField;
    @FXML private TextField impField;
    @FXML private ComboBox<String> natureCombo;
    @FXML private TextField budgField;
    @FXML private TextField montantField;
    @FXML private DatePicker dateEntreePicker;
    @FXML private ComboBox<String> moisCombo;
    @FXML private DatePicker dateVisaPicker;
    @FXML private DatePicker dateRejetPicker;
    @FXML private ComboBox<String> decisionCombo;
    @FXML private TextField motifRejetField;
    @FXML private DatePicker dateReponsePicker;
    @FXML private TextField contenuReponseField;
    @FXML private Text validationText;

    private Operation operation;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComboBoxes();
        setupValidation();
    }

    private void setupComboBoxes() {
        natureCombo.getItems().addAll(
                "MARCHE V", "MARCHE NV", "BC", "REGIE", "CONVENTION", "ESD", "SUBVENTION"
        );

        moisCombo.getItems().addAll(
                "JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"
        );

        decisionCombo.getItems().addAll("P", "R");

        dateEntreePicker.setValue(LocalDate.now());
        moisCombo.setValue(getCurrentMonth());
    }

    private void setupValidation() {
        montantField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                montantField.setText(oldValue);
            }
        });
    }

    public void setOperation(Operation operation, boolean isEditMode) {
        this.operation = operation;
        this.isEditMode = isEditMode;

        dialogTitle.setText(isEditMode ? "Modifier l'opération" : "Nouvelle opération");

        if (operation != null) {
            opField.setText(operation.getOp());
            ovCheqField.setText(operation.getOvCheq());
            impField.setText(operation.getImp());
            natureCombo.setValue(operation.getNature());
            budgField.setText(operation.getBudg());
            montantField.setText(String.valueOf(operation.getMontant()));

            if (operation.getDateEntree() != null) {
                dateEntreePicker.setValue(operation.getDateEntree());
            }

            moisCombo.setValue(operation.getMois());

            if (operation.getDateVisa() != null) {
                dateVisaPicker.setValue(operation.getDateVisa());
            }

            if (operation.getDateRejet() != null) {
                dateRejetPicker.setValue(operation.getDateRejet());
            }

            decisionCombo.setValue(operation.getDecision());
            motifRejetField.setText(operation.getMotifRejet());

            if (operation.getDateReponse() != null) {
                dateReponsePicker.setValue(operation.getDateReponse());
            }

            contenuReponseField.setText(operation.getContenuReponse());
        }
    }

    @FXML
    public void handleSave() {
        if (validateForm()) {
            if (operation == null) {
                operation = new Operation();
            }

            operation.setOp(opField.getText());
            operation.setOvCheq(ovCheqField.getText());
            operation.setImp(impField.getText());
            operation.setNature(natureCombo.getValue());
            operation.setBudg(budgField.getText());
            operation.setMontant(Double.parseDouble(montantField.getText()));
            operation.setDateEntree(dateEntreePicker.getValue());
            operation.setMois(moisCombo.getValue());
            operation.setDateVisa(dateVisaPicker.getValue());
            operation.setDateRejet(dateRejetPicker.getValue());
            operation.setDecision(decisionCombo.getValue());
            operation.setMotifRejet(motifRejetField.getText());
            operation.setDateReponse(dateReponsePicker.getValue());
            operation.setContenuReponse(contenuReponseField.getText());

            closeDialog();
        }
    }

    @FXML
    public void handleCancel() {
        operation = null;
        closeDialog();
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (opField.getText().isEmpty()) {
            errors.append("• Le champ OP est obligatoire\n");
        }

        if (natureCombo.getValue() == null) {
            errors.append("• La nature est obligatoire\n");
        }

        if (montantField.getText().isEmpty() || Double.parseDouble(montantField.getText()) <= 0) {
            errors.append("• Le montant doit être supérieur à 0\n");
        }

        if (dateEntreePicker.getValue() == null) {
            errors.append("• La date d'entrée est obligatoire\n");
        }

        if (moisCombo.getValue() == null) {
            errors.append("• Le mois est obligatoire\n");
        }

        if (errors.length() > 0) {
            validationText.setText(errors.toString());
            validationText.setVisible(true);
            return false;
        }

        validationText.setVisible(false);
        return true;
    }

    private void closeDialog() {
        Stage stage = (Stage) opField.getScene().getWindow();
        stage.close();
    }

    public Operation getOperation() {
        return operation;
    }

    private String getCurrentMonth() {
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        return months[LocalDate.now().getMonthValue() - 1];
    }
}