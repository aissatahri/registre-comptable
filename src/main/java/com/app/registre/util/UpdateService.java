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
            // Optional implementation not present: perform a lightweight fallback check using GitHub Releases
            fallbackCheckAndPrompt();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void fallbackCheckAndPrompt() {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.github.com/repos/aissatahri/registre-comptable/releases/latest"))
                        .GET()
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();
                java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) return null;
                String body = resp.body();
                // Extract "tag_name" value without regex to avoid preview string-template issues
                String tag = null;
                int tagKey = body.indexOf("\"tag_name\"");
                if (tagKey >= 0) {
                    int colon = body.indexOf(':', tagKey);
                    if (colon >= 0) {
                        int vStart = body.indexOf('"', colon + 1);
                        if (vStart >= 0) {
                            int vEnd = body.indexOf('"', vStart + 1);
                            if (vEnd >= 0) tag = body.substring(vStart + 1, vEnd);
                        }
                    }
                }

                // Find browser_download_url for version.json asset by scanning asset objects
                String versionJsonUrl = null;
                int nameKey = body.indexOf("\"name\"");
                while (nameKey >= 0) {
                    int colon = body.indexOf(':', nameKey);
                    if (colon < 0) break;
                    int s = body.indexOf('"', colon + 1);
                    if (s < 0) break;
                    int e = body.indexOf('"', s + 1);
                    if (e < 0) break;
                    String nameVal = body.substring(s + 1, e);
                    if ("version.json".equals(nameVal)) {
                        int bdKey = body.indexOf("\"browser_download_url\"", e);
                        if (bdKey >= 0) {
                            int colon2 = body.indexOf(':', bdKey);
                            if (colon2 >= 0) {
                                int s2 = body.indexOf('"', colon2 + 1);
                                int e2 = s2 >= 0 ? body.indexOf('"', s2 + 1) : -1;
                                if (s2 >= 0 && e2 >= 0) {
                                    versionJsonUrl = body.substring(s2 + 1, e2);
                                    break;
                                }
                            }
                        }
                    }
                    nameKey = body.indexOf("\"name\"", e + 1);
                }

                if (versionJsonUrl == null) {
                    // fallback: try the conventional download path (may 404)
                    versionJsonUrl = "https://github.com/aissatahri/registre-comptable/releases/latest/download/version.json";
                }

                // download version.json
                try {
                    java.net.http.HttpRequest r2 = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(versionJsonUrl)).GET().build();
                    java.net.http.HttpResponse<String> r2resp = client.send(r2, java.net.http.HttpResponse.BodyHandlers.ofString());
                    if (r2resp.statusCode() != 200) return null;
                    String vjson = r2resp.body();
                    java.util.regex.Matcher mv = java.util.regex.Pattern.compile("\\\"version\\\"\\s*:\\s*\\\"([0-9.]+)\\\"").matcher(vjson);
                    String remoteVersion = mv.find() ? mv.group(1) : tag != null ? tag.replaceFirst("^v", "") : null;
                    return remoteVersion;
                } catch (Exception ex) {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }).thenAccept(remoteVersion -> {
            if (remoteVersion == null) return;
            String local = getLocalVersion();
            boolean newer = isRemoteVersionNewer(local, remoteVersion);
            if (newer) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    a.setTitle("Mise à jour disponible");
                    a.setHeaderText("Nouvelle version disponible : v" + remoteVersion);
                    a.setContentText("Une nouvelle version est disponible. Voulez-vous ouvrir la page de release pour la télécharger ?");
                    java.util.Optional<javafx.scene.control.ButtonType> r = a.showAndWait();
                    if (r.isPresent() && r.get() == javafx.scene.control.ButtonType.OK) {
                        try {
                            java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://github.com/aissatahri/registre-comptable/releases/latest"));
                        } catch (Exception ignored) {}
                    }
                });
            } else {
                // optional: do nothing or small info; we skip info to avoid annoying users
            }
        });
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
