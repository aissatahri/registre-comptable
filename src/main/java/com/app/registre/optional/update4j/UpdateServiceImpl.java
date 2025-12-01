package com.app.registre.optional.update4j;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reflection-based UpdateService implementation that invokes update4j methods via reflection.
 * This makes the code resilient to different update4j API versions present on the classpath.
 */
public final class UpdateServiceImpl {
    private static final Logger LOGGER = Logger.getLogger(UpdateServiceImpl.class.getName());
    private static final String MANIFEST_URL = "https://github.com/aissatahri/registre-comptable/releases/latest/download/update4j.xml";
    private UpdateServiceImpl() {}

    public static void checkForUpdatesAndPrompt() {
        CompletableFuture.supplyAsync(() -> {
            try {
                String override = System.getProperty("update4j.manifest.url");
                String manifestUrl = override != null && !override.isBlank() ? override : MANIFEST_URL;
                String usedUrl = manifestUrl;
                String xml = null;
                try {
                    xml = readUrlAsString(manifestUrl);
                } catch (IOException ioe) {
                    // try GitHub API fallback
                    try {
                        String found = findManifestUrlFromGitHubApi();
                        if (found != null) {
                            xml = readUrlAsString(found);
                            usedUrl = found;
                        }
                    } catch (Exception ignore) {}
                    if (xml == null) throw ioe;
                }

                String remoteVersion = parseVersionFromManifest(xml);
                Object cfg = loadConfiguration(new URL(usedUrl));
                return new Object[] { cfg, remoteVersion };
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "No update manifest found or error reading manifest: " + e.getMessage());
                return null;
            }
        }).thenAccept(obj -> {
            if (obj == null) return;
            Object cfg = ((Object[]) obj)[0];
            String remoteVersion = (String) ((Object[]) obj)[1];

            String local = getLocalVersion();
            final boolean newer = remoteVersion != null && !remoteVersion.isBlank() && isRemoteVersionNewer(local, remoteVersion);

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

    private static Object loadConfiguration(URL url) throws Exception {
        Class<?> cfgClass = Class.forName("org.update4j.Configuration");
        // Try multiple read signatures
        try {
            Method m = cfgClass.getMethod("read", URL.class);
            return m.invoke(null, url);
        } catch (NoSuchMethodException ignored) {}

        try {
            Method m = cfgClass.getMethod("read", java.io.Reader.class);
            try (InputStream in = url.openStream(); java.io.Reader r = new java.io.InputStreamReader(in)) {
                return m.invoke(null, r);
            }
        } catch (NoSuchMethodException ignored) {}

        try {
            Method m = cfgClass.getMethod("read", java.io.InputStream.class);
            try (InputStream in = url.openStream()) {
                return m.invoke(null, in);
            }
        } catch (NoSuchMethodException ignored) {}

        throw new NoSuchMethodException("No suitable Configuration.read(...) method found in update4j on classpath");
    }

    private static void applyUpdateWithProgress(Object cfg) {
        Platform.runLater(() -> {
            final javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Mise à jour");
            final javafx.scene.layout.VBox box = new javafx.scene.layout.VBox();
            box.setSpacing(10);
            final javafx.scene.control.Label status = new javafx.scene.control.Label("Préparation...");
            final javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
            pi.setPrefSize(64, 64);
            box.getChildren().addAll(status, pi);
            dialog.getDialogPane().setContent(box);
            dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CANCEL);

            dialog.setResultConverter(btn -> null);
            CompletableFuture.runAsync(() -> {
                try {
                    Platform.runLater(() -> status.setText("Téléchargement..."));
                    invokeBootstrapUpdate(cfg);
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
                    final String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() + " - " + e.toString();
                    Platform.runLater(() -> {
                        dialog.close();
                        Alert err = new Alert(Alert.AlertType.ERROR);
                        err.setTitle("Erreur de mise à jour");
                        err.setHeaderText("Impossible d'appliquer la mise à jour");
                        err.setContentText(errorMsg);
                        err.showAndWait();
                    });
                }
            });

            dialog.showAndWait();
        });
    }

    private static void invokeBootstrapUpdate(Object cfg) throws Exception {
        Class<?> boot = Class.forName("org.update4j.Bootstrap");
        // Update4j 1.5.0: Bootstrap.update(Configuration config)
        Method updateMethod = boot.getMethod("update", cfg.getClass());
        updateMethod.invoke(null, cfg);
    }

    private static String readUrlAsString(String urlStr) throws IOException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
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

    private static String findManifestUrlFromGitHubApi() throws IOException {
        // Use the GitHub Releases API to find the asset named update4j.xml
        final String api = "https://api.github.com/repos/aissatahri/registre-comptable/releases/latest";
        String body = readUrlAsString(api);
        if (body == null) return null;

                // First try to find an asset object that contains name:"update4j.xml" and capture its browser_download_url
                Pattern p = Pattern.compile("\"name\"\\s*:\\s*\"update4j\\.xml\"[^{]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
            Matcher m = p.matcher(body);
        if (m.find()) {
            return m.group(1).replace("\\/", "/");
        }

        // Fallback: find any asset whose name contains 'update4j' and return its browser_download_url
            p = Pattern.compile("\"name\"\\s*:\\s*\"([^\\\"]*update4j[^\\\"]*)\"[^{]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        m = p.matcher(body);
            if (m.find()) {
                return m.group(2).replace("\\/", "/");
            }

        // As a last resort, try to pick any asset with .xml in the name and hope it's the manifest
            p = Pattern.compile("\"name\"\\s*:\\s*\"([^\\\"]+\\.xml)\"[^{]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
        m = p.matcher(body);
        if (m.find()) {
            return m.group(2).replace("\\/", "/");
        }

        return null;
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
