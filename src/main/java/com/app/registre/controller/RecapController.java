package com.app.registre.controller;

import com.app.registre.dao.RecapDAO;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import com.app.registre.util.DialogUtils;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

import java.util.Map;
import com.app.registre.dao.OperationDAO;

public class RecapController {

    @FXML private Text totalOpsText;
    @FXML private Text totalRecettesText;
    @FXML private Text totalSurRamText;
    @FXML private Text totalSurEngText;
    @FXML private Text totalDepensesText;
    @FXML private Text soldeText;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private BarChart<String, Number> soldeBarChart;
    @FXML private CategoryAxis monthCategoryAxis;
    @FXML private NumberAxis valueNumberAxis;
    @FXML private NumberAxis soldeNumberAxis;
    @FXML private TilePane monthCirclesPane;
    @FXML private javafx.scene.control.ComboBox<String> yearComboBox;
    @FXML private TableView<?> recapTable;

    private RecapDAO recapDAO;

    public void initialize() {
        recapDAO = new RecapDAO();
        configureCharts();
        setupYearSelector();
        refreshData();
    }

    private void configureCharts() {
        try {
            if (soldeBarChart != null) {
                soldeBarChart.setVisible(true);
                soldeBarChart.setMouseTransparent(false);
                soldeBarChart.setStyle("-fx-background-color: transparent;");
                soldeBarChart.setAnimated(false);
                soldeBarChart.setLegendVisible(true);
            }
            if (monthlyBarChart != null) {
                monthlyBarChart.setAnimated(false);
                monthlyBarChart.setLegendVisible(true);
            }
        } catch (Exception ignored) {
        }
    }

    private void setupYearSelector() {
        // populate years based on existing data; allow user to select the year for the chart
        java.util.Set<Integer> years = new java.util.TreeSet<>((a,b)->b-a); // descending
        try {
            java.sql.Connection conn = com.app.registre.dao.Database.getInstance().getConnection();
            // Extract years only from date_emission, normalizing integer epoch-ms values to localtime
            String yearExpr = "strftime('%Y', CASE WHEN typeof(date_emission)='integer' THEN datetime(date_emission/1000,'unixepoch','localtime') ELSE date_emission END)";
            String sql = "SELECT DISTINCT " + yearExpr + " AS y FROM operations WHERE date_emission IS NOT NULL ORDER BY y DESC";
            try (java.sql.Statement s = conn.createStatement(); java.sql.ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    String y = rs.getString("y");
                    if (y != null && !y.isBlank()) years.add(Integer.parseInt(y));
                }
            }
        } catch (Exception ignored) {}

        if (years.isEmpty()) {
            years.add(java.time.LocalDate.now().getYear());
        }

        for (Integer y : years) yearComboBox.getItems().add(y.toString());
        yearComboBox.setValue(yearComboBox.getItems().get(0));
        yearComboBox.valueProperty().addListener((o,ov,nv) -> updateCharts());
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
        // Build a combined chart for the selected year: recettes (bar), depenses (bar), solde cumulative (line)
        String yearStr = yearComboBox.getValue();
        int year = yearStr == null ? java.time.LocalDate.now().getYear() : Integer.parseInt(yearStr);

        // Clear old visualizations
        if (monthlyBarChart != null) monthlyBarChart.getData().clear();
        if (soldeBarChart != null) soldeBarChart.getData().clear();
        if (monthCirclesPane != null) monthCirclesPane.getChildren().clear();

        Map<Integer, Double> recettes = recapDAO.getRecetteParMoisForYear(year);
        Map<Integer, Double> depenses = recapDAO.getDepenseParMoisForYear(year);

        XYChart.Series<String, Number> recettesSeries = new XYChart.Series<>();
        recettesSeries.setName("Recettes");
        XYChart.Series<String, Number> depensesSeries = new XYChart.Series<>();
        depensesSeries.setName("Dépenses");

        XYChart.Series<String, Number> soldeDebutSeries = new XYChart.Series<>();
        soldeDebutSeries.setName("Solde départ");
        XYChart.Series<String, Number> soldeFinSeries = new XYChart.Series<>();
        soldeFinSeries.setName("Dernier solde");

        // We'll compute start balances using the same logic as MoisController (last solde of previous month)
        OperationDAO operationDAO = new OperationDAO();

        // Month name lookup for labels; we'll collect only months that actually have data
        java.util.List<String> months = java.util.Arrays.asList("JAN","FEV","MAR","AVR","MAI","JUN","JUI","AOU","SEP","OCT","NOV","DEC");
        java.util.List<String> visibleMonths = new java.util.ArrayList<>();
        java.util.Map<Integer, Double> startBalances = new java.util.HashMap<>();
        java.util.Map<Integer, Double> endBalances = new java.util.HashMap<>();

        double amountsMax = Double.NEGATIVE_INFINITY;
        double amountsMin = Double.POSITIVE_INFINITY;
        double soldeMax = Double.NEGATIVE_INFINITY;
        double soldeMin = Double.POSITIVE_INFINITY;

        for (int m = 1; m <= 12; m++) {
            String label = months.get(m - 1);
            double r = recettes.getOrDefault(m, 0.0);
            double d = depenses.getOrDefault(m, 0.0);

            // compute start as last solde of previous month, same logic as MoisController
            double start = 0.0;
            try {
                int prevMonth = m - 1;
                if (prevMonth >= 1) {
                    Double prev = operationDAO.getLastMontantForMonthYear(year, prevMonth);
                    if (prev == null) {
                        if (operationDAO.hasOperationsForMonthYear(year, prevMonth)) {
                            operationDAO.recomputeAllSoldes();
                            prev = operationDAO.getLastMontantForMonthYear(year, prevMonth);
                            if (prev == null) prev = 0.0;
                        } else {
                            prev = 0.0;
                        }
                    }
                    start = prev;
                } else {
                    start = 0.0; // January has no previous month in same year
                }
            } catch (Exception ignored) { start = 0.0; }

            // If an explicit 'Solde initial' exists for this month in operations, prefer it
            try {
                Double initial = operationDAO.getInitialSoldeForMonthYear(year, m);
                if (initial != null) {
                    start = initial;
                }
            } catch (Exception ignored) {}

            double end = start + (r - d);

            boolean hasOps = (Math.abs(r) > 1e-9) || (Math.abs(d) > 1e-9);

            // Record the start balance for this month (it is the last computed solde of previous month)
            startBalances.put(m, start);

            // Only add bars for months that actually have operations (recette or depense)
            if (hasOps) {
                recettesSeries.getData().add(new XYChart.Data<>(label, r));
                depensesSeries.getData().add(new XYChart.Data<>(label, d));
                soldeDebutSeries.getData().add(new XYChart.Data<>(label, start));
                soldeFinSeries.getData().add(new XYChart.Data<>(label, end));

                // track min/max separately for amounts and solde (only where data exists)
                amountsMax = Math.max(amountsMax, Math.max(r, d));
                amountsMin = Math.min(amountsMin, Math.min(r, d));
                soldeMax = Math.max(soldeMax, Math.max(start, end));
                soldeMin = Math.min(soldeMin, Math.min(start, end));
                // remember visible month for circle widget
                visibleMonths.add(label);
                // store sums so we can render per-month widgets later
            }

            // store end balance for this month (useful for reference)
            endBalances.put(m, end);
        }

        // For correctness we must not add the same Series instances to two different charts.
        // Keep recettes/dépenses on the main (left) BarChart and put solde début/fin on the right BarChart
        if (monthlyBarChart != null) {
            monthlyBarChart.getData().addAll(depensesSeries, recettesSeries);
        }
        // add separate solde series instances to the right-hand bar chart
        // Prepare number formatter (French) used by both charts and circle widgets
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("fr", "FR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        if (soldeBarChart != null) {
            XYChart.Series<String, Number> soldeDebutForSoldeChart = new XYChart.Series<>();
            soldeDebutForSoldeChart.setName(soldeDebutSeries.getName());
            XYChart.Series<String, Number> soldeFinForSoldeChart = new XYChart.Series<>();
            soldeFinForSoldeChart.setName(soldeFinSeries.getName());
            // copy data points into the new series instances
            for (XYChart.Data<String, Number> d : soldeDebutSeries.getData()) {
                soldeDebutForSoldeChart.getData().add(new XYChart.Data<>(d.getXValue(), d.getYValue()));
            }
            for (XYChart.Data<String, Number> d : soldeFinSeries.getData()) {
                soldeFinForSoldeChart.getData().add(new XYChart.Data<>(d.getXValue(), d.getYValue()));
            }
            soldeBarChart.getData().addAll(soldeDebutForSoldeChart, soldeFinForSoldeChart);
            // hide X axis of soldeBarChart (shared with main months axis)
            if (soldeBarChart.getXAxis() != null) soldeBarChart.getXAxis().setVisible(false);
            soldeBarChart.setAnimated(false);
            soldeBarChart.setLegendVisible(true);
            // configure solde axis to right side if accessible
            try {
                if (soldeNumberAxis != null) {
                    soldeNumberAxis.setSide(Side.RIGHT);
                    soldeNumberAxis.setLabel("Solde");
                }
            } catch (Exception ignored) {}
            // Format axes as currency (French locale) and attach tooltips + colors (uses nf declared above)
            if (valueNumberAxis != null) {
                valueNumberAxis.setTickLabelFormatter(new StringConverter<Number>() {
                    @Override public String toString(Number object) { return nf.format(object.doubleValue()); }
                    @Override public Number fromString(String string) { try { return nf.parse(string); } catch (Exception e) { return 0; } }
                });
            }

            // apply colors and tooltips for left chart series
            applySeriesStyling(depensesSeries, "#e74c3c", nf);
            applySeriesStyling(recettesSeries, "#27ae60", nf);

                // apply colors and tooltips for right chart series
            applySeriesStyling(soldeDebutForSoldeChart, "#e67e22", nf);
            applySeriesStyling(soldeFinForSoldeChart, "#3498db", nf);
        }

                // Render month circles UI: for each month create a circular widget and size cards to fit 4 per row
                        try {
                            if (monthCirclesPane != null) {
                                final NumberFormat fmt = nf;
                                // Allow prefColumns to drive the columns count; default to 4 when not set
                                final int columns = monthCirclesPane.getPrefColumns() > 0 ? monthCirclesPane.getPrefColumns() : 4;
                                monthCirclesPane.setPrefColumns(columns);
                                // Default minimum card width (can be overridden by setting monthCirclesPane.getProperties().put("minCardWidth", <Number/String>) )
                                double defaultMin = 140.0;
                                // Adjust card widths when the pane resizes so `columns` cards fit per row
                                monthCirclesPane.widthProperty().addListener((obs, oldW, newW) -> {
                                    double w = newW.doubleValue();
                                    javafx.geometry.Insets in = monthCirclesPane.getInsets();
                                    double insets = (in == null) ? 0 : (in.getLeft() + in.getRight());
                                    double totalGap = monthCirclesPane.getHgap() * (columns - 1);
                                    double available = Math.max(0, w - insets - totalGap);
                                    // determine min width from TilePane properties if provided
                                    double minWidth = defaultMin;
                                    try {
                                        Object p = monthCirclesPane.getProperties().get("minCardWidth");
                                        if (p instanceof Number) minWidth = ((Number) p).doubleValue();
                                        else if (p instanceof String) minWidth = Double.parseDouble((String) p);
                                    } catch (Exception ignore) {}
                                    double cardW = Math.max(minWidth, Math.floor(available / columns));
                                    final double finalCardW = cardW;
                                    Platform.runLater(() -> monthCirclesPane.getChildren().forEach(n -> { if (n instanceof VBox) ((VBox)n).setPrefWidth(finalCardW); }));
                                });

                                for (int i = 1; i <= 12; i++) {
                            String label = months.get(i-1);
                                    double r = recettes.getOrDefault(i, 0.0);
                            double d = depenses.getOrDefault(i, 0.0);
                            double start = startBalances.getOrDefault(i, 0.0);
                            double end = start + (r - d);
                                    VBox card = new VBox(6);
                            card.setAlignment(Pos.CENTER);
                                    // don't set a fixed pref width here; the width listener will size cards
                            // keep an inline style fallback but also expose CSS classes
                            card.setStyle("-fx-background-color: white; -fx-padding:12; -fx-border-color: #eee; -fx-border-radius:6; -fx-background-radius:6;");
                            card.getStyleClass().add("month-card");

                            Label monthLabel = new Label(label);
                            monthLabel.setFont(Font.font(14));

                            StackPane circleHolder = new StackPane();
                            Circle c = new Circle(70);
                            c.setStyle("-fx-fill: linear-gradient(#FFFFFF, #f0f6ff); -fx-stroke: #d0d7e6; -fx-stroke-width:1;");
                            VBox inside = new VBox(4);
                            inside.setAlignment(Pos.CENTER);

                            // Prefer explicit 'Solde initial' value when present for this month
                            String prevText;
                            double prevValue = start;
                            boolean hasExplicit = false;
                            try {
                                Double explicit = operationDAO.getInitialSoldeForMonthYear(year, i);
                                if (explicit != null) {
                                    prevValue = explicit;
                                    hasExplicit = true;
                                }
                            } catch (Exception ex) {
                                // ignore and fall back to computed start
                            }
                            if (hasExplicit) prevText = "Solde initiale: " + fmt.format(prevValue);
                            else prevText = "Solde préc.: " + fmt.format(prevValue);

                            Label prevLabel = new Label(prevText);
                            Label recLabel = new Label("Recette: " + fmt.format(r));
                            Label depLabel = new Label("Dépense: " + fmt.format(d));
                            Label endLabel = new Label("Solde fin: " + fmt.format(end));
                            prevLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size:11;");
                            recLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size:12; -fx-font-weight: bold;");
                            depLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size:12; -fx-font-weight: bold;");
                            endLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size:12; -fx-font-weight: bold;");
                            inside.getChildren().addAll(prevLabel, recLabel, depLabel, endLabel);
                            circleHolder.getChildren().addAll(c, inside);

                            card.getChildren().addAll(monthLabel, circleHolder);


                            // mark card when an explicit initial solde was used
                            if (hasExplicit) card.getStyleClass().add("initial");

                            // ensure loop variables are effectively final for use in lambdas
                            final int monthIndex = i;
                            final String monthLabelStr = label;

                            // Make card clickable: open the Mois view preselected for this month/year
                            card.setOnMouseClicked(evt -> {
                                try {
                                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/mois.fxml"));
                                    javafx.scene.Parent view = loader.load();
                                    Object ctrl = loader.getController();
                                    if (ctrl instanceof com.app.registre.controller.MoisController) {
                                        com.app.registre.controller.MoisController mc = (com.app.registre.controller.MoisController) ctrl;
                                        mc.openFor(year, monthIndex);
                                    }
                                    // Instead of opening a new window, replace the application's main center content
                                    // by finding the nearest BorderPane ancestor and setting its center to the loaded view.
                                    javafx.scene.Parent parent = monthCirclesPane;
                                    while (parent != null && !(parent instanceof javafx.scene.layout.BorderPane)) {
                                        parent = parent.getParent();
                                    }
                                    // Prefer replacing the application's main `contentArea` (StackPane) if available
                                    boolean replaced = false;
                                    try {
                                        javafx.scene.Scene scene = monthCirclesPane.getScene();
                                        if (scene != null) {
                                            javafx.scene.Node root = scene.getRoot();
                                            if (root != null) {
                                                javafx.scene.Node content = root.lookup("#contentArea");
                                                if (content instanceof javafx.scene.layout.StackPane) {
                                                    javafx.scene.layout.StackPane ca = (javafx.scene.layout.StackPane) content;
                                                    ca.getChildren().setAll(view);
                                                    replaced = true;
                                                    // set sidebar button active if sidebar exists
                                                    javafx.scene.Node sidebar = root.lookup("#sidebar");
                                                    if (sidebar instanceof javafx.scene.Parent) {
                                                        java.util.List<javafx.scene.control.Button> buttons = new java.util.ArrayList<>();
                                                        java.util.function.Consumer<javafx.scene.Node> collect = new java.util.function.Consumer<javafx.scene.Node>() {
                                                            @Override public void accept(javafx.scene.Node n) {
                                                                if (n instanceof javafx.scene.control.Button) buttons.add((javafx.scene.control.Button) n);
                                                                if (n instanceof javafx.scene.Parent) {
                                                                    for (javafx.scene.Node c : ((javafx.scene.Parent) n).getChildrenUnmodifiable()) accept(c);
                                                                }
                                                            }
                                                        };
                                                        collect.accept(sidebar);
                                                        for (javafx.scene.control.Button b : buttons) b.getStyleClass().remove("active");
                                                        for (javafx.scene.control.Button b : buttons) {
                                                            String t = b.getText() == null ? "" : b.getText().trim();
                                                            if (t.equalsIgnoreCase("Par Mois") || t.startsWith("Par Mois")) {
                                                                b.getStyleClass().add("active");
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception ignore) {}
                                    if (!replaced) {
                                        // fallback: open in new stage if we couldn't find the main BorderPane
                                        javafx.stage.Stage stage = new javafx.stage.Stage();
                                        stage.setTitle(monthLabelStr + " " + year);
                                        stage.initOwner(monthCirclesPane.getScene() == null ? null : monthCirclesPane.getScene().getWindow());
                                        stage.setScene(new javafx.scene.Scene(view));
                                        stage.show();
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    try {
                                        Alert a = new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la vue Mois: " + ex.getMessage(), javafx.scene.control.ButtonType.OK);
                                        DialogUtils.initOwner(a, monthCirclesPane);
                                        a.showAndWait();
                                    } catch (Exception ignore) {}
                                }
                            });

                            // add operations count badge
                            try {
                                int cnt = operationDAO.getCountForMonthYear(year, monthIndex);
                                Label cntLabel = new Label(cnt + " ops");
                                cntLabel.getStyleClass().add("month-count");
                                card.getChildren().add(cntLabel);
                            } catch (Exception ex) {
                                // ignore count failures
                            }

                            monthCirclesPane.getChildren().add(card);
                        }
                    }
                } catch (Exception ignored) {}

        // Choose which months to display on the X axis: only months that had operations
        try {
            java.util.List<String> finalMonths = visibleMonths.isEmpty() ? months : visibleMonths;
            if (monthCategoryAxis != null) {
                monthCategoryAxis.setCategories(javafx.collections.FXCollections.observableArrayList(finalMonths));
            }
            if (soldeBarChart != null && soldeBarChart.getXAxis() instanceof CategoryAxis) {
                CategoryAxis solX = (CategoryAxis) soldeBarChart.getXAxis();
                if (monthCategoryAxis != null) {
                    solX.setCategories(monthCategoryAxis.getCategories());
                } else {
                    solX.setCategories(javafx.collections.FXCollections.observableArrayList(finalMonths));
                }
                solX.setVisible(false);
            }
            // tune gaps so grouped bars appear visually together
            if (monthlyBarChart != null) {
                monthlyBarChart.setCategoryGap(20);
                monthlyBarChart.setBarGap(6);
            }
            if (soldeBarChart != null) {
                soldeBarChart.setCategoryGap(20);
                soldeBarChart.setBarGap(6);
            }
        } catch (Exception ignored) {}

        // Configure left axis for amounts (recettes/dépenses)
        if (valueNumberAxis != null) {
            if (!Double.isFinite(amountsMax) || !Double.isFinite(amountsMin)) {
                valueNumberAxis.setAutoRanging(true);
            } else {
                double margin = Math.max(1.0, Math.abs(amountsMax - amountsMin) * 0.1);
                valueNumberAxis.setAutoRanging(false);
                valueNumberAxis.setLowerBound(Math.min(0.0, amountsMin - margin));
                valueNumberAxis.setUpperBound(Math.max(0.0, amountsMax + margin));
                valueNumberAxis.setTickUnit((valueNumberAxis.getUpperBound() - valueNumberAxis.getLowerBound()) / 6.0);
            }
        }

        // Configure right axis for soldes (solde départ / dernier solde)
        try {
            if (soldeNumberAxis != null) {
                if (!Double.isFinite(soldeMax) || !Double.isFinite(soldeMin)) {
                    soldeNumberAxis.setAutoRanging(true);
                } else {
                    double margin = Math.max(100.0, Math.abs(soldeMax - soldeMin) * 0.1);
                    soldeNumberAxis.setAutoRanging(false);
                    soldeNumberAxis.setLowerBound(Math.min(0.0, soldeMin - margin));
                    soldeNumberAxis.setUpperBound(Math.max(0.0, soldeMax + margin));
                    soldeNumberAxis.setTickUnit((soldeNumberAxis.getUpperBound() - soldeNumberAxis.getLowerBound()) / 6.0);
                }
            }
        } catch (Exception ignored) {}
    }

    private void applySeriesStyling(XYChart.Series<String, Number> series, String colorHex, NumberFormat nf) {
        for (XYChart.Data<String, Number> d : series.getData()) {
            // ensure tooltip when node is available
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + colorHex + ";");
                    Tooltip t = new Tooltip(series.getName() + "\n" + d.getXValue() + ": " + nf.format(d.getYValue().doubleValue()));
                    Tooltip.install(newNode, t);

                    // add a numeric label above the bar
                    try {
                        Text label = new Text(nf.format(d.getYValue().doubleValue()));
                        label.getStyleClass().add("bar-value-label");
                        label.setMouseTransparent(true);

                        // add label to the same parent group as the bar node so it overlays on plot
                        if (newNode.getParent() instanceof Group) {
                            Group parent = (Group) newNode.getParent();
                            // ensure label added on JavaFX thread
                            Platform.runLater(() -> {
                                parent.getChildren().add(label);
                                // position label whenever bar bounds change
                                newNode.boundsInParentProperty().addListener((o,ob,nob) -> {
                                    positionLabelAbove(label, nob);
                                });
                                // initial position
                                Bounds b = newNode.getBoundsInParent();
                                positionLabelAbove(label, b);
                            });
                        }
                    } catch (Exception ignored) {}
                }
            });

            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill: " + colorHex + ";");
                Tooltip.install(d.getNode(), new Tooltip(series.getName() + "\n" + d.getXValue() + ": " + nf.format(d.getYValue().doubleValue())));
                // also add label for already-created nodes
                try {
                    Text label = new Text(nf.format(d.getYValue().doubleValue()));
                    label.getStyleClass().add("bar-value-label");
                    label.setMouseTransparent(true);
                    if (d.getNode().getParent() instanceof Group) {
                        Group parent = (Group) d.getNode().getParent();
                        parent.getChildren().add(label);
                        Bounds b = d.getNode().getBoundsInParent();
                        positionLabelAbove(label, b);
                        d.getNode().boundsInParentProperty().addListener((o,ob,nob) -> positionLabelAbove(label, nob));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void positionLabelAbove(Text label, Bounds b) {
        try {
            double centerX = b.getMinX() + b.getWidth() / 2.0;
            double x = centerX - label.getLayoutBounds().getWidth() / 2.0;
            double y = b.getMinY() - 4.0; // small gap above bar
            label.setLayoutX(x);
            label.setLayoutY(y);
        } catch (Exception ignored) {}
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