package com.app.registre.dao;

import com.app.registre.model.Operation;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OperationDAO {

    public void insert(Operation operation) {
        String sql = """
            INSERT INTO operations(op, ov_cheq, imp, nature, budg, montant,
                                 date_entree, date_visa, date_rejet, decision,
                                 motif_rejet, date_reponse, contenu_reponse, mois)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, operation.getOp());
            pstmt.setString(2, operation.getOvCheq());
            pstmt.setString(3, operation.getImp());
            pstmt.setString(4, operation.getNature());
            pstmt.setString(5, operation.getBudg());
            pstmt.setDouble(6, operation.getMontant());
            pstmt.setDate(7, operation.getDateEntree() != null ? Date.valueOf(operation.getDateEntree()) : null);
            pstmt.setDate(8, operation.getDateVisa() != null ? Date.valueOf(operation.getDateVisa()) : null);
            pstmt.setDate(9, operation.getDateRejet() != null ? Date.valueOf(operation.getDateRejet()) : null);
            pstmt.setString(10, operation.getDecision());
            pstmt.setString(11, operation.getMotifRejet());
            pstmt.setDate(12, operation.getDateReponse() != null ? Date.valueOf(operation.getDateReponse()) : null);
            pstmt.setString(13, operation.getContenuReponse());
            pstmt.setString(14, operation.getMois());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Operation operation) {
        String sql = """
            UPDATE operations SET
            op = ?, ov_cheq = ?, imp = ?, nature = ?, budg = ?, montant = ?,
            date_entree = ?, date_visa = ?, date_rejet = ?, decision = ?,
            motif_rejet = ?, date_reponse = ?, contenu_reponse = ?, mois = ?
            WHERE id = ?
            """;

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, operation.getOp());
            pstmt.setString(2, operation.getOvCheq());
            pstmt.setString(3, operation.getImp());
            pstmt.setString(4, operation.getNature());
            pstmt.setString(5, operation.getBudg());
            pstmt.setDouble(6, operation.getMontant());
            pstmt.setDate(7, operation.getDateEntree() != null ? Date.valueOf(operation.getDateEntree()) : null);
            pstmt.setDate(8, operation.getDateVisa() != null ? Date.valueOf(operation.getDateVisa()) : null);
            pstmt.setDate(9, operation.getDateRejet() != null ? Date.valueOf(operation.getDateRejet()) : null);
            pstmt.setString(10, operation.getDecision());
            pstmt.setString(11, operation.getMotifRejet());
            pstmt.setDate(12, operation.getDateReponse() != null ? Date.valueOf(operation.getDateReponse()) : null);
            pstmt.setString(13, operation.getContenuReponse());
            pstmt.setString(14, operation.getMois());
            pstmt.setInt(15, operation.getId());

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
        String sql = "SELECT * FROM operations ORDER BY date_entree DESC";

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
        String sql = "SELECT * FROM operations WHERE mois = ? ORDER BY date_entree DESC";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, mois);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                operations.add(mapResultSetToOperation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return operations;
    }

    public double getTotalMontantByMois(String mois) {
        String sql = "SELECT SUM(montant) as total FROM operations WHERE mois = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, mois);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
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

    private Operation mapResultSetToOperation(ResultSet rs) throws SQLException {
        Operation operation = new Operation();
        operation.setId(rs.getInt("id"));
        operation.setOp(rs.getString("op"));
        operation.setOvCheq(rs.getString("ov_cheq"));
        operation.setImp(rs.getString("imp"));
        operation.setNature(rs.getString("nature"));
        operation.setBudg(rs.getString("budg"));
        operation.setMontant(rs.getDouble("montant"));
        Date dateEntree = rs.getDate("date_entree");
        if (dateEntree != null) operation.setDateEntree(dateEntree.toLocalDate());

        Date dateVisa = rs.getDate("date_visa");
        if (dateVisa != null) operation.setDateVisa(dateVisa.toLocalDate());

        Date dateRejet = rs.getDate("date_rejet");
        if (dateRejet != null) operation.setDateRejet(dateRejet.toLocalDate());

        operation.setDecision(rs.getString("decision"));
        operation.setMotifRejet(rs.getString("motif_rejet"));

        Date dateReponse = rs.getDate("date_reponse");
        if (dateReponse != null) operation.setDateReponse(dateReponse.toLocalDate());

        operation.setContenuReponse(rs.getString("contenu_reponse"));
        operation.setMois(rs.getString("mois"));

        return operation;
    }
}
