package wrapper;

import wrapper.DataType.RouteNotFoundException;
import wrapper.DataType.VehicleExistedException;

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

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.scene.paint.Color;

public class SimulationWrapper implements Observer {
    private final static Logger LOG = Logger.getLogger(SimulationWrapper.class.getName());
    protected static SumoTraciConnection conn; 
    protected int delay = 200;
    protected boolean isPaused = false;

    protected final HashMap<String, TrafficLightWrapper> TrafficLightList = new HashMap<>();
    protected final HashMap<String, VehicleWrapper> VehicleList = new HashMap<>();
    protected final HashMap<String, RouteWrapper> RouteList = new HashMap<>(); 
    protected final HashMap<String, EdgeWrapper> EdgeList = new HashMap<>();
    protected final HashMap<String, Color> ColorQueue = new HashMap<>();
    // Track vehicle travel times
    protected final ArrayList<Double> CompletedTravelTimes = new ArrayList<>();

    public SimulationWrapper(String sumocfg, double step_length, String sumo_bin){
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true");
        LOG.info("Simulation created");
    }
    public SimulationWrapper(String sumocfg){
        String sumo_bin = "sumo";
        double step_length = 1;
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true");
        LOG.info("Simulation created");
    }
//===== SIMULATION STUFF ==================================
    public boolean isClosed() {
        return conn.isClosed();
    } 
    public boolean isPaused() {
        return isPaused;
    }
    public void Pause() {
        if (!isPaused) {isPaused = true;}
        else {isPaused = false;}
    }
    public void Start(){
        try {
            conn.runServer(); 
            conn.setOrder(1); 
            conn.addObserver(this);
            //start subscription to look out for departed (spawn in) and arrived (despawn) vehicle
            VariableSubscription vs = new VariableSubscription(SubscribtionVariable.simulation, 0, 100000 * 60, "");
            vs.addCommand(Constants.VAR_DEPARTED_VEHICLES_IDS);
            vs.addCommand(Constants.VAR_ARRIVED_VEHICLES_IDS);
            conn.do_subscription(vs);

            TrafficLightWrapper.updateTrafficLightIDs(this);
            EdgeWrapper.updateEdgeIDs(this);
            RouteWrapper.updateRouteIDs(this);
            LOG.info("Simulation started successfully");
        }
        catch(Exception e) {
            LOG.log(Level.SEVERE, "Failed to start simulation", e);
        }
    }
    public void Step(){
        if(!isPaused) {
            try {
                Thread.sleep(delay);
                conn.do_timestep();
            }
            catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to step simulation", e);
            }
        }
    }
    public void End() {
        conn.close();
    }
    public double getTime(int po) {
        try {
            double time = (double) conn.do_job_get(Simulation.getTime());
            if (po == 1) {LOG.info("Current Time: " + time);}
            return time;
        }
        catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to get simulation time", e);
            return -1;
        }
    }
    // update from subscription, abstract method of observer
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
                            VehicleWrapper vehicle = new VehicleWrapper(vehID, DataType.convertColor(color));
                            VehicleList.put(vehID, vehicle);
                            vehicle.departTime = this.getTime(0);
                            // start subscription of the vehicle
                            conn.do_subscription(vs);
                            if (ColorQueue.get(vehID) != null) {
                                Color defaultColor = ColorQueue.get(vehID);
                                double r = defaultColor.getRed();
                                double g = defaultColor.getGreen();
                                double b = defaultColor.getBlue();
                                this.setVehicleColor(vehID, r, g, b, 1);
                                ColorQueue.remove(vehID);
                            }
                        } 
                        catch (Exception e) {
                            LOG.log(Level.SEVERE, "subscription to " + vehID + " failed", e);
                        }
                    }
                }
            }
            else if (so.variable == Constants.VAR_ARRIVED_VEHICLES_IDS) {
                SumoStringList ssl = (SumoStringList) so.object;
                if (ssl.size() > 0) {
                    for (String vehID : ssl) {
                        try {
                            VehicleWrapper vehicle = VehicleList.get(vehID);
                            double travelTime = this.getTime(0) - vehicle.departTime;
                            CompletedTravelTimes.add(travelTime);
                            VehicleList.remove(vehID);
                        }
                        catch (Exception e) {
                            LOG.log(Level.SEVERE, "Unable to delete " + vehID, e);
                        }
                    }
                }
            }
        } 
        else if (so.response == ResponseType.VEHICLE_VARIABLE) { 
            VehicleWrapper vehicle = VehicleList.get(so.id);

            if (so.variable == Constants.VAR_SPEED) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                vehicle.speed = (double) sp.val;
            } 
            else if (so.variable == Constants.VAR_POSITION) {
                SumoPosition2D sc = (SumoPosition2D) so.object;
                vehicle.pos_x = sc.x;
                vehicle.pos_y = sc.y;
            }
            else if (so.variable == Constants.VAR_ANGLE) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                vehicle.angle = (double) sp.val;
            }
        }
        else if (so.response == ResponseType.TL_VARIABLE) { 
            if (so.variable == Constants.TL_RED_YELLOW_GREEN_STATE) { 
                SumoPrimitive sp = (SumoPrimitive) so.object;
                TrafficLightWrapper tl = TrafficLightList.get(so.id);
                tl.lightDef = (String) sp.val;
            }
        }
        else if (so.response == ResponseType.EDGE_VARIABLE) {
            EdgeWrapper edge = EdgeList.get(so.id);
            if (so.variable == Constants.LAST_STEP_VEHICLE_NUMBER) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                edge.density = (int) sp.val;
            }
            if (so.variable == Constants.VAR_CURRENT_TRAVELTIME) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                edge.travelTime = (double) sp.val;
            }
            if (so.variable == Constants.VAR_WAITING_TIME) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                edge.waitingTime = (double) sp.val;
                if (edge.waitingTime > 130) edge.congested = true;
                else edge.congested = false;
            }
        }
    }
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
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        int phaseNum = tl.getPhaseNum(this, 0);
        return phaseNum;
    }
    // get phase definition of a traffic light (current light state)
    public String getTLPhaseDef(String inputID) {
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        String phaseDef = tl.getPhaseDef(0);
        return phaseDef;
    }
    //get a traffic light definition and from, to lane ID of a link
    public List<String> getTLDefFromTo(String inputID, int index) {
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        if (index < tl.controlledLinksNum) {
            List<String> defFromTo = tl.getDefFromTo(index);
            return defFromTo;
        }
        else {return null;}
    }
    //
    public List<String> getTLControlledJunctions(String inputID) {
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        List<String> controlledJunctions = tl.getControlledJunctions(this, 0);
        return controlledJunctions;
    }
//===== MAKE COPY =========================================
    public DataType.TrafficLightData makeTLCopy(String inputID) {
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        return tl.makeCopy();
    }
//===== SETTER ============================================
    // set phase definition of a traffic light to the origin
    public void setTLPhaseDefOrigin(String inputID) {
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        tl.setPhaseDefOrigin(this);
    }
    // set to the next phase
    public void setTLPhaseNext(String inputID) {
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        tl.setPhaseNext(this);
    }
    // set the duration for current phase
    public void setTLPhaseDuration(String inputID, double inputTime) {
        TrafficLightWrapper tl = TrafficLightList.get(inputID);
        tl.setPhaseDuration(this, inputTime);
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
            VehicleWrapper vehicle = VehicleList.get(inputID);
            double VehiclePosX = vehicle.getPositionX(0);
            return VehiclePosX;
        }
        else {return 0;}
    }
    // get position y of the vehicle
    public double getVehiclePositionY(String inputID) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper vehicle = VehicleList.get(inputID);
            double VehiclePosY = vehicle.getPositionY(0);
            return VehiclePosY;
        }
        else {return 0;}
    }
    // get Vehicle speed
    public double getVehicleSpeed(String inputID) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper vehicle = VehicleList.get(inputID);
            double vehicleSpeed = vehicle.getSpeed(0);
            return vehicleSpeed;
        }
        else {return -1;}
    }
    // get Vehicle's color
    public Color getVehicleColor(String inputID) {
        VehicleWrapper vehicle = VehicleList.get(inputID);
        Color vehicleColor = vehicle.getColor(0);
        return vehicleColor;
    }
    // get Vehicle's angle
    public double getVehicleAngle(String inputID) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper vehicle = VehicleList.get(inputID);
            double vehicleAngle = vehicle.getAngle(0);
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
        for (VehicleWrapper vehicle : VehicleList.values()) {
            result += vehicle.speed;
        }

        if (VehicleList.size() != 0) {result /= VehicleList.size();}
        if (po == 1) {LOG.info("Average speed is " + result);}
        return result;
    }
//===== MAKE COPY =========================================
    public DataType.VehicleData makeVehicleCopy(String inputID) {
        VehicleWrapper vehicle = VehicleList.get(inputID);
        return vehicle.makeCopy();
    }
//===== SETTER ============================================
    public void setVehicleSpeed(String inputID, double inputSpeed) {
        if (VehicleList.containsKey(inputID)) {
            VehicleWrapper vehicle = VehicleList.get(inputID);
            vehicle.setSpeed(this, inputSpeed, 0);
        }
        else {LOG.info("Vehicle not exist, set speed failed");}
    }
    public void setVehicleColor(String inputID, double r, double g, double b, double a) {
        if (VehicleList.containsKey(inputID)) {  
            VehicleWrapper vehicle = VehicleList.get(inputID);
            vehicle.setColor(this, r, g, b, a);
        }
        else {LOG.info("Vehicle not exist, set color failed");}
    }
//===== ADDER =============================================
    //add a vehicle into selected route
    public void addVehicleNormal(String inputID, String inputRouteID) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (VehicleList.containsKey(inputID)) {throw new VehicleExistedException(inputID);}
            else if (!RouteList.containsKey(inputRouteID)) {throw new RouteNotFoundException(inputRouteID);}
            else VehicleWrapper.addVehicle(this, inputID, inputRouteID);
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Error when adding vehicle normally", e);
        }
    }
    //add vehicle with color into selected route
    public void addVehicleWithColor(String inputID, String inputRouteID, Color inputColor) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (VehicleList.containsKey(inputID)) {throw new VehicleExistedException(inputID);}
            else if (!RouteList.containsKey(inputRouteID)) {throw new RouteNotFoundException(inputRouteID);}
            else {VehicleWrapper.addVehicle(this, inputID, inputRouteID);}
            ColorQueue.put(inputID, inputColor);
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Error when adding vehicle with color", e);
        }
    }
//===== ROUTE STUFF ========================================
//===== GETTER =============================================
    //get number of available route
    public int getRouteNum(int po) {
        RouteWrapper.updateRouteIDs(this);
        int routeNum = RouteList.size();
        if (po == 1) {LOG.info("" + routeNum);}
        return routeNum;
    }
    //get first edge id of the route
    public String getRouteFirstEdge(String inputID) {
        RouteWrapper route = RouteList.get(inputID);
        return route.getFirstEdgeID(0);
    }
    //get route id list
    public List<String> getRouteIDsList() {
        List<String> routeIDs = new ArrayList<>(RouteList.keySet());
        return routeIDs;
    }
//===== MAKE COPY ==========================================
    public DataType.RouteData makeRouteCopy(String inputID) {
        RouteWrapper route = RouteList.get(inputID);
        return route.makeCopy();
    }
//===== EDGE STUFF =========================================
//===== GETTER =============================================
    public int getEdgeDensity(String inputID) {
        EdgeWrapper edge = EdgeList.get(inputID);
        return edge.density;
    }
    public double getEdgeTravelTime(String inputID) {
        EdgeWrapper edge = EdgeList.get(inputID);
        return edge.travelTime;
    }
    public double getEdgeWaitingTime(String inputID) {
        EdgeWrapper edge = EdgeList.get(inputID);
        return edge.waitingTime;
    }
    public boolean getEdgeCongested(String inputID) {
        EdgeWrapper edge = EdgeList.get(inputID);
        return edge.congested;
    }
    public List<String> getEdgeIDsList() {
        List<String> edgeIDs = new ArrayList<>(EdgeList.keySet());
        return edgeIDs;
    }
}
