package com.app.registre.util;

import com.app.registre.model.Operation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelUtilTest {

    @Test
    void exportThenImportOperations() throws Exception {
        Operation op = new Operation();
        op.setOp("OP-X");
        op.setOvCheqType("OV-X");
        op.setImp("IMP-X");
        op.setNature("BC");
        op.setBudg("B1");
        op.setMontant(42.5);
        op.setDateEntree(LocalDate.of(2022, 1, 10));
        op.setDateVisa(LocalDate.of(2022, 1, 12));
        op.setDateRejet(null);
        op.setDecision("P");
        op.setMotifRejet("");
        op.setDateReponse(LocalDate.of(2022, 1, 20));
        op.setContenuReponse("OK");
        op.setMois("JANVIER");

        List<Operation> ops = java.util.List.of(op);

        File temp = File.createTempFile("ops", ".xlsx");
        temp.deleteOnExit();

        ExcelUtil.exportOperationsToExcel(ops, temp.getAbsolutePath());
        List<Operation> imported = ExcelUtil.importOperationsFromExcel(temp.getAbsolutePath());

        assertEquals(1, imported.size());
        Operation r = imported.get(0);
        assertEquals("OP-X", r.getOp());
        assertEquals(42.5, r.getMontant(), 0.001);
        assertEquals("JANVIER", r.getMois());
        assertEquals("OK", r.getContenuReponse());
    }
}
