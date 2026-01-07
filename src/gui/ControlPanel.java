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

    @FXML private ComboBox<String> tlIDs;
    @FXML private TextField tlPhaseTime;
    @FXML private Button tlsetTime; 
    @FXML private Button tlNPhase;
    @FXML private Button tlNPhaseAll;

    @FXML private ComboBox<Color> vehColor;
    @FXML private ComboBox<String> injectVehRoute;
    @FXML private TextField injectVehNum;
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
    @FXML void vehInjectAct(ActionEvent event) {
        String chosenRoute = injectVehRoute.getValue();
        Color chosenColor = vehColor.getValue();
        String inputNum = injectVehNum.getText();
        int chosenNum = 1;
        if (this.checkIntConvertable(inputNum)) {chosenNum = Integer.parseInt(inputNum);}
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
    // interaction method
    // pause simulation
    private void PauseSim() {
        sim.Pause();
    }
    // inject vehicle
    private void VehicleInject(int num, String routeId, Color color) {
        System.out.println("should inject "+ num + " at " + routeId);
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
        if (input == 1) {
            this.mapCanvas.setUpdateRoute(true);
            List<String> routeIds = this.sim.getRouteIDsList();
            injectVehRoute.setItems(FXCollections.observableArrayList(routeIds));
            if (!routeIds.isEmpty()) {injectVehRoute.setValue(routeIds.get(0));}
            vehColor.setItems(FXCollections.observableArrayList(Color.RED, Color.BLUE, Color.YELLOW));
            vehColor.setValue(Color.RED);
        }
        if (input == 2) {
            List<String> tlIds = this.sim.getTLIDsList();
            tlIDs.setItems(FXCollections.observableArrayList(tlIds));
            if (!tlIds.isEmpty()) {tlIDs.setValue(tlIds.get(0));}
        }
    }
    private boolean checkIntConvertable(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {return false;}
    }
}