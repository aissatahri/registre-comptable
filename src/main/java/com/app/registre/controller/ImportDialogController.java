package com.app.registre.controller;

import com.app.registre.model.Operation;
import com.app.registre.util.ExcelUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ImportDialogController implements Initializable {

    @FXML private TextField filePathField;
    @FXML private CheckBox replaceDataCheck;
    @FXML private CheckBox validateDataCheck;
    @FXML private TableView<Operation> previewTable;
    @FXML private Label previewCountText;
    @FXML private Text errorText;

    private ObservableList<Operation> previewData;
    private File selectedFile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        previewData = FXCollections.observableArrayList();
        previewTable.setItems(previewData);
    }

    @FXML
    private void browseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le fichier Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );

        File file = fileChooser.showOpenDialog(filePathField.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            filePathField.setText(file.getAbsolutePath());
            loadPreview();
        }
    }

    private void loadPreview() {
        if (selectedFile != null) {
            try {
                List<Operation> operations = ExcelUtil.importOperationsFromExcel(selectedFile.getAbsolutePath());
                previewData.setAll(operations);
                previewCountText.setText(operations.size() + " opérations trouvées");
                errorText.setVisible(false);
            } catch (Exception e) {
                errorText.setText("Erreur lors du chargement: " + e.getMessage());
                errorText.setVisible(true);
                previewData.clear();
                previewCountText.setText("0 opérations trouvées");
            }
        }
    }

    @FXML
    private void handleImport() {
        if (selectedFile == null) {
            errorText.setText("Veuillez sélectionner un fichier");
            errorText.setVisible(true);
            return;
        }
        closeDialog(true);
    }

    @FXML
    private void handleCancel() {
        closeDialog(false);
    }

    private void closeDialog(boolean success) {
        Stage stage = (Stage) filePathField.getScene().getWindow();
        stage.setUserData(success ? selectedFile : null);
        stage.close();
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public boolean shouldReplaceData() {
        return replaceDataCheck.isSelected();
    }

    public boolean shouldValidateData() {
        return validateDataCheck.isSelected();
    }
}