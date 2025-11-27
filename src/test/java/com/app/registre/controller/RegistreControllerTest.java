package com.app.registre.controller;

import com.app.registre.model.Operation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class RegistreControllerTest {

    @BeforeAll
    static void initFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }

    @Test
    void filtersByMoisNatureAndSearch() throws Exception {
        RegistreController ctrl = new RegistreController();

        TableView<Operation> table = new TableView<>();
        TextField search = new TextField();
        ComboBox<String> mois = new ComboBox<>();
        ComboBox<String> nature = new ComboBox<>();
        javafx.scene.text.Text totalOps = new javafx.scene.text.Text();
        javafx.scene.text.Text totalMontant = new javafx.scene.text.Text();
        javafx.scene.text.Text payes = new javafx.scene.text.Text();
        javafx.scene.text.Text rejetes = new javafx.scene.text.Text();

        Operation a = new Operation("OP-1","OV","IMP","REGIE","B",100.0, LocalDate.now(),"JANVIER");
        a.setDecision("P");
        Operation b = new Operation("OP-2","OV","IMP","BC","B",200.0, LocalDate.now(),"FEVRIER");
        b.setDecision("R");

        javafx.collections.ObservableList<Operation> ops = javafx.collections.FXCollections.observableArrayList(a,b);

        setField(ctrl, "operationsTable", table);
        setField(ctrl, "searchField", search);
        setField(ctrl, "filterMois", mois);
        setField(ctrl, "filterNature", nature);
        setField(ctrl, "operations", ops);
        setField(ctrl, "operationDAO", new com.app.registre.dao.OperationDAO());
        setField(ctrl, "totalOperationsText", totalOps);
        setField(ctrl, "totalMontantText", totalMontant);
        setField(ctrl, "dossiersPayesText", payes);
        setField(ctrl, "dossiersRejetesText", rejetes);

        mois.setValue("JANVIER");
        nature.setValue("REGIE");
        search.setText("OP-1");

        java.lang.reflect.Method m = RegistreController.class.getDeclaredMethod("applyFilters");
        m.setAccessible(true);
        m.invoke(ctrl);
        assertEquals(1, table.getItems().size());
        assertEquals("OP-1", table.getItems().get(0).getOp());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
