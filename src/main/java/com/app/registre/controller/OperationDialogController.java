package com.app.registre.controller;

import com.app.registre.model.Operation;
import com.app.registre.util.DesignationLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.app.registre.dao.OperationDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class OperationDialogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(OperationDialogController.class);

    @FXML private Text dialogTitle;
    @FXML private TextField ovCheqNumber;
    @FXML private TextField impField;
    @FXML private TextField designationField;
    @FXML private ComboBox<String> natureCombo;
    @FXML private TextField nField;
    @FXML private ComboBox<String> budgCombo;
    @FXML private TextField exerciceField;
    @FXML private TextField beneficiaireField;
    @FXML private DatePicker dateEmissionPicker;
    
    @FXML private DatePicker dateVisaPicker;
    @FXML private TextField opOrField;
    @FXML private TextField recetteField;
    @FXML private TextField surRamField;
    @FXML private TextField surEngField;
    @FXML private TextField soldeField;
    @FXML private Label depenseField;
    @FXML private Text validationText;

    private Operation operation;
    private boolean isEditMode = false;
    private boolean saved = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComboBoxes();
        setupValidation();
        setupImpAutoFill();
        setupDepenseAutoCalc();
        if (soldeField != null) soldeField.setVisible(false);
    }

    private void setupDepenseAutoCalc() {
        if (surRamField != null) {
            surRamField.textProperty().addListener((o, ov, nv) -> updateDepenseFromFields());
        }
        if (surEngField != null) {
            surEngField.textProperty().addListener((o, ov, nv) -> updateDepenseFromFields());
        }
        // depenseField is now a Label (computed), no editable behavior required
    }

    private void updateDepenseFromFields() {
        try {
            Double sr = parseDouble(surRamField.getText());
            Double se = parseDouble(surEngField.getText());
            double dep = 0.0;
            if (sr != null) dep += sr;
            if (se != null) dep += se;
            if (depenseField != null) depenseField.setText(String.format("%.2f", dep));
        } catch (Exception ignored) {}
    }

    private void setupImpAutoFill() {
        if (impField == null || designationField == null) return;
        // When IMP changes, try to lookup designation and set it if found.
        impField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal == null || newVal.isBlank()) return;
                String found = DesignationLookup.getDesignationForIMP(newVal.trim());
                if (found != null) {
                    // Always update the designation when IMP changes and a mapping exists
                    designationField.setText(found);
                }
            } catch (Exception e) {
                // don't block UI on lookup errors
                System.err.println("Error during IMP lookup: " + e.getMessage());
            }
        });
    }

    private void setupComboBoxes() {
        natureCombo.getItems().addAll(
                "MARCHE V", "MARCHE NV", "BC", "REGIE", "CONVENTION", "ESD", "SUBVENTION"
        );
        budgCombo.getItems().addAll("INV", "EXP");

        dateEmissionPicker.setValue(LocalDate.now());
    }

    private void setupValidation() {
        java.util.function.BiConsumer<TextField, String> numeric = (tf, old) -> {
            tf.textProperty().addListener((obs, ov, nv) -> {
                if (!nv.matches("\\d*(\\.\\d*)?")) tf.setText(ov);
            });
        };
        // `n` is a free text field now (can contain letters); do not apply numeric restriction
        numeric.accept(opOrField, opOrField.getText());
        numeric.accept(recetteField, recetteField.getText());
        numeric.accept(surRamField, surRamField.getText());
        numeric.accept(surEngField, surEngField.getText());
    }

    public void setOperation(Operation operation, boolean isEditMode) {
        this.operation = operation;
        this.isEditMode = isEditMode;

        dialogTitle.setText(isEditMode ? "Modifier l'opération" : "Nouvelle opération");

        if (operation != null) {
            if (operation.getOvCheq() != null && ovCheqNumber != null) ovCheqNumber.setText(String.valueOf(operation.getOvCheq()));
            impField.setText(operation.getImp());
            designationField.setText(operation.getDesignation());
            natureCombo.setValue(operation.getNature());
            if (operation.getN() != null) nField.setText(operation.getN());
            budgCombo.setValue(operation.getBudg());
            exerciceField.setText(operation.getExercice());
            beneficiaireField.setText(operation.getBeneficiaire());
            if (operation.getDateEmission() != null) dateEmissionPicker.setValue(operation.getDateEmission());

            // mois removed from dialog

            if (operation.getDateVisa() != null) {
                dateVisaPicker.setValue(operation.getDateVisa());
            }

            if (operation.getOpOr() != null) opOrField.setText(String.valueOf(operation.getOpOr()));
            if (operation.getRecette() != null) recetteField.setText(String.valueOf(operation.getRecette()));
            if (operation.getSurRam() != null) surRamField.setText(String.valueOf(operation.getSurRam()));
            if (operation.getSurEng() != null) surEngField.setText(String.valueOf(operation.getSurEng()));
            if (operation.getDepense() != null) {
                try {
                    depenseField.setText(String.format("%,.2f", operation.getDepense()).replace(',', ' '));
                    // Use comma as decimal separator for display in French locale
                    depenseField.setText(String.format("%.2f", operation.getDepense()).replace('.', ','));
                } catch (Exception e) {
                    depenseField.setText(String.valueOf(operation.getDepense()));
                }
            }
            // If this is the special initial balance row, expose the solde field so user can edit it
            if (operation.getDesignation() != null && "Solde initial".equalsIgnoreCase(operation.getDesignation().trim())) {
                if (soldeField != null) {
                    soldeField.setVisible(true);
                    if (operation.getSolde() != null) soldeField.setText(String.format("%.2f", operation.getSolde()));
                }
            } else {
                if (soldeField != null) soldeField.setVisible(false);
            }
        }
    }

    @FXML
    public void handleSave() {
        log.info("handleSave called");
        if (!validateForm()) {
            log.info("Validation failed, not saving operation");
            return;
        }

        log.info("Validation passed, preparing operation to save");
            if (operation == null) {
                operation = new Operation();
            }

            // OP is no longer persisted in the DB; skip setting it here
            // OV/CHEQ number only (type removed from UI)
            operation.setOvCheq(parseInt(ovCheqNumber != null ? ovCheqNumber.getText() : null));
            operation.setOvCheqType(null);
            operation.setImp(impField.getText());
            operation.setNature(natureCombo.getValue());
            operation.setDesignation(designationField.getText());
            // `n` is textual now
            operation.setN(nField.getText() != null && !nField.getText().isBlank() ? nField.getText().trim() : null);
            operation.setBudg(budgCombo.getValue());
            operation.setExercice(exerciceField.getText());
            operation.setBeneficiaire(beneficiaireField.getText());
            operation.setDateEmission(dateEmissionPicker.getValue());
            operation.setDateVisa(dateVisaPicker.getValue());
            operation.setOpOr(parseInt(opOrField.getText()));

            // Recette, Sur RAM, Sur ENG
            Double recette = parseDouble(recetteField.getText());
            Double surRam = parseDouble(surRamField.getText());
            Double surEng = parseDouble(surEngField.getText());
            operation.setRecette(recette);
            operation.setSurRam(surRam);
            operation.setSurEng(surEng);

            // Depense = surRam + surEng
            double depense = 0.0;
            if (surRam != null) depense += surRam;
            if (surEng != null) depense += surEng;
            operation.setDepense(depense);
            // reflect depense in the depense field shown to the user
            if (depenseField != null) depenseField.setText(String.format("%.2f", depense));

            // Set date entree (record creation) and mois only if emissionDate is provided.
            java.time.LocalDate emissionDate = dateEmissionPicker.getValue();
            operation.setDateEntree(java.time.LocalDate.now());
            if (emissionDate != null) {
                operation.setMois(getMonthName(emissionDate));
            } else {
                operation.setMois(null);
            }

            // If editing/creating the special 'Solde initial' record, use the explicit solde field
            OperationDAO dao = new OperationDAO();
            if (operation.getDesignation() != null && "Solde initial".equalsIgnoreCase(operation.getDesignation().trim())) {
                // parse solde from soldeField if provided, otherwise keep computed behavior
                Double explicit = parseDouble(soldeField != null ? soldeField.getText() : null);
                if (explicit != null) {
                    operation.setMontant(explicit);
                } else if (operation.getSolde() != null) {
                    // preserve existing solde when editing, avoid accidental overwrite to 0
                    operation.setMontant(operation.getSolde());
                } else {
                    Double previousSolde;
                    if (emissionDate != null) {
                        previousSolde = dao.getLastMontantBeforeDate(emissionDate);
                    } else {
                        previousSolde = dao.getLastMontant();
                    }
                    if (previousSolde == null) previousSolde = 0.0;
                    double recetteVal = recette != null ? recette : 0.0;
                    double solde = previousSolde + recetteVal - depense;
                    operation.setMontant(solde);
                }
            } else {
                // Compute running solde: previousSolde + recette - depense
                Double previousSolde;
                if (emissionDate != null) {
                    previousSolde = dao.getLastMontantBeforeDate(emissionDate);
                } else {
                    previousSolde = dao.getLastMontant();
                }
                if (previousSolde == null) previousSolde = 0.0;
                double recetteVal = recette != null ? recette : 0.0;
                double solde = previousSolde + recetteVal - depense;
                operation.setMontant(solde);
            }

            // Update montant field displayed if any
            // Note: the UI column header shows 'Solde' but underlying model uses 'montant'
                log.info("Prepared operation: imp='{}' designation='{}' solde={}", operation.getImp(), operation.getDesignation(), operation.getMontant());
                // mark as saved; dialog owner will close the dialog when appropriate
                saved = true;
    }

    @FXML
    public void handleCancel() {
        operation = null;
        closeDialog();
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        // OP removed: no longer mandatory

        // Note: les combobox (nature, budg) et les dates peuvent désormais être nulles.
        // Si vous souhaitez rendre un champ obligatoire, ajoutez la vérification ci-dessous.

        // Mois removed: no longer required

        if (errors.length() > 0) {
            validationText.setText(errors.toString());
            validationText.setVisible(true);
            return false;
        }

        validationText.setVisible(false);
        return true;
    }

    private void closeDialog() {
        Stage stage = (Stage) dialogTitle.getScene().getWindow();
        stage.close();
    }

    private String getMonthName(java.time.LocalDate d) {
        if (d == null) return getCurrentMonth();
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        return months[d.getMonthValue() - 1];
    }

    public Operation getOperation() {
        return operation;
    }

    public boolean isSaved() {
        return saved;
    }

    private String getCurrentMonth() {
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        return months[LocalDate.now().getMonthValue() - 1];
    }

    private Integer parseInt(String s) {
        try { if (s == null || s.isBlank()) return null; return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }
    private Double parseDouble(String s) {
        try { if (s == null || s.isBlank()) return null; return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return null; }
    }
}
