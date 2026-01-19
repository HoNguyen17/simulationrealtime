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

// Graph class to manage and update charts (Nguyen)

public class Graph {
    // Charts
    private AreaChart<Number, Number> SpeedChart;
    private BarChart<String, Number> TravelTimeChart;
    private BarChart<String, Number> DensityChart;

    // Simulation reference
    private SimulationWrapper sim;

    // Data series for charts use from javafx.scene.chart.XYChart
    private final XYChart.Series<Number, Number> averageSpeedSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> travelTimeSeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> densitySeries = new XYChart.Series<>();

    // Set simulation reference
    public void setSimulation(SimulationWrapper sim) {
        this.sim = sim;
    }

    //Speed Chart
    /*
    It calculates the average speed of all vehicles in the simulation at each time step.
    The average speed is then plotted against the simulation time to show how vehicle speeds change over time
    */
    public void SpeedChart(AreaChart<Number, Number> SpeedChart) {
        if (SpeedChart == null) { //check null input
            this.SpeedChart = null;
            return;
        }
        this.SpeedChart = SpeedChart; //assign input to field
        setupSpeedChart();
    }

    private void setupSpeedChart() { //setup chart properties
        if (SpeedChart == null) return;
        // Configure once (avoid re-adding series)

        //check if series already added to speedchart before adding again 
        if (!SpeedChart.getData().contains(averageSpeedSeries)) { 
            SpeedChart.setAnimated(false); //disable animation
            SpeedChart.setLegendVisible(true); //show legend
            SpeedChart.setTitle("Average Vehicle Speed"); //set title

            averageSpeedSeries.setName("speed (m/s)"); //set series name
            SpeedChart.getData().add(averageSpeedSeries); //add series to chart
        }
    }

    // Update speed chart with new data
    public void updateSpeedCharts(AreaChart<Number, Number> SpeedChart) {
        if (SpeedChart == null || sim == null) return; //check null

        setupSpeedChart(); //ensure chart is set up
        // do not clear existing data to keep historical data points

        // Get average speed from simulationWrapper
        double averageSpeed = sim.getVehicleAverageSpeed(0);
        // Get current simulation time in seconds from simulationWrapper
        double tSec = sim.getTime(0);

        // Add new data point to series
        averageSpeedSeries.getData().add(new XYChart.Data<>(tSec, averageSpeed));
    }

    // Create a copy of the speed chart 
    public AreaChart<Number, Number> makeSpeedChartCopy() {
        // Create axes for the copy
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        // Set labels from original chart
        xAxis.setLabel(SpeedChart.getXAxis().getLabel());
        yAxis.setLabel(SpeedChart.getYAxis().getLabel());

        // Create a copy of the data series
        XYChart.Series<Number, Number> seriesCopy = new XYChart.Series<>();
        seriesCopy.setName("speed (m/s)");

        // Copy data points
        for (XYChart.Data<Number, Number> data : averageSpeedSeries.getData()) {
                seriesCopy.getData().add(new XYChart.Data<>(data.getXValue(), data.getYValue()));
        }
        // Create the AreaChart copy
        AreaChart<Number, Number> copy = new AreaChart<>(xAxis, yAxis);
        copy.setAnimated(false); //disable animation
        copy.setLegendVisible(true); //show legend
        copy.setTitle("Average Vehicle Speed"); //set title
        copy.getData().add(seriesCopy); //add series to chart
        return copy;
    } 

    //Travel Time Chart
    /*
    When the vehicle completes its route, its travel time is recorded.
    The travel times are categorized into intervals (e.g., 0-60s, 60-120s, etc.).
    The chart displays the number of vehicles that fall into each travel time category.
    */
    public void TravelTimeChart(BarChart<String, Number> TravelTimeChart) {
        if (TravelTimeChart == null) { //check null input
            this.TravelTimeChart = null;
            return;
        }
        this.TravelTimeChart = TravelTimeChart; //assign input to field
        setupTravelTimeChart(); 
    }
    private void setupTravelTimeChart() { //setup chart properties
        if (TravelTimeChart == null) return; 

        // check if series already added to travel time chart before adding again
        if (!TravelTimeChart.getData().contains(travelTimeSeries)) {
            TravelTimeChart.setAnimated(false);
            TravelTimeChart.setLegendVisible(true);
            TravelTimeChart.setTitle("Vehicle Travel Time");

            travelTimeSeries.setName("travel time (s)");
            TravelTimeChart.getData().add(travelTimeSeries);
        }
    }

    // Update travel time chart with new data
    public void updateTravelTimeChart(BarChart<String, Number> TravelTimeChart) {
        if (TravelTimeChart == null || sim == null) return;

        setupTravelTimeChart(); //ensure chart is set up
        travelTimeSeries.getData().clear(); //clear existing data

        // get completed travel times from simulationWrapper
        List<Double> travelTimes = sim.getCompletedTravelTimes();
        
        // Counters for each travel time category 
        int countTravel0 = 0;
        int countTravel1 = 0;
        int countTravel2 = 0;
        int countTravel3 = 0;
        int countTravel4 = 0;
        int countTravel5 = 0;
        
        // Categorize travel times 
        for (int i = 0; i < travelTimes.size(); i++) { // iterate through travel times
            double time = travelTimes.get(i); // get travel time
            //check which category the travel time falls into and increment corresponding counter
            if (time < 60){ 
                countTravel0++;
            }
            else if (time >= 60 && time < 120){
                 countTravel1++;
            } 
            else if (time >= 120 && time < 180) {
                countTravel2++;
            } 
            else if (time >= 180 && time < 240) {
                countTravel3++;
            } 
            else if (time >= 240 && time < 300) {
                countTravel4++;
            } 
            else {
                countTravel5++;
            }
        }
        // Add categorized data to series
        travelTimeSeries.getData().add(new XYChart.Data<>("0-60", countTravel0));
        travelTimeSeries.getData().add(new XYChart.Data<>("60-120", countTravel1));
        travelTimeSeries.getData().add(new XYChart.Data<>("120-180", countTravel2));
        travelTimeSeries.getData().add(new XYChart.Data<>("180-240", countTravel3));
        travelTimeSeries.getData().add(new XYChart.Data<>("240-300", countTravel4));
        travelTimeSeries.getData().add(new XYChart.Data<>("300+", countTravel5));
    }
    public BarChart<String, Number> makeTravelTimeChartCopy() { //create copy of travel time chart
        return this.makeBarChartCopy(TravelTimeChart, travelTimeSeries);
    }

    //Density Chart
    /*
    it calculates the density of vehicles on each edge of the road network.
    The density values are then categorized into ranges (e.g., 5-10 vehicles, 10-15 vehicles, etc.).
    The chart displays the number of edges that fall into each density category.
    */
    public void DensityChart(BarChart<String, Number> DensityChart) {
        if (DensityChart == null) {
            this.DensityChart = null;
            return;
        }
        this.DensityChart = DensityChart;
        setupDensityChart();
    }
    // Setup density chart properties
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

    // Update density chart with new data
    public void updateDensityChart(BarChart<String, Number> DensityChart) {
        if (this.DensityChart == null || sim == null) return;

        setupDensityChart(); //ensure chart is set up
        densitySeries.getData().clear(); //clear existing data

        // Get all edge IDs from simulationWrapper
        List<String> edgeIDs = sim.getEdgeIDsList();
        
        // Get density for each edge using getEdgeDensity from simulationWrapper
        List<Integer> densityedge = new ArrayList<>();
        for (String edgeID : edgeIDs) { // iterate through edge IDs
            densityedge.add(sim.getEdgeDensity(edgeID)); // get density and add to list
        }

        // Counters for each density category
        int count_edgeDensity1 = 0;
        int count_edgeDensity2 = 0;
        int count_edgeDensity3 = 0;
        int count_edgeDensity4 = 0;
        int count_edgeDensity5 = 0;

        // Categorize densities
        for (int density : densityedge) { // iterate through densities
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
        
        // Add categorized data to series
        densitySeries.getData().add(new XYChart.Data<>("5-10", count_edgeDensity1));
        densitySeries.getData().add(new XYChart.Data<>("10-15", count_edgeDensity2));
        densitySeries.getData().add(new XYChart.Data<>("15-20", count_edgeDensity3));
        densitySeries.getData().add(new XYChart.Data<>("20-25", count_edgeDensity4));
        densitySeries.getData().add(new XYChart.Data<>("25+", count_edgeDensity5));

    }
    // Create a copy of the density chart
    public BarChart<String, Number> makeDensityChartCopy() {
        return this.makeBarChartCopy(DensityChart, densitySeries);
    }

    // Generic method to create a copy of a BarChart with its data series
    public BarChart<String, Number> makeBarChartCopy(BarChart<String, Number> inputChart, //if input chart is bar chart can use this method
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
/*
make a copy is use for exporting chart as image in App.java
*/