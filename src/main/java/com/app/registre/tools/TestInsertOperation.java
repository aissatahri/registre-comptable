package com.app.registre.tools;

import com.app.registre.dao.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class TestInsertOperation {
    public static void main(String[] args) {
        String sql = "INSERT INTO operations(imp, designation, nature, n, budg, exercice, beneficiaire, date_emission, date_visa, op_or, ov_cheq, recette, sur_ram, sur_eng, depense, solde) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = Database.getInstance().getConnection();
             PreparedStatement p = conn.prepareStatement(sql)) {

            p.setString(1, "IMP-TEST-001");
            p.setString(2, "TEST INSERT");
            p.setString(3, "SUBVENTION");
            p.setNull(4, java.sql.Types.INTEGER);
            p.setString(5, "INV");
            p.setString(6, "2025");
            p.setString(7, "Unit Test");
            p.setDate(8, java.sql.Date.valueOf(LocalDate.now()));
            p.setDate(9, java.sql.Date.valueOf(LocalDate.now()));
            p.setInt(10, 0);
            p.setString(11, "OV");
            p.setDouble(12, 1500.0);
            p.setDouble(13, 0.0);
            p.setDouble(14, 0.0);
            p.setDouble(15, 0.0);
            p.setDouble(16, 1500.0);

            System.out.println("Inserting test operation via raw SQL...");
            int rows = p.executeUpdate();
            System.out.println("Rows affected: " + rows);

            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) as c FROM operations")) {
                if (rs.next()) System.out.println("Total operations after insert: " + rs.getInt("c"));
            }

            try (ResultSet rs = conn.createStatement().executeQuery("SELECT imp, designation, solde FROM operations ORDER BY ROWID DESC LIMIT 1")) {
                if (rs.next()) System.out.println("Last inserted: imp=" + rs.getString("imp") + " designation=" + rs.getString("designation") + " solde=" + rs.getDouble("solde"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
