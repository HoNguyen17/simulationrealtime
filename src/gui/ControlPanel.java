package gui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import java.util.List;

import wrapper.SimulationWrapper;

// This class is responsible for creating and managing the user interface elements (buttons and labels) that allow the user to interact with and control the running SUMO traffic simulation and its visualization
public class ControlPanel { // Composition -> the ControlPanel is tightly coupled with two other main classes
    private final MapCanvas mapCanvas; // used to trigger view-related actions (zoom, pan, reset)
    private final SimulationWrapper sim; // Used to send commands directly to the running SUMO simulation (e.g., add a vehicle, change a traffic light phase)
    private long uniqueID = 0; // a counter used to generate unique string IDs for new vehicles added during runtime

    // Constructor
    public ControlPanel(MapCanvas mapCanvas, SimulationWrapper inputSim) {
        this.mapCanvas = mapCanvas;
        this.sim = inputSim;
    }

    // This method sets up a vertical box (VBox) containing controls related to traffic signal management
    public ScrollPane createTrafficLightControls() {
        VBox box = new VBox(6);
        Label tlLbl = new Label("Traffic light");
        Button tlAutoBtn = new Button("Auto mode");
        Button tlManualBtn = new Button("Manual mode");
        Button tlNextPhaseBtn = new Button("Next phase");
        tlAutoBtn.setOnAction(e -> System.out.println("TL: auto"));
        tlManualBtn.setOnAction(e -> System.out.println("TL: manual"));
        tlNextPhaseBtn.setOnAction(e -> setTLNextPhaseAll()); // call setTLNextPhaseAll(), which iterates through all traffic lights and advances each one to its next phase
        box.getChildren().addAll(tlLbl, tlAutoBtn, tlManualBtn, tlNextPhaseBtn);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        return sp;
    }

    // Vehicle control section -> this method sets up controls for injecting new entities into the simulation
    public ScrollPane createVehicleControls() {
        //private static long idCounter = 0;
        VBox box = new VBox(6);
        Label vehicleLable = new Label("Vehicle");
        Button addVehicle = new Button("Add 1 vehicle");
        Button stressTest1 = new Button("Stress Test Max");
        addVehicle.setOnAction(e -> addSingleVehicle1()); // call addSingleVehicle1(), which generates a unique ID and uses sim.addVehicleBasic() to inject a vehicle onto the first available route
        stressTest1.setOnAction(e -> StressTest1()); // call StressTest1(), which repeatedly tries to inject many vehicles into all available routes to test system limits
        box.getChildren().addAll(vehicleLable, addVehicle, stressTest1);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        return sp;
    }

    // View control section -> this method sets up controls for manipulating the visual map provided by the MapCanvas
    public ScrollPane createViewControls() {
        VBox box = new VBox(6);
        Label viewLbl = new Label("View");
        Button zoomInBtn = new Button("Zoom in");
        Button zoomOutBtn = new Button("Zoom out");
        Button resetViewBtn = new Button("Reset view");
        zoomInBtn.setOnAction(e
                -> mapCanvas.zoomAtCenter(1.1));
        zoomOutBtn.setOnAction(e
                -> mapCanvas.zoomAtCenter(0.9));
        resetViewBtn.setOnAction(e
                -> { mapCanvas.fitAndCenter(); mapCanvas.render(); }); // reset view -> call mapCanvas.fitAndCenter() and mapCanvas.render() to reset the view to fit the entire road network back onto the canvas

        box.getChildren().addAll(viewLbl, zoomInBtn, zoomOutBtn, resetViewBtn);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        return sp;
    }
    // control operations
    public void setTLNextPhaseAll() { // set all traffic lights to next phase
        List<String> TLIDList = sim.getTLIDsList();
        for (String tlID : TLIDList) {
            sim.setTLPhaseNext(tlID);
        }
    }

    // Add a single vehicle with unique ID
    public void addSingleVehicle1() { //
        String newID = String.valueOf(uniqueID);
        uniqueID += 1;
        sim.addVehicleBasic(newID); // addVehicleBasic in SimulationWrapper
    }

    // Stress test: add max vehicles to all routes
    public void StressTest1() {
        for(int x = 0; x < 5; x++) {
            int temp = sim.getRouteNum(0); // getRouteNum in SimulationWrapper
            for(int y = 0; y < temp; y++) {
                String newID = String.valueOf(uniqueID);
                uniqueID += 1;
                sim.addVehicleNormal(newID, y); // addVehicleNormal in SimulationWrapper
                System.out.println("add" + newID + " into "+ x);
            }
        }
    }
}