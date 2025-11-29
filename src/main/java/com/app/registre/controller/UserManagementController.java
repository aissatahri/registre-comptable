package com.app.registre.controller;

import com.app.registre.dao.UserDAO;
import com.app.registre.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import com.app.registre.util.DialogUtils;

public class UserManagementController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colDisplayName;
    @FXML private TableColumn<User, String> colEmail;

    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getUsername()));
        colDisplayName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getDisplayName() != null ? c.getValue().getDisplayName() : ""));
        colEmail.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getEmail() != null ? c.getValue().getEmail() : ""));
        usersTable.setItems(users);
        refreshList();
    }

    private void refreshList() {
        users.clear();
        users.addAll(userDAO.listAllUsers());
    }

    @FXML
    private void handleAdd() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Nouvel utilisateur");
        d.setHeaderText("Créer un nouvel utilisateur");
        d.setContentText("Nom d'utilisateur:");
        java.util.Optional<String> res = d.showAndWait();
        if (res.isPresent()) {
            String username = res.get().trim();
            if (username.isEmpty()) return;
            // ask for password
            TextInputDialog pwd = new TextInputDialog();
            pwd.setTitle("Mot de passe");
            pwd.setHeaderText("Définir le mot de passe pour " + username);
            pwd.setContentText("Mot de passe:");
            java.util.Optional<String> p = pwd.showAndWait();
            if (p.isPresent()) {
                boolean ok = userDAO.createUser(username, p.get());
                if (!ok) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Impossible de créer l'utilisateur (existe déjà ?)" );
                    a.showAndWait();
                }
                else {
                    // after creating user, ask for display name and email
                    Dialog<ButtonType> infoDlg = new Dialog<>();
                    DialogUtils.initOwner(infoDlg, null);
                    infoDlg.setTitle("Profil utilisateur");
                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(10);
                    grid.setPadding(new javafx.geometry.Insets(10));
                    TextField displayField = new TextField();
                    TextField emailField = new TextField();
                    grid.add(new Label("Nom affiché:"), 0, 0);
                    grid.add(displayField, 1, 0);
                    grid.add(new Label("Email:"), 0, 1);
                    grid.add(emailField, 1, 1);
                    infoDlg.getDialogPane().setContent(grid);
                    infoDlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                    java.util.Optional<ButtonType> answer = infoDlg.showAndWait();
                    if (answer.isPresent() && answer.get() == ButtonType.OK) {
                        String dn = displayField.getText();
                        String em = emailField.getText();
                        userDAO.updateProfile(username, dn, em);
                    }
                    refreshList();
                }
            }
        }
    }

    @FXML
    private void handleEdit() {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Dialog<ButtonType> dlg = new Dialog<>();
        DialogUtils.initOwner(dlg, null);
        dlg.setTitle("Modifier profil");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10));
        TextField displayField = new TextField(sel.getDisplayName() != null ? sel.getDisplayName() : "");
        TextField emailField = new TextField(sel.getEmail() != null ? sel.getEmail() : "");
        grid.add(new Label("Nom affiché:"), 0, 0);
        grid.add(displayField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        java.util.Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            boolean ok = userDAO.updateProfile(sel.getUsername(), displayField.getText(), emailField.getText());
            if (!ok) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Impossible d'enregistrer le profil");
                a.showAndWait();
            }
            refreshList();
        }
    }

    @FXML
    private void handleDelete() {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer l'utilisateur '" + sel.getUsername() + "' ?", javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        java.util.Optional<javafx.scene.control.ButtonType> r = c.showAndWait();
        if (r.isPresent() && r.get() == javafx.scene.control.ButtonType.YES) {
            boolean ok = userDAO.deleteByUsername(sel.getUsername());
            if (!ok) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Impossible de supprimer l'utilisateur");
                a.showAndWait();
            }
            refreshList();
        }
    }

    @FXML
    private void handleChangePassword() {
        User sel = usersTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog pwd = new TextInputDialog();
        pwd.setTitle("Changer le mot de passe");
        pwd.setHeaderText("Changer le mot de passe pour " + sel.getUsername());
        pwd.setContentText("Nouveau mot de passe:");
        java.util.Optional<String> p = pwd.showAndWait();
        if (p.isPresent()) {
            boolean ok = userDAO.updatePassword(sel.getUsername(), p.get());
            if (!ok) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Impossible de changer le mot de passe");
                a.showAndWait();
            }
            refreshList();
        }
    }
}
