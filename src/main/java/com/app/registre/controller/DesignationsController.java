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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;

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
    @FXML private Button importBtn;
    @FXML private Button exportBtn;

    private ObservableList<DesignationEntry> items = FXCollections.observableArrayList();
    private Path csvPath = null;

    public void initialize() {
        colImp.setCellValueFactory(new PropertyValueFactory<>("imp"));
        colDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));
        table.setItems(items);
        loadFromCsv();

        // enable inline editing
        colImp.setCellFactory(TextFieldTableCell.forTableColumn());
        colDesignation.setCellFactory(TextFieldTableCell.forTableColumn());

        colImp.setOnEditCommit(ev -> {
            DesignationEntry entry = ev.getRowValue();
            String newImp = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
            if (newImp.isEmpty()) {
                showError("IMP ne peut pas être vide.");
                table.refresh();
                return;
            }
            // check uniqueness of IMP across other entries
            for (DesignationEntry e : items) {
                if (e != entry && e.getImp() != null && e.getImp().equalsIgnoreCase(newImp)) {
                    showError("Un enregistrement avec ce IMP existe déjà.");
                    table.refresh();
                    return;
                }
            }
            entry.setImp(newImp);
        });

        colDesignation.setOnEditCommit(ev -> {
            DesignationEntry entry = ev.getRowValue();
            String newDesc = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
            if (newDesc.isEmpty()) {
                showError("La désignation ne peut pas être vide.");
                table.refresh();
                return;
            }
            // check duplicate pair (IMP+Designation)
            for (DesignationEntry e : items) {
                if (e != entry && e.getImp() != null && e.getDesignation() != null) {
                    if (e.getImp().equalsIgnoreCase(entry.getImp()) && e.getDesignation().equalsIgnoreCase(newDesc)) {
                        showError("La désignation existe déjà pour ce IMP.");
                        table.refresh();
                        return;
                    }
                }
            }
            entry.setDesignation(newDesc);
        });

        addBtn.setOnAction(e -> addDesignation());
        editBtn.setOnAction(e -> editDesignation());
        deleteBtn.setOnAction(e -> deleteDesignation());
        saveBtn.setOnAction(e -> saveToCsv());
        importBtn.setOnAction(e -> importFromFile());
        exportBtn.setOnAction(e -> exportToFile());
    }

    private void loadFromCsv() {
        try {
            // Utiliser le fichier dans le dossier utilisateur (permet les modifications dans le JAR)
            csvPath = com.app.registre.util.DesignationFileManager.getDesignationsPath();

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
        if (imp.isEmpty()) { showError("IMP ne peut pas être vide."); return; }
        // enforce unique IMP if any existing entry has same IMP
        for (DesignationEntry e : items) {
            if (e.getImp() != null && e.getImp().equalsIgnoreCase(imp)) {
                showError("Un enregistrement avec ce IMP existe déjà.");
                return;
            }
        }
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Confirmez la suppression de la désignation sélectionnée ?");
        Optional<javafx.scene.control.ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == javafx.scene.control.ButtonType.OK) {
            items.remove(sel);
        }
    }

    private boolean containsIgnoreCase(String imp, String val) {
        for (DesignationEntry s : items) {
            if (s.getImp() != null && s.getDesignation() != null) {
                if (s.getImp().equalsIgnoreCase(imp) && s.getDesignation().equalsIgnoreCase(val)) return true;
            }
        }
        return false;
    }

    // Import from external CSV (semicolon separated). Merge and avoid duplicates.
    private void importFromFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importer designations depuis CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv", "*.txt"));
        File f = fc.showOpenDialog(table.getScene() == null ? null : table.getScene().getWindow());
        if (f == null) return;
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            int added = 0;
            for (String l : lines) {
                if (l == null) continue;
                String line = l.trim();
                if (line.isEmpty()) continue;
                if (line.toUpperCase().startsWith("IMP")) continue; // header
                String[] parts = line.split(";", 2);
                String imp = parts.length > 0 ? parts[0].trim() : "";
                String desc = parts.length > 1 ? parts[1].trim() : "";
                if (imp.isEmpty() && desc.isEmpty()) continue;
                // check if pair exists
                boolean exists = false;
                for (DesignationEntry e : items) {
                    if (e.getImp() != null && e.getDesignation() != null && e.getImp().equalsIgnoreCase(imp) && e.getDesignation().equalsIgnoreCase(desc)) { exists = true; break; }
                }
                if (!exists) {
                    // enforce unique IMP: if an entry with same IMP exists but different desc, skip and log
                    boolean impExists = false;
                    for (DesignationEntry e : items) if (e.getImp() != null && e.getImp().equalsIgnoreCase(imp)) { impExists = true; break; }
                    if (impExists) {
                        // skip adding to avoid duplicate IMP
                        continue;
                    }
                    items.add(new DesignationEntry(imp, desc));
                    added++;
                }
            }
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Import terminé. Lignes ajoutées: " + added);
            a.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur d'import: " + ex.getMessage());
        }
    }

    // Export current list to external CSV path chosen by user
    private void exportToFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les designations vers CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        fc.setInitialFileName("designations_export.csv");
        File f = fc.showSaveDialog(table.getScene() == null ? null : table.getScene().getWindow());
        if (f == null) return;
        try {
            ArrayList<String> out = new ArrayList<>();
            out.add("IMP;Designation");
            for (DesignationEntry s : items) {
                out.add((s.getImp() == null ? "" : s.getImp()) + ";" + (s.getDesignation() == null ? "" : s.getDesignation()));
            }
            Files.write(f.toPath(), out, StandardCharsets.UTF_8);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Export réussi: " + f.getAbsolutePath());
            a.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur d'export: " + ex.getMessage());
        }
    }

    private void saveToCsv() {
        try {
            if (csvPath == null) {
                csvPath = com.app.registre.util.DesignationFileManager.getDesignationsPath();
            }
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
            
            // Afficher le chemin du fichier sauvegardé
            String location = csvPath.toString();
            Alert a = new Alert(Alert.AlertType.INFORMATION, 
                "Fichier enregistré avec succès.\n\nEmplacement: " + location + 
                "\n\nNote: Redémarrez l'application pour que les modifications soient prises en compte dans l'autocomplétion.");
            a.setHeaderText("Sauvegarde réussie");
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
