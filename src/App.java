import gui.MapCanvas; //
import gui.VehicleRenderer;
import paser.Networkpaser;
import javafx.application.Application;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import wrapper.SimulationWrapper;


public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        Networkpaser.NetworkModel model = Networkpaser.load("../resource/test_2_traffic.net.xml");

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
        
        // Khởi động simulation và animation loop để cập nhật xe từ XML
        try {
            String configFile = "../resource/test_2_traffic.sumocfg";
            SimulationWrapper sim = new SimulationWrapper(configFile, 1.0, "sumo");
            sim.Start();
            
            VehicleRenderer vehicleRenderer = mapCanvas.getVehicleRenderer();
            
            // Chạy simulation trong thread riêng để không block UI
            Thread simulationThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        sim.Step();
                        Thread.sleep(100); // Delay 100ms giữa các bước
                    }
                } catch (InterruptedException e) {
                    // Thread bị interrupt, dừng simulation
                } catch (Exception e) {
                    System.err.println("Error in simulation thread: " + e.getMessage());
                }
            });
            simulationThread.setDaemon(true);
            simulationThread.start();
            
            // AnimationTimer để cập nhật và render UI
            AnimationTimer animationTimer = new AnimationTimer() {
                private long lastUpdate = 0;
                private final long UPDATE_INTERVAL = 100_000_000; // 100ms (nanoseconds)
                
                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= UPDATE_INTERVAL) {
                        try {
                            // Cập nhật xe từ simulation (xe từ XML)
                            vehicleRenderer.updateFromSimulation(sim);
                            
                            // Render lại
                            mapCanvas.render();
                            
                            lastUpdate = now;
                        } catch (Exception e) {
                            // Bỏ qua lỗi để không crash app
                            System.err.println("Error updating vehicles: " + e.getMessage());
                        }
                    }
                }
            };
            
            animationTimer.start();
            
            // Dừng animation và simulation khi đóng cửa sổ
            final SimulationWrapper finalSim = sim;
            stage.setOnCloseRequest(e -> {
                animationTimer.stop();
                simulationThread.interrupt();
                try {
                    finalSim.End();
                } catch (Exception ex) {
                    // Ignore
                }
            });
        } catch (Exception e) {
            System.err.println("Không thể khởi động simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        launch(args);
    }

}
