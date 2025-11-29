package com.app.registre.dao;

import com.app.registre.model.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OperationDAOTest {

    @BeforeEach
    void setupDb() {
        Database.reset();
        try {
            java.io.File tmp = java.io.File.createTempFile("test-dao-op", ".db");
            tmp.deleteOnExit();
            System.setProperty("db.url", "jdbc:sqlite:" + tmp.getAbsolutePath());
        } catch (java.io.IOException e) {
            System.setProperty("db.url", "jdbc:sqlite:target/test-dao-op.db");
        }
        try {
            java.sql.Connection c = Database.getInstance().getConnection();
            try (java.sql.Statement st = c.createStatement()) {
                st.executeUpdate("DELETE FROM operations");
            }
        } catch (Exception ignored) {}
    }

    @Test
    void insertAndFindAll() {
        OperationDAO dao = new OperationDAO();

        Operation op = new Operation();
        op.setOp("OP-001");
        op.setOvCheqType("OV1");
        op.setImp("IMP1");
        op.setNature("BC");
        op.setBudg("B1");
        op.setMontant(1234.56);
        op.setDateEntree(LocalDate.now());
        op.setMois("JANVIER");
        op.setDecision("P");

        dao.insert(op);

        List<Operation> all = dao.findAll();
        assertEquals(1, all.size());
        Operation r = all.get(0);
        assertEquals("OP-001", r.getOp());
        assertEquals(1234.56, r.getMontant(), 0.001);
        assertEquals("JANVIER", r.getMois());
    }

    @Test
    void updateAndStats() {
        OperationDAO dao = new OperationDAO();

        Operation op = new Operation();
        op.setOp("OP-002");
        op.setNature("BC");
        op.setMontant(500);
        op.setDateEntree(LocalDate.now());
        op.setMois("FEVRIER");
        op.setDecision("R");
        dao.insert(op);

        List<Operation> all = dao.findAll();
        Operation saved = all.get(0);
        saved.setNature("REGIE");
        dao.update(saved);

        assertEquals(1, dao.getCountByDecision("R"));
        assertEquals(500.0, dao.getTotalMontantByMois("FEVRIER"), 0.001);
        List<Operation> feb = dao.findByMois("FEVRIER");
        assertEquals(1, feb.size());
        assertEquals("REGIE", feb.get(0).getNature());
    }
}
