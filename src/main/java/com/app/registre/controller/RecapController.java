package com.app.registre.controller;

import com.app.registre.dao.RecapDAO;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

import java.util.Map;

public class RecapController {

    @FXML private Text totalOpsText;
    @FXML private Text totalRecettesText;
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
        double solde = totalRecettes - totalDepenses;

        totalOpsText.setText(String.valueOf(totalOps));
        totalRecettesText.setText(String.format("%,.2f", totalRecettes));
        totalDepensesText.setText(String.format("%,.2f", totalDepenses));
        soldeText.setText(String.format("%,.2f", solde));
    }

    private void updateCharts() {
        Map<String, Double> totauxParMois = recapDAO.getTotauxParMois();

        moisPieChart.getData().clear();
        for (Map.Entry<String, Double> entry : totauxParMois.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue());
            moisPieChart.getData().add(data);
        }
    }

    @FXML
    private void generateReport() {
        showInfo("Génération de rapport à implémenter");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}