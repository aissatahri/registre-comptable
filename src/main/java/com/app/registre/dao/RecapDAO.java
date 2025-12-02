package com.app.registre.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecapDAO {
    private static final Logger log = LoggerFactory.getLogger(RecapDAO.class);

    public Map<String, Double> getTotauxParMois() {
        Map<String, Double> totaux = new HashMap<>();
        // If explicit textual 'mois' values exist in the table, prefer grouping by that column
        String countMoisSql = "SELECT COUNT(*) as c FROM operations WHERE imp IS NOT NULL AND imp != '' AND mois IS NOT NULL AND TRIM(mois) <> ''";
        try (Connection conn = Database.getInstance().getConnection()) {

            // Log total rows for diagnostics
            try (Statement stmtCount = conn.createStatement(); ResultSet totalRs = stmtCount.executeQuery("SELECT COUNT(*) as c FROM operations WHERE imp IS NOT NULL AND imp != ''")) {
                if (totalRs.next()) log.info("Total operations rows in DB: {}", totalRs.getInt("c"));
            } catch (SQLException ex) {
                log.warn("Unable to query operations count: {}", ex.getMessage());
            }

            // Dump all rows for debugging using a dedicated statement (avoid multiple open ResultSets on same Statement)
            try (Statement stmtDump = conn.createStatement(); ResultSet all = stmtDump.executeQuery("SELECT id, imp, solde, depense, recette FROM operations ORDER BY id")) {
                while (all.next()) {
                    log.info("ROW id={} imp={} solde={} depense={} recette={}", all.getInt("id"), all.getString("imp"), all.getObject("solde"), all.getObject("depense"), all.getObject("recette"));
                }
            } catch (SQLException ex) {
                log.warn("Unable to dump operations rows: {}", ex.getMessage());
            }

            int c = 0;
            try (Statement stmt = conn.createStatement(); ResultSet cnt = stmt.executeQuery(countMoisSql)) {
                if (cnt.next()) c = cnt.getInt("c");
            }

            if (c > 0) {
                // Group by textual mois column (e.g. 'JANVIER'). Use Java aggregation to avoid SQLite strangeness on grouping.
                String sql = "SELECT COALESCE(solde, 0) as val FROM operations";
                try (Statement stmt2 = conn.createStatement(); ResultSet rs = stmt2.executeQuery(sql)) {
                    while (rs.next()) {
                        String m = rs.getString("mois");
                        double v = rs.getDouble("val");
                        log.info("Row read for mois='{}' val={}", m, v);
                        totaux.put(m, totaux.getOrDefault(m, 0.0) + v);
                    }
                }
                log.info("Computed months totals (from textual mois): {}", totaux);
                return totaux;
            }

            // Fallback: derive month from date_emission or date_visa, normalizing integer epoch-ms to localtime
            String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa))='integer' THEN datetime(COALESCE(date_emission,date_visa)/1000,'unixepoch','localtime') ELSE COALESCE(date_emission,date_visa) END";
            String sql = "SELECT strftime('%m', " + dateExpr + ") as mois_num, SUM(solde) as total FROM operations GROUP BY mois_num ORDER BY mois_num";
            try (Statement stmt3 = conn.createStatement(); ResultSet rs = stmt3.executeQuery(sql)) {
                while (rs.next()) {
                    String moisNum = rs.getString("mois_num");
                    String moisName = monthNameFromNumber(moisNum);
                    totaux.put(moisName, rs.getDouble("total"));
                }
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
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa))='integer' THEN datetime(COALESCE(date_emission,date_visa)/1000,'unixepoch','localtime') ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT strftime('%m', " + dateExpr + ") as mois_num, SUM(recette) as total FROM operations GROUP BY mois_num ORDER BY mois_num";

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

    public Map<Integer, Double> getRecetteParMoisForYear(int year) {
        Map<Integer, Double> totaux = new HashMap<>();
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa))='integer' THEN datetime(COALESCE(date_emission,date_visa)/1000,'unixepoch','localtime') ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT strftime('%m', " + dateExpr + ") as mois_num, SUM(recette) as total FROM operations WHERE strftime('%Y', " + dateExpr + ") = ? GROUP BY mois_num ORDER BY mois_num";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.replace("?", String.format("'%04d'", year)))) {

            while (rs.next()) {
                int mois = Integer.parseInt(rs.getString("mois_num"));
                totaux.put(mois, rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totaux;
    }

    public Map<Integer, Double> getDepenseParMoisForYear(int year) {
        Map<Integer, Double> totaux = new HashMap<>();
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa))='integer' THEN datetime(COALESCE(date_emission,date_visa)/1000,'unixepoch','localtime') ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT strftime('%m', " + dateExpr + ") as mois_num, SUM(COALESCE(depense, COALESCE(sur_ram,0) + COALESCE(sur_eng,0))) as total FROM operations WHERE strftime('%Y', " + dateExpr + ") = ? GROUP BY mois_num ORDER BY mois_num";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.replace("?", String.format("'%04d'", year)))) {

            while (rs.next()) {
                int mois = Integer.parseInt(rs.getString("mois_num"));
                totaux.put(mois, rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totaux;
    }

    public Double getLastSoldeBeforeYear(int year) {
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa))='integer' THEN datetime(COALESCE(date_emission,date_visa)/1000,'unixepoch','localtime') ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT solde, " + dateExpr + " as d FROM operations WHERE strftime('%Y', " + dateExpr + ") < ? ORDER BY " + dateExpr + " DESC LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.replace("?", String.format("'%04d'", year)))) {
            if (rs.next()) return rs.getDouble("solde");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public Map<String, Double> getDepenseParMois() {
        Map<String, Double> totaux = new HashMap<>();
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa))='integer' THEN datetime(COALESCE(date_emission,date_visa)/1000,'unixepoch','localtime') ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT strftime('%m', " + dateExpr + ") as mois_num, SUM(COALESCE(depense, COALESCE(sur_ram,0) + COALESCE(sur_eng,0))) as total FROM operations GROUP BY mois_num ORDER BY mois_num";

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
        // Sum recettes: use recette column, fallback to solde when recette is NULL
        String sql = "SELECT SUM(COALESCE(recette, solde)) as total FROM operations WHERE nature IN ('SUBVENTION', 'RECETTE')";

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
        // Sum depense: prefer stored 'depense', fallback to montant for expense natures, or sur_ram+sur_eng when depense is NULL
        // Only consider operations that are not recettes/subventions
        String sql = "SELECT SUM(COALESCE(depense, COALESCE(montant, COALESCE(sur_ram,0) + COALESCE(sur_eng,0)))) as total FROM operations WHERE COALESCE(nature,'') NOT IN ('SUBVENTION','RECETTE')";

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
        String sql = "SELECT COUNT(*) as total FROM operations WHERE imp IS NOT NULL AND imp != ''";

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
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa))='integer' THEN datetime(COALESCE(date_emission,date_visa)/1000,'unixepoch','localtime') ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT solde FROM operations ORDER BY " + dateExpr + " DESC LIMIT 1";
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