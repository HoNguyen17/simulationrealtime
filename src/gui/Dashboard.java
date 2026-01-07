package gui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import wrapper.SimulationWrapper;

public class Dashboard extends VBox {
    private final MapCanvas mapCanvas;
    private final ControlPanel controlPanel;

    public Dashboard(MapCanvas mapCanvas, SimulationWrapper input) {
        this.mapCanvas = mapCanvas;
        this.controlPanel = new ControlPanel();
        this.controlPanel.setMapCanvas(mapCanvas, input);

        this.setSpacing(10);
        this.setPadding(new Insets(12));
        this.setPrefWidth(280);

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");


        this.getChildren().addAll(title);

        System.out.println("Dashboard initialized. Note: Controls are loaded via FXML.");
    }
}