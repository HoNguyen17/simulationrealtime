import gui.MapCanvas;
import gui.Transform;
import gui.ControlPanel;

import paser.Networkpaser;

import wrapper.SimulationWrapper;
import wrapper.DataType.TrafficLightData;
import wrapper.DataType.VehicleData;
import wrapper.DataType.RouteData;

import tracker.Statistic;

import java.util.List;
import java.util.ArrayList;

import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoPosition2D;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;
import java.io.File;

public class App extends Application {
    private MapCanvas mapCanvas;
    private Transform transform;
    private AnimationTimer simulationTimer;// Field to hold the timer instance
    private SimulationWrapper simulationWrapper; // Field to hold the simulation wrapper
    private Thread simulationThread; // background simulation stepper
    private volatile boolean simRunning = false;

    private static final String NET_FILE_RELATIVE = "simulationrealtime/resource/test_7_huge.net.xml";
    private static final String SUMOCFG_FILE_RELATIVE = "simulationrealtime/resource/test_7_huge.sumocfg";

    private Networkpaser.NetworkModel model;

    /**
     * Helper method to find resource file by trying multiple possible paths
     */
    private static String findResourceFile(String relativePath) {
        // Try different possible locations
        String[] possiblePaths = {
            relativePath,  // Direct path (if running from workspace root)
            "../" + relativePath,  // One level up
            "../../" + relativePath,  // Two levels up
            "../../../" + relativePath,  // Three levels up (from out/production/traffic_light-main)
            "../../../../" + relativePath,  // Four levels up
            System.getProperty("user.dir") + "/" + relativePath  // Absolute from user.dir
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return path;
            }
        }

        // If none found, return the original path (will throw error with better message)
        return relativePath;
    }

    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        String netFile = findResourceFile(NET_FILE_RELATIVE);
        model = Networkpaser.parse(netFile);
        // Canvas bản đồ chuyển thành MapCanvas để quản lý pan/zoom/vẽ
        mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        //mapCanvas.render();

        //Start simulation
        String sumocfgFile = findResourceFile(SUMOCFG_FILE_RELATIVE);
        simulationWrapper = new SimulationWrapper(sumocfgFile); // initialize with SUMO config file
        simulationWrapper.setDelay(200); //  set step delay in ms
        simulationWrapper.Start();
        Statistic.initialize(simulationWrapper);


        // background thread to advance SUMO steps
        simRunning = true;
        simulationThread = new Thread(() -> {
            while (simRunning && !simulationWrapper.isClosed()) {
                simulationWrapper.Step();
                Statistic.addNewData();
            }
        }, "Sumo-Stepper");
        simulationThread.setDaemon(true);
        simulationThread.start();


//FXML thing
        FXMLLoader load_fxml = new FXMLLoader(getClass().getResource("/gui/DecApp.fxml"));
        Parent root;
        try {root = load_fxml.load();}
        catch (Exception e) {
            System.err.println("fail to load FXML: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        ControlPanel controller_fxml = load_fxml.getController();
        if (controller_fxml != null) {controller_fxml.setMapCanvas(mapCanvas, simulationWrapper);}
//FXML thing

        // UI timer to fetch data and render vehicles
        simulationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Create copy of current vehicle datas from wrapper
                List<VehicleData> vehDatas = new ArrayList<>();
                List<String> vehIds = simulationWrapper.getVehicleIDsList();
                if (vehIds != null) {
                    for (String vehId : vehIds) {
                        vehDatas.add(simulationWrapper.makeVehicleCopy(vehId));
                    }
                }

                // Create copy of current traffic light datas from wrapper
                List<TrafficLightData> tlDatas = new ArrayList<>();
                List<String> tlIds = simulationWrapper.getTLIDsList();
                if (tlIds != null) {
                    for (String tlId : tlIds) {
                        tlDatas.add(simulationWrapper.makeTLCopy(tlId));
                    }
                }
                // Create copy of current route datas from wrapper
                if(mapCanvas.getRenderMode() == 1 && mapCanvas.getUpdateRoute()) {
                    List<RouteData> rouData = new ArrayList<>();
                    List<String> routeIds = simulationWrapper.getRouteIDsList();
                    if (routeIds != null) {
                        for (String routeId : routeIds) {
                            rouData.add(simulationWrapper.makeRouteCopy(routeId));
                        }
                    }
                    mapCanvas.setRouteData(rouData);
                }

                // Set the copied datas into mapCanvas and render
                mapCanvas.setVehicleData(vehDatas);
                mapCanvas.setTrafficLightData(tlDatas);
                mapCanvas.render();

                if (controller_fxml != null) {
                    controller_fxml.updateUI(now);
                }
            }
        };
        simulationTimer.start();

        stage.setTitle("SUMO Network Dashboard");
        stage.setScene(new Scene(root));
        stage.setMaximized(true);
        stage.show();

        // Ensure proper shutdown
        stage.setOnCloseRequest(e -> {
            simRunning = false;
            if (simulationTimer != null) simulationTimer.stop();
            if (simulationThread != null) {
                try { simulationThread.join(500); } catch (InterruptedException ex) { /* ignore */ }
            }
            // Export statistics before closing (CSV export with file chooser)
            Statistic.exportCSV(stage);
            simulationWrapper.End();
        });

    }
    public static void main(String[] args) {launch(args);}
}