import gui.MapCanvas;
import gui.Dashboard;
import gui.Transform;
import gui.ControlPanel;

import paser.Networkpaser;

import wrapper.SimulationWrapper;
import wrapper.DataType.TrafficLightData;
import wrapper.DataType.VehicleData;

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

public class App extends Application {
    private MapCanvas mapCanvas;
    private Transform transform;
    private AnimationTimer simulationTimer;// Field to hold the timer instance
    private SimulationWrapper simulationWrapper; // Field to hold the simulation wrapper
    private Thread simulationThread; // background simulation stepper
    private volatile boolean simRunning = false;

    private static final String NET_FILE = "../resource/test_7_huge.net.xml";
    private static final String SUMOCFG_FILE = "../resource/test_7_huge.sumocfg";

    private Networkpaser.NetworkModel model;


    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        model = Networkpaser.parse(NET_FILE);
        // Canvas bản đồ chuyển thành MapCanvas để quản lý pan/zoom/vẽ
        mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        //mapCanvas.render();

        //Start simulation
        simulationWrapper = new SimulationWrapper(SUMOCFG_FILE); // initialize with SUMO config file
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