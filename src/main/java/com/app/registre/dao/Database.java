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

public class Database {
    private static final String DEFAULT_URL = "jdbc:sqlite:registre.db";
    private static Database instance;
    private Connection connection;
    private final String dbUrl = System.getProperty("db.url", DEFAULT_URL);

    private Database() {
        try {
            connection = DriverManager.getConnection(dbUrl);
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
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
    }

    private void createTables() {
        String createOperationsTable = """
            CREATE TABLE IF NOT EXISTS operations (
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

        String createPaiementsTable = """
            CREATE TABLE IF NOT EXISTS paiements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                annee TEXT,
                type TEXT,
                montant REAL,
                categorie TEXT
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createOperationsTable);
            stmt.execute(createPaiementsTable);
            ensureOperationNewColumns();
            migrateRemoveOpColumn();
            migrateToFormSchema();
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

            // Desired schema exactly as requested by the user
            String[] desired = new String[]{
                    "id",
                    "imp",
                    "designation",
                    "nature",
                    "n",
                    "budg",
                    "exercice",
                    "beneficiaire",
                    "date_emission",
                    "date_visa",
                    "op_or",
                    "ov_cheq_type",
                    "ov_cheq",
                    "recette",
                    "sur_ram",
                    "sur_eng",
                    "depense",
                    "solde"
            };

            java.util.Set<String> existSet = new java.util.HashSet<>(existing);

            // If the table already has exactly these columns (ignoring order of columns), skip
            boolean allPresent = true;
            for (String d : desired) if (!existSet.contains(d)) { allPresent = false; break; }
            if (allPresent && existSet.size() == desired.length) return;

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
}
