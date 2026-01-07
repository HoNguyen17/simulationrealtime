package gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.AreaChart;

import javafx.scene.chart.XYChart;
import javafx.scene.control.TableView;
import wrapper.DataType.VehicleData;
import wrapper.SimulationWrapper;

import java.util.List;

public class Stats {
    private AreaChart<Number, Number> staSimChart;
    private SimulationWrapper sim;

    // view hotspot table
    private TableView<HotspotData> hotspotTable;

    // --- Chart state (Average speed) ---
    private final XYChart.Series<Number, Number> avgSpeedSeries = new XYChart.Series<>();
    private long chartStartNanos = 0; // timestamp of first data point
    private static final int MAX_POINTS = 20; // keep last N points

    public void setSimulation(SimulationWrapper sim) {
        this.sim = sim;
    }

    /** Wire the FXML AreaChart into this Stats instance. Safe to call multiple times. */
    @SuppressWarnings("unchecked")
    public void setStaSimChart(AreaChart<?, ?> staSim) {
        if (staSim == null) {
            this.staSimChart = null;
            return;
        }
        this.staSimChart = (AreaChart<Number, Number>) staSim;
        setupStaSimChartIfPresent();
    }

    private void setupStaSimChartIfPresent() {
        if (staSimChart == null) return;

        // Configure once (avoid re-adding series)
        if (!staSimChart.getData().contains(avgSpeedSeries)) {
            staSimChart.setAnimated(false);
            staSimChart.setLegendVisible(true);
            staSimChart.setTitle("Average Vehicle Speed");

            avgSpeedSeries.setName("speed (m/s)");
            staSimChart.getData().add(avgSpeedSeries);
        }
    }

    public void setHotspotTable(TableView<?> table) {
        this.hotspotTable = (TableView<HotspotData>) table;
    }

    /** Call this from App's AnimationTimer (JavaFX UI thread). */
    public void updateCharts(List<VehicleData> vehicles, long nowNanos) {
        if (staSimChart == null || sim == null) return;

        // Ensure chart is configured even if chart was swapped/reset.
        setupStaSimChartIfPresent();

        // Use SimulationWrapper's average speed
        double avgSpeed;
        List<String> ids = sim.getVehicleIDsList();
        if (ids == null || ids.isEmpty()) {
            avgSpeed = 0.0;
        } else {
            avgSpeed = sim.getVehicleAverageSpeed(0);
            if (Double.isNaN(avgSpeed) || Double.isInfinite(avgSpeed)) {
                avgSpeed = 0.0;
            }
        }

        double tSec;
        if (chartStartNanos == 0) {
            chartStartNanos = nowNanos;
            tSec = 0.0;
            avgSpeedSeries.getData().clear();
        } else {
            tSec = (nowNanos - chartStartNanos) / 1_000_000_000.0;
        }

        avgSpeedSeries.getData().add(new XYChart.Data<>(tSec, avgSpeed));

        // update the hotspots every 1 second
        if (Math.round(tSec) % 1 == 0 && hotspotTable != null) {
            List<String> hotEdges = sim.getTopCongestedEdges();

            // create the data list for the table
            javafx.collections.ObservableList<HotspotData> data = javafx.collections.FXCollections.observableArrayList();

            for (String edgeId : hotEdges) {
                // we can fetch actual speed from sim here if we have a method for it
                data.add(new HotspotData(edgeId, 0.0));
            }

            hotspotTable.setItems(data);

            System.out.println("UI Updated with Hotspots: " + hotEdges);
        }

        /* optional:
        // Keep chart bounded
        if (avgSpeedSeries.getData().size() > MAX_POINTS) {
            avgSpeedSeries.getData().remove(0, avgSpeedSeries.getData().size() - MAX_POINTS);
        }
            */
    }
}