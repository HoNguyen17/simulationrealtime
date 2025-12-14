import gui.MapCanvas;
import gui.Dashboard;

import paser.Networkpaser;
import wrapper.SimulationWrapper;


import javafx.animation.AnimationTimer;
import java.util.List;
import java.util.ArrayList;
import gui.Transform;
import javafx.scene.paint.Color;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoPosition2D;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class App extends Application {
    private MapCanvas mapCanvas;
    private Transform transform;
    private AnimationTimer simulationTimer;// Field to hold the timer instance
    private SimulationWrapper simulationWrapper; // Field to hold the simulation wrapper
    private Thread simulationThread; // background simulation stepper
    private volatile boolean simRunning = false;

    private static final String NET_FILE = "simulationrealtime/resource/Netedit_requirement.net.xml";
    private static final String SUMOCFG_FILE = "simulationrealtime/resource/Netedit_testrun.sumocfg";

    private Networkpaser.NetworkModel model;




    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        model = Networkpaser.parse(NET_FILE);
        // Canvas bản đồ chuyển thành MapCanvas để quản lý pan/zoom/vẽ
        mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        mapCanvas.render();


        // Sidebar trái: dùng Dashboard trong package gui
        Dashboard dashboard = new Dashboard(mapCanvas);

        BorderPane root = new BorderPane();
        root.setLeft(dashboard);
        root.setCenter(mapCanvas.getCanvas());

        stage.setTitle("SUMO Network Dashboard");
        stage.setScene(new Scene(root));
        stage.show();

        //Start simulation
        simulationWrapper = new SimulationWrapper(SUMOCFG_FILE); // initialize with SUMO config file
        simulationWrapper.setDelay(500); //  set step delay in ms
        simulationWrapper.Start();
        
        // Set simulation wrapper to map canvas for traffic light rendering
        mapCanvas.setSimulationWrapper(simulationWrapper);

        // background thread to advance SUMO steps
        simRunning = true;
        simulationThread = new Thread(() -> {
            while (simRunning && !simulationWrapper.isClosed()) {
                simulationWrapper.Step();
            }
        }, "Sumo-Stepper");
        simulationThread.setDaemon(true);
        simulationThread.start();

        // UI timer to fetch data and render vehicles
        simulationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                List<String> ids = simulationWrapper.getVehicleIDsList();
                List<MapCanvas.VehicleData> vds = new ArrayList<>();

                if (ids != null) {
                    for (String id : ids) {
                        SumoPosition2D pos = simulationWrapper.getVehiclePosition(id);
                        if (pos == null) continue;
                        double angle = simulationWrapper.getVehicleAngle(id);
                        SumoColor sc = simulationWrapper.getVehicleColor(id);

                        //  add vehicke Color
                        Color vehicleColor = Color.WHITE;
                        vds.add(new MapCanvas.VehicleData(id, pos.x, pos.y, angle, vehicleColor));
                    }
                }
                mapCanvas.setVehicleData(vds);
                mapCanvas.render();

                // Traffic lights: build lane-end bars data
                List<MapCanvas.TrafficLightData> tlDatas = new ArrayList<>();
                List<String> tlIds = simulationWrapper.getTLIDsList();
                if (tlIds != null) {
                    for (String tlId : tlIds) {
                        String def = simulationWrapper.getTLPhaseDef(tlId); // e.g., "rygR..."
                        if (def == null) continue;

                        List<Character> states = new ArrayList<>();
                        for (int i = 0; i < def.length(); i++) states.add(def.charAt(i));

                        // Collect from/to lane ids per controlled link
                        List<String> fromLaneIds = new ArrayList<>();
                        List<String> toLaneIds = new ArrayList<>();
                        int links = simulationWrapper.getTLControlledLinksNum(tlId);
                        for (int i = 0; i < links; i++) {
                            List<String> defFromTo = simulationWrapper.getTLDefFromTo(tlId, i);
                            if (defFromTo == null || defFromTo.size() < 3) continue;
                            // format from wrapper: [stateChar, fromLaneId, toLaneId]
                            fromLaneIds.add(defFromTo.get(1));
                            toLaneIds.add(defFromTo.get(2));
                        }

                        // Position (x,y) not needed for bar rendering; pass 0,0
                        tlDatas.add(new MapCanvas.TrafficLightData(tlId, 0, 0, states, fromLaneIds, toLaneIds));
                    }
                }
                mapCanvas.setTrafficLightData(tlDatas);

            }
        };
        simulationTimer.start();

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