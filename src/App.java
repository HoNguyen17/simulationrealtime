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

// Main application class
public class App extends Application {
    private MapCanvas mapCanvas;
    private Transform transform;
    private AnimationTimer simulationTimer;// Field to hold the timer instance
    private SimulationWrapper simulationWrapper; // Field to hold the simulation wrapper
    private Thread simulationThread; // background simulation stepper
    private volatile boolean simRunning = false;

    private static final String NET_FILE = "..\\resource\\test_7_huge.net.xml";
    private static final String SUMOCFG_FILE = "..\\resource\\test_7_huge.sumocfg";

    private Networkpaser.NetworkModel model;



    // Application start method
    @Override
    public void start(Stage stage) throws Exception{
        // load Parse network file
        model = Networkpaser.parse(NET_FILE);

        // Initialize MapCanvas and set model
        mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter(); // Fit and center the network from mapCanvas
        mapCanvas.render(); // Initial render of the map by MapCanvas



        //Start simulation
        simulationWrapper = new SimulationWrapper(SUMOCFG_FILE); // initialize with SUMO config file
        simulationWrapper.setDelay(200); //  set step delay in ms
        simulationWrapper.Start(); // start the simulation

        // background thread to advance SUMO steps
        simRunning = true;
        simulationThread = new Thread(() -> {
            while (simRunning && !simulationWrapper.isClosed()) { // check if simulation is still running
                simulationWrapper.Step();
            }
        }, "Sumo-Stepper");
        simulationThread.setDaemon(true); // set as daemon so it won't block JVM exit
        simulationThread.start(); // start the background thread

        // UI timer to fetch data and render vehicles
        simulationTimer = new AnimationTimer() { // create AnimationTimer instance
            @Override
            public void handle(long now) { // called every frame
                // Vehicles: build vehicle data
                List<String> ids = simulationWrapper.getVehicleIDsList(); // get all vehicle IDs from simulationWrapper
                List<MapCanvas.VehicleData> vds = new ArrayList<>(); // list to hold vehicle data for MapCanvas

                if (ids != null) {
                    for (String id : ids) {
                        SumoPosition2D pos = simulationWrapper.getVehiclePosition(id); // get vehicle position from simulationWrapper
                        if (pos == null) continue;
                        double angle = simulationWrapper.getVehicleAngle(id);
                        SumoColor suColor = simulationWrapper.getVehicleColor(id); // get vehicle color from simulationWrapper

                        //  add vehicle Color
                        double tempR = ((double)(suColor.r & 0xFF))/255;
                        double tempG = ((double)(suColor.g & 0xFF))/255;
                        double tempB = ((double)(suColor.b & 0xFF))/255;
                        double tempA = ((double)(suColor.a & 0xFF))/255;
                        Color vehicleColor = new Color(tempR, tempG, tempB, tempA);
                        vds.add(new MapCanvas.VehicleData(id, pos.x, pos.y, angle, vehicleColor));
                    }
                }
                mapCanvas.setVehicleData(vds); // update vehicle data in MapCanvas
                mapCanvas.render(); // render the updated map

                // Traffic lights: build lane-end bars data
                List<MapCanvas.TrafficLightData> tlDatas = new ArrayList<>(); // list to hold traffic light data for MapCanvas
                List<String> tlIds = simulationWrapper.getTLIDsList(); // get all traffic light IDs from simulationWrapper
                if (tlIds != null) {
                    for (String tlId : tlIds) {
                    String def = simulationWrapper.getTLPhaseDef(tlId); // get traffic light phase definition from simulationWrapper
                    if (def == null) continue;

                    List<Character> states = new ArrayList<>(); // list to hold states per phase
                    for (int i = 0; i < def.length(); i++) states.add(def.charAt(i)); // populate states list

                    // Collect from/to lane ids per controlled link
                    List<String> fromLaneIds = new ArrayList<>();
                    List<String> toLaneIds = new ArrayList<>();
                    int links = simulationWrapper.getTLControlledLinksNum(tlId);
                    for (int i = 0; i < links; i++) {
                        List<String> defFromTo = simulationWrapper.getTLDefFromTo(tlId, i); // get from/to lane ids from simulationWrapper
                        if (defFromTo == null || defFromTo.size() < 3) continue;
                        // format from wrapper: [stateChar, fromLaneId, toLaneId]
                        fromLaneIds.add(defFromTo.get(1));
                        toLaneIds.add(defFromTo.get(2));
                }

                // Position (x,y) not needed for bar rendering; pass 0,0
                tlDatas.add(new MapCanvas.TrafficLightData(tlId, 0, 0, states, fromLaneIds, toLaneIds)); // add traffic light data to list
            }
        }
        mapCanvas.setTrafficLightData(tlDatas); // update traffic light data in MapCanvas
                
        }
        };
        simulationTimer.start(); // start the AnimationTimer

        // Use Dashboard for controls from gui package
        Dashboard dashboard = new Dashboard(mapCanvas, simulationWrapper);
        // Layout setup
        BorderPane root = new BorderPane();
        root.setLeft(dashboard);
        root.setCenter(mapCanvas.getCanvas());
        // Show the stage
        stage.setTitle("SUMO Network Dashboard");
        stage.setScene(new Scene(root));
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

    // Main method use Run to start application
    public static void main(String[] args) {
        launch(args);
    }
}