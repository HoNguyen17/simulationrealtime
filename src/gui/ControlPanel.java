package gui;

import javafx.fxml.FXML;

import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Node; 

import javafx.stage.Stage;
import javafx.stage.FileChooser;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.animation.AnimationTimer;

import javafx.event.ActionEvent; 
import javafx.event.Event; 

import javafx.print.PrinterJob;
import javafx.print.PageLayout;

import javafx.geometry.Pos;

import java.util.ArrayList;
import java.util.List;

import wrapper.SimulationWrapper;
import wrapper.DataType.VehicleData;
import wrapper.DataType;

import tracker.Statistic;

public class ControlPanel {
    // --- FXML ---
    @FXML private StackPane mapContainer;
    @FXML private Button exportButton;
    @FXML private ComboBox<String> exportType;

    @FXML private Label simTime;
    @FXML private Button simPause;
    @FXML private Button simPlay;
    @FXML private Button simTest;//for testing
    @FXML private ComboBox<String> filterOnOff;
    @FXML private ComboBox<String> filterColor;
    @FXML private TextField filterSpeed;

    @FXML private AreaChart<Number, Number> avgSpeed;
    @FXML private BarChart<String, Number> travelTime;
    @FXML private BarChart<String, Number> density;
    @FXML private TableView<?> staTLTable;
    @FXML private TableView<?> staVehTable;

    @FXML private ComboBox<String> tlIDs;
    @FXML private TextField tlPhaseTime;
    @FXML private Button tlsetTime; 
    @FXML private Button tlNPhase;
    @FXML private Button tlNPhaseAll;

    @FXML private ComboBox<String> vehColor;
    @FXML private ComboBox<String> injectVehRoute;
    @FXML private TextField injectVehNum;
    @FXML private Button vehInject;

    private MapCanvas mapCanvas;
    private SimulationWrapper sim;
    private final Graph stats = new Graph();

    private volatile boolean simRunning = false;
    private long idCounter = 0;

    public void initialize() {
        stats.SpeedChart(avgSpeed);
        stats.TravelTimeChart(travelTime);
        stats.DensityChart(density);
        // simulation tab
        filterOnOff.setItems(FXCollections.observableArrayList("Off", "On"));
        filterOnOff.setValue("Off");
        filterColor.setItems(FXCollections.observableArrayList(DataType.colorOptions));
        filterColor.setValue("Default");
        // add vehicle tab
        vehColor.setItems(FXCollections.observableArrayList(DataType.colorOptions));
        vehColor.setValue("Default");
        // export tab
        exportType.setItems(FXCollections.observableArrayList("PDF", "CSV"));
        exportType.setValue("CSV");
    }
    public void setMapCanvas(MapCanvas mapCanvas, SimulationWrapper inputSim) {
        this.mapCanvas = mapCanvas;
        this.sim = inputSim;
        this.stats.setSimulation(inputSim);

        if (mapContainer != null) {
            mapContainer.getChildren().add(mapCanvas.getCanvas());
            mapCanvas.getCanvas().widthProperty().bind(mapContainer.widthProperty());
            mapCanvas.getCanvas().heightProperty().bind(mapContainer.heightProperty());
        }
    }

    @FXML void simPlayAct(ActionEvent event) {System.out.println("start");}
    @FXML void simPauseAct(ActionEvent event) {this.sim.Pause(false);}
    @FXML void simTestAct(ActionEvent event) {this.SimTest();}

    @FXML void switchFilterAct(ActionEvent event) {
        String chosenFilter = filterOnOff.getValue();
        String inputSpeed = filterSpeed.getText();
        double chosenSpeed = 1000;
        String chosenColor = filterColor.getValue();
        if (this.checkIntConvertable(inputSpeed)) {chosenSpeed = Integer.parseInt(inputSpeed);}
        if (chosenFilter == "On") {
            this.mapCanvas.setFilter(true);
        }
        else this.mapCanvas.setFilter(false);
    }
    
    @FXML void colorFilterAct(ActionEvent event) {
        String chosenColor = filterColor.getValue();
        this.mapCanvas.setColorFilter(chosenColor);
    }

    @FXML void speedFilterAct(ActionEvent event) {
        String inputSpeed = filterSpeed.getText();
        double chosenSpeed = 1000;
        if (this.checkIntConvertable(inputSpeed)) {chosenSpeed = Integer.parseInt(inputSpeed);}
        this.mapCanvas.setSpeedFilter(chosenSpeed);
    }

    @FXML void exportPressed(ActionEvent event) { 
        this.sim.Pause(true);
        String chosenType = exportType.getValue();
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        if (chosenType.equals("CSV")) {
            this.exportCSVMenu(stage);
        }
        else {
            this.exportPDFMenu(stage);
        }
    }
    @FXML void exportTypeAct(ActionEvent event) { }

    @FXML void chosenTLAct(ActionEvent event) {
        String chosenTL = tlIDs.getValue();
        if(chosenTL != null) mapCanvas.setHightLightJunctions(this.sim.getTLControlledJunctions(chosenTL));
    }

    @FXML void tlNPhaseAct(ActionEvent event) {
        String chosenTL = tlIDs.getValue();
        this.sim.setTLPhaseNext(chosenTL);
    }
    
    // set tl phase event
    @FXML void tlSetTimeAct(ActionEvent event) {
        String chosenTL = tlIDs.getValue();
        String inputTime = tlPhaseTime.getText();
        int chosenTime = 0; 
        if (this.checkIntConvertable(inputTime)) {
            chosenTime = Integer.parseInt(inputTime);
            this.sim.setTLPhaseDuration(chosenTL, chosenTime);
        }
    }
    // tl next phase all event
    @FXML void tlNPhaseAllAct(ActionEvent event) {sim.setTLPhaseNextAll();}
    // inhect vehicle event
    @FXML void chosenRouteAct(ActionEvent event) {
        String chosenRoute = injectVehRoute.getValue();
        String startEdge = null;
        if (chosenRoute != null) startEdge = sim.getRouteFirstEdge(chosenRoute);
        this.mapCanvas.setHightLightEdge(startEdge);
    }
    //inject button clicked
    @FXML void vehInjectAct(ActionEvent event) {
        String chosenRoute = injectVehRoute.getValue();
        String inputColor = vehColor.getValue();
        Color chosenColor = Color.RED;
        if (inputColor != null) chosenColor = DataType.convertColor(inputColor);
        String inputNum = injectVehNum.getText();
        int chosenNum = 1;
        if (this.checkIntConvertable(inputNum)) chosenNum = Integer.parseInt(inputNum);
        this.VehicleInject(chosenNum, chosenRoute, chosenColor);
    }
    
    @FXML void modeChangeAct(Event event) {
        Tab selectedTab = (Tab) event.getTarget();
        if (selectedTab.isSelected()) {
            String tabName = selectedTab.getText();
            if (tabName.equals("Simulation")) {this.changeRenderMode(0);} 
            else if (tabName.equals("Add Vehicle")) {this.changeRenderMode(1);}
            else if (tabName.equals("Traffic Light")) {this.changeRenderMode(2);}
        }
    }

    public void updateUI(long nowNanos) {
        stats.updateSpeedCharts(avgSpeed);
        stats.updateTravelTimeChart(travelTime);
        stats.updateDensityChart(density);
        simTime.setText("" + sim.getTime(0));
    }
    // interaction methods

    private void VehicleInject(int num, String routeId, Color color) {
        if (1 <= num && num <= 300) {
            for (int i = 0; i < num; i++) {
                this.sim.addVehicleWithColor(String.format("v_%d", this.idCounter), routeId, color);
                this.idCounter++;
            }
        }   
    }
    // test (easy to break)
    private void SimTest() {
        Color colour = new Color(0.5,0.5,0.5,1);
        sim.addVehicleWithColor("test", "r_1",colour);
        sim.setVehicleColor("f_0.0",1,1,1,1);
    }
    // change mode
    private void changeRenderMode(int input) {
        if (this.mapCanvas != null) {
            this.mapCanvas.setRenderMode(input);
        }
        if (input == 0) {
            System.out.println("Switch to normal mode");
        }
        if (input == 1) {
            this.mapCanvas.setUpdateRoute(true);
            List<String> routeIds = this.sim.getRouteIDsList();
            injectVehRoute.setItems(FXCollections.observableArrayList(routeIds));
            if (!routeIds.isEmpty()) {injectVehRoute.setValue(routeIds.get(0));}
        }
        if (input == 2) {
            List<String> tlIds = this.sim.getTLIDsList();
            tlIDs.setItems(FXCollections.observableArrayList(tlIds));
            if (!tlIds.isEmpty()) {tlIDs.setValue(tlIds.get(0));}
            String chosenTL = tlIDs.getValue();
            mapCanvas.setHightLightJunctions(this.sim.getTLControlledJunctions(chosenTL));
        }
    }

    private boolean checkIntConvertable(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {return false;}
    }

    private void exportCSVMenu(Stage stage) {
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("ALL - Export all data");
        filterOptions.add("CONGESTED_ONLY - Export only congested edges");
        
        ChoiceDialog<String> filterDialog = new ChoiceDialog<>("ALL - Export all data", filterOptions);
        filterDialog.setTitle("CSV Export Filter");
        filterDialog.setHeaderText("Select export filter");
        filterDialog.setContentText("Choose filter type:");
        
        java.util.Optional<String> filterResult = filterDialog.showAndWait();
            
        if (!filterResult.isPresent()) return;
        
        String selectedOption = filterResult.get();
        
        // Extract filter type from selected option
        String filterType = "ALL";
        if (selectedOption.startsWith("CONGESTED_ONLY")) {
            filterType = "CONGESTED_ONLY";
        }
        
        // Export CSV to user-selected location with filter
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export CSV File");
        fileChooser.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        
        //Set default filename with timestamp
        fileChooser.setInitialFileName("edgeData.csv");
        String destinationPath = Statistic.exportCSV(fileChooser.showSaveDialog(stage), filterType);
        
        if (destinationPath != null) {
            String message = String.format("File saved to: %s:", destinationPath);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText("CSV file exported");
            alert.setContentText(message);
            alert.showAndWait();
        }
    }

    private void exportPDFMenu(Stage stage) {
        // Create a container to hold all charts 
        VBox chartContainer = new VBox(20); 
        chartContainer.setStyle("-fx-padding: 30; -fx-background-color: white; -fx-alignment: center;");
        chartContainer.setPrefWidth(600);
        chartContainer.setPrefHeight(820);
        chartContainer.setAlignment(Pos.CENTER);
        chartContainer.getChildren().add(stats.makeSpeedChartCopy());
        chartContainer.getChildren().add(stats.makeTravelTimeChartCopy());
        chartContainer.getChildren().add(stats.makeDensityChartCopy());

        if (Statistic.exportPDF(chartContainer, stage)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText("PDF export completed");
            alert.setContentText("The charts have been exported to PDF. " +
                "If you selected a PDF printer, the file should be saved.");
            alert.showAndWait();
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText("Failed to export PDF");
            alert.setContentText("The print job failed. Please try again.");
            alert.showAndWait();
        }
    }
}