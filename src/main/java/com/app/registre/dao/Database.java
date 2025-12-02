package com.app.registre.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
    private static final Logger log = LoggerFactory.getLogger(Database.class);
    // Default to a file under %LOCALAPPDATA%/RegistreComptable/registre.db (or user.home if LOCALAPPDATA missing)
    private static final String DEFAULT_URL = computeDefaultUrl();
    private static Database instance;
    private Connection connection;
    // Allow programmatic override before instance creation
    private static String dbUrl = System.getProperty("db.url", DEFAULT_URL);

    private static String computeDefaultUrl() {
        String localApp = System.getenv("LOCALAPPDATA");
        if (localApp == null || localApp.isBlank()) localApp = System.getProperty("user.home");
        java.nio.file.Path appDir = java.nio.file.Paths.get(localApp, "RegistreComptable");
        try {
            java.nio.file.Files.createDirectories(appDir);
        } catch (Exception ignored) {
        }
        java.nio.file.Path db = appDir.resolve("registre.db");
        return "jdbc:sqlite:" + db.toAbsolutePath().toString();
    }

    private Database() {
        try {
            // If a system property 'db.url' is present at instantiation time, prefer it (tests set it after reset)
            String prop = System.getProperty("db.url");
            if (prop != null && !prop.isBlank()) {
                dbUrl = prop;
            }
            log.debug("Opening database connection to: {}", dbUrl);
            connection = DriverManager.getConnection(dbUrl);
            createTables();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to open DB connection to " + dbUrl + ": " + e.getMessage(), e);
        }
    }

    /**
     * Programmatically set the JDBC URL used by the Database singleton.
     * Must be called before Database.getInstance() to take effect.
     */
    public static synchronized void setDbUrl(String url) {
        if (url == null || url.isBlank()) return;
        // sanitize input: trim and remove surrounding quotes
        String cleaned = url.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        // Remove any control characters (BOM or stray non-printables)
        cleaned = cleaned.replaceAll("\\p{C}", "");

        // If a full JDBC URL was provided (possibly repeated), strip all leading jdbc:sqlite: prefixes
        while (cleaned.startsWith("jdbc:sqlite:")) {
            cleaned = cleaned.substring("jdbc:sqlite:".length());
        }

        if (cleaned.isBlank()) return;

        // If the cleaned value is not an absolute path, resolve it under %LOCALAPPDATA%/RegistreComptable
        java.nio.file.Path candidate = java.nio.file.Paths.get(cleaned);
        if (!candidate.isAbsolute()) {
            String localApp = System.getenv("LOCALAPPDATA");
            if (localApp == null || localApp.isBlank()) localApp = System.getProperty("user.home");
            java.nio.file.Path appDir = java.nio.file.Paths.get(localApp, "RegistreComptable");
            try { java.nio.file.Files.createDirectories(appDir); } catch (Exception ignored) {}
            candidate = appDir.resolve(cleaned);
        }

        // Now construct a proper JDBC URL
        String finalUrl = "jdbc:sqlite:" + candidate.toAbsolutePath().toString();

        // If an instance exists, reset it to allow switching DB at runtime
        if (instance != null) {
            try {
                instance.getConnection().close();
            } catch (Exception ignored) {}
            instance = null;
        }
        dbUrl = finalUrl;
        log.info("Database URL set to: {}", dbUrl);
    }

    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(dbUrl);
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to open DB connection to " + dbUrl + ": " + e.getMessage(), e);
        }
    }

    public static void reset() {
        if (instance != null) {
            try {
                if (instance.connection != null && !instance.connection.isClosed()) {
                    instance.connection.close();
                }
            } catch (SQLException ignored) {}
            instance = null;
        }
        // Re-read system property in case tests or runtime changed it.
        // Only override the current dbUrl if a specific system property was provided.
        String prop = System.getProperty("db.url");
        if (prop != null && !prop.isBlank()) {
            dbUrl = prop;
        }
    }

    private void createTables() {
        // Create operations table
        String createOperationsTable = """
            CREATE TABLE IF NOT EXISTS operations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                imp TEXT,
                designation TEXT,
                nature TEXT,
                n TEXT,
                budg TEXT,
                exercice TEXT,
                beneficiaire TEXT,
                date_emission DATE,
                date_visa DATE,
                op_or INTEGER,
                ov_cheq_type TEXT,
                ov_cheq INTEGER,
                recette REAL,
                sur_ram REAL,
                sur_eng REAL,
                depense REAL,
                solde REAL
            )
            """;

        // Create users table
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                display_name TEXT,
                email TEXT
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createOperationsTable);
            stmt.execute(createUsersTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
