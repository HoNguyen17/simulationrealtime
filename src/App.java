import gui.MapCanvas;
import gui.MapCanvas.VehicleData;

import paser.Networkpaser;
import wrapper.SimulationWrapper;
import wrapper.VehicleWrapper;
import wrapper.TrafficLightWrapper;
import de.tudresden.sumo.cmd.Vehicle;
import javafx.animation.AnimationTimer;
import java.util.List;
import java.util.ArrayList;
import javafx.application.Platform;
import de.tudresden.sumo.objects.SumoPosition2D;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;


public class App extends Application {
    private SimulationWrapper simulationWrapper; // Field for TRACI connection
    private AnimationTimer simulationTimer; // Field to hold the timer instance

    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới


        Networkpaser.NetworkModel model =
                Networkpaser.load("C:\\Users\\LENOVO\\IdeaProjects\\simulationrealtime\\resource\\test_2_traffic.net.xml");


        // Canvas bản đồ chuyển thành MapCanvas để quản lý pan/zoom/vẽ
        MapCanvas mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        mapCanvas.render();

        // NEW SIMULATION STARTUP
        try {
            simulationWrapper = new SimulationWrapper(
                    "C:\\Users\\LENOVO\\IdeaProjects\\simulationrealtime\\resource\\test_2_traffic.sumocfg" // Path to your config
            );
            simulationWrapper.conn.runServer(); // Assuming this connects TraCI
        } catch (Exception e) {
            System.err.println("Failed to start SUMO or connect TraCI: " + e.getMessage());
            return;
        }

        // --- NEW REAL-TIME ANIMATION LOOP ---
        simulationTimer = new AnimationTimer() { // <-- 1. Assign timer to the field
            private long lastUpdate = 0;
            private static final long UPDATE_INTERVAL = 50_000_000; // ~20 FPS (50ms)

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    try {
                        // 1. Step the simulation and fetch new vehicle state
                        simulationWrapper.Step();

                        // 2. Fetch all vehicle data
                        List<String> vehicleIDs = VehicleWrapper.getIDList(simulationWrapper, 0);
                        List<VehicleData> vehicleDataList = new ArrayList<>();

                        for (String id : vehicleIDs) {
                            VehicleWrapper vehicle = new VehicleWrapper(id);
                            SumoPosition2D pos = vehicle.getPosition(simulationWrapper, 0);
                            double angle = vehicle.getAngle(simulationWrapper, 0); // Get vehicle rotation

                            if (pos != null) {
                                // Convert SumoPosition2D and angle into the VehicleData format
                                vehicleDataList.add(new VehicleData(
                                        id,
                                        pos.x,
                                        pos.y,
                                        angle,
                                        javafx.scene.paint.Color.RED // Default color for all vehicles
                                ));
                            }
                        }

                        // 3. Update Canvas and Render
                        mapCanvas.setVehicleData(vehicleDataList);
                        mapCanvas.render();

                        lastUpdate = now;

                    } catch (Exception e) {
                        System.err.println("Simulation Loop Error: " + e.getMessage());
                        e.printStackTrace();
                        this.stop();
                    }
                }
            }
        }; // End of AnimationTimer definition
        simulationTimer.start(); // Start the timer

        // --- NEW CLEANUP LOGIC ---
        stage.setOnCloseRequest(e -> {
            System.out.println("Stopping simulation and exiting...");

            // 1. Stop the timer FIRST to prevent subsequent TraCI calls
            simulationTimer.stop();

            // 2. Close the TraCI connection safely, checking if it's NOT closed
            try {
                if (simulationWrapper != null && !simulationWrapper.conn.isClosed()) { // <-- FIX IS HERE
                    simulationWrapper.conn.close();
                }
            } catch (Exception ignore) {}

            Platform.exit();
        });

        // Sidebar trái: control dashboard
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(12));
        sidebar.setPrefWidth(280);

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Điều khiển tốc độ mô phỏng Simulation speed
        Label speedLbl = new Label("########");
        Slider speedSlider = new Slider(0.1, 5.0, 1.0);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            // TODO: nối với SimulationController.setSpeed(newV.doubleValue());
            System.out.println("Speed: " + newV.doubleValue());
        });


        // Điều khiển đèn giao thông
        Label tlLbl = new Label("Traffic light");
        Button tlAutoBtn = new Button("######");//"Auto mode"
        Button tlManualBtn = new Button("######");//"Manual mode"
        Button tlNextPhaseBtn = new Button("######");//"Next phase"
        tlAutoBtn.setOnAction(e -> {
            // TODO: TrafficLightController.setAuto(true);
            System.out.println("TL: auto");
        });
        tlManualBtn.setOnAction(e -> {
            // TODO: TrafficLightController.setAuto(false);
            System.out.println("TL: manual");
        });
        tlNextPhaseBtn.setOnAction(e -> {
            // TODO: TrafficLightController.stepPhase();
            System.out.println("TL: next phase");
        });

        /* // Điều khiển xe
        Label vehLbl = new Label("Vehicles");
        Button spawnVehBtn = new Button("Spawn vehicle");
        Button clearVehBtn = new Button("Clear vehicles");
        spawnVehBtn.setOnAction(e -> {
            // TODO: VehicleController.spawnAtSelectedJunction();
            System.out.println("Vehicle: spawn");
        });
        clearVehBtn.setOnAction(e -> {
            // TODO: VehicleController.clearAll();
            System.out.println("Vehicle: clear");
        });
        */

        // Zoom nhanh
        Label viewLbl = new Label("View");
        Button zoomInBtn = new Button("######");//"Zoom in"
        Button zoomOutBtn = new Button("######");//"Zoom out"
        Button resetViewBtn = new Button("######");//"Reset view"
        zoomInBtn.setOnAction(e -> { mapCanvas.zoomAtCenter(1.1); });
        zoomOutBtn.setOnAction(e -> { mapCanvas.zoomAtCenter(0.9); });
        resetViewBtn.setOnAction(e -> { mapCanvas.fitAndCenter(); mapCanvas.render(); });

        //Dashboard layout
        sidebar.getChildren().addAll(
                title,
                speedLbl, speedSlider,
                tlLbl, tlAutoBtn, tlManualBtn, tlNextPhaseBtn,
                //vehLbl,spawnVehBtn, clearVehBtn,
                viewLbl, zoomInBtn, zoomOutBtn, resetViewBtn
        );

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(mapCanvas.getCanvas());

        stage.setTitle("SUMO Network Dashboard");
        stage.setScene(new Scene(root));
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}

