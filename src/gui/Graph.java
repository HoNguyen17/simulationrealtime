package gui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;

import wrapper.DataType.VehicleData;
import wrapper.SimulationWrapper;

import java.util.List;

public class Graph {
    private AreaChart<Number, Number> SpeedChart;
    private BarChart<String, Number> TravelTimeChart;
    private BarChart<String, Number> DensityChart;

    private SimulationWrapper sim;

    private final XYChart.Series<Number, Number> averageSpeedSeries = new XYChart.Series<>();
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
        if (!SpeedChart.getData().contains(averageSpeedSeries)) {
            SpeedChart.setAnimated(false);
            SpeedChart.setLegendVisible(true);
            SpeedChart.setTitle("Average Vehicle Speed");

            averageSpeedSeries.setName("speed (m/s)");
            SpeedChart.getData().add(averageSpeedSeries);
        }
    }

    public void updateSpeedCharts(AreaChart<Number, Number> SpeedChart) {
        if (SpeedChart == null || sim == null) return;

        setupSpeedChart();

        double averageSpeed = sim.getVehicleAverageSpeed(0);
        double tSec = sim.getTime(0);

        averageSpeedSeries.getData().add(new XYChart.Data<>(tSec, averageSpeed));
    }
    public AreaChart<Number, Number> makeSpeedChartCopy() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(SpeedChart.getXAxis().getLabel());
        yAxis.setLabel(SpeedChart.getYAxis().getLabel());

        XYChart.Series<Number, Number> seriesCopy = new XYChart.Series<>();
        seriesCopy.setName("speed (m/s)");
        for (XYChart.Data<Number, Number> data : averageSpeedSeries.getData()) {
                seriesCopy.getData().add(new XYChart.Data<>(data.getXValue(), data.getYValue()));
        }

        AreaChart<Number, Number> copy = new AreaChart<>(xAxis, yAxis);
        copy.setAnimated(false);
        copy.setLegendVisible(true);
        copy.setTitle("Average Vehicle Speed");
        copy.getData().add(seriesCopy);
        return copy;
    } 

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
        
        List<Double> travelTimes = sim.getCompletedTravelTimes();
        
        int countTravel0 = 0;
        int countTravel1 = 0;
        int countTravel2 = 0;
        int countTravel3 = 0;
        int countTravel4 = 0;
        int countTravel5 = 0;
        
        // Categorize travel times 
        for (int i = 0; i < travelTimes.size(); i++) {
            double time = travelTimes.get(i);
            if (time < 60) countTravel0++;
            else if (time < 120) countTravel1++;
            else if (time < 180) countTravel2++;
            else if (time < 240) countTravel3++;
            else if (time < 300) countTravel4++;
            else countTravel5++;
        }
        travelTimeSeries.getData().add(new XYChart.Data<>("0-60", countTravel0));
        travelTimeSeries.getData().add(new XYChart.Data<>("60-120", countTravel1));
        travelTimeSeries.getData().add(new XYChart.Data<>("120-180", countTravel2));
        travelTimeSeries.getData().add(new XYChart.Data<>("180-240", countTravel3));
        travelTimeSeries.getData().add(new XYChart.Data<>("240-300", countTravel4));
        travelTimeSeries.getData().add(new XYChart.Data<>("300+", countTravel5));
    }
    public BarChart<String, Number> makeTravelTimeChartCopy() {
        return this.makeBarChartCopy(TravelTimeChart, travelTimeSeries);
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

            densitySeries.setName("density (vehicles)");
            DensityChart.getData().add(densitySeries);
        }
    }

    public void updateDensityChart(BarChart<String, Number> DensityChart) {
        if (this.DensityChart == null || sim == null) return;

        setupDensityChart();
        densitySeries.getData().clear();

        // Get all edge IDs from simulation
        List<String> edgeIDs = sim.getEdgeIDsList();
        
        // Get density for each edge using getEdgeDensity
        List<Integer> densityedge = new ArrayList<>();
        for (String edgeID : edgeIDs) {
            densityedge.add(sim.getEdgeDensity(edgeID));
        }

        int count_edgeDensity1 = 0;
        int count_edgeDensity2 = 0;
        int count_edgeDensity3 = 0;
        int count_edgeDensity4 = 0;
        int count_edgeDensity5 = 0;

        for (int density : densityedge) {
            if (density >= 5 && density < 10) {
                count_edgeDensity1++;
            } else if (density >= 10 && density < 15) {
                count_edgeDensity2++;
            } else if (density >= 15 && density < 20) {
                count_edgeDensity3++;
            } else if (density >= 20 && density < 25) {
                count_edgeDensity4++;
            } else if (density >= 25) {
                count_edgeDensity5++;
            }
        }
        
        densitySeries.getData().add(new XYChart.Data<>("5-10", count_edgeDensity1));
        densitySeries.getData().add(new XYChart.Data<>("10-15", count_edgeDensity2));
        densitySeries.getData().add(new XYChart.Data<>("15-20", count_edgeDensity3));
        densitySeries.getData().add(new XYChart.Data<>("20-25", count_edgeDensity4));
        densitySeries.getData().add(new XYChart.Data<>("25+", count_edgeDensity5));

    }
    public BarChart<String, Number> makeDensityChartCopy() {
        return this.makeBarChartCopy(DensityChart, densitySeries);
    }

    public BarChart<String, Number> makeBarChartCopy(BarChart<String, Number> inputChart, 
        XYChart.Series<String, Number> inputSeries) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();     
        
        xAxis.setLabel(inputChart.getXAxis().getLabel());
        yAxis.setLabel(inputChart.getYAxis().getLabel());

        XYChart.Series<String, Number> seriesCopy = new XYChart.Series<>();
        seriesCopy.setName(inputSeries.getName());
        for (XYChart.Data<String, Number> data : inputSeries.getData()) {
                seriesCopy.getData().add(new XYChart.Data<>(data.getXValue(), data.getYValue()));
        }

        BarChart<String, Number> copy = new BarChart<>(xAxis, yAxis);
        copy.setAnimated(false);
        copy.setLegendVisible(true);
        copy.setTitle(inputChart.getTitle());

        copy.getData().add(seriesCopy);
        return copy;
    }
}
