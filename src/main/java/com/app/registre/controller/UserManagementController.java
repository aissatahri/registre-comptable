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

public class UserManagementController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUsername;

    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colUsername.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getUsername()));
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
                refreshList();
            }
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
