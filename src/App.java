import gui.MapCanvas;
import gui.Transform;
import gui.ControlPanel;

import paser.Networkpaser;

import wrapper.SimulationWrapper;
import wrapper.DataType.TrafficLightData;
import wrapper.DataType.VehicleData;
import wrapper.DataType.RouteData;

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

    // File names (will search in multiple locations)
    private static final String NET_FILE_NAME = "test_2_traffic.net.xml";
    private static final String SUMOCFG_FILE_NAME = "test_2_traffic.sumocfg";

    private Networkpaser.NetworkModel model;

    // Helper method to find file in multiple possible locations
    private static String findFile(String fileName) {
        // Try different possible paths
        String[] possiblePaths = {
            "simulationrealtime/resource/" + fileName,
            "../resource/" + fileName,
            "resource/" + fileName,
            "../simulationrealtime/resource/" + fileName,
            System.getProperty("user.dir") + "/simulationrealtime/resource/" + fileName
        };
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return path;
            }
        }
        
        // If not found, return the first path (will throw error with clear message)
        return possiblePaths[0];
    }

    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        String netFile = findFile(NET_FILE_NAME);
        model = Networkpaser.parse(netFile);
        // Canvas bản đồ chuyển thành MapCanvas để quản lý pan/zoom/vẽ
        mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        //mapCanvas.render();

        //Start simulation
        String sumoCfgFile = findFile(SUMOCFG_FILE_NAME);
        simulationWrapper = new SimulationWrapper(sumoCfgFile); // initialize with SUMO config file
        simulationWrapper.setDelay(200); //  set step delay in ms
        simulationWrapper.Start();

        // background thread to advance SUMO steps
        simRunning = true;
        simulationThread = new Thread(() -> {
            while (simRunning && !simulationWrapper.isClosed()) {
                simulationWrapper.Step();
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
            simulationWrapper.End();
        });

    }
    public static void main(String[] args) {launch(args);}
}