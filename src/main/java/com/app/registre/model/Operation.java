package com.app.registre.model;

import java.time.LocalDate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Operation {
    private int id;
    private String op;
    private String ovCheqType;
    private Integer ovCheq;
    private String imp;
    private String nature;
    private String budg;
    private Double solde;
    private LocalDate dateEntree;
    private LocalDate dateVisa;
    private LocalDate dateRejet;
    private String decision;
    private String motifRejet;
    private LocalDate dateReponse;
    private String contenuReponse;
    private String mois;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    private String designation;
    private String n;
    private String exercice;
    private String beneficiaire;
    private LocalDate dateEmission;
    private Integer opOr;
    private Double recette;
    private Double surRam;
    private Double surEng;
    private Double depense;

    public Operation() {}

    public Operation(String op, String ovCheqType, String imp, String nature, String budg,
                     Double solde, LocalDate dateEntree, String mois) {
        this.op = op;
        // backward-compatible constructor: second param used as ovCheqType (e.g. "OV"/"CHEQ")
        this.ovCheqType = ovCheqType;
        this.ovCheq = null;
        this.imp = imp;
        this.nature = nature;
        this.budg = budg;
        this.solde = solde;
        this.dateEntree = dateEntree;
        this.mois = mois;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }

    public String getOvCheqType() { return ovCheqType; }
    public void setOvCheqType(String ovCheqType) { this.ovCheqType = ovCheqType; }

    public Integer getOvCheq() { return ovCheq; }
    public void setOvCheq(Integer ovCheq) { this.ovCheq = ovCheq; }

    public String getImp() { return imp; }
    public void setImp(String imp) { this.imp = imp; }

    public String getNature() { return nature; }
    public void setNature(String nature) { this.nature = nature; }

    public String getBudg() { return budg; }
    public void setBudg(String budg) { this.budg = budg; }

    // Backing field renamed to `solde`. Keep getMontant/setMontant for compatibility
    public Double getSolde() { return solde; }
    public void setSolde(Double solde) { this.solde = solde; }

    public double getMontant() { return solde == null ? 0.0 : solde; }
    public void setMontant(double montant) { this.solde = montant; }

    public LocalDate getDateEntree() { return dateEntree; }
    public void setDateEntree(LocalDate dateEntree) { this.dateEntree = dateEntree; }

    public LocalDate getDateVisa() { return dateVisa; }
    public void setDateVisa(LocalDate dateVisa) { this.dateVisa = dateVisa; }

    public LocalDate getDateRejet() { return dateRejet; }
    public void setDateRejet(LocalDate dateRejet) { this.dateRejet = dateRejet; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getMotifRejet() { return motifRejet; }
    public void setMotifRejet(String motifRejet) { this.motifRejet = motifRejet; }

    public LocalDate getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDate dateReponse) { this.dateReponse = dateReponse; }

    public String getContenuReponse() { return contenuReponse; }
    public void setContenuReponse(String contenuReponse) { this.contenuReponse = contenuReponse; }

    public String getMois() { return mois; }
    public void setMois(String mois) { this.mois = mois; }

    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    public BooleanProperty selectedProperty() { return selected; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getN() { return n; }
    public void setN(String n) { this.n = n; }

    public String getExercice() { return exercice; }
    public void setExercice(String exercice) { this.exercice = exercice; }

    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    public Integer getOpOr() { return opOr; }
    public void setOpOr(Integer opOr) { this.opOr = opOr; }

    public Double getRecette() { return recette; }
    public void setRecette(Double recette) { this.recette = recette; }

    public Double getSurRam() { return surRam; }
    public void setSurRam(Double surRam) { this.surRam = surRam; }

    public Double getSurEng() { return surEng; }
    public void setSurEng(Double surEng) { this.surEng = surEng; }

    public Double getDepense() { return depense; }
    public void setDepense(Double depense) { this.depense = depense; }
}
