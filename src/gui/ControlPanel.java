package gui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import java.util.List;

import wrapper.SimulationWrapper;


public class ControlPanel {
    private final MapCanvas mapCanvas;
    private SimulationWrapper sim;
    private long uniqueID = 0;

    public ControlPanel(MapCanvas mapCanvas, SimulationWrapper inputSim) {
        this.mapCanvas = mapCanvas;
        this.sim = inputSim;
    }

    public ScrollPane createTrafficLightControls() {
        VBox box = new VBox(6);
        Label tlLbl = new Label("Traffic light");
        Button tlAutoBtn = new Button("Auto mode");
        Button tlManualBtn = new Button("Manual mode");
        Button tlNextPhaseBtn = new Button("Next phase");
        tlAutoBtn.setOnAction(e -> System.out.println("TL: auto"));
        tlManualBtn.setOnAction(e -> System.out.println("TL: manual"));
        tlNextPhaseBtn.setOnAction(e -> setTLNextPhaseAll());
        box.getChildren().addAll(tlLbl, tlAutoBtn, tlManualBtn, tlNextPhaseBtn);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        return sp;
    }

    public ScrollPane createVehicleControls() {
        //private static long idCounter = 0;
        VBox box = new VBox(6);
        Label vehicleLable = new Label("Vehicle");
        Button addVehicle = new Button("Add 1 vehicle");
        Button stressTest1 = new Button("Stress Test Max");
        addVehicle.setOnAction(e -> addSingleVehicle1());
        stressTest1.setOnAction(e -> StressTest1());
        box.getChildren().addAll(vehicleLable, addVehicle, stressTest1);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        return sp;
    }

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
            -> { mapCanvas.fitAndCenter(); mapCanvas.render(); });

        box.getChildren().addAll(viewLbl, zoomInBtn, zoomOutBtn, resetViewBtn);
        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        return sp;
    }
// control operations
    public void setTLNextPhaseAll() {
        List<String> TLIDList = sim.getTLIDsList();
        for (String tlID : TLIDList) {
            sim.setTLPhaseNext(tlID);
        }
    } 
    public void addSingleVehicle1() {
        String newID = String.valueOf(uniqueID);
        uniqueID += 1;
        sim.addVehicleBasic(newID);
    }
    public void StressTest1() {
        for(int x = 0; x < 5; x++) {
            int temp = sim.getRouteNum(0);
            for(int y = 0; y < temp; y++) {
                String newID = String.valueOf(uniqueID);
                uniqueID += 1;
                sim.addVehicleNormal(newID, y);
                System.out.println("add" + newID + " into "+ x);
            }
        }
    }
}