package com.app.registre.optional.update4j;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import org.update4j.Configuration;
import org.update4j.Bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UpdateServiceImpl {
    private static final Logger LOGGER = Logger.getLogger(UpdateServiceImpl.class.getName());
    private static final String MANIFEST_URL = "https://github.com/aissatahri/registre-comptable/releases/latest/download/update4j.xml";
    private UpdateServiceImpl() {}

    public static void checkForUpdatesAndPrompt() {
        CompletableFuture.supplyAsync(() -> {
            try {
                String xml = readUrlAsString(MANIFEST_URL);
                String remoteVersion = parseVersionFromManifest(xml);
                Configuration cfg = Configuration.read(new URL(MANIFEST_URL));
                return new Object[] { cfg, remoteVersion };
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "No update manifest found or error reading manifest: " + e.getMessage());
                return null;
            }
        }).thenAccept(obj -> {
            if (obj == null) return;
            Configuration cfg = (Configuration) ((Object[]) obj)[0];
            String remoteVersion = (String) ((Object[]) obj)[1];

            String local = getLocalVersion();
            boolean newer = false;
            if (remoteVersion != null && !remoteVersion.isBlank()) {
                newer = isRemoteVersionNewer(local, remoteVersion);
            }

            Platform.runLater(() -> {
                if (!newer) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION, "Aucune nouvelle version disponible.", ButtonType.OK);
                    info.setTitle("Mise à jour");
                    info.showAndWait();
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Mise à jour disponible");
                alert.setHeaderText("Une mise à jour est disponible (v" + remoteVersion + ")");
                alert.setContentText("Voulez-vous télécharger et appliquer la mise à jour maintenant ?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    applyUpdateWithProgress(cfg);
                }
            });
        });
    }

    private static void applyUpdateWithProgress(Configuration cfg) {
        Platform.runLater(() -> {
            javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Mise à jour");
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox();
            box.setSpacing(10);
            javafx.scene.control.Label status = new javafx.scene.control.Label("Préparation...");
            javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
            pi.setPrefSize(64, 64);
            box.getChildren().addAll(status, pi);
            dialog.getDialogPane().setContent(box);
            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);

            dialog.setResultConverter(btn -> null);
            CompletableFuture.runAsync(() -> {
                try {
                    Platform.runLater(() -> status.setText("Téléchargement..."));
                    Bootstrap.update(cfg, null, false);
                    Platform.runLater(() -> status.setText("Application..."));
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> {
                        dialog.close();
                        Alert info = new Alert(Alert.AlertType.INFORMATION, "La mise à jour a été appliquée. Veuillez relancer l'application.", ButtonType.OK);
                        info.setTitle("Mise à jour appliquée");
                        info.showAndWait();
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'application de la mise à jour: " + e.getMessage(), e);
                    Platform.runLater(() -> {
                        dialog.close();
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setTitle("Erreur de mise à jour");
                        err.setHeaderText("Impossible d'appliquer la mise à jour");
                        err.setContentText(e.getMessage());
                        err.showAndWait();
                    });
                }
            });

            dialog.showAndWait();
        });
    }

    private static String readUrlAsString(String urlStr) throws IOException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(urlStr)).GET().build();
        try {
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) return resp.body();
            throw new IOException("HTTP " + resp.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private static String parseVersionFromManifest(String xml) {
        if (xml == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/download/v([0-9]+(?:\\.[0-9]+)*)/").matcher(xml);
        if (m.find()) return m.group(1);
        m = java.util.regex.Pattern.compile("registre-comptable-([0-9]+(?:\\.[0-9]+)*)\\.jar").matcher(xml);
        if (m.find()) return m.group(1);
        return null;
    }

    private static boolean isRemoteVersionNewer(String local, String remote) {
        if (local == null || local.isBlank()) return true;
        try {
            String[] ls = local.split("\\.");
            String[] rs = remote.split("\\.");
            int n = Math.max(ls.length, rs.length);
            for (int i = 0; i < n; i++) {
                int lv = i < ls.length ? Integer.parseInt(ls[i]) : 0;
                int rv = i < rs.length ? Integer.parseInt(rs[i]) : 0;
                if (rv > lv) return true;
                if (rv < lv) return false;
            }
            return false;
        } catch (Exception e) {
            return !remote.equals(local);
        }
    }

    public static String getLocalVersion() {
        String v = UpdateServiceImpl.class.getPackage().getImplementationVersion();
        if (v != null && !v.isBlank()) return v;
        try (InputStream in = UpdateServiceImpl.class.getResourceAsStream("/version.txt")) {
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
                    String s = r.readLine();
                    if (s != null) return s.trim();
                }
            }
        } catch (IOException ignored) {}
        return "0.0.0";
    }
}
