package com.app.registre.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DesignationLookup {
    private static final Map<String, String> MAP;

    static {
        Map<String, String> m = new HashMap<>();
        
        // Essayer d'abord de charger depuis le fichier utilisateur (si modifié)
        try {
            Path userFile = DesignationFileManager.getDesignationsPath();
            if (Files.exists(userFile)) {
                for (String line : Files.readAllLines(userFile, StandardCharsets.UTF_8)) {
                    parseLine(line, m);
                }
            }
        } catch (Exception e) {
            // Fallback sur les ressources du JAR
            InputStream in = DesignationLookup.class.getResourceAsStream("/data/designations.csv");
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line = br.readLine(); // header
                    while ((line = br.readLine()) != null) {
                        parseLine(line, m);
                    }
                } catch (Exception ex) {
                    System.err.println("Erreur lors du chargement des designations: " + ex.getMessage());
                }
            }
        }
        
        MAP = Collections.unmodifiableMap(m);
    }
    
    private static void parseLine(String line, Map<String, String> map) {
        if (line == null) return;
        line = line.trim();
        if (line.isEmpty() || line.toUpperCase().startsWith("IMP")) return; // skip header
        
        String[] parts = line.split(";", 2);
        if (parts.length >= 2) {
            String key = parts[0].trim();
            String val = parts[1].trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                map.put(key, val);
            }
        }
    }

    public static String getDesignationForIMP(String imp) {
        if (imp == null) return null;
        String k = imp.trim();
        // try exact
        if (MAP.containsKey(k)) return MAP.get(k);
        // try prefix match (some codes like 6122 vs 61227)
        for (int len = k.length(); len > 0; len--) {
            String sub = k.substring(0, len);
            if (MAP.containsKey(sub)) return MAP.get(sub);
        }
        return null;
    }
}
