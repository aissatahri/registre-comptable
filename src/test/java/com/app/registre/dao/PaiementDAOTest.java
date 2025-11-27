package com.app.registre.dao;

import com.app.registre.model.Paiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaiementDAOTest {

    @BeforeEach
    void setupDb() {
        Database.reset();
        try {
            java.io.File tmp = java.io.File.createTempFile("test-dao-pay", ".db");
            tmp.deleteOnExit();
            System.setProperty("db.url", "jdbc:sqlite:" + tmp.getAbsolutePath());
        } catch (java.io.IOException e) {
            System.setProperty("db.url", "jdbc:sqlite:target/test-dao-pay.db");
        }
        try {
            java.sql.Connection c = Database.getInstance().getConnection();
            try (java.sql.Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM paiements");
            }
        } catch (Exception ignored) {}
    }

    @Test
    void insertAndTotals() {
        PaiementDAO dao = new PaiementDAO();

        dao.insert(new Paiement("2022", "INV", 1000.0, "INFRA"));
        dao.insert(new Paiement("2022", "EXP", 250.0, "FONC"));
        dao.insert(new Paiement("2023", "INV", 500.0, "INFRA"));

        List<Paiement> all = dao.findAll();
        assertEquals(3, all.size());

        double inv2022 = dao.getTotalByAnneeAndType("2022", "INV");
        double exp2022 = dao.getTotalByAnneeAndType("2022", "EXP");
        assertEquals(1000.0, inv2022, 0.001);
        assertEquals(250.0, exp2022, 0.001);

        List<Paiement> y2022 = dao.findByAnnee("2022");
        assertEquals(2, y2022.size());
    }
}
