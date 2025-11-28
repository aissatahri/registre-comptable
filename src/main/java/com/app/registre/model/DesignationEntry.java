package com.app.registre.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DesignationEntry {
    private final StringProperty imp = new SimpleStringProperty();
    private final StringProperty designation = new SimpleStringProperty();

    public DesignationEntry() {}

    public DesignationEntry(String imp, String designation) {
        this.imp.set(imp == null ? "" : imp);
        this.designation.set(designation == null ? "" : designation);
    }

    public String getImp() { return imp.get(); }
    public void setImp(String v) { imp.set(v); }
    public StringProperty impProperty() { return imp; }

    public String getDesignation() { return designation.get(); }
    public void setDesignation(String v) { designation.set(v); }
    public StringProperty designationProperty() { return designation; }
}
