package com.app.registre.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class RecapDAO {

    public Map<String, Double> getTotauxParMois() {
        Map<String, Double> totaux = new HashMap<>();
        // Derive month from date_emission or date_visa (SQLite strftime('%m',...))
        String sql = "SELECT COALESCE(strftime('%m', date_emission), strftime('%m', date_visa)) as mois_num, SUM(solde) as total FROM operations GROUP BY mois_num ORDER BY mois_num";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String moisNum = rs.getString("mois_num");
                String moisName = monthNameFromNumber(moisNum);
                totaux.put(moisName, rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totaux;
    }

    public Map<String, Double> getTotauxParNature() {
        Map<String, Double> totaux = new HashMap<>();
        String sql = "SELECT nature, SUM(solde) as total FROM operations GROUP BY nature ORDER BY nature";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                totaux.put(rs.getString("nature"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totaux;
    }

    public Map<String, Double> getRecetteParMois() {
        Map<String, Double> totaux = new HashMap<>();
        String sql = "SELECT COALESCE(strftime('%m', date_emission), strftime('%m', date_visa)) as mois_num, SUM(recette) as total FROM operations GROUP BY mois_num ORDER BY mois_num";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String moisNum = rs.getString("mois_num");
                String moisName = monthNameFromNumber(moisNum);
                totaux.put(moisName, rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totaux;
    }

    public Map<String, Double> getDepenseParMois() {
        Map<String, Double> totaux = new HashMap<>();
        String sql = "SELECT COALESCE(strftime('%m', date_emission), strftime('%m', date_visa)) as mois_num, SUM(COALESCE(depense, COALESCE(sur_ram,0) + COALESCE(sur_eng,0))) as total FROM operations GROUP BY mois_num ORDER BY mois_num";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String moisNum = rs.getString("mois_num");
                String moisName = monthNameFromNumber(moisNum);
                totaux.put(moisName, rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totaux;
    }

    public double getTotalRecettes() {
        // Sum the 'recette' column (not 'solde') for operations that are recettes/subventions
        String sql = "SELECT SUM(recette) as total FROM operations WHERE nature IN ('SUBVENTION', 'RECETTE')";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Retourne la somme totale de sur_ram + sur_eng pour toutes les opérations.
     */
    public double getTotalSur() {
        String sql = "SELECT SUM(COALESCE(sur_ram,0) + COALESCE(sur_eng,0)) as total FROM operations";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public double getTotalSurRam() {
        String sql = "SELECT SUM(COALESCE(sur_ram,0)) as total FROM operations";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public double getTotalSurEng() {
        String sql = "SELECT SUM(COALESCE(sur_eng,0)) as total FROM operations";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public double getTotalDepenses() {
        // Sum depense: prefer stored 'depense' but fallback to (sur_ram + sur_eng) when depense is NULL
        // Sum depense across all operations (use sur_ram+sur_eng when depense is NULL)
        String sql = "SELECT SUM(COALESCE(depense, COALESCE(sur_ram,0) + COALESCE(sur_eng,0))) as total FROM operations";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public int getTotalOperations() {
        String sql = "SELECT COUNT(*) as total FROM operations";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Map<String, Integer> getStatsDecisions() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT decision, COUNT(*) as count FROM operations GROUP BY decision";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stats.put(rs.getString("decision"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }

    /**
     * Retourne le dernier solde enregistré (solde de la dernière opération selon date_emission/date_visa).
     */
    public double getDernierSolde() {
        String sql = "SELECT solde FROM operations ORDER BY COALESCE(date_emission, date_visa) DESC LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("solde");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private String monthNameFromNumber(String moisNum) {
        if (moisNum == null) return "INCONNU";
        try {
            int m = Integer.parseInt(moisNum);
            String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN", "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
            if (m >= 1 && m <= 12) return months[m - 1];
        } catch (NumberFormatException ignored) {}
        return "INCONNU";
    }
}