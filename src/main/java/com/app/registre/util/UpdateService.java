package com.app.registre.util;

/**
 * Stub UpdateService that delegates to an optional implementation if present.
 *
 * The full Update4j-based implementation is provided under
 * `src/optional/update4j` as `com.app.registre.optional.update4j.UpdateServiceImpl`.
 * To enable runtime updates, move that implementation into the main sources and
 * add the update4j dependency to your `pom.xml`.
 */
public final class UpdateService {
    private UpdateService() {}

    public static void checkForUpdatesAndPrompt() {
        try {
            Class<?> impl = Class.forName("com.app.registre.optional.update4j.UpdateServiceImpl");
            java.lang.reflect.Method m = impl.getMethod("checkForUpdatesAndPrompt");
            m.invoke(null);
        } catch (ClassNotFoundException e) {
            // show a simple message: feature not available
            javafx.application.Platform.runLater(() -> {
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "La fonctionnalité de mise à jour n'est pas disponible (module manquant).", javafx.scene.control.ButtonType.OK);
                a.setTitle("Mise à jour");
                a.showAndWait();
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getLocalVersion() {
        try {
            Class<?> impl = Class.forName("com.app.registre.optional.update4j.UpdateServiceImpl");
            java.lang.reflect.Method m = impl.getMethod("getLocalVersion");
            Object r = m.invoke(null);
            if (r != null) return r.toString();
        } catch (Exception ignored) {}
        // fallback: read version.txt
        try (java.io.InputStream in = UpdateService.class.getResourceAsStream("/version.txt")) {
            if (in != null) {
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
                    String s = r.readLine();
                    if (s != null) return s.trim();
                }
            }
        } catch (java.io.IOException ignored) {}
        return "0.0.0";
    }
}
