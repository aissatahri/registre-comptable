package com.app.registre.controller;

import com.app.registre.model.Paiement;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Locale;

public class PaiementDialogController {

    @FXML private ComboBox<String> anneeField;
    @FXML private ComboBox<String> typeField;
    @FXML private TextField categorieField;
    @FXML private TextField montantField;
    @FXML private Text validationText;

    private Paiement paiement;
    private boolean editMode = false;
    private final BooleanProperty valid = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        // Pré-remplir champs utiles
        anneeField.getItems().addAll("2020","2021","2022","2023","2024","2025");
        anneeField.setValue("2022");

        typeField.getItems().addAll("INV","EXP");
        typeField.setValue("INV");

        // validation reactive
        montantField.textProperty().addListener((obs, oldV, newV) -> validateForm());
        anneeField.valueProperty().addListener((obs, o, n) -> validateForm());
        typeField.valueProperty().addListener((obs, o, n) -> validateForm());
    }

    public boolean handleSave() {
        validationText.setVisible(false);

        String annee = anneeField.getValue();
        String type = typeField.getValue();
        String categorie = categorieField.getText();
        String montantText = montantField.getText();

        if (annee == null || annee.isBlank()) {
            validationText.setText("L'année est requise.");
            validationText.setVisible(true);
            return false;
        }
        if (type == null || type.isBlank()) {
            validationText.setText("Le type est requis.");
            validationText.setVisible(true);
            return false;
        }
        double montant;
        try {
            montant = Double.parseDouble(montantText.replace(',', '.'));
        } catch (Exception e) {
            validationText.setText("Montant invalide.");
            validationText.setVisible(true);
            return false;
        }

        if (montant <= 0) {
            validationText.setText("Le montant doit être supérieur à 0.");
            validationText.setVisible(true);
            return false;
        }

        if (editMode && this.paiement != null) {
            // mettre à jour l'objet existant
            this.paiement.setAnnee(annee);
            this.paiement.setType(type);
            this.paiement.setCategorie(categorie != null ? categorie : "");
            this.paiement.setMontant(montant);
        } else {
            paiement = new Paiement(annee, type, montant, categorie != null ? categorie : "");
        }
        return true;
    }

    public Paiement getPaiement() {
        return paiement;
    }

    public BooleanProperty validProperty() {
        return valid;
    }

    private void validateForm() {
        validationText.setVisible(false);
        String annee = anneeField.getValue();
        String type = typeField.getValue();
        String montantText = montantField.getText();

        if (annee == null || annee.isBlank()) {
            valid.set(false);
            return;
        }
        if (type == null || type.isBlank()) {
            valid.set(false);
            return;
        }
        if (montantText == null || montantText.isBlank()) {
            valid.set(false);
            return;
        }
        try {
            double m = Double.parseDouble(montantText.replace(',', '.'));
            valid.set(m > 0);
        } catch (Exception ex) {
            valid.set(false);
        }
    }

    public void setPaiement(Paiement paiement, boolean isEdit) {
        this.editMode = isEdit;
        if (paiement != null) {
            this.paiement = paiement;
            anneeField.setValue(paiement.getAnnee());
            typeField.setValue(paiement.getType());
            categorieField.setText(paiement.getCategorie());
            // format montant with simple decimal point (no grouping)
            montantField.setText(String.format(Locale.US, "%.2f", paiement.getMontant()));
            validateForm();
        }
    }
}
