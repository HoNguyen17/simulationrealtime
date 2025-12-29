package gui;

import paser.Networkpaser;
import wrapper.DataType.TrafficLightData;
import wrapper.DataType.VehicleData;
import wrapper.SimulationWrapper;

import javafx.scene.paint.Color;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;


/**
 * Utility class to export vehicle and traffic light data to CSV file
 */
public class CSVExporter {
    
    /**
     * Export all vehicle and traffic light information to CSV file
     * @param vehicleDataList List of vehicle data
     * @param trafficLightDataList List of traffic light data
     * @param simulationWrapper Wrapper to get edge/lane information from SUMO
     * @param networkModel Network model to get edge information
     * @param filePath CSV file path to save
     * @return true if export successful, false if error
     */
    public static boolean exportToCSV(
            List<VehicleData> vehicleDataList,
            List<TrafficLightData> trafficLightDataList,
            SimulationWrapper simulationWrapper,
            Networkpaser.NetworkModel networkModel,
            String filePath) {
        
        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            // Write header for vehicle section
            writer.append("=== VEHICLE INFORMATION ===\n");
            writer.append("Vehicle ID,Speed (m/s),Color R,Color G,Color B,Color A,Current Lane ID,Current Edge ID,Route ID,Route Edges (from->to)\n");
            
            // Write vehicle data
            for (VehicleData vehData : vehicleDataList) {
                if (vehData == null || !vehData.getValidity()) continue;
                
                String vehID = vehData.getID(0);
                double speed = vehData.getSpeed(0);
                Color color = vehData.getColor(0);
                
                // Get lane, edge, and route information from SUMO
                String laneID = "";
                String edgeID = "";
                String routeID = "";
                String routeEdgesStr = "";
                
                if (simulationWrapper != null && !simulationWrapper.isClosed()) {
                    try {
                        laneID = simulationWrapper.getVehicleLaneID(vehID);
                        edgeID = simulationWrapper.getVehicleRoadID(vehID);
                        routeID = simulationWrapper.getVehicleRouteID(vehID);
                        
                        // Get route edges to show lane path (from -> to)
                        List<String> routeEdges = simulationWrapper.getVehicleRouteEdges(vehID);
                        if (routeEdges != null && !routeEdges.isEmpty()) {
                            routeEdgesStr = String.join("->", routeEdges);
                        }
                        
                        // If road ID not available, try to extract from lane ID
                        if ((edgeID == null || edgeID.isEmpty()) && laneID != null && !laneID.isEmpty()) {
                            // Lane ID usually has format: edgeID_laneIndex
                            int underscoreIndex = laneID.lastIndexOf('_');
                            if (underscoreIndex > 0) {
                                edgeID = laneID.substring(0, underscoreIndex);
                            } else {
                                edgeID = laneID;
                            }
                        }
                    } catch (Exception e) {
                        // If cannot get from SUMO, leave empty
                        System.err.println("Error getting vehicle info for " + vehID + ": " + e.getMessage());
                    }
                }
                
                // Write data to CSV
                writer.append(escapeCSV(vehID)).append(",");
                writer.append(String.format("%.3f", speed)).append(",");
                writer.append(String.format("%.3f", color.getRed())).append(",");
                writer.append(String.format("%.3f", color.getGreen())).append(",");
                writer.append(String.format("%.3f", color.getBlue())).append(",");
                writer.append(String.format("%.3f", color.getOpacity())).append(",");
                writer.append(escapeCSV(laneID)).append(",");
                writer.append(escapeCSV(edgeID)).append(",");
                writer.append(escapeCSV(routeID)).append(",");
                writer.append(escapeCSV(routeEdgesStr)).append("\n");
            }
            
            // Add empty line between sections
            writer.append("\n");
            
            // Write header for traffic light section
            writer.append("=== TRAFFIC LIGHT INFORMATION ===\n");
            writer.append("Traffic Light ID,Current State (Full),Link Index,From Lane ID,To Lane ID,Link State (r/y/g),Lane Path (from->to)\n");
            
            // Write traffic light data
            for (TrafficLightData tlData : trafficLightDataList) {
                if (tlData == null) continue;
                
                String tlID = tlData.getID(0);
                String lightDef = tlData.getPhaseDef(0);
                int controlledLinksNum = tlData.getControlledLinksNum();
                
                // Write each controlled link
                for (int i = 0; i < controlledLinksNum; i++) {
                    List<String> defFromTo = tlData.getDefFromTo(i);
                    if (defFromTo == null || defFromTo.size() < 3) continue;
                    
                    String linkState = defFromTo.get(0); // r, y, or g
                    String fromLaneID = defFromTo.get(1);
                    String toLaneID = defFromTo.get(2);
                    String lanePath = fromLaneID + "->" + toLaneID;
                    
                    writer.append(escapeCSV(tlID)).append(",");
                    writer.append(escapeCSV(lightDef != null ? lightDef : "")).append(",");
                    writer.append(String.valueOf(i)).append(",");
                    writer.append(escapeCSV(fromLaneID)).append(",");
                    writer.append(escapeCSV(toLaneID)).append(",");
                    writer.append(escapeCSV(linkState)).append(",");
                    writer.append(escapeCSV(lanePath)).append("\n");
                }
            }
            
            // Write export information
            writer.append("\n");
            writer.append("=== EXPORT INFORMATION ===\n");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.append("Export Time,").append(dateFormat.format(new Date())).append("\n");
            writer.append("Total Vehicles,").append(String.valueOf(vehicleDataList.size())).append("\n");
            writer.append("Total Traffic Lights,").append(String.valueOf(trafficLightDataList.size())).append("\n");
            
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
}



