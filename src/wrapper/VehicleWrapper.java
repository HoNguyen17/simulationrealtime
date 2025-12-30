package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Vehicletype;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;

import javafx.scene.paint.Color;

import java.util.List;
import java.util.ArrayList;

class VehicleWrapper extends DataType.VehicleData {
    protected double departureTime = -1; // time when vehicle departed
    
    // constructor
    VehicleWrapper(String inputID, Color inputColor){
        super(inputID);
        color = inputColor;
        System.out.println("Added vehicle " + inputID + ".");
    }
    
    // set departure time
    public void setDepartureTime(double time) {
        this.departureTime = time;
    }
    //=================GETTER================================
    // get Vehicle's ID list
    public static List<String> getIDList(SimulationWrapper temp, int po) { // the method should be static, because it returns all vehicles, not one
        try {
            @SuppressWarnings("unchecked")
            List<String> idList = (List<String>) temp.conn.do_job_get(Vehicle.getIDList());
            if (po==1) {System.out.println(String.format("ID list of all vehicle in the current simulation: %s", idList));}
            return idList;
        }
        catch(Exception e) {
            System.out.println("Cannot get vehicle ID list." + e.getMessage());
            return null;
        }
    }

    // get Vehicle's type ID (each type of vehicle gets the different ID)
    public String getTypeID(SimulationWrapper temp, int po) {
        try {
            String typeID = (String) temp.conn.do_job_get(Vehicle.getTypeID(ID));
            if (po==1) {System.out.println(String.format("Type ID of vehicle %s: %s", typeID, ID));}
            return typeID;
        }
        catch(Exception e) {
            System.out.println("Cannot get type ID list of vehicle " + ID + e.getMessage());
        }
        return null;
    }

    // get Vehicle's color as a SumoColor object
    // public SumoColor getColor(int po) {
    //     // SUMO default color (undefined)
    //     if (color.r == -1 && color.g == -1 && color.b == 0 && color.a == -1 && po == 1) {
    //         System.out.println("Vehicle " + ID + " has no custom color (using SUMO default which has the format r#g#b#a): " + color);
    //     }
    //     else if (po == 1) {System.out.println(String.format("Color of vehicle " + ID + ": " + color));}
    //     return color;
    // }
    //=================MAKE COPY=============================
    public DataType.VehicleData makeCopy() {
        DataType.VehicleData copy = new DataType.VehicleData(ID);
        copy.speed = this.speed;
        copy.pos_x = this.pos_x;
        copy.pos_y = this.pos_y;
        copy.angle = this.angle;
        copy.color = this.color;
        return copy;
    }
    //=================SETTER================================
    // set Vehicle's speed -> override the vehicle's internal speed logic until another command is sent or the simulation takes over
    public void setSpeed(SimulationWrapper temp, double inputSpeed, int po) {
        try {
            temp.conn.do_job_set(Vehicle.setSpeed(ID, inputSpeed));
            if  (po==1) {System.out.println(String.format("Set the speed of the vehicle that has the ID %s into %.3f m/s", ID, speed));}
        }
        catch(Exception e) {
            System.out.println("Cannot set the speed of the vehicle that has the ID " + ID + e.getMessage());
        }
    }

    // set Vehicle's color, also update the local (color) variable in the wrapper object
    public void setColor(SimulationWrapper temp, int r, int g, int b, int a) {
        System.out.println("vehiclewrapper0");
        try {
            System.out.println("vehiclewrapper1");
            SumoColor inputColor = new SumoColor(r, g, b, a);
            temp.conn.do_job_set(Vehicle.setColor(ID, inputColor));
            System.out.println("vehiclewrapper2");
            color = new Color(r, g, b, a);
        }
        catch(Exception e) {
            System.out.println("Cannot set the color of the vehicle that has the ID " + ID + e.getMessage());
        }
    }
    public void set_test() {
        System.out.println("still wotk");
    }
    //=================STATIC================================
    // injecting a new vehicle into the simulation
    protected static void addVehicle(SimulationWrapper temp, String inputID, String inputRoute) { 
        try {
            temp.conn.do_job_set(Vehicle.add(inputID, "DEFAULT_VEHTYPE", inputRoute, 0, 0, 0, (byte)0)); // default vehicle type has the initial departure time, position, and speed = 0
        }
        catch (Exception e) {System.out.println("add vehicle fail");}
    }
    //=================STATISTICS METHODS===================
    // get vehicle's current edge ID
    public String getEdgeID(SimulationWrapper temp) {
        try {
            String edgeID = (String) temp.conn.do_job_get(Vehicle.getRoadID(ID));
            return edgeID != null ? edgeID : "";
        } catch (Exception e) {
            return "";
        }
    }
    // get vehicle's travel time (time since departure)
    public double getTravelTime(SimulationWrapper temp) {
        if (departureTime < 0) {
            return 0.0; // vehicle hasn't departed yet or departure time not set
        }
        try {
            double currentTime = temp.getTime(0);
            if (currentTime < 0) {
                return 0.0;
            }
            return currentTime - departureTime;
        } catch (Exception e) {
            return 0.0;
        }
    }
    // Static method to calculate average speed of all vehicles
    public static double calculateAverageSpeed(SimulationWrapper temp) {
        double totalSpeed = 0;
        int count = 0;
        for (VehicleWrapper veh : temp.VehicleList.values()) {
            totalSpeed += veh.speed;
            count++;
        }
        return count > 0 ? totalSpeed / count : 0.0;
    }
    // Static method to calculate vehicle density per edge
    public static java.util.Map<String, Integer> calculateVehicleDensityPerEdge(SimulationWrapper temp) {
        java.util.Map<String, Integer> densityMap = new java.util.HashMap<>();
        for (VehicleWrapper veh : temp.VehicleList.values()) {
            String edgeID = veh.getEdgeID(temp);
            if (!edgeID.isEmpty()) {
                densityMap.put(edgeID, densityMap.getOrDefault(edgeID, 0) + 1);
            }
        }
        return densityMap;
    }
    // Static method to identify congestion hotspots (edges with high density or low speed)
    public static java.util.List<String> identifyCongestionHotspots(SimulationWrapper temp, int minDensity, double maxSpeed) {
        java.util.List<String> hotspots = new ArrayList<>();
        java.util.Map<String, Integer> densityMap = calculateVehicleDensityPerEdge(temp);
        java.util.Map<String, Double> avgSpeedPerEdge = new java.util.HashMap<>();
        java.util.Map<String, Integer> speedCountPerEdge = new java.util.HashMap<>();
        
        // Calculate average speed per edge
        for (VehicleWrapper veh : temp.VehicleList.values()) {
            String edgeID = veh.getEdgeID(temp);
            if (!edgeID.isEmpty()) {
                double currentSpeed = avgSpeedPerEdge.getOrDefault(edgeID, 0.0);
                int count = speedCountPerEdge.getOrDefault(edgeID, 0);
                avgSpeedPerEdge.put(edgeID, currentSpeed + veh.speed);
                speedCountPerEdge.put(edgeID, count + 1);
            }
        }
        
        // Calculate final average speeds
        for (String edgeID : avgSpeedPerEdge.keySet()) {
            int count = speedCountPerEdge.get(edgeID);
            if (count > 0) {
                avgSpeedPerEdge.put(edgeID, avgSpeedPerEdge.get(edgeID) / count);
            }
        }
        
        // Identify hotspots: high density OR low average speed
        for (String edgeID : densityMap.keySet()) {
            int density = densityMap.get(edgeID);
            double avgSpeed = avgSpeedPerEdge.getOrDefault(edgeID, Double.MAX_VALUE);
            if (density >= minDensity || avgSpeed <= maxSpeed) {
                hotspots.add(edgeID);
            }
        }
        
        return hotspots;
    }
    // Static method to calculate travel time distribution
    public static java.util.Map<String, Double> calculateTravelTimeDistribution(SimulationWrapper temp) {
        java.util.Map<String, Double> travelTimeMap = new java.util.HashMap<>();
        for (VehicleWrapper veh : temp.VehicleList.values()) {
            double travelTime = veh.getTravelTime(temp);
            if (travelTime > 0) {
                travelTimeMap.put(veh.ID, travelTime);
            }
        }
        return travelTimeMap;
    }
}