import gui.MapCanvas; //
//import gui.VehicleRenderer;
//import wrapper.SimulationWrapper;
import gui.Dashboard;
import paser.Networkpaser;


//import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class App extends Application {
    private MapCanvas mapCanvas;
    //private SimulationWrapper sim;
    @Override
    public void start(Stage stage) throws Exception{
        // Tải model mạng lưới
        Networkpaser.NetworkModel model = Networkpaser.load("../resource/Netedit_requirement.net.xml");

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
        /*
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

        */
    }
    public static void main(String[] args) {
        launch(args);
    }
}