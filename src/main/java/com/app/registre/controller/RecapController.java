package com.app.registre.controller;

import com.app.registre.dao.RecapDAO;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import com.app.registre.util.DialogUtils;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

import java.util.Map;

public class RecapController {

    @FXML private Text totalOpsText;
    @FXML private Text totalRecettesText;
    @FXML private Text totalSurRamText;
    @FXML private Text totalSurEngText;
    @FXML private Text totalDepensesText;
    @FXML private Text soldeText;
    @FXML private PieChart moisPieChart;
    @FXML private TableView<?> recapTable;

    private RecapDAO recapDAO;

    public void initialize() {
        recapDAO = new RecapDAO();
        refreshData();
    }

    @FXML
    private void refreshData() {
        updateStatistics();
        updateCharts();
    }

    private void updateStatistics() {
        int totalOps = recapDAO.getTotalOperations();
        double totalRecettes = recapDAO.getTotalRecettes();
        double totalDepenses = recapDAO.getTotalDepenses();
        double totalSurRam = recapDAO.getTotalSurRam();
        double totalSurEng = recapDAO.getTotalSurEng();
        // Use the latest recorded solde if available (reflects running balance), fallback to computed
        double dernierSolde = recapDAO.getDernierSolde();

        totalOpsText.setText(String.valueOf(totalOps));
        totalRecettesText.setText(String.format("%,.2f", totalRecettes));
        totalSurRamText.setText(String.format("%,.2f", totalSurRam));
        totalSurEngText.setText(String.format("%,.2f", totalSurEng));
        totalDepensesText.setText(String.format("%,.2f", totalDepenses));
        soldeText.setText(String.format("%,.2f", dernierSolde));
    }

    private void updateCharts() {
        // Show recette distribution by month on the pie chart (depense could be added similarly)
        Map<String, Double> recettesParMois = recapDAO.getRecetteParMois();

        moisPieChart.getData().clear();
        for (Map.Entry<String, Double> entry : recettesParMois.entrySet()) {
            double value = entry.getValue() == null ? 0.0 : entry.getValue();
            PieChart.Data data = new PieChart.Data(entry.getKey(), value);
            moisPieChart.getData().add(data);
        }
    }

    @FXML
    private void generateReport() {
        showInfo("Génération de rapport à implémenter");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        DialogUtils.initOwner(alert, recapTable);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}