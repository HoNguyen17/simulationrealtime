import gui.MapCanvas; //
import paser.Networkpaser;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;


public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        Networkpaser.NetworkModel model = Networkpaser.load("../resource/Netedit_requirement.net.xml");

        // Canvas bản đồ chuyển thành MapCanvas để quản lý pan/zoom/vẽ
        MapCanvas mapCanvas = new MapCanvas(1000, 800);
        mapCanvas.setModel(model);
        mapCanvas.fitAndCenter();
        mapCanvas.render();

        // Sidebar trái: control dashboard
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(12));
        sidebar.setPrefWidth(280);

        Label title = new Label("Dashboard");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Điều khiển tốc độ mô phỏng Simulation speed
        Label speedLbl = new Label("########");
        Slider speedSlider = new Slider(0.1, 5.0, 1.0);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.valueProperty().addListener((obs, oldV, newV) -> {
            // TODO: nối với SimulationController.setSpeed(newV.doubleValue());
            System.out.println("Speed: " + newV.doubleValue());
        });
         

        // Điều khiển đèn giao thông 
        Label tlLbl = new Label("Traffic light");
        Button tlAutoBtn = new Button("######");//"Auto mode"
        Button tlManualBtn = new Button("######");//"Manual mode"
        Button tlNextPhaseBtn = new Button("######");//"Next phase"
        tlAutoBtn.setOnAction(e -> {
            // TODO: TrafficLightController.setAuto(true);
            System.out.println("TL: auto");
        });
        tlManualBtn.setOnAction(e -> {
            // TODO: TrafficLightController.setAuto(false);
            System.out.println("TL: manual");
        });
        tlNextPhaseBtn.setOnAction(e -> {
            // TODO: TrafficLightController.stepPhase();
            System.out.println("TL: next phase");
        });

        /* 
        // Điều khiển xe
        Label vehLbl = new Label("Vehicles");
        Button spawnVehBtn = new Button("Spawn vehicle");
        Button clearVehBtn = new Button("Clear vehicles");
        spawnVehBtn.setOnAction(e -> {
            // TODO: VehicleController.spawnAtSelectedJunction();
            System.out.println("Vehicle: spawn");
        });
        clearVehBtn.setOnAction(e -> {
            // TODO: VehicleController.clearAll();
            System.out.println("Vehicle: clear");
        });
        */

        // Zoom nhanh
        Label viewLbl = new Label("View");
        Button zoomInBtn = new Button("######");//"Zoom in"
        Button zoomOutBtn = new Button("######");//"Zoom out"
        Button resetViewBtn = new Button("######");//"Reset view"
        zoomInBtn.setOnAction(e -> { mapCanvas.zoomAtCenter(1.1); });
        zoomOutBtn.setOnAction(e -> { mapCanvas.zoomAtCenter(0.9); });
        resetViewBtn.setOnAction(e -> { mapCanvas.fitAndCenter(); mapCanvas.render(); });

        //Dashboard layout
        sidebar.getChildren().addAll(
            title,
            speedLbl, speedSlider,
            tlLbl, tlAutoBtn, tlManualBtn, tlNextPhaseBtn,
            //vehLbl,spawnVehBtn, clearVehBtn,
            viewLbl, zoomInBtn, zoomOutBtn, resetViewBtn
        );

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(mapCanvas.getCanvas());

        stage.setTitle("SUMO Network Dashboard");
        stage.setScene(new Scene(root));
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }

}
