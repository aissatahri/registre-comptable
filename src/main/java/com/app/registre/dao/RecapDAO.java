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
        String sql = "SELECT mois, SUM(montant) as total FROM operations GROUP BY mois ORDER BY mois";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                totaux.put(rs.getString("mois"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return totaux;
    }

    public Map<String, Double> getTotauxParNature() {
        Map<String, Double> totaux = new HashMap<>();
        String sql = "SELECT nature, SUM(montant) as total FROM operations GROUP BY nature ORDER BY nature";

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

    public double getTotalRecettes() {
        String sql = "SELECT SUM(montant) as total FROM operations WHERE nature IN ('SUBVENTION', 'RECETTE')";

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

    public double getTotalDepenses() {
        String sql = "SELECT SUM(montant) as total FROM operations WHERE nature NOT IN ('SUBVENTION', 'RECETTE')";

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
}