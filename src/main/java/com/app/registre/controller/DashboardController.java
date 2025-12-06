package com.app.registre.controller;

import com.app.registre.dao.DashboardDAO;
import com.app.registre.model.DashboardStats;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Map;

public class DashboardController {

    @FXML
    private ComboBox<Integer> yearCombo;
    @FXML
    private ComboBox<String> monthCombo;
    @FXML
    private Label soldeActuelLabel;
    @FXML
    private Label soldeSubtitle;
    @FXML
    private Label recettesMoisLabel;
    @FXML
    private Label recettesMoisSubtitle;
    @FXML
    private Label depensesMoisLabel;
    @FXML
    private Label depensesMoisSubtitle;
    @FXML
    private Label depensesExpMoisLabel;
    @FXML
    private Label depensesInvMoisLabel;
    @FXML
    private Label recettesAnneeLabel;
    @FXML
    private Label depensesAnneeLabel;
    @FXML
    private Label depensesExpAnneeLabel;
    @FXML
    private Label depensesInvAnneeLabel;
    @FXML
    private Label soldeInitialLabel;
    @FXML
    private Label balanceAnneeLabel;
    @FXML
    private LineChart<String, Number> evolutionChart;
    @FXML
    private VBox alertContainer;
    @FXML
    private Label alertLabel;

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final DecimalFormat formatter = new DecimalFormat("#,##0.00");

    @FXML
    public void initialize() {
        // Initialiser le combo des années
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 5; year <= currentYear + 1; year++) {
            yearCombo.getItems().add(year);
        }
        yearCombo.setValue(currentYear);

        // Initialiser le combo des mois
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};
        monthCombo.getItems().addAll(months);
        monthCombo.setValue(months[LocalDate.now().getMonthValue() - 1]);

        // Sélectionner par défaut le dernier mois saisi si disponible
        LocalDate latest = dashboardDAO.getLatestOperationDate();
        if (latest != null) {
            int lastYear = latest.getYear();
            if (!yearCombo.getItems().contains(lastYear)) {
                yearCombo.getItems().add(lastYear);
            }
            yearCombo.setValue(lastYear);

            String lastMonth = toFrenchMonth(latest);
            if (lastMonth != null && monthCombo.getItems().contains(lastMonth)) {
                monthCombo.setValue(lastMonth);
            }
        }

        // Charger le dashboard
        Platform.runLater(this::loadDashboard);
    }

    @FXML
    private void onYearChange() {
        loadDashboard();
    }

    @FXML
    private void onMonthChange() {
        loadDashboard();
    }

    @FXML
    private void refreshDashboard() {
        loadDashboard();
    }

    private void loadDashboard() {
        Integer year = yearCombo.getValue();
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        String mois = monthCombo != null ? monthCombo.getValue() : null;

        System.out.println("========================================");
        System.out.println("DASHBOARD: Chargement pour l'année " + year);
        System.out.println("========================================");

        // Charger les statistiques
        DashboardStats stats = dashboardDAO.getDashboardStats(year, mois);

        System.out.println("DASHBOARD: Stats reçues - Solde: " + stats.getSoldeCourant() + 
                          ", Recettes mois: " + stats.getRecettesMois() + 
                          ", Dépenses mois: " + stats.getDepensesMois());

        // Mettre à jour les KPIs
        updateKPIs(stats);

        // Mettre à jour le graphique d'évolution
        updateEvolutionChart(year);

        // Vérifier et afficher les alertes
        checkAlerts(stats);
    }

    private void updateKPIs(DashboardStats stats) {
        // Solde actuel
        soldeActuelLabel.setText(formatter.format(stats.getSoldeCourant()) + " DH");
        soldeActuelLabel.setStyle(stats.getSoldeCourant() < 0 
            ? "-fx-text-fill: #e74c3c; -fx-font-size: 28px; -fx-font-weight: bold;" 
            : "-fx-text-fill: #27ae60; -fx-font-size: 28px; -fx-font-weight: bold;");

        // Recettes du mois
        recettesMoisLabel.setText(formatter.format(stats.getRecettesMois()) + " DH");
        recettesMoisSubtitle.setText(stats.getMoisCourant() + " " + stats.getAnneeCourante());

        // Dépenses du mois
        depensesMoisLabel.setText(formatter.format(stats.getDepensesMois()) + " DH");
        depensesMoisSubtitle.setText(stats.getNombreOperationsMois() + " opération(s)");
        depensesExpMoisLabel.setText(formatter.format(stats.getDepensesExpMois()) + " DH");
        depensesInvMoisLabel.setText(formatter.format(stats.getDepensesInvMois()) + " DH");

        // Solde initial (utilisé pour le calcul annuel)
        double soldeInitial = stats.getSoldeInitial();
        soldeInitialLabel.setText(formatter.format(soldeInitial) + " DH");
        soldeInitialLabel.setStyle(soldeInitial < 0
            ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
            : "-fx-text-fill: #2980b9; -fx-font-weight: bold;");

        // Statistiques annuelles
        recettesAnneeLabel.setText(formatter.format(stats.getRecettesAnnee()) + " DH");
        depensesAnneeLabel.setText(formatter.format(stats.getDepensesAnnee()) + " DH");
        depensesExpAnneeLabel.setText(formatter.format(stats.getDepensesExpAnnee()) + " DH");
        depensesInvAnneeLabel.setText(formatter.format(stats.getDepensesInvAnnee()) + " DH");
        
        double balanceAnnee = stats.getBalanceAnnee();
        balanceAnneeLabel.setText(formatter.format(balanceAnnee) + " DH");
        balanceAnneeLabel.setStyle(balanceAnnee < 0 
            ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 16px;" 
            : "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
    }

    private void updateEvolutionChart(int year) {
        evolutionChart.getData().clear();

        Map<String, Double> evolution = dashboardDAO.getEvolutionSoldeAnnuelle(year);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Solde");

        String[] moisFr = {"JAN", "FEV", "MAR", "AVR", "MAI", "JUN", 
                          "JUL", "AOU", "SEP", "OCT", "NOV", "DEC"};
        String[] moisFull = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                            "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};

        for (int i = 0; i < moisFull.length; i++) {
            Double solde = evolution.get(moisFull[i]);
            if (solde != null) {
                series.getData().add(new XYChart.Data<>(moisFr[i], solde));
            }
        }

        evolutionChart.getData().add(series);
        
        // Styling du graphique
        evolutionChart.setCreateSymbols(true);
        evolutionChart.lookup(".chart-series-line").setStyle("-fx-stroke: #3498db; -fx-stroke-width: 3px;");
    }

    private void checkAlerts(DashboardStats stats) {
        final String warn = "⚠"; // plain warning symbol to avoid missing glyph boxes
        final String info = "ℹ";
        StringBuilder alerts = new StringBuilder();

        // Alerte solde négatif
        if (stats.getSoldeCourant() < 0) {
            alerts.append(warn).append(" ATTENTION : Solde négatif (")
                  .append(formatter.format(stats.getSoldeCourant()))
                  .append(" DH)\n");
        }

        // Alerte déficit mensuel
        if (stats.getBalanceMois() < 0) {
            alerts.append(warn).append(" Déficit ce mois : ")
                  .append(formatter.format(Math.abs(stats.getBalanceMois())))
                  .append(" DH\n");
        }

        // Alerte dépenses supérieures aux recettes annuelles
        if (stats.getBalanceAnnee() < 0) {
            alerts.append(warn).append(" Déficit annuel : ")
                  .append(formatter.format(Math.abs(stats.getBalanceAnnee())))
                  .append(" DH\n");
        }

        // Alerte solde faible (< 10000)
        if (stats.getSoldeCourant() > 0 && stats.getSoldeCourant() < 10000) {
            alerts.append(info).append(" Solde faible : Envisager de limiter les dépenses\n");
        }

        // Afficher ou masquer le conteneur d'alertes
        if (alerts.length() > 0) {
            alertLabel.setText(alerts.toString().trim());
            alertContainer.setVisible(true);
            alertContainer.setManaged(true);
        } else {
            alertContainer.setVisible(false);
            alertContainer.setManaged(false);
        }
    }

    private String toFrenchMonth(LocalDate date) {
        switch (date.getMonth()) {
            case JANUARY: return "JANVIER";
            case FEBRUARY: return "FEVRIER";
            case MARCH: return "MARS";
            case APRIL: return "AVRIL";
            case MAY: return "MAI";
            case JUNE: return "JUIN";
            case JULY: return "JUILLET";
            case AUGUST: return "AOUT";
            case SEPTEMBER: return "SEPTEMBRE";
            case OCTOBER: return "OCTOBRE";
            case NOVEMBER: return "NOVEMBRE";
            case DECEMBER: return "DECEMBRE";
            default: return "";
        }
    }
}
