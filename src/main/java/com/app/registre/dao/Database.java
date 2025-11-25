package com.app.registre.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
                op TEXT,
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
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
