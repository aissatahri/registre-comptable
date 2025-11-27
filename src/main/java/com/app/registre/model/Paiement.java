package com.app.registre.model;

public class Paiement {
    private int id;
    private String annee;
    private String type;
    private double montant;
    private String categorie;

    public Paiement() {}

    public Paiement(String annee, String type, double montant, String categorie) {
        this.annee = annee;
        this.type = type;
        this.montant = montant;
        this.categorie = categorie;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAnnee() { return annee; }
    public void setAnnee(String annee) { this.annee = annee; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
}