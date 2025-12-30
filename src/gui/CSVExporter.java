package gui;

import wrapper.SimulationWrapper;
import wrapper.DataType.VehicleData;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Utility class to export traffic statistics to CSV file
 */
public class CSVExporter {
    
    /**
     * Export traffic statistics to CSV file
     * @param simulationWrapper Wrapper to get statistics from SUMO
     * @param filePath CSV file path to save
     * @return true if export successful, false if error
     */
    public static boolean exportToCSV(
            SimulationWrapper simulationWrapper,
            String filePath) {
        
        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            if (simulationWrapper == null || simulationWrapper.isClosed()) {
                System.err.println("Simulation wrapper is null or closed");
                return false;
            }
            
            // ========== TABLE 1: VEHICLE DATA ==========
            writer.append("=== VEHICLE DATA ===\n");
            writer.append("Vehicle,Color,Speed,depart,arrival,duration\n");
            
            // Get all vehicle data
            List<String> vehicleIDs = simulationWrapper.getVehicleIDsList();
            Map<String, Double> travelTimeMap = simulationWrapper.getTravelTimeDistribution();
            double currentTime = simulationWrapper.getTime(0);
            
            // Write data for each vehicle
            for (String vehID : vehicleIDs) {
                // Get vehicle data
                VehicleData vehData = simulationWrapper.makeVehicleCopy(vehID);
                if (vehData == null || !vehData.getValidity()) continue;
                
                // Vehicle ID
                writer.append(escapeCSV(vehID)).append(",");
                
                // Color (hex format #RRGGBB)
                javafx.scene.paint.Color color = simulationWrapper.getVehicleColor(vehID);
                String colorHex = colorToHex(color);
                writer.append(escapeCSV(colorHex)).append(",");
                
                // Speed
                double speed = vehData.getSpeed(0);
                writer.append(String.format("%.3f", speed)).append(",");
                
                // Departure time
                double departTime = simulationWrapper.getVehicleDepartureTime(vehID);
                writer.append(String.format("%.3f", departTime)).append(",");
                
                // Arrival time (0 if not arrived yet)
                double arrivalTime = simulationWrapper.getVehicleArrivalTime(vehID);
                if (arrivalTime > 0) {
                    writer.append(String.format("%.3f", arrivalTime)).append(",");
                } else {
                    writer.append(","); // Empty if not arrived
                }
                
                // Duration (travel time)
                double duration = travelTimeMap.getOrDefault(vehID, 0.0);
                writer.append(String.format("%.3f", duration)).append("\n");
            }
            
            writer.append("\n");
            
            // ========== TABLE 2: EDGE STATISTICS ==========
            writer.append("=== EDGE STATISTICS ===\n");
            writer.append("time,edge_id,meanSpeed,density\n");
            
            // Get edge statistics
            Map<String, Integer> densityMap = simulationWrapper.getVehicleDensityPerEdge();
            Map<String, Double> meanSpeedMap = simulationWrapper.getMeanSpeedPerEdge();
            
            // Write data for each edge
            for (Map.Entry<String, Integer> entry : densityMap.entrySet()) {
                String edgeID = entry.getKey();
                int density = entry.getValue();
                double meanSpeed = meanSpeedMap.getOrDefault(edgeID, 0.0);
                
                // time
                writer.append(String.format("%.3f", currentTime)).append(",");
                
                // edge_id
                writer.append(escapeCSV(edgeID)).append(",");
                
                // meanSpeed
                writer.append(String.format("%.3f", meanSpeed)).append(",");
                
                // density
                writer.append(String.valueOf(density)).append("\n");
            }
            
            // Write export information at the end
            writer.append("\n");
            writer.append("=== EXPORT INFORMATION ===\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.append("Export Time,").append(dateFormat.format(new Date())).append("\n");
            writer.append("Total Vehicles,").append(String.valueOf(vehicleIDs.size())).append("\n");
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Escape special characters in CSV
     */
    private static String escapeCSV(String value) {
        if (value == null) return "";
        // Nếu có dấu phẩy, dấu ngoặc kép hoặc xuống dòng, cần đặt trong dấu ngoặc kép
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * Convert JavaFX Color to hex string (#RRGGBB)
     */
    private static String colorToHex(javafx.scene.paint.Color color) {
        if (color == null) return "#000000";
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}



