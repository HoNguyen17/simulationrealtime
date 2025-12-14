package gui;


import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;




public class ControlPanel {
    private final MapCanvas mapCanvas;

    public ControlPanel(MapCanvas mapCanvas) {
        this.mapCanvas = mapCanvas;
    }

    public ScrollPane createTrafficLightControls() {
        VBox box = new VBox(6);
        Label tlLbl = new Label("Traffic light");
        Button tlAutoBtn = new Button("Auto mode");
        Button tlManualBtn = new Button("Manual mode");
        Button tlNextPhaseBtn = new Button("Next phase");
        tlAutoBtn.setOnAction(e -> System.out.println("TL: auto"));
        tlManualBtn.setOnAction(e -> System.out.println("TL: manual"));
        tlNextPhaseBtn.setOnAction(e -> System.out.println("TL: next phase"));
        box.getChildren().addAll(tlLbl, tlAutoBtn, tlManualBtn, tlNextPhaseBtn);
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


}