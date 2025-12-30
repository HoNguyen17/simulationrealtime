
package gui;

import javafx.fxml.FXML;

import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
    @FXML private ChoiceBox<?> injectVehRoute;
    @FXML private Button vehIn;

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
    @FXML void expBtnAct(ActionEvent event) {
        this.ExportData();
    }
    @FXML void expTypeAct(ActionEvent event) {
        MenuItem source = (MenuItem) event.getSource();
        if (source == expTypeCSV) {
            this.ExportData();
        } else if (source == expTypePDF) {
            // PDF export not implemented yet
            System.out.println("PDF export not implemented yet");
        }
    }
    @FXML void tlIDAct(ActionEvent event) { }
    @FXML void tlNPhaseAct(ActionEvent event) { }
    @FXML void tlNPhaseAllAct(ActionEvent event) {sim.setTLPhaseNextAll();}
    @FXML void tlPhaseAct(ActionEvent event) { }
    @FXML void vehColorAct(ActionEvent event) { }
    @FXML void vehIDAct(ActionEvent event) {}
    @FXML void vehInAct(ActionEvent event) {
        this.VehicleInject(Integer.parseInt(injectVehNum.getText()));
    }

    @FXML void modeChangeAct(Event event) {
        Tab selectedTab = (Tab) event.getTarget();
        if (selectedTab.isSelected()) {
            String tabName = selectedTab.getText();
            if (tabName.equals("Simulation")) {this.changeRenderMode(0);}
            else if (tabName.equals("Vehicle")) {this.changeRenderMode(1);}
            else if (tabName.equals("Traffic Light")) {this.changeRenderMode(2);}
        }
    }
    // interaction method
    // pause simulation
    private void PauseSim() {
        sim.Pause();
    }
    // inject vehicle
    private void VehicleInject(int num) {
        System.out.println("should inject "+ num);
    }
    // test (easy to break)
    private void SimTest() {
        sim.getRouteFirstEdge("r_0");
    }
    // change mode
    private void changeRenderMode(int input) {
        if (this.mapCanvas != null) {
            this.mapCanvas.setRenderMode(input);
        }
        if (input == 1) {
            this.mapCanvas.setUpdateRoute(true);
        }
    }
    // export data to CSV
    private void ExportData() {
        if (mapCanvas == null || sim == null) {
            System.err.println("Cannot export: MapCanvas or SimulationWrapper is null");
            return;
        }

        // Show file chooser dialog
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Statistics to CSV");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("simulation_statistics_" + 
            System.currentTimeMillis() + ".csv");

        // Get the stage from any control
        Stage stage = (Stage) expBtn.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            boolean success = CSVExporter.exportToCSV(
                sim,
                file.getAbsolutePath()
            );

            if (success) {
                System.out.println("Data exported successfully to: " + file.getAbsolutePath());
                // Show success message
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Success");
                alert.setHeaderText(null);
                alert.setContentText("Data exported successfully to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } else {
                System.err.println("Failed to export data");
                // Show error message
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export data. Please check console for details.");
                alert.showAndWait();
            }
        }
    }
}
