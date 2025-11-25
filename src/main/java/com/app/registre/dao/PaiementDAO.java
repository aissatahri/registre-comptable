package com.app.registre.dao;

import com.app.registre.model.Paiement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaiementDAO {

    public void insert(Paiement paiement) {
        String sql = "INSERT INTO paiements(annee, type, montant, categorie) VALUES(?,?,?,?)";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, paiement.getAnnee());
            pstmt.setString(2, paiement.getType());
            pstmt.setDouble(3, paiement.getMontant());
            pstmt.setString(4, paiement.getCategorie());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Paiement> findAll() {
        List<Paiement> paiements = new ArrayList<>();
        String sql = "SELECT * FROM paiements ORDER BY annee, type";

        try (Connection conn = Database.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                paiements.add(mapResultSetToPaiement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paiements;
    }

    public List<Paiement> findByAnnee(String annee) {
        List<Paiement> paiements = new ArrayList<>();
        String sql = "SELECT * FROM paiements WHERE annee = ? ORDER BY type, categorie";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, annee);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                paiements.add(mapResultSetToPaiement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paiements;
    }

    public double getTotalByAnneeAndType(String annee, String type) {
        String sql = "SELECT SUM(montant) as total FROM paiements WHERE annee = ? AND type = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, annee);
            pstmt.setString(2, type);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public double getTotalRecettesAll() {
        String sql = "SELECT SUM(montant) as total FROM paiements WHERE type IN ('RECETTE','INV')";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void update(Paiement paiement) {
        String sql = "UPDATE paiements SET annee = ?, type = ?, montant = ?, categorie = ? WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, paiement.getAnnee());
            pstmt.setString(2, paiement.getType());
            pstmt.setDouble(3, paiement.getMontant());
            pstmt.setString(4, paiement.getCategorie());
            pstmt.setInt(5, paiement.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM paiements WHERE id = ?";

        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Paiement mapResultSetToPaiement(ResultSet rs) throws SQLException {
        Paiement paiement = new Paiement();
        paiement.setId(rs.getInt("id"));
        paiement.setAnnee(rs.getString("annee"));
        paiement.setType(rs.getString("type"));
        paiement.setMontant(rs.getDouble("montant"));
        paiement.setCategorie(rs.getString("categorie"));
        return paiement;
    }
}
