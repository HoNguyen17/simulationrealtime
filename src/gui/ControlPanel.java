package gui;

import javafx.fxml.FXML;

import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent; 
import javafx.event.Event; 

import java.util.ArrayList;
import java.util.List;

import wrapper.SimulationWrapper;

public class ControlPanel {
    // --- CÁC BIẾN FXML (Giữ nguyên) ---
    @FXML private StackPane mapContainer;
    @FXML private Button expBtn;
    @FXML private MenuButton expType;
    @FXML private MenuItem expTypeCSV;
    @FXML private MenuItem expTypePDF;

    @FXML private Button simPause;
    @FXML private Button simPlay;
    @FXML private Button simTest;//for testing

    @FXML private LineChart<?, ?> staSim;
    @FXML private TableView<?> staTLTable;
    @FXML private TableView<?> staVehTable;

    @FXML private TextField tlID;
    @FXML private Button tlNPhase;
    @FXML private Button tlNPhaseAll;
    @FXML private TextField tlPhase;

    @FXML private ColorPicker vehColor;
    @FXML private TextField injectVehNum;
    @FXML private ChoiceBox<String> injectVehRoute;
    @FXML private Button vehInject;

    // --- BIẾN CỤC BỘ ---
    private MapCanvas mapCanvas;
    private SimulationWrapper sim;

    private volatile boolean simRunning = false;
    private long idCounter = 0;

    // --- HÀM SET MAP (Kết nối với App.java) ---
    // Chỉ cần nhận MapCanvas để hiển thị
    public void setMapCanvas(MapCanvas mapCanvas, SimulationWrapper inputSim) {
        this.mapCanvas = mapCanvas;
        this.sim = inputSim;

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
    @FXML void expBtnAct(ActionEvent event) { }
    @FXML void expTypeAct(ActionEvent event) { }
    @FXML void tlIDAct(ActionEvent event) { }
    @FXML void tlNPhaseAct(ActionEvent event) { }
    @FXML void tlNPhaseAllAct(ActionEvent event) {sim.setTLPhaseNextAll();}
    @FXML void tlPhaseAct(ActionEvent event) { }
    @FXML void vehColorAct(ActionEvent event) { }
    @FXML void vehNumAct(ActionEvent event) {}

    @FXML void vehInjectAct(ActionEvent event) {
        String chosenRoute = injectVehRoute.getValue();
        String inputNum = injectVehNum.getText();
        int chosenNum = 1;
        if (this.checkIntConvertable(inputNum)) {chosenNum = Integer.parseInt(inputNum);}
        this.VehicleInject(chosenNum, chosenRoute);
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
    // interaction method
    // pause simulation
    private void PauseSim() {
        sim.Pause();
    }
    // inject vehicle
    private void VehicleInject(int num, String routeId) {
        System.out.println("should inject "+ num + " at " + routeId);
        for (int i = 0; i < num; i++) {
            this.sim.addVehicleNormal(String.format("v_%d", this.idCounter), routeId);
            this.idCounter++;
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
        if (input == 1) {
            this.mapCanvas.setUpdateRoute(true);
            List<String> routeIds = this.sim.getRouteIDsList();
            injectVehRoute.setItems(FXCollections.observableArrayList(routeIds));
            if (!routeIds.isEmpty()) {injectVehRoute.setValue(routeIds.get(0));}
        }
    }
    private boolean checkIntConvertable(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {return false;}
    }
}