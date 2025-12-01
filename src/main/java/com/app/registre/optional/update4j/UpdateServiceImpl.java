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
        System.err.println("[UpdateServiceImpl] D��marrage de la v��rification des mises ��  jour...");
        CompletableFuture.supplyAsync(() -> {
            try {
                System.err.println("[UpdateServiceImpl] Lecture du manifest depuis: " + MANIFEST_URL);
                String override = System.getProperty("update4j.manifest.url");
                String manifestUrl = override != null && !override.isBlank() ? override : MANIFEST_URL;
                String usedUrl = manifestUrl;
                String xml = null;
                try {
                    xml = readUrlAsString(manifestUrl);
                    System.err.println("[UpdateServiceImpl] Manifest t��l��charg�� avec succ��s");
                } catch (IOException ioe) {
                    System.err.println("[UpdateServiceImpl] ��chec lecture manifest, tentative fallback API GitHub: " + ioe.getMessage());
                    // try GitHub API fallback
                    try {
                        String found = findManifestUrlFromGitHubApi();
                        if (found != null) {
                            xml = readUrlAsString(found);
                            usedUrl = found;
                            System.err.println("[UpdateServiceImpl] Manifest trouv�� via API GitHub: " + found);
                        }
                    } catch (Exception ignore) {
                        System.err.println("[UpdateServiceImpl] Fallback API GitHub ��chou��: " + ignore.getMessage());
                    }
                    if (xml == null) throw ioe;
                }

                String remoteVersion = parseVersionFromManifest(xml);
                System.err.println("[UpdateServiceImpl] Version distante: " + remoteVersion);
                Object cfg = loadConfiguration(new URL(usedUrl));
                System.err.println("[UpdateServiceImpl] Configuration charg��e");
                return new Object[] { cfg, remoteVersion };
            } catch (Exception e) {
                System.err.println("[UpdateServiceImpl] Erreur lors de la v��rification: " + e.getMessage());
                e.printStackTrace();
                LOGGER.log(Level.INFO, "No update manifest found or error reading manifest: " + e.getMessage());
                return null;
            }
        }).thenAccept(obj -> {
            System.err.println("[UpdateServiceImpl] Traitement du r��sultat...");
            if (obj == null) {
                System.err.println("[UpdateServiceImpl] Aucun r��sultat (erreur pr��c��dente)");
                return;
            }
            Object cfg = ((Object[]) obj)[0];
            String remoteVersion = (String) ((Object[]) obj)[1];

            String local = getLocalVersion();
            System.err.println("[UpdateServiceImpl] Version locale: " + local);
            final boolean newer = remoteVersion != null && !remoteVersion.isBlank() && isRemoteVersionNewer(local, remoteVersion);
            System.err.println("[UpdateServiceImpl] Version plus r��cente disponible? " + newer);

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
                    System.err.println("[UpdateServiceImpl] ===== DÉBUT TÉLÉCHARGEMENT =====");
                    Platform.runLater(() -> status.setText("Téléchargement..."));
                    
                    // Afficher les informations de configuration
                    try {
                        Method getVersion = cfg.getClass().getMethod("getVersion");
                        String version = (String) getVersion.invoke(cfg);
                        System.err.println("[UpdateServiceImpl] Version du manifest: " + version);
                        
                        Method getFiles = cfg.getClass().getMethod("getFiles");
                        Object filesList = getFiles.invoke(cfg);
                        System.err.println("[UpdateServiceImpl] Nombre de fichiers: " + ((java.util.List<?>) filesList).size());
                        
                        for (Object fileRef : (java.util.List<?>) filesList) {
                            Method getUri = fileRef.getClass().getMethod("getUri");
                            Object uri = getUri.invoke(fileRef);
                            System.err.println("[UpdateServiceImpl] - Fichier URI: " + uri);
                        }
                    } catch (Exception e) {
                        System.err.println("[UpdateServiceImpl] Erreur lors de l'affichage des infos: " + e.getMessage());
                    }
                    
                    // Création d'un UpdateHandler pour suivre la progression
                    Class<?> updateHandlerClass = Class.forName("org.update4j.service.UpdateHandler");
                    System.err.println("[UpdateServiceImpl] UpdateHandler class trouvée: " + updateHandlerClass.getName());
                    
                    Object updateHandlerInstance = java.lang.reflect.Proxy.newProxyInstance(
                        updateHandlerClass.getClassLoader(),
                        new Class<?>[]{updateHandlerClass},
                        (proxy, method, args) -> {
                            String methodName = method.getName();
                            System.err.println("[UpdateServiceImpl] UpdateHandler." + methodName + " appelé");
                            if ("updateDownloadProgress".equals(methodName)) {
                                float progress = (float) args[0];
                                Platform.runLater(() -> status.setText(String.format("Téléchargement... %.0f%%", progress * 100)));
                            } else if ("doneDownloads".equals(methodName)) {
                                Platform.runLater(() -> status.setText("Téléchargement terminé"));
                            }
                            return null;
                        }
                    );
                    
                    System.err.println("[UpdateServiceImpl] UpdateHandler proxy créé");
                    
                    // Appel de la méthode update(UpdateHandler) pour vraiment télécharger
                    Method updateMethod = cfg.getClass().getMethod("update", updateHandlerClass);
                    System.err.println("[UpdateServiceImpl] Méthode update trouvée: " + updateMethod.getName());
                    System.err.println("[UpdateServiceImpl] Appel de config.update(handler)...");
                    
                    boolean success = (boolean) updateMethod.invoke(cfg, updateHandlerInstance);
                    
                    System.err.println("[UpdateServiceImpl] Résultat du téléchargement: " + success);
                    
                    Platform.runLater(() -> status.setText("Application..."));
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                    
                    Platform.runLater(() -> {
                        dialog.close();
                        if (success) {
                            Alert info = new Alert(Alert.AlertType.INFORMATION);
                            info.setTitle("Mise à jour appliquée");
                            info.setHeaderText("La mise à jour a été téléchargée avec succès");
                            info.setContentText("L'application va redémarrer pour appliquer les changements.");
                            info.getButtonTypes().setAll(ButtonType.OK);
                            info.showAndWait();
                            
                            // Utiliser le launcher intégré d'update4j pour redémarrer
                            try {
                                System.err.println("[UpdateServiceImpl] Lancement via update4j launcher...");
                                Method launchMethod = cfg.getClass().getMethod("launch");
                                launchMethod.invoke(cfg);
                                
                                // Quitter l'application actuelle
                                Platform.exit();
                                System.exit(0);
                            } catch (Exception e) {
                                System.err.println("[UpdateServiceImpl] Erreur lors du lancement: " + e.getMessage());
                                e.printStackTrace();
                                
                                // Fallback: redémarrage manuel
                                String newJarPath = getDownloadedJarPath(cfg);
                                System.err.println("[UpdateServiceImpl] Fallback - Nouveau JAR téléchargé: " + newJarPath);
                                restartApplication(newJarPath);
                            }
                        } else {
                            Alert warn = new Alert(Alert.AlertType.WARNING, "La mise à jour a échoué. Consultez les logs pour plus de détails.", ButtonType.OK);
                            warn.setTitle("Échec de la mise à jour");
                            warn.showAndWait();
                        }
                    });
                } catch (Exception e) {
                    // Déballer InvocationTargetException pour obtenir la vraie cause
                    Throwable cause = e;
                    if (e instanceof java.lang.reflect.InvocationTargetException) {
                        cause = ((java.lang.reflect.InvocationTargetException) e).getTargetException();
                    }
                    
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'application de la mise à jour: " + cause.getMessage(), cause);
                    System.err.println("[UpdateServiceImpl] ERREUR: " + cause.getClass().getName() + ": " + cause.getMessage());
                    cause.printStackTrace();
                    
                    final String errorMsg = cause.getMessage() != null && !cause.getMessage().isEmpty() 
                        ? cause.getMessage() 
                        : cause.getClass().getSimpleName();
                        
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

    private static String getDownloadedJarPath(Object configInstance) {
        try {
            // Récupérer le basePath depuis la configuration
            Method getBasePath = configInstance.getClass().getMethod("getBasePath");
            Object basePath = getBasePath.invoke(configInstance);
            
            // Récupérer la liste des fichiers
            Method getFiles = configInstance.getClass().getMethod("getFiles");
            Object filesList = getFiles.invoke(configInstance);
            
            if (filesList instanceof java.util.List) {
                for (Object fileRef : (java.util.List<?>) filesList) {
                    Method getPath = fileRef.getClass().getMethod("getPath");
                    Object path = getPath.invoke(fileRef);
                    String pathStr = path.toString();
                    
                    // Trouver le fichier JAR
                    if (pathStr.endsWith(".jar")) {
                        // Résoudre le chemin complet avec le basePath
                        if (basePath != null) {
                            // basePath est un Path object
                            Method resolve = basePath.getClass().getMethod("resolve", String.class);
                            Object resolved = resolve.invoke(basePath, pathStr);
                            Method toAbsolutePath = resolved.getClass().getMethod("toAbsolutePath");
                            Object absolute = toAbsolutePath.invoke(resolved);
                            return absolute.toString();
                        }
                        // Sinon, utiliser le répertoire de travail actuel
                        return new java.io.File(System.getProperty("user.dir"), pathStr).getAbsolutePath();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[UpdateServiceImpl] Erreur lors de la récupération du chemin du JAR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static void restartApplication(String newJarPath) {
        try {
            String javaBin = System.getProperty("java.home") + "/bin/java";
            String jarToLaunch = newJarPath;
            
            // Si le nouveau JAR n'est pas trouvé, utiliser le JAR actuel
            if (jarToLaunch == null || jarToLaunch.isEmpty()) {
                System.err.println("[UpdateServiceImpl] Nouveau JAR non trouvé, utilisation du JAR actuel");
                jarToLaunch = UpdateServiceImpl.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            }
            
            // Sur Windows, enlever le "/" initial si présent
            if (jarToLaunch.startsWith("/") && jarToLaunch.contains(":")) {
                jarToLaunch = jarToLaunch.substring(1);
            }
            
            System.err.println("[UpdateServiceImpl] Redémarrage avec: " + javaBin + " -jar " + jarToLaunch);
            
            // Construction de la commande de redémarrage
            ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", jarToLaunch);
            builder.directory(new java.io.File(System.getProperty("user.dir")));
            builder.start();
            
            // Arrêt de l'application actuelle après un court délai
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    System.exit(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } catch (Exception e) {
            System.err.println("[UpdateServiceImpl] Erreur lors du redémarrage: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Erreur de redémarrage");
                error.setHeaderText("Impossible de redémarrer automatiquement");
                error.setContentText("Veuillez fermer et relancer l'application manuellement.\n\nErreur: " + e.getMessage());
                error.showAndWait();
            });
        }
    }
}
