import gui.MapCanvas;
import gui.Transform;
import gui.ControlPanel;

import paser.Networkpaser;

import wrapper.SimulationWrapper;
import wrapper.DataType.TrafficLightData;
import wrapper.DataType.VehicleData;
import wrapper.DataType.RouteData;

import tracker.Statistic;

import logger.AppLog;

import java.util.List;
import java.util.ArrayList;

import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoPosition2D;

import javafx.animation.AnimationTimer;
import javafx.application.Application;

import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;

import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

import java.util.logging.Logger;
import java.util.logging.Level;

public class App extends Application {
    private MapCanvas mapCanvas;
    private Transform transform;
    private AnimationTimer simulationTimer;// Field to hold the timer instance
    private SimulationWrapper simulationWrapper; // Field to hold the simulation wrapper
    private Thread simulationThread; // background simulation stepper
    private volatile boolean simRunning = false;

    private static final String NET_FILE = "../SumoConfig/test_8_larger.net.xml";
    private static final String SUMOCFG_FILE = "../SumoConfig/test_8_larger.sumocfg";
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private Networkpaser.NetworkModel model;

    @Override
    public void start(Stage stage) throws Exception {
        // Set up logger
        AppLog.initialize();
        // Parse data from .net file to model
        model = Networkpaser.parse(NET_FILE);
        // set up MapCanvas
        mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        //Start simulation
        simulationWrapper = new SimulationWrapper(SUMOCFG_FILE);
        simulationWrapper.setDelay(200); 
        simulationWrapper.Start();
        Statistic.initialize(simulationWrapper);
        // background thread to advance SUMO steps
        simRunning = true;
        simulationThread = new Thread(() -> {
            while (simRunning && !simulationWrapper.isClosed()) {
                simulationWrapper.Step();
                Statistic.addNewEdgeData();
            }
        }, "Sumo-Stepper");
        simulationThread.start();
        
        FXMLLoader load_fxml = new FXMLLoader(getClass().getResource("/gui/DecApp.fxml"));
        Parent root;
        try {root = load_fxml.load();} 
        catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load FXML.", e);
            return;
        }
        
        ControlPanel controller_fxml = load_fxml.getController();
        if (controller_fxml != null) {controller_fxml.setMapCanvas(mapCanvas, simulationWrapper);}

        // UI timer to fetch data and render vehicles
        simulationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!simulationWrapper.isPaused()) {
                    // Create copy of current vehicle datas from wrapper
                    List<VehicleData> vehicleDatas = new ArrayList<>();
                    List<String> vehicleIds = simulationWrapper.getVehicleIDsList();
                    if (vehicleIds != null) {
                        for (String vehId : vehicleIds) {
                            vehicleDatas.add(simulationWrapper.makeVehicleCopy(vehId));
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
                        List<RouteData> routeData = new ArrayList<>();
                        List<String> routeIds = simulationWrapper.getRouteIDsList();
                        if (routeIds != null) {
                            for (String routeId : routeIds) {
                                routeData.add(simulationWrapper.makeRouteCopy(routeId));
                            }
                        }
                        mapCanvas.setRouteData(routeData);
                    }
                    // Set the copied datas into mapCanvas and render
                    mapCanvas.setVehicleData(vehicleDatas);
                    mapCanvas.setTrafficLightData(tlDatas);
                    mapCanvas.render();
                }

                if (controller_fxml != null) {
                controller_fxml.updateUI(now); // Update UI elements in ControlPanel (Nguyen)
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
            simulationWrapper.End();
        });
    }
    public static void main(String[] args) {launch(args);}
}