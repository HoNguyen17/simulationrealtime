package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import it.polito.appeal.traci.TraCIException;

import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Route;

import de.tudresden.sumo.objects.SumoVehicleData;
import de.tudresden.sumo.objects.SumoStringList;
import de.tudresden.sumo.objects.SumoPrimitive;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;

import de.tudresden.sumo.config.Constants;

import de.tudresden.sumo.subscription.VariableSubscription;
import de.tudresden.sumo.subscription.SubscribtionVariable;
import de.tudresden.sumo.subscription.SubscriptionObject;
import de.tudresden.sumo.subscription.ResponseType;

import de.tudresden.sumo.util.Observer;
import de.tudresden.sumo.util.Observable;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.paint.Color;

// Logging
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

public class SimulationWrapper implements Observer {
    // creating an instance of logger
    private final static Logger LOG = Logger.getLogger(SimulationWrapper.class.getName());
    // use static because a static block (also called a static initializer) runs once and only once when the class is first loaded into memory
    static {
        Handler fileHandler;
        Handler consoleHandler;
        try {
            // 1. Setup File Handler
            fileHandler = new java.util.logging.FileHandler("logfile.log", true);
            LOG.addHandler(fileHandler);

            // use XML format tại trong slide thầy làm thế =)))
            java.util.logging.Formatter xmlFormat = new java.util.logging.XMLFormatter();
            fileHandler.setFormatter(xmlFormat);
            fileHandler.setLevel(Level.ALL);

            // 2. Setup Console Handler
            consoleHandler = new java.util.logging.ConsoleHandler();
            LOG.addHandler(consoleHandler);

            // Console only shows WARNING and higher
            consoleHandler.setLevel(Level.WARNING);

            // Simple text format for the console
            java.util.logging.Formatter consoleFormat = new java.util.logging.SimpleFormatter();
            consoleHandler.setFormatter(consoleFormat);

            // Ensure the main logger allows all levels through to the handlers
            LOG.setLevel(Level.ALL);

            // Disable default console logging to avoid duplicate messages
            LOG.setUseParentHandlers(false);

        } catch (java.io.IOException | SecurityException e) {
            // Basic exception handling as shown in your notes
            System.err.println("Logging setup failed: " + e.getMessage());
        }
    }

    protected static SumoTraciConnection conn; //core connection object used to send commands to and receive data from the running SUMO simulation
    protected int delay = 200;
    protected boolean isPaused = false;

    // HashMaps to store custom wrapper objects for easier management
    protected final HashMap<String, TrafficLightWrapper> TrafficLightList = new HashMap<>();
    protected final HashMap<String, VehicleWrapper> VehicleList = new HashMap<>();
    protected final HashMap<String, RouteWrapper> RouteList = new HashMap<>(); 
    protected final HashMap<String, EdgeWrapper> EdgeList = new HashMap<>();
    protected final HashMap<String, Color> ColorQueue = new HashMap<>();
    // Track vehicle travel times
    protected final HashMap<String, Double> VehicleDepartTime = new HashMap<>();
    protected final ArrayList<Double> CompletedTravelTimes = new ArrayList<>();
    // Constructor 1
    public SimulationWrapper(String sumocfg, double step_length, String sumo_bin){
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true"); //start sumo immediately
        LOG.info("Simulation created by constructor 1");
    }
    // Constructor 2
    public SimulationWrapper(String sumocfg){
        String sumo_bin = "sumo";
        double step_length = 1;
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true"); //start sumo immediately
        LOG.info("Simulation created with config: " + sumocfg);
    }
//===== SIMULATION STUFF ==================================
    // check if connection is closed
    public boolean isClosed() {
        return conn.isClosed();
    } 
    public void Pause() {
        if (!isPaused) {isPaused = true;}
        else {isPaused = false;}
    }
    // start simulation, update TrafficLightList, more will be implemented
    public void Start(){
        try {
            conn.runServer(); 
            conn.setOrder(1); 
            conn.addObserver(this);// add observer
            //start subscription to look out for departed (spawn in) and arrived (despawn) vehicle
            VariableSubscription vs = new VariableSubscription(SubscribtionVariable.simulation, 0, 100000 * 60, "");
            vs.addCommand(Constants.VAR_DEPARTED_VEHICLES_IDS);
            vs.addCommand(Constants.VAR_ARRIVED_VEHICLES_IDS);
            conn.do_subscription(vs);//start the subscription

            TrafficLightWrapper.updateTrafficLightIDs(this);
            EdgeWrapper.updateEdgeIDs(this);
            RouteWrapper.updateRouteIDs(this);
            LOG.info("Simulation started successfully");
        }
        catch(Exception e) {
            LOG.log(Level.SEVERE, "Failed to start simulation", e);
        }
    }
    // do a simulation's time step
    public void Step(){
        if(!isPaused) {
            try {
                Thread.sleep(delay);
                conn.do_timestep();
            }
            catch(Exception e) {LOG.log(Level.SEVERE, "Failed to step simulation", e);}
        }
    }
    // close simulation
    public void End() {
        conn.close();
    }
    // get current simulation time
    public double getTime(int po) {
        try {
            double time = (double)conn.do_job_get(Simulation.getTime());
            if (po == 1) {LOG.info("Current Time: " + time);}
            return time;
        }
        catch(Exception e) {LOG.log(Level.SEVERE, "Failed to get simulation time", e);}
        return -1;
    }
    //(new) update from subscription, abstract method of observer
    public void update(Observable arg0, SubscriptionObject so) { 
        if (so.response == ResponseType.SIM_VARIABLE) { 
            if (so.variable == Constants.VAR_DEPARTED_VEHICLES_IDS) {
                SumoStringList ssl = (SumoStringList) so.object; 
                if (ssl.size() > 0) {
                    for (String vehID : ssl) {
                        // for each new vehicle, starts a new subscription for that vehicle's position, speed, and angle
                        VariableSubscription vs = new VariableSubscription(SubscribtionVariable.vehicle, 0, 100000 * 60, vehID);
                        vs.addCommand(Constants.VAR_POSITION);
                        vs.addCommand(Constants.VAR_SPEED);
                        vs.addCommand(Constants.VAR_ANGLE);
                        
                        try {
                            // create a vehicle wrapper object and add to the VehicleList hash map
                            SumoColor color = (SumoColor)conn.do_job_get(Vehicle.getColor(vehID));
                            VehicleWrapper y = new VehicleWrapper(vehID, DataType.convertColor(color));
                            VehicleList.put(vehID, y);
                            VehicleDepartTime.put(vehID, getTime(0)); // track depart time
                            // start subscription of the vehicle
                            conn.do_subscription(vs);
                            if (ColorQueue.get(vehID) != null) {
                                Color defaultColor = ColorQueue.get(vehID);
                                double r = defaultColor.getRed();
                                double g = defaultColor.getGreen();
                                double b = defaultColor.getBlue();
                                //System.out.println("color " + r + " " + g);
                                this.setVehicleColor(vehID, r, g, b, 1);
                                ColorQueue.remove(vehID);
                            }
                        }
                        // use placeholder here because (update) run thousands of times, so saving CPU cycles help our simulation running smoothly
                        // when using string concatenation, Java builds that string every single time the code runs, even if the Logger turned off
                        // with placeholders, the Logger checks the level first. If the level is hidden, it ignores the variables entirely
                        catch (Exception ex) {LOG.log(Level.SEVERE, "subscription to {0} failed", new Object[]{vehID,ex});}
                    }
                }
            }
            else if (so.variable == Constants.VAR_ARRIVED_VEHICLES_IDS) {// when a vehicle has reached its end point
                SumoStringList ssl = (SumoStringList) so.object;
                if (ssl.size() > 0) {
                    for (String vehID : ssl) {
                        try {
                            // calculate travel time Nguyen
                            if (VehicleDepartTime.containsKey(vehID)) {
                                double travelTime = getTime(0) - VehicleDepartTime.get(vehID);
                                CompletedTravelTimes.add(travelTime);
                                VehicleDepartTime.remove(vehID);
                            }
                            VehicleList.remove(vehID);
                            LOG.log(Level.INFO, "Vehicle {0} has been departed and deleted from hashmap", vehID);
                        }
                        catch (Exception ex) {
                            LOG.log(Level.WARNING, "Unable to delete {0} from hashmap", new Object[]{vehID, ex});
                        }
                    }
                }
            }
        } 
        else if (so.response == ResponseType.VEHICLE_VARIABLE) { // vehicle variables
            VehicleWrapper x = VehicleList.get(so.id);
            // update the speed, position, angle of a VehicleWrapper object in the VehicleList based on the received data
            if (so.variable == Constants.VAR_SPEED) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                x.speed = (double) sp.val;
            } 
            else if (so.variable == Constants.VAR_POSITION) {
                SumoPosition2D sc = (SumoPosition2D) so.object;
                x.pos_x = sc.x;
                x.pos_y = sc.y;
            }
            else if (so.variable == Constants.VAR_ANGLE) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                x.angle = (double) sp.val;
            }
        }
        else if (so.response == ResponseType.TL_VARIABLE) { // traffic light variables
            if (so.variable == Constants.TL_RED_YELLOW_GREEN_STATE) { // update the lightDef of a TrafficLightWrapper in the TrafficLightList
                SumoPrimitive sp = (SumoPrimitive) so.object;
                TrafficLightWrapper x = TrafficLightList.get(so.id);
                x.lightDef = (String) sp.val;
            }
        }
        else if (so.response == ResponseType.EDGE_VARIABLE) {
            EdgeWrapper x = EdgeList.get(so.id);
            if (so.variable == Constants.LAST_STEP_VEHICLE_NUMBER) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                x.density = (int) sp.val;
            }
            if (so.variable == Constants.VAR_CURRENT_TRAVELTIME) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                x.travelTime = (double) sp.val;
            }
            if (so.variable == Constants.VAR_WAITING_TIME) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                x.waitingTime = (double) sp.val;
                if (x.waitingTime > 130) x.congested = true;
                else x.congested = false;
            }
        }
    }
    // set delay
    public void setDelay(int input) {
        delay = input;
    } 
//===== TRAFFIC LIGHT STUFF ===============================
//===== GETTER ============================================
    // get a list of traffic light IDs
    public List<String> getTLIDsList() {
        List<String> tlIDs = new ArrayList<>(TrafficLightList.keySet());
        return tlIDs;
    }
    // get phase number of a traffic light
    public int getTLPhaseNum(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        int phaseNum = x.getPhaseNum(this, 0);
        return phaseNum;
    }
    // get phase definition of a traffic light (current light state)
    public String getTLPhaseDef(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        String phaseDef = x.getPhaseDef(0);
        return phaseDef;
    }
    //get a traffic light definition and from, to lane ID of a link
    public List<String> getTLDefFromTo(String inputID, int index) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        if (index < x.controlledLinksNum) {
            List<String> defFromTo = x.getDefFromTo(index);
            return defFromTo;
        }
        else {return null;}
    }
    public List<String> getTLControlledJunctions(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        List<String> controlledJunctions = x.getControlledJunctions(this, 0);
        return controlledJunctions;
    }
//===== MAKE COPY =========================================
    public DataType.TrafficLightData makeTLCopy(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        return x.makeCopy();
    }
//===== SETTER ============================================
    // set phase definition of a traffic light to the origin
    public void setTLPhaseDefOrigin(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.setPhaseDefOrigin(this);
    }
    // set to the next phase
    public void setTLPhaseNext(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.setPhaseNext(this);
    }
    // set the duration for current phase
    public void setTLPhaseDuration(String inputID, double inputTime) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.setPhaseDuration(this, inputTime);
    }
    // set all traffic light to next phase
    public void setTLPhaseNextAll() {
        List<String> tlIDs = new ArrayList<>(TrafficLightList.keySet());
        for(String tlID : tlIDs) {
            setTLPhaseNext(tlID);
        }
    }
//===== VEHICLE STUFF =====================================
//===== GETTER ============================================
    // get position x of the vehicle
    public double getVehiclePositionX(String inputID) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper x = VehicleList.get(inputID);
            double VehiclePosX = x.getPositionX(1);
            return VehiclePosX;
        }
        else {return 0;}
    }
    // get position y of the vehicle
    public double getVehiclePositionY(String inputID) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper x = VehicleList.get(inputID);
            double VehiclePosY = x.getPositionY(1);
            return VehiclePosY;
        }
        else {return 0;}
    }
    // get Vehicle speed
    public double getVehicleSpeed(String inputID) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper x = VehicleList.get(inputID);
            double vehicleSpeed = x.getSpeed(0);
            return vehicleSpeed;
        }
        else {return -1;}
    }
    // get Vehicle's color NEED FIX
    // public SumoColor getVehicleColor(String inputID) {
    //     VehicleWrapper x = VehicleList.get(inputID);
    //     SumoColor vehicleColor = x.getColor(0);
    //     return vehicleColor;
    // }
    // get Vehicle's angle
    public double getVehicleAngle(String inputID) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper x = VehicleList.get(inputID);
            double vehicleAngle = x.getAngle(0);
            return vehicleAngle;
        }
        return 0;
    }
    // get Vehicle's ID list
    public List<String> getVehicleIDsList() {
        List<String> returnVehicleList = new ArrayList<>(VehicleList.keySet());
        return returnVehicleList;
    }
    // get completed travel times list Nguyen
    public List<Double> getCompletedTravelTimes() {
        return new ArrayList<>(CompletedTravelTimes);
    }
    // get average speed of all vehicle
    public double getVehicleAverageSpeed(int po) {
        double result = 0;
        for (VehicleWrapper x : VehicleList.values()) {result += x.speed;}
        if (VehicleList.size() != 0) result /= VehicleList.size();
        if (po == 1) {LOG.info("Average speed is " + result);}
        return result;
    }
//===== MAKE COPY =========================================
    public DataType.VehicleData makeVehicleCopy(String inputID) {
        VehicleWrapper x = VehicleList.get(inputID);
        return x.makeCopy();
    }
//===== SETTER ============================================
    // set Vehicle's speed
    public void setVehicleSpeed(String inputID, double inputSpeed) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper x = VehicleList.get(inputID);
            x.setSpeed(this, inputSpeed, 0);
        }
        else {LOG.log(Level.WARNING, "Vehicle not exist");}
    }
    // set Vehicle's color
    public void setVehicleColor(String inputID, double r, double g, double b, double a) {
        if (VehicleList.containsKey(inputID)) {  
            VehicleWrapper x = VehicleList.get(inputID);
            x.setColor(this, r, g, b, a);
        }
        else {LOG.log(Level.WARNING, "Vehicle not exist");}
    }
//===== ADDER =============================================
    // add a vehicle into the 1st route in RouteList (might not work)
    public void addVehicleBasic(String inputID) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (RouteList.size() == 0) {LOG.log(Level.WARNING, "No available routes");}
            else {VehicleWrapper.addVehicle(this, inputID, "r_0");}
        }
        catch (Exception e) {LOG.log(Level.WARNING, "Invalid injection", e);}
    }
    // add a vehicle into selected route
    public void addVehicleNormal(String inputID, String inputRouteID) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (1==0) {LOG.log(Level.WARNING, "Invalid injection");}
            else {
                VehicleWrapper.addVehicle(this, inputID, inputRouteID);
                LOG.log(Level.INFO, "Added vehicle {0} to {1}", new Object[]{inputID, inputRouteID});
            }
        }
        catch (Exception e) {LOG.log(Level.WARNING, "Error when adding vehicle {0} normally", new Object[]{inputID, e});}
    }
    // add vehicle with color
    public void addVehicleWithColor(String inputID, String inputRouteID, Color inputColor) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (1==0) {LOG.log(Level.WARNING, "Invalid injection");}
            else {VehicleWrapper.addVehicle(this, inputID, inputRouteID);}
            ColorQueue.put(inputID, inputColor);
        }
        catch (Exception e) {LOG.log(Level.WARNING, "Error when adding vehicle with color", e);}
    }
//===== ROUTE STUFF ========================================
//===== GETTER =============================================
    //get number of available route
    public int getRouteNum(int po) {
        RouteWrapper.updateRouteIDs(this);
        int routeNum = RouteList.size();
        if (po == 1) {LOG.info("Number of routes is " + routeNum);}
        return routeNum;
    }
    // get first edge id of the route
    public String getRouteFirstEdge(String inputID) {
        RouteWrapper x = RouteList.get(inputID);
        return x.getFirstEdgeID(0);
    }
    // get route id list
    public List<String> getRouteIDsList() {
        List<String> routeIDs = new ArrayList<>(RouteList.keySet());
        return routeIDs;
    }
//===== MAKE COPY ==========================================
    public DataType.RouteData makeRouteCopy(String inputID) {
        RouteWrapper x = RouteList.get(inputID);
        return x.makeCopy();
    }
//===== EDGE STUFF =========================================
//===== GETTER =============================================
    public int getEdgeDensity(String inputID) {
        EdgeWrapper x = EdgeList.get(inputID);
        return x.density;
    }
    public double getEdgeTravelTime(String inputID) {
        EdgeWrapper x = EdgeList.get(inputID);
        return x.travelTime;
    }
    public double getEdgeWaitingTime(String inputID) {
        EdgeWrapper x = EdgeList.get(inputID);
        return x.waitingTime;
    }
    public boolean getEdgeCongested(String inputID) {
        EdgeWrapper x = EdgeList.get(inputID);
        return x.congested;
    }
    public List<String> getEdgeIDsList() {
        List<String> edgeIDs = new ArrayList<>(EdgeList.keySet());
        return edgeIDs;
    }
}
