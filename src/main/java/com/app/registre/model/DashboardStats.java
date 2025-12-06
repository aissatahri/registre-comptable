package com.app.registre.model;

public class DashboardStats {
    private double soldeCourant;
    private double soldeInitial;
    private double recettesMois;
    private double depensesMois;
    private double depensesExpMois;
    private double depensesInvMois;
    private double recettesAnnee;
    private double depensesAnnee;
    private double depensesExpAnnee;
    private double depensesInvAnnee;
    private int nombreOperationsMois;
    private String moisCourant;
    private int anneeCourante;

    public DashboardStats() {
    }

    public DashboardStats(double soldeCourant, double soldeInitial, double recettesMois, double depensesMois,
                          double depensesExpMois, double depensesInvMois,
                          double recettesAnnee, double depensesAnnee, double depensesExpAnnee, double depensesInvAnnee,
                          int nombreOperationsMois,
                          String moisCourant, int anneeCourante) {
        this.soldeCourant = soldeCourant;
        this.soldeInitial = soldeInitial;
        this.recettesMois = recettesMois;
        this.depensesMois = depensesMois;
        this.depensesExpMois = depensesExpMois;
        this.depensesInvMois = depensesInvMois;
        this.recettesAnnee = recettesAnnee;
        this.depensesAnnee = depensesAnnee;
        this.depensesExpAnnee = depensesExpAnnee;
        this.depensesInvAnnee = depensesInvAnnee;
        this.nombreOperationsMois = nombreOperationsMois;
        this.moisCourant = moisCourant;
        this.anneeCourante = anneeCourante;
    }

    public double getSoldeCourant() {
        return soldeCourant;
    }

    public void setSoldeCourant(double soldeCourant) {
        this.soldeCourant = soldeCourant;
    }

    public double getSoldeInitial() {
        return soldeInitial;
    }

    public void setSoldeInitial(double soldeInitial) {
        this.soldeInitial = soldeInitial;
    }

    public double getRecettesMois() {
        return recettesMois;
    }

    public void setRecettesMois(double recettesMois) {
        this.recettesMois = recettesMois;
    }

    public double getDepensesMois() {
        return depensesMois;
    }

    public void setDepensesMois(double depensesMois) {
        this.depensesMois = depensesMois;
    }

    public double getDepensesExpMois() {
        return depensesExpMois;
    }

    public void setDepensesExpMois(double depensesExpMois) {
        this.depensesExpMois = depensesExpMois;
    }

    public double getDepensesInvMois() {
        return depensesInvMois;
    }

    public void setDepensesInvMois(double depensesInvMois) {
        this.depensesInvMois = depensesInvMois;
    }

    public double getRecettesAnnee() {
        return recettesAnnee;
    }

    public void setRecettesAnnee(double recettesAnnee) {
        this.recettesAnnee = recettesAnnee;
    }

    public double getDepensesAnnee() {
        return depensesAnnee;
    }

    public void setDepensesAnnee(double depensesAnnee) {
        this.depensesAnnee = depensesAnnee;
    }

    public double getDepensesExpAnnee() {
        return depensesExpAnnee;
    }

    public void setDepensesExpAnnee(double depensesExpAnnee) {
        this.depensesExpAnnee = depensesExpAnnee;
    }

    public double getDepensesInvAnnee() {
        return depensesInvAnnee;
    }

    public void setDepensesInvAnnee(double depensesInvAnnee) {
        this.depensesInvAnnee = depensesInvAnnee;
    }

    public int getNombreOperationsMois() {
        return nombreOperationsMois;
    }

    public void setNombreOperationsMois(int nombreOperationsMois) {
        this.nombreOperationsMois = nombreOperationsMois;
    }

    public String getMoisCourant() {
        return moisCourant;
    }

    public void setMoisCourant(String moisCourant) {
        this.moisCourant = moisCourant;
    }

    public int getAnneeCourante() {
        return anneeCourante;
    }

    public void setAnneeCourante(int anneeCourante) {
        this.anneeCourante = anneeCourante;
    }

    public double getBalanceMois() {
        return recettesMois - depensesMois;
    }

    public double getBalanceAnnee() {
        return soldeInitial + recettesAnnee - depensesAnnee;
    }
}
