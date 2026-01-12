package gui;

import javafx.fxml.FXML;

import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.animation.AnimationTimer;

import javafx.event.ActionEvent;
import javafx.event.Event;

import java.util.ArrayList;
import java.util.List;

import wrapper.SimulationWrapper;
import wrapper.DataType.VehicleData;
import wrapper.DataType;

import tracker.Statistic;
import javafx.stage.Stage;
import javafx.scene.Node;

public class ControlPanel {
    // --- CÁC BIẾN FXML (Giữ nguyên) ---
    @FXML private StackPane mapContainer;
    @FXML private Button expBtn;
    @FXML private MenuButton expType;
    @FXML private MenuItem expTypeCSV;
    @FXML private MenuItem expTypePDF;
    @FXML private MenuItem expTypeCSVVehicle;
    @FXML private MenuItem expTypeCSVEdge;

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

    // --- BIẾN CỤC BỘ ---
    private MapCanvas mapCanvas;
    private SimulationWrapper sim;
    private final Graph stats = new Graph();

    private volatile boolean simRunning = false;
    private long idCounter = 0;
    private String selectedExportType = ""; // No default - user must select from menu

    // --- HÀM SET MAP (Kết nối với App.java) ---
    // Chỉ cần nhận MapCanvas để hiển thị
    public void initialize() {
        stats.SpeedChart(avgSpeed);
        stats.TravelTimeChart(travelTime);
        stats.DensityChart(density);
        
        // Set default text for export type menu (user must select)
        if (expType != null) {
            expType.setText("Select Type");
        }

        //simulation
        filterOnOff.setItems(FXCollections.observableArrayList("Off", "On"));
        filterOnOff.setValue("Off");
        filterColor.setItems(FXCollections.observableArrayList(DataType.colorOptions));
        filterColor.setValue("Default");
        //add vehicle
        vehColor.setItems(FXCollections.observableArrayList(DataType.colorOptions));
        vehColor.setValue("Default");
        //traffic light
    }
    public void setMapCanvas(MapCanvas mapCanvas, SimulationWrapper inputSim) {
        this.mapCanvas = mapCanvas;
        this.sim = inputSim;
        this.stats.setSimulation(inputSim);

        if (mapContainer != null) {
            // Thêm Map vào giao diện
            mapContainer.getChildren().add(mapCanvas.getCanvas());
            // Căn chỉnh kích thước
            mapCanvas.getCanvas().widthProperty().bind(mapContainer.widthProperty());
            mapCanvas.getCanvas().heightProperty().bind(mapContainer.heightProperty());
        }
    }

    @FXML void simPlayAct(ActionEvent event) {System.out.println("start");}
    @FXML void simPauseAct(ActionEvent event) {this.PauseSim();}
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

    @FXML void expBtnAct(ActionEvent event) {
        try {
            // Get stage from event source
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            
            // Check if export type is selected, if not show error
            if (selectedExportType == null || selectedExportType.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Export Type Not Selected", 
                    "Please select an export type (PDF or CSV) from the Type menu first.");
                return;
            }
            
            if (selectedExportType.equals("PDF")) {
                // Export PDF with graphs using PrinterJob
                boolean success = Statistic.exportPDF(stage, avgSpeed, travelTime, density);
                // Alert is shown inside exportPDF method
            } else if (selectedExportType.equals("CSV") || 
                       selectedExportType.equals("CSV Vehicle") || 
                       selectedExportType.equals("CSV Edge")) {
                // CSV export - Show filter selection dialog
                List<String> filterOptions = new ArrayList<>();
                filterOptions.add("ALL - Export all data");
                filterOptions.add("CONGESTED_ONLY - Export only congested edges");
                
                ChoiceDialog<String> filterDialog = new ChoiceDialog<>("ALL - Export all data", filterOptions);
                filterDialog.setTitle("CSV Export Filter");
                filterDialog.setHeaderText("Select export filter");
                filterDialog.setContentText("Choose filter type:");
                
                java.util.Optional<String> filterResult = filterDialog.showAndWait();
                
                // If user cancels dialog, don't export
                if (!filterResult.isPresent()) {
                    return;
                }
                
                String selectedOption = filterResult.get();
                
                // Extract filter type from selected option
                String filterType = "ALL";
                if (selectedOption.startsWith("CONGESTED_ONLY")) {
                    filterType = "CONGESTED_ONLY";
                }
                
                // Export CSV to user-selected location with filter
                boolean success = Statistic.exportCSV(stage, filterType);
                // Alert is shown inside exportCSV method
            } else {
                showAlert(Alert.AlertType.WARNING, "Unknown Export Type", 
                    "Please select a valid export type (PDF or CSV) from the Type menu.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Export Error", 
                "An error occurred during export: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML void expTypeAct(ActionEvent event) {
        // This is called when MenuButton is clicked, not when MenuItem is selected
        // We need to handle MenuItem clicks separately
    }
    
    @FXML void expTypePDFAct(ActionEvent event) {
        selectedExportType = "PDF";
        expType.setText("PDF");
    }
    
    @FXML void expTypeCSVAct(ActionEvent event) {
        selectedExportType = "CSV";
        expType.setText("CSV");
    }
    
    @FXML void expTypeCSVVehicleAct(ActionEvent event) {
        selectedExportType = "CSV Vehicle";
        expType.setText("CSV Vehicle");
    }
    
    @FXML void expTypeCSVEdgeAct(ActionEvent event) {
        selectedExportType = "CSV Edge";
        expType.setText("CSV Edge");
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

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
    // interaction method
    // pause simulation
    private void PauseSim() {
        sim.Pause();
    }
    // inject vehicle
    private void VehicleInject(int num, String routeId, Color color) {
        if (1 <= num && num <= 300) {
            for (int i = 0; i < num; i++) {
                this.sim.addVehicleWithColor(String.format("v_%d", this.idCounter), routeId, color);
                this.idCounter++;
            }
        }
        // injectionThread = new Thread(() -> {
        //         for (int i = 0; i < num; i++) {
        //             this.sim.addVehicleWithColor(String.format("v_%d", this.idCounter), routeId, color);
        //             this.idCounter++;
        //         }
        // });
        // injectionThread.start();
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
}