package com.app.registre.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Gère le fichier designations.csv en le copiant du JAR vers le dossier utilisateur
 * pour permettre les modifications.
 */
public class DesignationFileManager {
    private static final String APP_DIR = "RegistreComptable";
    private static final String DESIGNATIONS_FILE = "designations.csv";
    
    /**
     * Retourne le chemin du fichier designations.csv dans le dossier utilisateur.
     * Copie le fichier depuis les ressources si nécessaire.
     */
    public static Path getDesignationsPath() throws IOException {
        Path appDir = getAppDirectory();
        Path csvFile = appDir.resolve(DESIGNATIONS_FILE);
        
        // Si le fichier n'existe pas, copier depuis les ressources du JAR
        if (!Files.exists(csvFile)) {
            copyFromResources(csvFile);
        }
        
        return csvFile;
    }
    
    /**
     * Retourne le dossier de l'application dans AppData/Local
     */
    private static Path getAppDirectory() throws IOException {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home") + "/.local/share";
        }
        
        Path appDir = Path.of(localAppData, APP_DIR);
        if (!Files.exists(appDir)) {
            Files.createDirectories(appDir);
        }
        
        return appDir;
    }
    
    /**
     * Copie le fichier designations.csv depuis les ressources vers le dossier utilisateur
     */
    private static void copyFromResources(Path destination) throws IOException {
        InputStream in = DesignationFileManager.class.getResourceAsStream("/data/designations.csv");
        if (in == null) {
            // Créer un fichier vide avec en-tête si la ressource n'existe pas
            Files.writeString(destination, "IMP;Designation\n", StandardCharsets.UTF_8);
        } else {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            in.close();
        }
    }
    
    /**
     * Recharge le fichier depuis les ressources (réinitialisation)
     */
    public static void resetToDefault() throws IOException {
        Path csvFile = getAppDirectory().resolve(DESIGNATIONS_FILE);
        copyFromResources(csvFile);
    }
}
