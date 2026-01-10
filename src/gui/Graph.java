package gui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import wrapper.DataType.VehicleData;
import wrapper.SimulationWrapper;

import java.util.List;

public class Graph {
    private AreaChart<Number, Number> SpeedChart;
    private BarChart<String, Number> TravelTimeChart;
    private BarChart<String, Number> DensityChart;
    

    private SimulationWrapper sim;

    // --- Chart state  ---
    private final XYChart.Series<Number, Number> avgSpeedSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> travelTimeSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> densitySeries = new XYChart.Series<>();

    public void setSimulation(SimulationWrapper sim) {
        this.sim = sim;
    }

    //Speed Chart
    public void SpeedChart(AreaChart<Number, Number> SpeedChart) {
        if (SpeedChart == null) {
            this.SpeedChart = null;
            return;
        }
        this.SpeedChart = SpeedChart;
        setupSpeedChart();
    }

    private void setupSpeedChart() {
        if (SpeedChart == null) return;

        // Configure once (avoid re-adding series)
        if (!SpeedChart.getData().contains(avgSpeedSeries)) {
            SpeedChart.setAnimated(false);
            SpeedChart.setLegendVisible(true);
            SpeedChart.setTitle("Average Vehicle Speed");

            avgSpeedSeries.setName("speed (m/s)");
            SpeedChart.getData().add(avgSpeedSeries);
        }
    }

    /** Call this from App's AnimationTimer (JavaFX UI thread). */
    public void updateSpeedCharts(AreaChart<Number, Number> SpeedChart) {
        if (SpeedChart == null || sim == null) return;

        // Ensure chart is configured even if chart was swapped/reset.
        setupSpeedChart();

        // Use SimulationWrapper's average speed in SimulationWrapper
        double avgSpeed = sim.getVehicleAverageSpeed(0);
        // Time in seconds by getTime in SimulationWrapper
        double tSec = sim.getTime(0);

        avgSpeedSeries.getData().add(new XYChart.Data<>(tSec, avgSpeed));

        /* optional:
        // Keep chart bounded
        if (avgSpeedSeries.getData().size() > MAX_POINTS) {
            avgSpeedSeries.getData().remove(0, avgSpeedSeries.getData().size() - MAX_POINTS);
        }
            */
    }

    //Travel Time Chart
    public void TravelTimeChart(BarChart<String, Number> TravelTimeChart) {
        if (TravelTimeChart == null) {
            this.TravelTimeChart = null;
            return;
        }
        this.TravelTimeChart = TravelTimeChart;
        setupTravelTimeChart();
    }
    private void setupTravelTimeChart() {
        if (TravelTimeChart == null) return;

        // Configure once (avoid re-adding series)
        if (!TravelTimeChart.getData().contains(travelTimeSeries)) {
            TravelTimeChart.setAnimated(false);
            TravelTimeChart.setLegendVisible(true);
            TravelTimeChart.setTitle("Vehicle Travel Time");

            travelTimeSeries.setName("travel time (s)");
            TravelTimeChart.getData().add(travelTimeSeries);
        }
    }

    public void updateTravelTimeChart(BarChart<String, Number> TravelTimeChart) {
        if (TravelTimeChart == null || sim == null) return;

        setupTravelTimeChart();
        travelTimeSeries.getData().clear();
        
        // Get completed travel times from simulation
        List<Double> travelTimes = sim.getCompletedTravelTimes();
        
        // Count vehicles in each bucket
        int count0 = 0, count1 = 0, count2 = 0, count3 = 0, count4 = 0, count5 = 0;
        
        for (int i = 0; i < travelTimes.size(); i++) {
            double time = travelTimes.get(i);
            if (time < 60){ 
                count0++;
            }
            else if (time < 120) {
                count1++;
            }
            else if (time < 180) {
                count2++;
            }
            else if (time < 240) {
                count3++;
            }
            else if (time < 300) {
                count4++;
            }
            else {
                count5++;
            }
        }
        
        // Add data to chart
        travelTimeSeries.getData().add(new XYChart.Data<>("0-60", count0));
        travelTimeSeries.getData().add(new XYChart.Data<>("60-120", count1));
        travelTimeSeries.getData().add(new XYChart.Data<>("120-180", count2));
        travelTimeSeries.getData().add(new XYChart.Data<>("180-240", count3));
        travelTimeSeries.getData().add(new XYChart.Data<>("240-300", count4));
        travelTimeSeries.getData().add(new XYChart.Data<>("300+", count5));
    }
    //Density Chart
    public void DensityChart(BarChart<String, Number> DensityChart) {
        if (DensityChart == null) {
            this.DensityChart = null;
            return;
        }
        this.DensityChart = DensityChart;
        setupDensityChart();
    }
    private void setupDensityChart() {
        if (DensityChart == null) return;
        // Configure once (avoid re-adding series)
        if (!DensityChart.getData().contains(densitySeries)) {
            DensityChart.setAnimated(false);
            DensityChart.setLegendVisible(true);
            DensityChart.setTitle("Vehicle Density per Edge");

            densitySeries.setName("Vehicles");
            DensityChart.getData().add(densitySeries);
        }
    }

    /** 
     * Update the density bar chart showing vehicle count per edge.
     * Y-axis: Number of vehicles
     * X-axis: List of all edges
     * Call this from App's AnimationTimer (JavaFX UI thread).
     */
    public void updateDensityChart(BarChart<String, Number> DensityChart) {
        if (this.DensityChart == null || sim == null) return;

        setupDensityChart();
        densitySeries.getData().clear();

        // Get all edge IDs from simulation
        List<String> edgeIDs = sim.getEdgeIDsList();
        
        // Add data to chart (only edges with vehicles)
        for (String edgeId : edgeIDs) {
            int density = sim.getEdgeDensity(edgeId);
            if (density > 0) {
                densitySeries.getData().add(new XYChart.Data<>(edgeId, density));
            }
        }
    }

}
