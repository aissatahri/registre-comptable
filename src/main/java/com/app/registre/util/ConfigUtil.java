package com.app.registre.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigUtil {
    private static Path getConfigFile() {
        String localApp = System.getenv("LOCALAPPDATA");
        if (localApp == null || localApp.isBlank()) localApp = System.getProperty("user.home");
        Path dir = Paths.get(localApp, "RegistreComptable");
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
        return dir.resolve("config.properties");
    }

    public static String get(String key) {
        Path cfg = getConfigFile();
        if (!Files.exists(cfg)) return null;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(cfg)) {
            p.load(in);
            return p.getProperty(key);
        } catch (IOException e) {
            return null;
        }
    }

    public static void set(String key, String value) {
        Path cfg = getConfigFile();
        Properties p = new Properties();
        try {
            if (Files.exists(cfg)) try (InputStream in = Files.newInputStream(cfg)) { p.load(in); }
            p.setProperty(key, value);
            try (OutputStream out = Files.newOutputStream(cfg)) { p.store(out, "RegistreComptable configuration"); }
        } catch (IOException ignored) {}
    }

    public static void remove(String key) {
        Path cfg = getConfigFile();
        if (!Files.exists(cfg)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(cfg)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        if (p.containsKey(key)) {
            p.remove(key);
            try (OutputStream out = Files.newOutputStream(cfg)) { p.store(out, "RegistreComptable configuration"); } catch (IOException ignored) {}
        }
    }
}
