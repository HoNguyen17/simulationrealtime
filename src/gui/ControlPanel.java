package gui;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.event.ActionEvent; 
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
    @FXML private LineChart<?, ?> staSim;
    @FXML private TableView<?> staTLTable;
    @FXML private TableView<?> staVehTable;
    @FXML private TextField tlID;
    @FXML private Button tlNPhase;
    @FXML private TextField tlPhase;
    @FXML private ColorPicker vehColor;
    @FXML private TextField vehID;
    @FXML private Button vehIn;
    @FXML private ChoiceBox<?> vehRoute;

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

    @FXML void simPlayAct(ActionEvent event) {this.PauseSim();}
    @FXML void simPauseAct(ActionEvent event) {this.PauseSim();}
    @FXML void expBtnAct(ActionEvent event) { }
    @FXML void expTypeAct(ActionEvent event) { }
    @FXML void tlIDAct(ActionEvent event) { }
    @FXML void tlNPhaseAct(ActionEvent event) { }
    @FXML void tlPhaseAct(ActionEvent event) { }
    @FXML void vehColorAct(ActionEvent event) { }
    @FXML void vehIDAct(ActionEvent event) { }
    @FXML void vehInAct(ActionEvent event) { }
    // interaction method
    private void PauseSim() {
        sim.Pause();
    }
}