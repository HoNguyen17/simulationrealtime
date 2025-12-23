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
        this.controlPanel = new ControlPanel(mapCanvas, input);
        this.setSpacing(10);
        this.setPadding(new Insets(12));
        this.setPrefWidth(280);

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");


        var tls = controlPanel.createTrafficLightControls();
        var veh = controlPanel.createVehicleControls();
        var view = controlPanel.createViewControls();
        
        
        this.getChildren().addAll(title, tls, veh, view);
    }
}