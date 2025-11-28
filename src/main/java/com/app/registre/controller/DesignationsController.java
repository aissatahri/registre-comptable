package com.app.registre.controller;

import com.app.registre.model.DesignationEntry;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.ArrayList;

public class DesignationsController {

    @FXML private TableView<DesignationEntry> table;
    @FXML private TableColumn<DesignationEntry, String> colImp;
    @FXML private TableColumn<DesignationEntry, String> colDesignation;
    @FXML private Button addBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;

    private ObservableList<DesignationEntry> items = FXCollections.observableArrayList();
    private Path csvPath = null;

    public void initialize() {
        colImp.setCellValueFactory(new PropertyValueFactory<>("imp"));
        colDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));
        table.setItems(items);
        loadFromCsv();

        addBtn.setOnAction(e -> addDesignation());
        editBtn.setOnAction(e -> editDesignation());
        deleteBtn.setOnAction(e -> deleteDesignation());
        saveBtn.setOnAction(e -> saveToCsv());
    }

    private void loadFromCsv() {
        try {
            URL res = getClass().getResource("/data/designations.csv");
            if (res != null) {
                try {
                    csvPath = Path.of(res.toURI());
                } catch (Exception ex) {
                    csvPath = Path.of("src/main/resources/data/designations.csv");
                }
            } else {
                csvPath = Path.of("src/main/resources/data/designations.csv");
            }

            List<String> lines = new ArrayList<>();
            if (Files.exists(csvPath)) {
                lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
            }
            // parse semicolon-separated values, skip header if present
            LinkedHashSet<DesignationEntry> set = new LinkedHashSet<>();
            boolean first = true;
            for (String l : lines) {
                if (l == null) continue;
                String line = l.trim();
                if (line.isEmpty()) continue;
                // Detect header
                if (first && line.toUpperCase().startsWith("IMP")) { first = false; continue; }
                first = false;
                String[] parts = line.split(";", 2);
                String imp = parts.length > 0 ? parts[0].trim() : "";
                String desc = parts.length > 1 ? parts[1].trim() : "";
                set.add(new DesignationEntry(imp, desc));
            }
            items.setAll(set);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Impossible de charger designations.csv: " + ex.getMessage());
        }
    }

    private void addDesignation() {
        // ask for IMP then designation
        TextInputDialog impDlg = new TextInputDialog();
        impDlg.setTitle("Ajouter une désignation");
        impDlg.setHeaderText(null);
        impDlg.setContentText("IMP:");
        Optional<String> impRes = impDlg.showAndWait();
        if (impRes.isEmpty()) return;
        String imp = impRes.get().trim();
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Ajouter une désignation");
        dlg.setHeaderText(null);
        dlg.setContentText("Designation:");
        Optional<String> res = dlg.showAndWait();
        if (res.isPresent()) {
            String val = res.get().trim();
            if (val.isEmpty()) return;
            if (containsIgnoreCase(imp, val)) {
                showError("La désignation existe déjà.");
                return;
            }
            items.add(new DesignationEntry(imp, val));
        }
    }

    private void editDesignation() {
        DesignationEntry sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("Sélectionnez une ligne à modifier.");
            return;
        }
        TextInputDialog impDlg = new TextInputDialog(sel.getImp());
        impDlg.setTitle("Modifier la désignation");
        impDlg.setHeaderText(null);
        impDlg.setContentText("IMP:");
        Optional<String> impRes = impDlg.showAndWait();
        if (impRes.isEmpty()) return;
        String imp = impRes.get().trim();

        TextInputDialog dlg = new TextInputDialog(sel.getDesignation());
        dlg.setTitle("Modifier la désignation");
        dlg.setHeaderText(null);
        dlg.setContentText("Designation:");
        Optional<String> res = dlg.showAndWait();
        if (res.isPresent()) {
            String val = res.get().trim();
            if (val.isEmpty()) return;
            if (!(imp.equalsIgnoreCase(sel.getImp()) && val.equalsIgnoreCase(sel.getDesignation())) && containsIgnoreCase(imp, val)) {
                showError("La désignation existe déjà.");
                return;
            }
            sel.setImp(imp);
            sel.setDesignation(val);
            table.refresh();
        }
    }

    private void deleteDesignation() {
        DesignationEntry sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showError("Sélectionnez une ligne à supprimer.");
            return;
        }
        items.remove(sel);
    }

    private boolean containsIgnoreCase(String imp, String val) {
        for (DesignationEntry s : items) {
            if (s.getImp() != null && s.getDesignation() != null) {
                if (s.getImp().equalsIgnoreCase(imp) && s.getDesignation().equalsIgnoreCase(val)) return true;
            }
        }
        return false;
    }

    private void saveToCsv() {
        try {
            if (csvPath == null) csvPath = Path.of("src/main/resources/data/designations.csv");
            // ensure parent exists
            File parent = csvPath.toFile().getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            // write unique lines preserving order
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (DesignationEntry s : items) {
                if (s != null && !(s.getDesignation() == null || s.getDesignation().trim().isEmpty())) {
                    String line = (s.getImp() == null ? "" : s.getImp().trim()) + ";" + s.getDesignation().trim();
                    set.add(line);
                }
            }
            // include header
            ArrayList<String> out = new ArrayList<>();
            out.add("IMP;Designation");
            out.addAll(set);
            Files.write(csvPath, out, StandardCharsets.UTF_8);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Fichier enregistré.");
            a.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur lors de l'enregistrement: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.showAndWait();
    }
}
