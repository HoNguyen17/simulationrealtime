package gui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import wrapper.SimulationWrapper;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;
import java.util.List;
import java.util.ArrayList;

/**
 * Class để quản lý việc render xe trên canvas
 */
public class VehicleRenderer {
    
    /**
     * Cấu trúc dữ liệu để lưu thông tin xe
     */
    public static class VehicleData {
        public double x, y;
        public double angle; // góc hướng (degrees)
        public Color color;
        
        public VehicleData(double x, double y, double angle, Color color) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.color = color;
        }
    }
    
    private List<VehicleData> vehicles = new ArrayList<>();
    
    /**
     * Cập nhật danh sách xe từ SimulationWrapper
     */
    public void updateFromSimulation(SimulationWrapper sim) {
        if (sim == null) {
            clear();
            return;
        }
        
        List<String> vehicleIDs = sim.getIDList();
        if (vehicleIDs == null || vehicleIDs.isEmpty()) {
            clear();
            return;
        }
        
        List<VehicleData> vehicleDataList = new ArrayList<>();
        for (String vehicleID : vehicleIDs) {
            SumoPosition2D position = sim.getPosition(vehicleID);
            if (position == null) continue;
            
            double angle = sim.getAngle(vehicleID);
            SumoColor sumoColor = sim.getColor(vehicleID);
            
            // Chuyển đổi SumoColor sang JavaFX Color
            Color color;
            if (sumoColor != null && !(sumoColor.r == -1 && sumoColor.g == -1 && sumoColor.b == 0 && sumoColor.a == -1)) {
                // Chuẩn hóa giá trị màu từ (0-255) sang (0.0-1.0)
                color = Color.rgb(
                    Math.max(0, Math.min(255, sumoColor.r)),
                    Math.max(0, Math.min(255, sumoColor.g)),
                    Math.max(0, Math.min(255, sumoColor.b)),
                    sumoColor.a >= 0 ? sumoColor.a / 255.0 : 1.0
                );
            } else {
                // Màu mặc định (xanh dương)
                color = Color.BLUE;
            }
            
            vehicleDataList.add(new VehicleData(position.x, position.y, angle, color));
        }
        
        this.vehicles = vehicleDataList;
    }
    
    /**
     * Thiết lập danh sách xe trực tiếp
     */
    public void setVehicles(List<VehicleData> vehicles) {
        this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
    }
    
    /**
     * Lấy danh sách xe hiện tại
     */
    public List<VehicleData> getVehicles() {
        return new ArrayList<>(vehicles);
    }
    
    /**
     * Xóa tất cả xe
     */
    public void clear() {
        this.vehicles.clear();
    }
    
    /**
     * Render tất cả xe lên GraphicsContext
     * @param g GraphicsContext để vẽ
     * @param scale Tỷ lệ zoom
     * @param offsetX Offset X
     * @param offsetY Offset Y
     */
    public void render(GraphicsContext g, double scale, double offsetX, double offsetY) {
        for (VehicleData vehicle : vehicles) {
            double x = vehicle.x * scale + offsetX;
            double y = vehicle.y * scale + offsetY;
            
            // Vẽ xe dưới dạng hình chữ nhật có góc xoay
            g.save(); // Lưu transform hiện tại
            g.translate(x, y);
            g.rotate(vehicle.angle);
            
            // Thiết lập màu xe
            g.setFill(vehicle.color);
            g.setStroke(Color.BLACK);
            g.setLineWidth(0.5);
            
            // Vẽ hình chữ nhật (kích thước: 4x2 units, căn giữa)
            double vehicleWidth = 4.0 * scale;
            double vehicleHeight = 2.0 * scale;
            g.fillRect(-vehicleWidth / 2, -vehicleHeight / 2, vehicleWidth, vehicleHeight);
            g.strokeRect(-vehicleWidth / 2, -vehicleHeight / 2, vehicleWidth, vehicleHeight);
            
            g.restore(); // Khôi phục transform
        }
    }
    
    /**
     * Lấy số lượng xe hiện tại
     */
    public int getVehicleCount() {
        return vehicles.size();
    }
}

