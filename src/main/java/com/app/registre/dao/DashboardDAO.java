package com.app.registre.dao;

import com.app.registre.model.DashboardStats;

import java.sql.*;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class DashboardDAO {

    /**
     * Récupère les statistiques du tableau de bord pour le mois et l'année en cours
     */
    public DashboardStats getDashboardStats() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        String month = toFrenchMonth(now);
        
        return getDashboardStats(year, month);
    }

    /**
     * Récupère les statistiques pour une année donnée
     */
    public DashboardStats getDashboardStats(int year, String selectedMonth) {
        DashboardStats stats = new DashboardStats();
        stats.setAnneeCourante(year);
        
        try (Connection conn = Database.getInstance().getConnection()) {
              Double soldeInitial = fetchSoldeInitial(conn);
              stats.setSoldeInitial(soldeInitial != null ? soldeInitial : 0.0);

            // Mois sélectionné ou mois courant si non renseigné
            String monthNum = null;
            if (selectedMonth != null && !selectedMonth.isBlank()) {
                monthNum = toMonthNumber(selectedMonth);
                stats.setMoisCourant(selectedMonth);
            } else {
                LocalDate now = LocalDate.now();
                monthNum = String.format("%02d", now.getMonthValue());
                stats.setMoisCourant(toFrenchMonth(now));
            }

            // Solde actuel : dernier solde de l'année (inchangé)
            String sqlLastSolde = "SELECT solde FROM operations WHERE strftime('%Y', datetime(date_emission/1000, 'unixepoch')) = ? " +
                    "ORDER BY date_emission DESC, id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sqlLastSolde)) {
                ps.setString(1, String.valueOf(year));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.setSoldeCourant(rs.getDouble("solde"));
                }
            }

            // Recettes et dépenses du mois sélectionné
            if (monthNum != null) {
                String sqlMois = "SELECT " +
                        "COALESCE(SUM(recette), 0) as total_recettes, " +
                        "COALESCE(SUM(depense), 0) as total_depenses, " +
                        "COALESCE(SUM(CASE WHEN UPPER(TRIM(budg)) = 'EXP' THEN depense END), 0) as total_depenses_exp, " +
                        "COALESCE(SUM(CASE WHEN UPPER(TRIM(budg)) = 'INV' THEN depense END), 0) as total_depenses_inv, " +
                        "COUNT(*) as nb_ops " +
                        "FROM operations WHERE " +
                        "imp IS NOT NULL AND imp != '' " +
                        "AND strftime('%m', datetime(date_emission/1000, 'unixepoch')) = ? " +
                        "AND strftime('%Y', datetime(date_emission/1000, 'unixepoch')) = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlMois)) {
                    ps.setString(1, monthNum);
                    ps.setString(2, String.valueOf(year));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        stats.setRecettesMois(rs.getDouble("total_recettes"));
                        stats.setDepensesMois(rs.getDouble("total_depenses"));
                        stats.setDepensesExpMois(rs.getDouble("total_depenses_exp"));
                        stats.setDepensesInvMois(rs.getDouble("total_depenses_inv"));
                        stats.setNombreOperationsMois(rs.getInt("nb_ops"));
                    }
                }
            }

            // Recettes et dépenses de l'année
            String sqlAnnee = "SELECT " +
                    "COALESCE(SUM(recette), 0) as total_recettes, " +
                    "COALESCE(SUM(depense), 0) as total_depenses, " +
                    "COALESCE(SUM(CASE WHEN UPPER(TRIM(budg)) = 'EXP' THEN depense END), 0) as total_depenses_exp, " +
                    "COALESCE(SUM(CASE WHEN UPPER(TRIM(budg)) = 'INV' THEN depense END), 0) as total_depenses_inv " +
                    "FROM operations WHERE strftime('%Y', datetime(date_emission/1000, 'unixepoch')) = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlAnnee)) {
                ps.setString(1, String.valueOf(year));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.setRecettesAnnee(rs.getDouble("total_recettes"));
                    stats.setDepensesAnnee(rs.getDouble("total_depenses"));
                    stats.setDepensesExpAnnee(rs.getDouble("total_depenses_exp"));
                    stats.setDepensesInvAnnee(rs.getDouble("total_depenses_inv"));
                    System.out.println("DEBUG: Année " + year + " - Recettes: " + rs.getDouble("total_recettes") + ", Dépenses: " + rs.getDouble("total_depenses"));
                }
            }

        } catch (SQLException e) {
            System.err.println("ERREUR SQL Dashboard: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("DEBUG: Stats finales - Solde: " + stats.getSoldeCourant() + ", Recettes année: " + stats.getRecettesAnnee());
        return stats;
    }

    /**
     * Retourne la date la plus récente (date_emission ou date_visa) trouvée dans les opérations.
     */
    public LocalDate getLatestOperationDate() {
        String sql = "SELECT COALESCE(date_emission, date_visa) AS d FROM operations " +
            "WHERE COALESCE(date_emission, date_visa) IS NOT NULL " +
            "ORDER BY COALESCE(date_emission, date_visa) DESC, id DESC LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                long millis = rs.getLong("d");
                if (!rs.wasNull()) {
                    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        } catch (SQLException e) {
            System.err.println("ERREUR SQL getLatestOperationDate: " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère l'évolution mensuelle du solde pour une année donnée
     * Retourne une Map : mois -> solde de fin
     */
    public Map<String, Double> getEvolutionSoldeAnnuelle(int year) {
        Map<String, Double> evolution = new LinkedHashMap<>();
        String[] moisFr = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                           "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};

        try (Connection conn = Database.getInstance().getConnection()) {
            // Pour chaque mois, récupérer le dernier solde
            for (int i = 0; i < moisFr.length; i++) {
                String mois = moisFr[i];
                String moisNum = String.format("%02d", i + 1); // 01, 02, 03, etc.
                
                String sql = "SELECT solde FROM operations " +
                        "WHERE strftime('%m', datetime(date_emission/1000, 'unixepoch')) = ? " +
                        "AND strftime('%Y', datetime(date_emission/1000, 'unixepoch')) = ? " +
                        "ORDER BY date_emission DESC, id DESC LIMIT 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, moisNum);
                    ps.setString(2, String.valueOf(year));
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        evolution.put(mois, rs.getDouble("solde"));
                    } else {
                        // Si pas d'opération ce mois, prendre le solde du mois précédent
                        Double soldePrecedent = getPreviousMonthSolde(evolution, mois);
                        evolution.put(mois, soldePrecedent != null ? soldePrecedent : 0.0);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return evolution;
    }

    /**
     * Récupère le solde du mois précédent dans l'évolution
     */
    private Double getPreviousMonthSolde(Map<String, Double> evolution, String moisCourant) {
        String[] moisFr = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                           "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        
        int index = Arrays.asList(moisFr).indexOf(moisCourant);
        if (index > 0) {
            for (int i = index - 1; i >= 0; i--) {
                Double solde = evolution.get(moisFr[i]);
                if (solde != null) {
                    return solde;
                }
            }
        }
        return null;
    }

    private Double fetchSoldeInitial(Connection conn) throws SQLException {
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa)) = 'integer' "
            + "THEN datetime(COALESCE(date_emission,date_visa)/1000, 'unixepoch', 'localtime') "
            + "ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT solde FROM operations WHERE LOWER(TRIM(designation)) LIKE 'solde initial%' "
            + "ORDER BY " + dateExpr + " ASC, id ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double v = rs.getDouble("solde");
                return rs.wasNull() ? null : v;
            }
        }
        return null;
    }

    /**
     * Convertit un LocalDate en mois français
     */
    private String toFrenchMonth(LocalDate date) {
        switch (date.getMonth()) {
            case JANUARY: return "JANVIER";
            case FEBRUARY: return "FEVRIER";
            case MARCH: return "MARS";
            case APRIL: return "AVRIL";
            case MAY: return "MAI";
            case JUNE: return "JUIN";
            case JULY: return "JUILLET";
            case AUGUST: return "AOUT";
            case SEPTEMBER: return "SEPTEMBRE";
            case OCTOBER: return "OCTOBRE";
            case NOVEMBER: return "NOVEMBRE";
            case DECEMBER: return "DECEMBRE";
            default: return "";
        }
    }

    private String toMonthNumber(String frenchMonth) {
        if (frenchMonth == null) return null;
        String m = frenchMonth.trim().toUpperCase();
        switch (m) {
            case "JANVIER": return "01";
            case "FEVRIER": return "02";
            case "MARS": return "03";
            case "AVRIL": return "04";
            case "MAI": return "05";
            case "JUIN": return "06";
            case "JUILLET": return "07";
            case "AOUT": return "08";
            case "SEPTEMBRE": return "09";
            case "OCTOBRE": return "10";
            case "NOVEMBRE": return "11";
            case "DECEMBRE": return "12";
            default: return null;
        }
    }
}
