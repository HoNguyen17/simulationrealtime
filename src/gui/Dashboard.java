package gui;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import wrapper.SimulationWrapper;

// Dashboard panel containing control sections
// This is a JavaFX layout container (VBox) intended to serve as the main control panel interface for the SUMO traffic simulation application
// Inheritance: Dashboard extends VBox, meaning it arranges its child components vertically
// Composition: It relies on two key components passed to its constructor: MapCanvas and SimulationWrapper
// Encapsulation: It contains an instance of ControlPanel, which is where the actual buttons and action logic are defined. The Dashboard class acts primarily as the visual container and organizer for these controls
public class Dashboard extends VBox {
    private final MapCanvas mapCanvas;
    private final ControlPanel controlPanel;

    // Constructor
    public Dashboard(MapCanvas mapCanvas, SimulationWrapper input) {
        this.mapCanvas = mapCanvas;
        this.controlPanel = new ControlPanel(mapCanvas, input); // create the ControlPanel, passing the visualization (mapCanvas) and simulation interface (input) so that the buttons and controls can interact with both the view and the simulator
        this.setSpacing(10); // vertical spacing between elements
        this.setPadding(new Insets(12)); // padding around the edges
        this.setPrefWidth(280); // set a fixed width for the control panel

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");


        // call the factory methods on the controlPanel instance to create the three main sections (Traffic Lights, Vehicles, and View controls). These sections are then added sequentially to the Dashboard VBox, structuring the application's control area
        var tls = controlPanel.createTrafficLightControls();
        var veh = controlPanel.createVehicleControls();
        var view = controlPanel.createViewControls();


        this.getChildren().addAll(title, tls, veh, view);
    }
}