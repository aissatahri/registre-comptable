package com.app.registre.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DesignationLookup {
    private static final Map<String, String> MAP;

    static {
        Map<String, String> m = new HashMap<>();
        InputStream in = DesignationLookup.class.getResourceAsStream("/data/designations.csv");
        if (in != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line = br.readLine(); // header
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(";", 2);
                    if (parts.length >= 2) {
                        String key = parts[0].trim();
                        String val = parts[1].trim();
                        if (!key.isEmpty() && !val.isEmpty()) m.put(key, val);
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement des designations: " + e.getMessage());
            }
        }
        MAP = Collections.unmodifiableMap(m);
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
