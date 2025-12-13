package com.app.registre.dao;

import com.app.registre.model.Operation;
import java.sql.*;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationDAO {
    private static final Logger log = LoggerFactory.getLogger(OperationDAO.class);

    public void insert(Operation operation) {
        String sql = """
            INSERT INTO operations(op, art, par, lig, imp, designation, nature, n, budg, exercice, beneficiaire,
                                   date_emission, date_entree, op_or, ov_cheq_type, ov_cheq, recette, sur_ram, sur_eng, depense, solde, montant, decision, mois)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Log key fields to help troubleshooting insert issues
                log.debug("Attempting insert operation: imp='{}', designation='{}', recette={}, surRam={}, surEng={}, depense={}, solde={}",
                    operation.getImp(), operation.getDesignation(), operation.getRecette(), operation.getSurRam(), operation.getSurEng(), operation.getDepense(), operation.getSolde());

                // Parameters correspond to the VALUES(...) above
                int idx = 1;
                // legacy 'op' field
                pstmt.setString(idx++, operation.getOp());
                if (operation.getArt() != null) pstmt.setInt(idx++, operation.getArt()); else { pstmt.setNull(idx++, java.sql.Types.INTEGER); }
                if (operation.getPar() != null) pstmt.setInt(idx++, operation.getPar()); else { pstmt.setNull(idx++, java.sql.Types.INTEGER); }
                if (operation.getLig() != null) pstmt.setInt(idx++, operation.getLig()); else { pstmt.setNull(idx++, java.sql.Types.INTEGER); }
                pstmt.setString(idx++, operation.getImp());
                pstmt.setString(idx++, operation.getDesignation());
                pstmt.setString(idx++, operation.getNature());
                if (operation.getN() != null) pstmt.setString(idx++, operation.getN()); else { pstmt.setNull(idx++, java.sql.Types.VARCHAR); }
                pstmt.setString(idx++, operation.getBudg());
                pstmt.setString(idx++, operation.getExercice());
                pstmt.setString(idx++, operation.getBeneficiaire());
                pstmt.setDate(idx++, operation.getDateEmission() != null ? Date.valueOf(operation.getDateEmission()) : null);
                pstmt.setDate(idx++, operation.getDateEntree() != null ? Date.valueOf(operation.getDateEntree()) : null);
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
                // montant (legacy) - mirror solde if set
                if (operation.getSolde() != null) pstmt.setDouble(idx++, operation.getSolde()); else { pstmt.setNull(idx++, java.sql.Types.REAL); }
                pstmt.setString(idx++, operation.getDecision());
                pstmt.setString(idx++, operation.getMois());
                // Log the key values and DB URL that will be used to help debug test failures
                log.info("DB URL: {}", System.getProperty("db.url"));
                log.info("Inserting operation: op='{}' montant={} mois='{}' solde={}", operation.getOp(), operation.getMontant(), operation.getMois(), operation.getSolde());

            int rows = pstmt.executeUpdate();
            log.info("Insert completed, rows affected={}", rows);
        } catch (SQLException e) {
            log.error("Error inserting operation (imp='{}', designation='{}'): {}", operation != null ? operation.getImp() : "-", operation != null ? operation.getDesignation() : "-", e.toString(), e);
        }
    }

    public void update(Operation operation) {
            String sql = """
            UPDATE operations SET
            op = ?, art = ?, par = ?, lig = ?, imp = ?, designation = ?, nature = ?, n = ?, budg = ?, exercice = ?, beneficiaire = ?,
            date_emission = ?, date_entree = ?, op_or = ?, ov_cheq_type = ?, ov_cheq = ?, recette = ?, sur_ram = ?, sur_eng = ?, depense = ?, solde = ?, montant = ?, decision = ?, mois = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int idx2 = 1;
            pstmt.setString(idx2++, operation.getOp());
            if (operation.getArt() != null) pstmt.setInt(idx2++, operation.getArt()); else { pstmt.setNull(idx2++, java.sql.Types.INTEGER); }
            if (operation.getPar() != null) pstmt.setInt(idx2++, operation.getPar()); else { pstmt.setNull(idx2++, java.sql.Types.INTEGER); }
            if (operation.getLig() != null) pstmt.setInt(idx2++, operation.getLig()); else { pstmt.setNull(idx2++, java.sql.Types.INTEGER); }
            pstmt.setString(idx2++, operation.getImp());
            pstmt.setString(idx2++, operation.getDesignation());
            pstmt.setString(idx2++, operation.getNature());
            if (operation.getN() != null) pstmt.setString(idx2++, operation.getN()); else { pstmt.setNull(idx2++, java.sql.Types.VARCHAR); }
            pstmt.setString(idx2++, operation.getBudg());
            pstmt.setString(idx2++, operation.getExercice());
            pstmt.setString(idx2++, operation.getBeneficiaire());
            pstmt.setDate(idx2++, operation.getDateEmission() != null ? Date.valueOf(operation.getDateEmission()) : null);
            pstmt.setDate(idx2++, operation.getDateEntree() != null ? Date.valueOf(operation.getDateEntree()) : null);
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
            if (operation.getSolde() != null) pstmt.setDouble(idx2++, operation.getSolde()); else { pstmt.setNull(idx2++, java.sql.Types.REAL); }
            pstmt.setString(idx2++, operation.getDecision());
            pstmt.setString(idx2++, operation.getMois());
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
        String sql = "SELECT * FROM operations ORDER BY date_emission ASC";

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

    /**
     * Recherche des opérations filtrées par ART, PAR et LIG. Si une valeur est null,
     * elle n'est pas prise en compte dans le filtre.
     */
    public List<Operation> findByArtParLig(Integer art, Integer par, Integer lig) {
        List<Operation> operations = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM operations");
        java.util.List<Object> params = new java.util.ArrayList<>();
        boolean first = true;
        if (art != null) {
            sql.append(first ? " WHERE " : " AND "); sql.append("art = ?"); params.add(art); first = false;
        }
        if (par != null) {
            sql.append(first ? " WHERE " : " AND "); sql.append("par = ?"); params.add(par); first = false;
        }
        if (lig != null) {
            sql.append(first ? " WHERE " : " AND "); sql.append("lig = ?"); params.add(lig); first = false;
        }
        sql.append(" ORDER BY date_emission ASC");

        try (Connection conn = DriverManager.getConnection(Database.getDbUrl());
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) pstmt.setObject(i + 1, params.get(i));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) operations.add(mapResultSetToOperation(rs));
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
        String sql = "SELECT * FROM operations ORDER BY date_emission ASC";

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
        String sql = "SELECT COUNT(*) as c FROM operations WHERE imp IS NOT NULL AND imp != ''";
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
        String sql = "SELECT solde FROM operations WHERE date_emission < ? ORDER BY date_emission DESC LIMIT 1";
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
        String sql = "SELECT 1 FROM operations WHERE date_emission < ? LIMIT 1";
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
    * Retourne le dernier solde enregistré (la plus récente opération selon date_emission).
     * Si aucune opération, retourne 0.0
     */
    public Double getLastMontant() {
        String dateExpr = "CASE WHEN typeof(date_emission) = 'integer' "
            + "THEN datetime(date_emission/1000, 'unixepoch', 'localtime') "
            + "ELSE date_emission END";
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
    * Utilise `date_emission` dans ORDER BY.
     * Si aucune opération pour ce mois/année, retourne null.
     */
    public Double getLastSoldeForMonthYear(int year, int month) {
        String dateExpr = "CASE WHEN typeof(date_emission) = 'integer' "
            + "THEN datetime(date_emission/1000, 'unixepoch', 'localtime') "
            + "ELSE date_emission END";

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
     */
    public Double getLastMontantForMonthYear(int year, int month) {
        // Delegate to the robust implementation that handles epoch ms and ISO dates
        return getLastSoldeForMonthYear(year, month);
    }

    /**
     * Compatibility method: détecte s'il existe des opérations pour le mois/année donnés.
     */
    public boolean hasOperationsForMonthYear(int year, int month) {
        String dateExpr = "CASE WHEN typeof(date_emission) = 'integer' "
            + "THEN datetime(date_emission/1000, 'unixepoch', 'localtime') "
            + "ELSE date_emission END";
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

    /**
     * Retourne le nombre d'opérations pour un mois/année donnés.
     */
    public int getCountForMonthYear(int year, int month) {
        String dateExpr = "CASE WHEN typeof(date_emission) = 'integer' "
            + "THEN datetime(date_emission/1000, 'unixepoch', 'localtime') "
            + "ELSE date_emission END";
        String sql = "SELECT COUNT(*) as c FROM operations WHERE imp IS NOT NULL AND imp != '' "
            + "AND strftime('%Y', " + dateExpr + ") = ? "
            + "AND strftime('%m', " + dateExpr + ") = printf('%02d', ?)";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(year));
            pstmt.setInt(2, month);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("c");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
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
     * Recherche un enregistrement d'initialisation de solde pour un mois/année donnés.
     * On considère ici les opérations dont la désignation commence par 'solde initial' (insensible à la casse).
     * Retourne la valeur de solde si trouvée, sinon null.
     */
    public Double getInitialSoldeForMonthYear(int year, int month) {
        String dateExpr = "CASE WHEN typeof(date_emission) = 'integer' "
            + "THEN datetime(date_emission/1000, 'unixepoch', 'localtime') "
            + "ELSE date_emission END";
        String sql = "SELECT solde FROM operations WHERE LOWER(TRIM(designation)) LIKE 'solde initial%' "
            + "AND strftime('%Y', " + dateExpr + ") = ? "
            + "AND strftime('%m', " + dateExpr + ") = printf('%02d', ?) "
            + "ORDER BY " + dateExpr + " DESC LIMIT 1";
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
    * Recompute cumulative solde for all operations ordered by date_emission and
     * persist new solde values. This is useful after changing the initial balance.
     */
    public void recomputeAllSoldes() {
        String dateExpr = "CASE WHEN typeof(date_emission) = 'integer' "
            + "THEN datetime(date_emission/1000, 'unixepoch', 'localtime') "
            + "ELSE date_emission END";
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
        if (hasColumn(rs, "op")) operation.setOp(rs.getString("op"));
        operation.setImp(rs.getString("imp"));
        operation.setArt(rs.getObject("art") != null ? rs.getInt("art") : null);
        operation.setPar(rs.getObject("par") != null ? rs.getInt("par") : null);
        operation.setLig(rs.getObject("lig") != null ? rs.getInt("lig") : null);
        operation.setDesignation(rs.getString("designation"));
        operation.setNature(rs.getString("nature"));
        operation.setN(rs.getString("n"));
        operation.setBudg(rs.getString("budg"));
        operation.setExercice(rs.getString("exercice"));
        operation.setBeneficiaire(rs.getString("beneficiaire"));
        
        // Dates
        Date dateEmission = rs.getDate("date_emission");
        if (dateEmission != null) operation.setDateEmission(dateEmission.toLocalDate());
        
        // Integers nullables
        if (hasColumn(rs, "op_or")) {
            int opOr = rs.getInt("op_or");
            operation.setOpOr(rs.wasNull() ? null : opOr);
        }
        
        if (hasColumn(rs, "ov_cheq_type")) {
            operation.setOvCheqType(rs.getString("ov_cheq_type"));
        }
        if (hasColumn(rs, "ov_cheq")) {
            int ovCheq = rs.getInt("ov_cheq");
            operation.setOvCheq(rs.wasNull() ? null : ovCheq);
        }
        
        // Doubles nullables
        if (hasColumn(rs, "recette")) {
            double recette = rs.getDouble("recette");
            operation.setRecette(rs.wasNull() ? null : recette);
        }
        if (hasColumn(rs, "sur_ram")) {
            double surRam = rs.getDouble("sur_ram");
            operation.setSurRam(rs.wasNull() ? null : surRam);
        }
        if (hasColumn(rs, "sur_eng")) {
            double surEng = rs.getDouble("sur_eng");
            operation.setSurEng(rs.wasNull() ? null : surEng);
        }
        if (hasColumn(rs, "depense")) {
            double depense = rs.getDouble("depense");
            operation.setDepense(rs.wasNull() ? null : depense);
        }
        if (hasColumn(rs, "solde")) {
            double solde = rs.getDouble("solde");
            operation.setSolde(rs.wasNull() ? null : solde);
        }

        return operation;
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                if (md.getColumnName(i).equalsIgnoreCase(columnName)) return true;
            }
        } catch (SQLException e) {
            // ignore and fall through
        }
        return false;
    }
}
