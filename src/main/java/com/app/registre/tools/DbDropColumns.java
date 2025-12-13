package com.app.registre.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbDropColumns {
    public static void main(String[] args) {
        String dbFile = "registre_export_2025-12-12.db";
        if (args.length > 0) dbFile = args[0];
        Path dbPath = Path.of(dbFile);
        if (!Files.exists(dbPath)) {
            System.err.println("Fichier introuvable: " + dbPath.toAbsolutePath());
            System.exit(2);
        }

        try {
            // Backup
            Path bak = dbPath.resolveSibling(dbPath.getFileName().toString() + ".bak");
            Files.copy(dbPath, bak, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Sauvegarde créée: " + bak.toAbsolutePath());

            String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();
            try (Connection conn = DriverManager.getConnection(url)) {
                conn.setAutoCommit(false);
                try (Statement st = conn.createStatement()) {
                    // Create new table without the unwanted columns
                    String create = "CREATE TABLE IF NOT EXISTS operations_new ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "art INTEGER,"
                            + "par INTEGER,"
                            + "lig INTEGER,"
                            + "imp TEXT,"
                            + "designation TEXT,"
                            + "nature TEXT,"
                            + "n TEXT,"
                            + "budg TEXT,"
                            + "exercice TEXT,"
                            + "beneficiaire TEXT,"
                            + "date_emission DATE,"
                            + "op_or INTEGER,"
                            + "ov_cheq INTEGER,"
                            + "recette REAL,"
                            + "sur_ram REAL,"
                            + "sur_eng REAL,"
                            + "depense REAL,"
                            + "solde REAL"
                            + ")";
                    st.execute(create);

                    // Copy allowed columns from old table -> new table
                    String copy = "INSERT INTO operations_new (id, art, par, lig, imp, designation, nature, n, budg, exercice, beneficiaire, date_emission, op_or, ov_cheq, recette, sur_ram, sur_eng, depense, solde) "
                            + "SELECT id, art, par, lig, imp, designation, nature, n, budg, exercice, beneficiaire, date_emission, op_or, ov_cheq, recette, sur_ram, sur_eng, depense, solde FROM operations";
                    st.executeUpdate(copy);

                    // Drop old table and rename
                    st.execute("DROP TABLE operations");
                    st.execute("ALTER TABLE operations_new RENAME TO operations");

                    conn.commit();
                    System.out.println("Migration terminée: colonnes supprimées et données copiées.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Migration échouée: " + e.getMessage());
            System.exit(3);
        }
    }
}
