import gui.MapCanvas;
import gui.MapCanvas.VehicleData;
import gui.Dashboard;

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


public class App extends Application {
    private MapCanvas mapCanvas;
    private AnimationTimer simulationTimer;// Field to hold the timer instance
    private SimulationWrapper simulationWrapper; // Field to hold the simulation wrapper
    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        Networkpaser.NetworkModel model = Networkpaser.load("../resource/Netedit_requirement.net.xml");

        // Canvas bản đồ chuyển thành MapCanvas để quản lý pan/zoom/vẽ
        mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        mapCanvas.render();

        // NEW SIMULATION STARTUP
        try {
            simulationWrapper = new SimulationWrapper(
                    "..\\resource\\Netedit_testrun.sumocfg" // Path to your config
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
                                        javafx.scene.paint.Color.WHITE // Default color for all vehicles
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





        // Sidebar trái: dùng Dashboard trong package gui
        Dashboard dashboard = new Dashboard(mapCanvas);

        BorderPane root = new BorderPane();
        root.setLeft(dashboard);
        root.setCenter(mapCanvas.getCanvas());



        stage.setTitle("SUMO Network Dashboard");
        stage.setScene(new Scene(root));
        stage.show();

    }
    public static void main(String[] args) {
        launch(args);
    }
}