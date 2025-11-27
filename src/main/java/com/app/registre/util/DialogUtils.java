package com.app.registre.util;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Window;

public final class DialogUtils {

    private DialogUtils() {}

    public static Window getOwnerWindow(Node node) {
        if (node == null) return null;
        if (node.getScene() == null) return null;
        return node.getScene().getWindow();
    }

    public static void initOwner(Dialog<?> dialog, Node reference) {
        if (dialog == null) return;
        Window owner = getOwnerWindow(reference);
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
    }

    public static void initOwner(Alert alert, Node reference) {
        if (alert == null) return;
        Window owner = getOwnerWindow(reference);
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
    }
}
