package tracker;

import wrapper.SimulationWrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.paint.Color;

public class Statistic {
    private static SimulationWrapper sim = null;
    
    // Data structures for statistics
    private static final Map<String, VehicleStats> vehicleStatsMap = new ConcurrentHashMap<>();
    private static final List<VehicleStats> completedVehicles = new ArrayList<>();
    private static final List<EdgeStats> edgeStatsHistory = new ArrayList<>();
    private static final List<String> lastVehicleList = new ArrayList<>(); // Track previous vehicle list
    private static final List<Double> lastCompletedTravelTimes = new ArrayList<>(); // Track previous completed times
    
    // Vehicle statistics class
    private static class VehicleStats {
        String vehicleId;
        String color;
        double speed;
        double departTime;
        double arrivalTime;
        double duration;
        
        VehicleStats(String vehicleId, String color, double speed, double departTime) {
            this.vehicleId = vehicleId;
            this.color = color;
            this.speed = speed;
            this.departTime = departTime;
            this.arrivalTime = -1;
            this.duration = -1;
        }
    }
    
    // Edge statistics class
    private static class EdgeStats {
        double time;
        String edgeId;
        double meanSpeed;
        int density;
        
        EdgeStats(double time, String edgeId, double meanSpeed, int density) {
            this.time = time;
            this.edgeId = edgeId;
            this.meanSpeed = meanSpeed;
            this.density = density;
        }
    }
    
    public static boolean initialize(SimulationWrapper input) {
        sim = input;
        try {
            // Create tracker directory if it doesn't exist
            File trackerDir = new File("tracker");
            if (!trackerDir.exists()) {
                trackerDir.mkdirs();
            }
            
            // CSV files will be created on export, not during initialization
            
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while initializing statistics.");
            e.printStackTrace();
        }
        return false;
    }
    
    // Track vehicle when it departs
    public static void trackVehicleDepart(String vehicleId, Color color, double speed, double departTime) {
        String colorStr = colorToString(color);
        vehicleStatsMap.put(vehicleId, new VehicleStats(vehicleId, colorStr, speed, departTime));
    }
    
    // Track vehicle when it arrives
    public static void trackVehicleArrival(String vehicleId, double arrivalTime) {
        VehicleStats stats = vehicleStatsMap.remove(vehicleId);
        if (stats != null) {
            stats.arrivalTime = arrivalTime;
            stats.duration = arrivalTime - stats.departTime;
            synchronized (completedVehicles) {
                completedVehicles.add(stats);
            }
            // Write to CSV immediately
            writeVehicleStatsToCSV(stats);
        }
    }
    
    // Convert Color to hex string (#RRGGBB)
    private static String colorToString(Color color) {
        if (color == null) return "#000000";
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
    
    // Write vehicle stats to CSV (removed - will write only on export)
    private static void writeVehicleStatsToCSV(VehicleStats stats) {
        // Vehicle stats are now stored in memory and written only on export
    }
    
    // Calculate mean speed for an edge
    private static double calculateEdgeMeanSpeed(String edgeId) {
        if (sim == null) return 0.0;
        
        List<String> vehicleIds = sim.getVehicleIDsList();
        if (vehicleIds == null || vehicleIds.isEmpty()) return 0.0;
        
        // Try to get vehicles on this edge
        // Note: This is a simplified approach. In a real implementation,
        // you would need to query which vehicles are on which edge
        double totalSpeed = 0.0;
        int count = 0;
        
        // For now, we'll use average speed of all vehicles as approximation
        // In a full implementation, you'd query vehicle's current edge
        for (String vehId : vehicleIds) {
            double speed = sim.getVehicleSpeed(vehId);
            if (speed > 0) {
                totalSpeed += speed;
                count++;
            }
        }
        
        return count > 0 ? totalSpeed / count : 0.0;
    }
    
    public static boolean addNewData() {
        if (sim == null) return false;
        
        try {
            // Create tracker directory if it doesn't exist
            File trackerDir = new File("tracker");
            if (!trackerDir.exists()) {
                trackerDir.mkdirs();
            }
            
            double currentTime = sim.getTime(0);
            List<String> edges = sim.getEdgeIDsList();
            List<String> currentVehicleList = sim.getVehicleIDsList();
            List<Double> currentCompletedTravelTimes = sim.getCompletedTravelTimes();
            
            // Track new vehicles (vehicles that appeared since last check)
            for (String vehId : currentVehicleList) {
                if (!vehicleStatsMap.containsKey(vehId) && !lastVehicleList.contains(vehId)) {
                    // New vehicle detected - track it
                    try {
                        wrapper.DataType.VehicleData vehData = sim.makeVehicleCopy(vehId);
                        if (vehData != null) {
                            Color vehColor = vehData.getColor(0);
                            double vehSpeed = vehData.getSpeed(0);
                            // Estimate depart time (current time or slightly before)
                            double departTime = currentTime;
                            trackVehicleDepart(vehId, vehColor, vehSpeed, departTime);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to track new vehicle " + vehId + ": " + e.getMessage());
                    }
                }
            }
            
            // Track completed vehicles (vehicles that disappeared since last check)
            for (String vehId : lastVehicleList) {
                if (!currentVehicleList.contains(vehId) && vehicleStatsMap.containsKey(vehId)) {
                    // Vehicle completed - track arrival
                    // Use completed travel times to estimate arrival time
                    if (currentCompletedTravelTimes.size() > lastCompletedTravelTimes.size()) {
                        // New travel time was added, estimate arrival
                        double estimatedArrivalTime = currentTime;
                        trackVehicleArrival(vehId, estimatedArrivalTime);
                    }
                }
            }
            
            // Update last known state
            lastVehicleList.clear();
            lastVehicleList.addAll(currentVehicleList);
            lastCompletedTravelTimes.clear();
            lastCompletedTravelTimes.addAll(currentCompletedTravelTimes);
            
            // Collect edge statistics (store in memory, write on export)
            for (String edgeId : edges) {
                int density = sim.getEdgeDensity(edgeId);
                if (density > 0) {
                    double meanSpeed = calculateEdgeMeanSpeed(edgeId);
                    EdgeStats edgeStat = new EdgeStats(currentTime, edgeId, meanSpeed, density);
                    edgeStatsHistory.add(edgeStat);
                }
            }
            
            // Update vehicle speeds in tracking map
            for (Map.Entry<String, VehicleStats> entry : vehicleStatsMap.entrySet()) {
                String vehId = entry.getKey();
                VehicleStats stats = entry.getValue();
                if (currentVehicleList.contains(vehId)) {
                    double currentSpeed = sim.getVehicleSpeed(vehId);
                    if (currentSpeed > 0) {
                        stats.speed = currentSpeed; // Update speed
                    }
                }
            }
            
            // Data is stored in memory, will be exported on demand
            
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while adding new data.");
            e.printStackTrace();
        }
        return false;
    }
    
    // Export Vehicle CSV only
    public static boolean exportVehicleCSV() {
        try {
            File trackerDir = new File("tracker");
            if (!trackerDir.exists()) {
                trackerDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            
            try (FileWriter writer = new FileWriter("tracker/vehicle_statistics_" + timestamp + ".csv")) {
                writer.write("Vehicle,Color,Speed,Depart,Arrival,Duration\n");
                synchronized (completedVehicles) {
                    // Sort by depart time
                    completedVehicles.sort((a, b) -> Double.compare(a.departTime, b.departTime));
                    for (VehicleStats v : completedVehicles) {
                        writer.write(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f\n",
                            v.vehicleId, v.color, v.speed, v.departTime, v.arrivalTime, v.duration));
                    }
                }
            }
            
            System.out.println("Vehicle CSV exported successfully: vehicle_statistics_" + timestamp + ".csv");
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while exporting vehicle CSV.");
            e.printStackTrace();
        }
        return false;
    }
    
    // Export Edge CSV only
    public static boolean exportEdgeCSV() {
        try {
            File trackerDir = new File("tracker");
            if (!trackerDir.exists()) {
                trackerDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            
            try (FileWriter writer = new FileWriter("tracker/edge_statistics_" + timestamp + ".csv")) {
                writer.write("Time,Edge_ID,MeanSpeed,Density\n");
                // Sort by time
                edgeStatsHistory.sort((a, b) -> {
                    int timeCompare = Double.compare(a.time, b.time);
                    if (timeCompare != 0) return timeCompare;
                    return a.edgeId.compareTo(b.edgeId);
                });
                for (EdgeStats e : edgeStatsHistory) {
                    writer.write(String.format("%.2f,%s,%.2f,%d\n",
                        e.time, e.edgeId, e.meanSpeed, e.density));
                }
            }
            
            System.out.println("Edge CSV exported successfully: edge_statistics_" + timestamp + ".csv");
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while exporting edge CSV.");
            e.printStackTrace();
        }
        return false;
    }
    
    // Export CSV with all data sorted by time
    public static boolean exportCSV() {
        boolean vehicleSuccess = exportVehicleCSV();
        boolean edgeSuccess = exportEdgeCSV();
        return vehicleSuccess && edgeSuccess;
    }
    
    // Export PDF summary with charts, metrics, and timestamps
    public static boolean export() {
        try {
            File trackerDir = new File("tracker");
            if (!trackerDir.exists()) {
                trackerDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            // For now, create an HTML report that can be converted to PDF
            // In a production environment, you would use a library like iText or Apache PDFBox
            String htmlFileName = "tracker/simulation_summary_" + timestamp + ".html";
            
            try (FileWriter htmlWriter = new FileWriter(htmlFileName)) {
                htmlWriter.write("<!DOCTYPE html>\n");
                htmlWriter.write("<html><head><title>Simulation Summary</title>");
                htmlWriter.write("<script src='https://cdn.jsdelivr.net/npm/chart.js@3.9.1/dist/chart.min.js'></script>");
                htmlWriter.write("<style>");
                htmlWriter.write("body { font-family: Arial, sans-serif; margin: 20px; }");
                htmlWriter.write("h1 { color: #333; }");
                htmlWriter.write("h2 { color: #555; border-bottom: 2px solid #4CAF50; padding-bottom: 5px; }");
                htmlWriter.write("table { border-collapse: collapse; width: 100%; margin: 20px 0; }");
                htmlWriter.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
                htmlWriter.write("th { background-color: #4CAF50; color: white; }");
                htmlWriter.write("tr:nth-child(even) { background-color: #f2f2f2; }");
                htmlWriter.write(".metric { background-color: #e7f3ff; padding: 15px; margin: 10px 0; border-radius: 5px; }");
                htmlWriter.write(".chart-container { width: 100%; max-width: 800px; margin: 20px 0; height: 400px; }");
                htmlWriter.write("</style></head><body>");
                
                htmlWriter.write("<h1>SUMO Simulation Summary Report</h1>");
                htmlWriter.write("<div class='metric'>");
                htmlWriter.write("<p><strong>Generated:</strong> " + new Date().toString() + "</p>");
                if (sim != null) {
                    htmlWriter.write("<p><strong>Simulation End Time:</strong> " + String.format("%.2f", sim.getTime(0)) + " seconds</p>");
                }
                htmlWriter.write("</div>");
                
                // Charts similar to Graph class
                // 1. Average Vehicle Speed Chart (Area Chart like Graph.SpeedChart)
                htmlWriter.write("<h2>Average Vehicle Speed Over Time</h2>");
                htmlWriter.write("<div class='chart-container'>");
                htmlWriter.write("<canvas id='avgSpeedChart'></canvas>");
                htmlWriter.write("</div>");
                
                // 2. Travel Time Distribution Chart (Bar Chart like Graph.TravelTimeChart)
                htmlWriter.write("<h2>Vehicle Travel Time Distribution</h2>");
                htmlWriter.write("<div class='chart-container'>");
                htmlWriter.write("<canvas id='travelTimeChart'></canvas>");
                htmlWriter.write("</div>");
                
                // 3. Density Chart (Bar Chart like Graph.DensityChart)
                htmlWriter.write("<h2>Vehicle Density per Edge</h2>");
                htmlWriter.write("<div class='chart-container'>");
                htmlWriter.write("<canvas id='densityChart'></canvas>");
                htmlWriter.write("</div>");
                
                // Vehicle Statistics Summary
                synchronized (completedVehicles) {
                    if (!completedVehicles.isEmpty()) {
                        htmlWriter.write("<h2>Vehicle Statistics Summary</h2>");
                        htmlWriter.write("<div class='metric'>");
                        htmlWriter.write("<p><strong>Total Vehicles Completed:</strong> " + completedVehicles.size() + "</p>");
                        
                        double avgDuration = 0;
                        double avgSpeed = 0;
                        double minDuration = Double.MAX_VALUE;
                        double maxDuration = 0;
                        double minSpeed = Double.MAX_VALUE;
                        double maxSpeed = 0;
                        
                        for (VehicleStats v : completedVehicles) {
                            avgDuration += v.duration;
                            avgSpeed += v.speed;
                            if (v.duration < minDuration) minDuration = v.duration;
                            if (v.duration > maxDuration) maxDuration = v.duration;
                            if (v.speed < minSpeed) minSpeed = v.speed;
                            if (v.speed > maxSpeed) maxSpeed = v.speed;
                        }
                        if (completedVehicles.size() > 0) {
                            avgDuration /= completedVehicles.size();
                            avgSpeed /= completedVehicles.size();
                        }
                        
                        htmlWriter.write("<p><strong>Average Travel Duration:</strong> " + String.format("%.2f", avgDuration) + " seconds</p>");
                        htmlWriter.write("<p><strong>Min/Max Duration:</strong> " + String.format("%.2f / %.2f", minDuration, maxDuration) + " seconds</p>");
                        htmlWriter.write("<p><strong>Average Speed:</strong> " + String.format("%.2f", avgSpeed) + " m/s</p>");
                        htmlWriter.write("<p><strong>Min/Max Speed:</strong> " + String.format("%.2f / %.2f", minSpeed, maxSpeed) + " m/s</p>");
                        htmlWriter.write("</div>");
                        
                        htmlWriter.write("<h3>Completed Vehicles (All " + completedVehicles.size() + " vehicles)</h3>");
                        htmlWriter.write("<table>");
                        htmlWriter.write("<tr><th>Vehicle ID</th><th>Color</th><th>Speed (m/s)</th><th>Depart (s)</th><th>Arrival (s)</th><th>Duration (s)</th></tr>");
                        
                        // Sort by depart time
                        List<VehicleStats> sortedVehicles = new ArrayList<>(completedVehicles);
                        sortedVehicles.sort((a, b) -> Double.compare(a.departTime, b.departTime));
                        
                        for (VehicleStats v : sortedVehicles) {
                            htmlWriter.write(String.format("<tr><td>%s</td><td style='background-color:%s;color:%s'>%s</td><td>%.2f</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>",
                                v.vehicleId, v.color, getContrastColor(v.color), v.color, v.speed, v.departTime, v.arrivalTime, v.duration));
                        }
                        htmlWriter.write("</table>");
                    }
                }
                
                // Generate charts similar to Graph class
                htmlWriter.write("<script>");
                
                // 1. Average Vehicle Speed Chart (Area Chart like Graph.SpeedChart)
                // Collect average speed over time from edgeStatsHistory
                if (!edgeStatsHistory.isEmpty() && sim != null) {
                    // Group by time and calculate average speed
                    Map<Double, List<Double>> speedByTime = new HashMap<>();
                    for (EdgeStats e : edgeStatsHistory) {
                        speedByTime.computeIfAbsent(e.time, k -> new ArrayList<>()).add(e.meanSpeed);
                    }
                    
                    List<Double> timeList = new ArrayList<>(speedByTime.keySet());
                    timeList.sort(Double::compareTo);
                    
                    htmlWriter.write("const avgSpeedTimeLabels = [");
                    for (int i = 0; i < timeList.size(); i++) {
                        if (i > 0) htmlWriter.write(",");
                        htmlWriter.write(String.format("%.2f", timeList.get(i)));
                    }
                    htmlWriter.write("]; const avgSpeedData = [");
                    for (int i = 0; i < timeList.size(); i++) {
                        if (i > 0) htmlWriter.write(",");
                        double time = timeList.get(i);
                        List<Double> speeds = speedByTime.get(time);
                        double avg = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                        htmlWriter.write(String.format("%.2f", avg));
                    }
                    htmlWriter.write("];");
                    htmlWriter.write("new Chart(document.getElementById('avgSpeedChart'), {");
                    htmlWriter.write("type: 'line', data: { labels: avgSpeedTimeLabels, datasets: [{label: 'speed (m/s)', data: avgSpeedData, borderColor: 'rgb(75, 192, 192)', backgroundColor: 'rgba(75, 192, 192, 0.2)', fill: true}]},");
                    htmlWriter.write("options: {responsive: true, scales: {x: {title: {display: true, text: 't(s)'}}, y: {title: {display: true, text: 'Avg speed (m/s)'}}}, plugins: {title: {display: true, text: 'Average Vehicle Speed'}}}});");
                }
                
                // 2. Travel Time Distribution Chart (Bar Chart like Graph.TravelTimeChart)
                synchronized (completedVehicles) {
                    if (!completedVehicles.isEmpty()) {
                        int count0 = 0, count1 = 0, count2 = 0, count3 = 0, count4 = 0, count5 = 0;
                        for (VehicleStats v : completedVehicles) {
                            double time = v.duration;
                            if (time < 60) count0++;
                            else if (time < 120) count1++;
                            else if (time < 180) count2++;
                            else if (time < 240) count3++;
                            else if (time < 300) count4++;
                            else count5++;
                        }
                        htmlWriter.write("new Chart(document.getElementById('travelTimeChart'), {");
                        htmlWriter.write("type: 'bar', data: { labels: ['0-60', '60-120', '120-180', '180-240', '240-300', '300+'],");
                        htmlWriter.write("datasets: [{label: 'travel time (s)', data: [" + count0 + "," + count1 + "," + count2 + "," + count3 + "," + count4 + "," + count5 + "], backgroundColor: 'rgba(54, 162, 235, 0.6)'}]},");
                        htmlWriter.write("options: {responsive: true, scales: {y: {beginAtZero: true}}, plugins: {title: {display: true, text: 'Vehicle Travel Time'}}}});");
                    }
                }
                
                // 3. Density Chart (Bar Chart like Graph.DensityChart)
                if (!edgeStatsHistory.isEmpty()) {
                    // Get latest density for each edge
                    Map<String, Integer> latestDensity = new HashMap<>();
                    for (EdgeStats e : edgeStatsHistory) {
                        latestDensity.put(e.edgeId, e.density);
                    }
                    
                    List<String> edgeIds = new ArrayList<>(latestDensity.keySet());
                    edgeIds.sort(String::compareTo);
                    
                    htmlWriter.write("const densityEdgeLabels = [");
                    for (int i = 0; i < edgeIds.size(); i++) {
                        if (i > 0) htmlWriter.write(",");
                        htmlWriter.write("'" + edgeIds.get(i) + "'");
                    }
                    htmlWriter.write("]; const densityData = [");
                    for (int i = 0; i < edgeIds.size(); i++) {
                        if (i > 0) htmlWriter.write(",");
                        htmlWriter.write(String.valueOf(latestDensity.get(edgeIds.get(i))));
                    }
                    htmlWriter.write("];");
                    htmlWriter.write("new Chart(document.getElementById('densityChart'), {");
                    htmlWriter.write("type: 'bar', data: { labels: densityEdgeLabels, datasets: [{label: 'Vehicles', data: densityData, backgroundColor: 'rgba(255, 99, 132, 0.6)'}]},");
                    htmlWriter.write("options: {responsive: true, scales: {y: {beginAtZero: true}}, plugins: {title: {display: true, text: 'Vehicle Density per Edge'}}}});");
                }
                
                htmlWriter.write("</script>");
                
                // Edge Statistics Summary with Charts
                if (!edgeStatsHistory.isEmpty()) {
                    htmlWriter.write("<h2>Edge Statistics Summary</h2>");
                    htmlWriter.write("<div class='metric'>");
                    htmlWriter.write("<p><strong>Total Edge Data Points:</strong> " + edgeStatsHistory.size() + "</p>");
                    
                    // Calculate averages and find unique edges
                    double totalMeanSpeed = 0;
                    int totalDensity = 0;
                    Map<String, List<EdgeStats>> edgeMap = new HashMap<>();
                    double minTime = Double.MAX_VALUE;
                    double maxTime = 0;
                    
                    for (EdgeStats e : edgeStatsHistory) {
                        totalMeanSpeed += e.meanSpeed;
                        totalDensity += e.density;
                        if (e.time < minTime) minTime = e.time;
                        if (e.time > maxTime) maxTime = e.time;
                        edgeMap.computeIfAbsent(e.edgeId, k -> new ArrayList<>()).add(e);
                    }
                    if (edgeStatsHistory.size() > 0) {
                        totalMeanSpeed /= edgeStatsHistory.size();
                        totalDensity /= edgeStatsHistory.size();
                    }
                    
                    htmlWriter.write("<p><strong>Simulation Time Range:</strong> " + String.format("%.2f - %.2f", minTime, maxTime) + " seconds</p>");
                    htmlWriter.write("<p><strong>Average Mean Speed:</strong> " + String.format("%.2f", totalMeanSpeed) + " m/s</p>");
                    htmlWriter.write("<p><strong>Average Density:</strong> " + String.format("%.0f", (double)totalDensity) + " vehicles</p>");
                    htmlWriter.write("<p><strong>Unique Edges:</strong> " + edgeMap.size() + "</p>");
                    htmlWriter.write("</div>");
                    
                    htmlWriter.write("<h3>Edge Statistics (All " + edgeStatsHistory.size() + " data points, sorted by time)</h3>");
                    htmlWriter.write("<table>");
                    htmlWriter.write("<tr><th>Time (s)</th><th>Edge ID</th><th>Mean Speed (m/s)</th><th>Density</th></tr>");
                    
                    edgeStatsHistory.sort((a, b) -> {
                        int timeCompare = Double.compare(a.time, b.time);
                        if (timeCompare != 0) return timeCompare;
                        return a.edgeId.compareTo(b.edgeId);
                    });
                    
                    for (EdgeStats e : edgeStatsHistory) {
                        htmlWriter.write(String.format("<tr><td>%.2f</td><td>%s</td><td>%.2f</td><td>%d</td></tr>",
                            e.time, e.edgeId, e.meanSpeed, e.density));
                    }
                    htmlWriter.write("</table>");
                }
                
                htmlWriter.write("<hr>");
                htmlWriter.write("<p><em>Note: Full data is available in CSV files in the tracker directory.</em></p>");
                htmlWriter.write("<p><em>Files: vehicle_statistics.csv, edge_statistics.csv</em></p>");
                htmlWriter.write("</body></html>");
            }
            
            System.out.println("Simulation summary exported to: " + htmlFileName);
            System.out.println("Note: To convert to PDF, open the HTML file in a browser and use Print to PDF.");
            
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while exporting statistics.");
            e.printStackTrace();
        }
        return false;
    }
    
    // Get completed vehicles count (for external access)
    public static int getCompletedVehiclesCount() {
        synchronized (completedVehicles) {
            return completedVehicles.size();
        }
    }
    
    // Get edge stats count (for external access)
    public static int getEdgeStatsCount() {
        return edgeStatsHistory.size();
    }
    
    // Helper method to get contrast color for text
    private static String getContrastColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) return "#000000";
        try {
            int r = Integer.parseInt(hexColor.substring(1, 3), 16);
            int g = Integer.parseInt(hexColor.substring(3, 5), 16);
            int b = Integer.parseInt(hexColor.substring(5, 7), 16);
            // Calculate luminance
            double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
            return luminance > 0.5 ? "#000000" : "#FFFFFF";
        } catch (Exception e) {
            return "#000000";
        }
    }
}
