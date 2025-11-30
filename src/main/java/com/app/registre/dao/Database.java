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
        // Create operations table with the cleaned schema (only the requested columns)
        String createOperationsTable = """
            CREATE TABLE IF NOT EXISTS operations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                op TEXT,
                imp TEXT,
                designation TEXT,
                nature TEXT,
                n TEXT,
                budg TEXT,
                exercice TEXT,
                beneficiaire TEXT,
                date_emission DATE,
                date_entree DATE,
                date_visa DATE,
                date_rejet DATE,
                decision TEXT,
                motif_rejet TEXT,
                date_reponse DATE,
                contenu_reponse TEXT,
                mois TEXT,
                op_or INTEGER,
                ov_cheq_type TEXT,
                ov_cheq INTEGER,
                recette REAL,
                sur_ram REAL,
                sur_eng REAL,
                depense REAL,
                solde REAL,
                montant REAL
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createOperationsTable);
            // Ensure users table exists for authentication
            ensureUsersTable();
            // Run migrations to normalize older schemas into the cleaned form
            // NOTE: do not remove legacy `op` column here — keep it for compatibility
            // with older data and the DAO which may still reference it.
            migrateToFormSchema();
            // Ensure any missing columns are added (safe no-op if schema already matches)
            ensureOperationNewColumns();
            // Backfill depense for existing rows where depense is NULL
            backfillDepenseIfNull();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update existing rows where `depense` is NULL by setting
     * depense = COALESCE(depense, COALESCE(sur_ram,0) + COALESCE(sur_eng,0)).
     * This ensures depense is populated for older records.
     */
    private void backfillDepenseIfNull() {
        String sql = "UPDATE operations SET depense = COALESCE(sur_ram,0) + COALESCE(sur_eng,0) WHERE depense IS NULL";
        try (Statement stmt = connection.createStatement()) {
            int updated = stmt.executeUpdate(sql);
            if (updated > 0) System.out.println("Backfilled depense for " + updated + " rows");
        } catch (SQLException e) {
            System.err.println("Warning: unable to backfill depense column: " + e.getMessage());
        }
    }

    /**
     * Recreate the `operations` table so that it contains only the fields
     * that correspond to the current operation form. Copies existing data
     * for matching columns and leaves other columns NULL.
     */
    private void migrateToFormSchema() {
        try (Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info('operations')")) {

            java.util.List<String> existing = new java.util.ArrayList<>();
            while (rs.next()) existing.add(rs.getString("name"));

            // Diagnostic: log existing columns to help debug unexpected migrations/duplicates
            log.info("Existing operations columns before migration: {}", existing);

                // Desired schema: include current form fields AND preserve legacy columns
                // such as `op`, `montant`, `date_entree`, `decision`, `mois` to
                // maintain compatibility with older imports/tests that expect them.
                String[] desired = new String[]{
                    "id",
                    "op",
                    "imp",
                    "designation",
                    "nature",
                    "n",
                    "budg",
                    "exercice",
                    "beneficiaire",
                    "date_emission",
                    "date_entree",
                    "date_visa",
                    "date_rejet",
                    "decision",
                    "motif_rejet",
                    "date_reponse",
                    "contenu_reponse",
                    "mois",
                    "op_or",
                    "ov_cheq_type",
                    "ov_cheq",
                    "recette",
                    "sur_ram",
                    "sur_eng",
                    "depense",
                    "montant",
                    "solde"
                };

            java.util.Set<String> existSet = new java.util.HashSet<>(existing);

            // If the table already has exactly these columns (ignoring order of columns), skip
            boolean allPresent = true;
            for (String d : desired) if (!existSet.contains(d)) { allPresent = false; break; }
            if (allPresent && existSet.size() == desired.length) {
                log.info("No migration needed for operations table (columns match desired schema)");
                return;
            }
            log.info("Migration will run: desired size={}, existing size={}, allPresent={}", desired.length, existSet.size(), allPresent);

            // Backup the DB file before destructive migration
            try {
                String url = dbUrl; // e.g. jdbc:sqlite:registre.db
                if (url != null && url.startsWith("jdbc:sqlite:")) {
                    String dbPath = url.substring("jdbc:sqlite:".length());
                    Path src = Paths.get(dbPath);
                    if (Files.exists(src)) {
                        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                        Path dst = Paths.get(dbPath + ".bak." + ts);
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (Exception ex) {
                // backup failure should not block migration, just log to stderr
                System.err.println("Warning: unable to backup DB before migration: " + ex.getMessage());
            }

            connection.setAutoCommit(false);

            String createNew = """
                CREATE TABLE operations_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    op TEXT,
                    imp TEXT,
                    designation TEXT,
                    nature TEXT,
                    n TEXT,
                    budg TEXT,
                    exercice TEXT,
                    beneficiaire TEXT,
                    date_emission DATE,
                    date_entree DATE,
                    date_visa DATE,
                    date_rejet DATE,
                    decision TEXT,
                    motif_rejet TEXT,
                    date_reponse DATE,
                    contenu_reponse TEXT,
                    mois TEXT,
                    op_or INTEGER,
                    ov_cheq_type TEXT,
                    ov_cheq INTEGER,
                    recette REAL,
                    sur_ram REAL,
                    sur_eng REAL,
                    depense REAL,
                    montant REAL,
                    solde REAL
                )
                """;

            try (Statement s = connection.createStatement()) {
                s.execute(createNew);

                // Build INSERT SELECT mapping; for solde, map from existing 'montant' if present
                StringBuilder insertCols = new StringBuilder();
                StringBuilder selectCols = new StringBuilder();
                for (int i = 0; i < desired.length; i++) {
                    String col = desired[i];
                    if (i > 0) { insertCols.append(", "); selectCols.append(", "); }
                    insertCols.append(col);
                    if (col.equals("solde")) {
                        if (existSet.contains("montant")) selectCols.append("montant AS solde"); else selectCols.append("NULL AS solde");
                    } else if (existSet.contains(col)) {
                        selectCols.append(col);
                    } else {
                        selectCols.append("NULL AS ").append(col);
                    }
                }

                String copy = "INSERT INTO operations_new(" + insertCols.toString() + ") SELECT " + selectCols.toString() + " FROM operations";
                s.executeUpdate(copy);

                s.executeUpdate("DROP TABLE operations");
                s.executeUpdate("ALTER TABLE operations_new RENAME TO operations");
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignore) {}
            e.printStackTrace();
        }
    }

    /**
     * Si la table `operations` contient encore la colonne `op`, on recrée la table
     * sans cette colonne et on copie les données pour effectuer la migration.
     */
    private void migrateRemoveOpColumn() {
        try (Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info('operations')")) {

            java.util.List<String> cols = new java.util.ArrayList<>();
            while (rs.next()) cols.add(rs.getString("name"));

            if (!cols.contains("op")) return; // déjà migré

            connection.setAutoCommit(false);

            String createNew = """
                CREATE TABLE operations_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ov_cheq TEXT,
                    imp TEXT,
                    nature TEXT,
                    budg TEXT,
                    montant REAL,
                    date_entree DATE,
                    date_visa DATE,
                    date_rejet DATE,
                    decision TEXT,
                    motif_rejet TEXT,
                    date_reponse DATE,
                    contenu_reponse TEXT,
                    mois TEXT
                )
                """;

            try (Statement s = connection.createStatement()) {
                s.execute(createNew);

                // Copier les colonnes existantes (sans op)
                String copy = "INSERT INTO operations_new(id, ov_cheq, imp, nature, budg, montant, date_entree, date_visa, date_rejet, decision, motif_rejet, date_reponse, contenu_reponse, mois) "
                        + "SELECT id, ov_cheq, imp, nature, budg, montant, date_entree, date_visa, date_rejet, decision, motif_rejet, date_reponse, contenu_reponse, mois FROM operations";
                s.executeUpdate(copy);

                s.executeUpdate("DROP TABLE operations");
                s.executeUpdate("ALTER TABLE operations_new RENAME TO operations");
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignore) {}
            e.printStackTrace();
        }
    }

    private void ensureOperationNewColumns() {
        String[] alters = new String[]{
                "ALTER TABLE operations ADD COLUMN designation TEXT",
            "ALTER TABLE operations ADD COLUMN n TEXT",
                "ALTER TABLE operations ADD COLUMN exercice TEXT",
                "ALTER TABLE operations ADD COLUMN beneficiaire TEXT",
                "ALTER TABLE operations ADD COLUMN date_emission DATE",
                "ALTER TABLE operations ADD COLUMN op_or INTEGER",
                "ALTER TABLE operations ADD COLUMN recette REAL",
                "ALTER TABLE operations ADD COLUMN sur_ram REAL",
                "ALTER TABLE operations ADD COLUMN sur_eng REAL",
                "ALTER TABLE operations ADD COLUMN depense REAL"
        };

        try (java.sql.Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info('operations')")) {
            java.util.Set<String> cols = new java.util.HashSet<>();
            while (rs.next()) cols.add(rs.getString("name"));
            for (String sql : alters) {
                String col = sql.substring(sql.indexOf("ADD COLUMN") + 10).trim().split(" ")[0];
                if (!cols.contains(col)) {
                    try (java.sql.Statement s2 = connection.createStatement()) { s2.execute(sql); }
                    cols.add(col);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a simple users table for authentication if it does not exist.
     * Columns: id, username (unique), password_hash
     */
    private void ensureUsersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                display_name TEXT,
                email TEXT
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            ensureUsersNewColumns();
        } catch (SQLException e) {
            System.err.println("Warning: unable to create users table: " + e.getMessage());
        }
    }

    private void ensureUsersNewColumns() {
        String[] alters = new String[]{
                "ALTER TABLE users ADD COLUMN display_name TEXT",
                "ALTER TABLE users ADD COLUMN email TEXT"
        };
        try (Statement stmt = connection.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("PRAGMA table_info('users')")) {
            java.util.Set<String> cols = new java.util.HashSet<>();
            while (rs.next()) cols.add(rs.getString("name"));
            for (String sql : alters) {
                String col = sql.substring(sql.indexOf("ADD COLUMN") + 10).trim().split(" ")[0];
                if (!cols.contains(col)) {
                    try (Statement s2 = connection.createStatement()) { s2.execute(sql); }
                    cols.add(col);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
