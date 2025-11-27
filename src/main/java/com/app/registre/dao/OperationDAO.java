package com.app.registre.dao;

import com.app.registre.model.Operation;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationDAO {
    private static final Logger log = LoggerFactory.getLogger(OperationDAO.class);

    public void insert(Operation operation) {
        String sql = """
            INSERT INTO operations(imp, designation, nature, n, budg, exercice, beneficiaire,
                                   date_emission, date_visa, op_or, ov_cheq_type, ov_cheq, recette, sur_ram, sur_eng, depense, solde)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Log key fields to help troubleshooting insert issues
                log.debug("Attempting insert operation: imp='{}', designation='{}', recette={}, surRam={}, surEng={}, depense={}, solde={}",
                    operation.getImp(), operation.getDesignation(), operation.getRecette(), operation.getSurRam(), operation.getSurEng(), operation.getDepense(), operation.getSolde());

                // Parameters correspond to the VALUES(...) above
                int idx = 1;
                pstmt.setString(idx++, operation.getImp());
                pstmt.setString(idx++, operation.getDesignation());
                pstmt.setString(idx++, operation.getNature());
                if (operation.getN() != null) pstmt.setString(idx++, operation.getN()); else { pstmt.setNull(idx++, java.sql.Types.VARCHAR); }
                pstmt.setString(idx++, operation.getBudg());
                pstmt.setString(idx++, operation.getExercice());
                pstmt.setString(idx++, operation.getBeneficiaire());
                pstmt.setDate(idx++, operation.getDateEmission() != null ? Date.valueOf(operation.getDateEmission()) : null);
                pstmt.setDate(idx++, operation.getDateVisa() != null ? Date.valueOf(operation.getDateVisa()) : null);
                if (operation.getOpOr() != null) pstmt.setInt(idx++, operation.getOpOr()); else { pstmt.setNull(idx++, java.sql.Types.INTEGER); }
                // ov_cheq_type (TEXT)
                pstmt.setString(idx++, operation.getOvCheqType());
                // ov_cheq (INTEGER)
                if (operation.getOvCheq() != null) pstmt.setInt(idx++, operation.getOvCheq()); else { pstmt.setNull(idx++, java.sql.Types.INTEGER); }
                if (operation.getRecette() != null) pstmt.setDouble(idx++, operation.getRecette()); else { pstmt.setNull(idx++, java.sql.Types.REAL); }
                if (operation.getSurRam() != null) pstmt.setDouble(idx++, operation.getSurRam()); else { pstmt.setNull(idx++, java.sql.Types.REAL); }
                if (operation.getSurEng() != null) pstmt.setDouble(idx++, operation.getSurEng()); else { pstmt.setNull(idx++, java.sql.Types.REAL); }
                if (operation.getDepense() != null) pstmt.setDouble(idx++, operation.getDepense()); else { pstmt.setNull(idx++, java.sql.Types.REAL); }
                if (operation.getSolde() != null) pstmt.setDouble(idx++, operation.getSolde()); else { pstmt.setNull(idx++, java.sql.Types.REAL); }

            int rows = pstmt.executeUpdate();
            log.info("Insert completed, rows affected={}", rows);
        } catch (SQLException e) {
            log.error("Error inserting operation (imp='{}', designation='{}'): {}", operation != null ? operation.getImp() : "-", operation != null ? operation.getDesignation() : "-", e.toString(), e);
        }
    }

    public void update(Operation operation) {
            String sql = """
            UPDATE operations SET
            imp = ?, designation = ?, nature = ?, n = ?, budg = ?, exercice = ?, beneficiaire = ?,
            date_emission = ?, date_visa = ?, op_or = ?, ov_cheq_type = ?, ov_cheq = ?, recette = ?, sur_ram = ?, sur_eng = ?, depense = ?, solde = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int idx2 = 1;
            pstmt.setString(idx2++, operation.getImp());
            pstmt.setString(idx2++, operation.getDesignation());
            pstmt.setString(idx2++, operation.getNature());
            if (operation.getN() != null) pstmt.setString(idx2++, operation.getN()); else { pstmt.setNull(idx2++, java.sql.Types.VARCHAR); }
            pstmt.setString(idx2++, operation.getBudg());
            pstmt.setString(idx2++, operation.getExercice());
            pstmt.setString(idx2++, operation.getBeneficiaire());
            pstmt.setDate(idx2++, operation.getDateEmission() != null ? Date.valueOf(operation.getDateEmission()) : null);
            pstmt.setDate(idx2++, operation.getDateVisa() != null ? Date.valueOf(operation.getDateVisa()) : null);
            if (operation.getOpOr() != null) pstmt.setInt(idx2++, operation.getOpOr()); else { pstmt.setNull(idx2++, java.sql.Types.INTEGER); }
            // ov_cheq_type
            pstmt.setString(idx2++, operation.getOvCheqType());
            // ov_cheq numeric
            if (operation.getOvCheq() != null) pstmt.setInt(idx2++, operation.getOvCheq()); else { pstmt.setNull(idx2++, java.sql.Types.INTEGER); }
            if (operation.getRecette() != null) pstmt.setDouble(idx2++, operation.getRecette()); else { pstmt.setNull(idx2++, java.sql.Types.REAL); }
            if (operation.getSurRam() != null) pstmt.setDouble(idx2++, operation.getSurRam()); else { pstmt.setNull(idx2++, java.sql.Types.REAL); }
            if (operation.getSurEng() != null) pstmt.setDouble(idx2++, operation.getSurEng()); else { pstmt.setNull(idx2++, java.sql.Types.REAL); }
            if (operation.getDepense() != null) pstmt.setDouble(idx2++, operation.getDepense()); else { pstmt.setNull(idx2++, java.sql.Types.REAL); }
            if (operation.getSolde() != null) pstmt.setDouble(idx2++, operation.getSolde()); else { pstmt.setNull(idx2++, java.sql.Types.REAL); }
            pstmt.setInt(idx2++, operation.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM operations WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        String sql = "DELETE FROM operations";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Operation> findAll() {
        List<Operation> operations = new ArrayList<>();
        // Order ASC so the latest inserted operation appears at the bottom of the table
        String sql = "SELECT * FROM operations ORDER BY COALESCE(date_emission, date_visa) ASC";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                operations.add(mapResultSetToOperation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return operations;
    }

    public List<Operation> findByMois(String mois) {
        List<Operation> operations = new ArrayList<>();
        // The DB no longer contains a dedicated 'mois' column; return all operations
        // and let the caller filter by month if needed.
        String sql = "SELECT * FROM operations ORDER BY COALESCE(date_emission, date_visa) ASC";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                operations.add(mapResultSetToOperation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return operations;
    }

    public int countOperations() {
        String sql = "SELECT COUNT(*) as c FROM operations";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("c");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public double getTotalMontantByMois(String mois) {
        // No 'mois' column available in DB; return total solde across all operations
        String sql = "SELECT SUM(solde) as total FROM operations";
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
     * Retourne le dernier montant (solde) d'une opération antérieure à la date fournie.
     * Si aucune opération antérieure, retourne 0.0
     */
    public Double getLastMontantBeforeDate(java.time.LocalDate date) {
        String sql = "SELECT solde FROM operations WHERE COALESCE(date_emission, date_visa) < ? ORDER BY COALESCE(date_emission, date_visa) DESC LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double v = rs.getDouble("solde");
                    return rs.wasNull() ? null : v;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retourne vrai s'il existe au moins une opération strictement antérieure à la date fournie.
     */
    public boolean hasOperationBeforeDate(java.time.LocalDate date) {
        String sql = "SELECT 1 FROM operations WHERE COALESCE(date_emission, date_visa) < ? LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retourne le dernier solde enregistré (la plus récente opération selon date_emission/date_visa).
     * Si aucune opération, retourne 0.0
     */
    public Double getLastMontant() {
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa)) = 'integer' "
                + "THEN datetime(COALESCE(date_emission,date_visa)/1000, 'unixepoch', 'localtime') "
                + "ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT solde FROM operations ORDER BY " + dateExpr + " DESC, id DESC LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double v = rs.getDouble("solde");
                return rs.wasNull() ? null : v;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retourne le dernier solde enregistré pour un mois et une année donnés (même année seulement).
     * Si aucune opération pour ce mois/année, retourne null.
     */
    /**
     * Retourne le dernier solde enregistré pour un mois et une année donnés (même année seulement).
     * Utilise `date_emission` et `date_visa` dans ORDER BY comme demandé.
     * Si aucune opération pour ce mois/année, retourne null.
     */
    public Double getLastSoldeForMonthYear(int year, int month) {
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa)) = 'integer' "
            + "THEN datetime(COALESCE(date_emission,date_visa)/1000, 'unixepoch', 'localtime') "
            + "ELSE COALESCE(date_emission,date_visa) END";

        String sql = "SELECT solde FROM operations "
            + "WHERE strftime('%Y', " + dateExpr + ") = ? "
            + "AND strftime('%m', " + dateExpr + ") = printf('%02d', ?) "
            + "ORDER BY " + dateExpr + " DESC, id DESC LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(year));
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double v = rs.getDouble("solde");
                    return rs.wasNull() ? null : v;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Compatibility method with expected name: retourne le dernier solde pour le mois/année.
     * Uses COALESCE(date_emission, date_visa) as requested.
     */
    public Double getLastMontantForMonthYear(int year, int month) {
        // Delegate to the robust implementation that handles epoch ms and ISO dates
        return getLastSoldeForMonthYear(year, month);
    }

    /**
     * Compatibility method: détecte s'il existe des opérations pour le mois/année donnés.
     */
    public boolean hasOperationsForMonthYear(int year, int month) {
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa)) = 'integer' "
            + "THEN datetime(COALESCE(date_emission,date_visa)/1000, 'unixepoch', 'localtime') "
            + "ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT 1 FROM operations WHERE strftime('%Y', " + dateExpr + ") = ? "
                + "AND strftime('%m', " + dateExpr + ") = printf('%02d', ?) LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(year));
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retourne vrai s'il existe au moins une opération pour le mois/année donnés.
     */
    public boolean hasOperationsInMonthYear(int year, int month) {
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa)) = 'integer' "
                + "THEN datetime(COALESCE(date_emission,date_visa)/1000, 'unixepoch') "
                + "ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT 1 FROM operations WHERE strftime('%Y', " + dateExpr + ") = ? "
                + "AND strftime('%m', " + dateExpr + ") = printf('%02d', ?) LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(year));
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getCountByDecision(String decision) {
        String sql = "SELECT COUNT(*) as count FROM operations WHERE decision = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, decision);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Find the operation record used as the initial balance (designation = 'Solde initial').
     * Returns null if not found.
     */
    public Operation findInitialOperation() {
        String sql = "SELECT * FROM operations WHERE LOWER(TRIM(designation)) = 'solde initial' LIMIT 1";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return mapResultSetToOperation(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Recompute cumulative solde for all operations ordered by date_emission/date_visa and
     * persist new solde values. This is useful after changing the initial balance.
     */
    public void recomputeAllSoldes() {
        String dateExpr = "CASE WHEN typeof(COALESCE(date_emission,date_visa)) = 'integer' "
            + "THEN datetime(COALESCE(date_emission,date_visa)/1000, 'unixepoch', 'localtime') "
            + "ELSE COALESCE(date_emission,date_visa) END";
        String sql = "SELECT * FROM operations ORDER BY " + dateExpr + " ASC, id ASC";
        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            java.util.List<Operation> ops = new java.util.ArrayList<>();
            while (rs.next()) ops.add(mapResultSetToOperation(rs));

            double prevSolde = 0.0;
            int start = 0;
            for (int i = 0; i < ops.size(); i++) {
                Operation o = ops.get(i);
                if (o.getDesignation() != null && "Solde initial".equalsIgnoreCase(o.getDesignation().trim())) {
                    prevSolde = o.getSolde() != null ? o.getSolde() : 0.0;
                    start = i + 1;
                    break;
                }
            }

            for (int i = start; i < ops.size(); i++) {
                Operation o = ops.get(i);
                double recette = o.getRecette() != null ? o.getRecette() : 0.0;
                // If depense is not set, compute it from sur_ram + sur_eng
                double depense = 0.0;
                if (o.getDepense() != null) {
                    depense = o.getDepense();
                } else {
                    double sr = o.getSurRam() != null ? o.getSurRam() : 0.0;
                    double se = o.getSurEng() != null ? o.getSurEng() : 0.0;
                    depense = sr + se;
                }
                double computed = prevSolde + recette - depense;
                o.setSolde(computed);
                try { update(o); } catch (Exception ignored) {}
                prevSolde = computed;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Operation mapResultSetToOperation(ResultSet rs) throws SQLException {
        Operation operation = new Operation();
        operation.setId(rs.getInt("id"));
        try { operation.setOvCheqType(rs.getString("ov_cheq_type")); } catch (SQLException ignored) {}
        try { int ov = rs.getInt("ov_cheq"); operation.setOvCheq(rs.wasNull() ? null : ov); } catch (SQLException ignored) {}
        operation.setImp(rs.getString("imp"));
        operation.setNature(rs.getString("nature"));
        operation.setBudg(rs.getString("budg"));
        try { operation.setSolde(rs.getDouble("solde")); } catch (SQLException ignored) {}
        try { Date dateEntree = rs.getDate("date_entree"); if (dateEntree != null) operation.setDateEntree(dateEntree.toLocalDate()); } catch (SQLException ignored) {}
        try { Date dateVisa = rs.getDate("date_visa"); if (dateVisa != null) operation.setDateVisa(dateVisa.toLocalDate()); } catch (SQLException ignored) {}
        try { Date dateRejet = rs.getDate("date_rejet"); if (dateRejet != null) operation.setDateRejet(dateRejet.toLocalDate()); } catch (SQLException ignored) {}
        try { operation.setDecision(rs.getString("decision")); } catch (SQLException ignored) {}
        try { operation.setMotifRejet(rs.getString("motif_rejet")); } catch (SQLException ignored) {}
        try { Date dateReponse = rs.getDate("date_reponse"); if (dateReponse != null) operation.setDateReponse(dateReponse.toLocalDate()); } catch (SQLException ignored) {}
        try { operation.setContenuReponse(rs.getString("contenu_reponse")); } catch (SQLException ignored) {}
        try { operation.setMois(rs.getString("mois")); } catch (SQLException ignored) {}
        try { operation.setDesignation(rs.getString("designation")); } catch (SQLException ignored) {}
        try { String nVal = rs.getString("n"); operation.setN(nVal); } catch (SQLException ignored) {}
        try { operation.setExercice(rs.getString("exercice")); } catch (SQLException ignored) {}
        try { operation.setBeneficiaire(rs.getString("beneficiaire")); } catch (SQLException ignored) {}
        try { Date de = rs.getDate("date_emission"); if (de != null) operation.setDateEmission(de.toLocalDate()); } catch (SQLException ignored) {}
        try { int opOr = rs.getInt("op_or"); operation.setOpOr(rs.wasNull() ? null : opOr); } catch (SQLException ignored) {}
        try { double r = rs.getDouble("recette"); operation.setRecette(rs.wasNull() ? null : r); } catch (SQLException ignored) {}
        try { double sr = rs.getDouble("sur_ram"); operation.setSurRam(rs.wasNull() ? null : sr); } catch (SQLException ignored) {}
        try { double se = rs.getDouble("sur_eng"); operation.setSurEng(rs.wasNull() ? null : se); } catch (SQLException ignored) {}
        try { double d = rs.getDouble("depense"); operation.setDepense(rs.wasNull() ? null : d); } catch (SQLException ignored) {}

        return operation;
    }
}
