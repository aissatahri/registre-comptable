package com.app.registre.dao;

import com.app.registre.model.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RecapDAOTest {

    @BeforeEach
    void setupDb() throws Exception {
        Database.reset();
        java.io.File tmp = java.io.File.createTempFile("test-recap", ".db");
        tmp.deleteOnExit();
        System.setProperty("db.url", "jdbc:sqlite:" + tmp.getAbsolutePath());
        Database.getInstance().getConnection();

        // Populate operations
        OperationDAO dao = new OperationDAO();
        Operation jan = new Operation();
        jan.setOp("JAN-1"); jan.setNature("SUBVENTION"); jan.setMontant(1000); jan.setDateEntree(LocalDate.now()); jan.setMois("JANVIER");
        dao.insert(jan);

        Operation feb = new Operation();
        feb.setOp("FEB-1"); feb.setNature("REGIE"); feb.setMontant(300); feb.setDateEntree(LocalDate.now()); feb.setMois("FEVRIER");
        dao.insert(feb);

        Operation feb2 = new Operation();
        feb2.setOp("FEB-2"); feb2.setNature("REGIE"); feb2.setMontant(200); feb2.setDateEntree(LocalDate.now()); feb2.setMois("FEVRIER");
        dao.insert(feb2);
    }

    @Test
    void aggregations() {
        RecapDAO recap = new RecapDAO();

        Map<String, Double> mois = recap.getTotauxParMois();
        assertEquals(2, mois.size());
        assertEquals(1000.0, mois.get("JANVIER"), 0.001);
        assertEquals(500.0, mois.get("FEVRIER"), 0.001);

        Map<String, Double> nature = recap.getTotauxParNature();
        assertTrue(nature.get("SUBVENTION") >= 1000.0);
        assertTrue(nature.get("REGIE") >= 500.0);

        double recettes = recap.getTotalRecettes();
        double depenses = recap.getTotalDepenses();
        assertEquals(1000.0, recettes, 0.001);
        assertEquals(500.0, depenses, 0.001);

        assertEquals(3, recap.getTotalOperations());
    }
}
