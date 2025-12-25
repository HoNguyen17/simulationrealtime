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

public class SimulationWrapper implements Observer {
    protected static SumoTraciConnection conn; //core connection object used to send commands to and receive data from the running SUMO simulation
    protected int delay = 200;
    protected boolean isPaused = false;

    // HashMaps to store custom wrapper objects for easier management
    protected final HashMap<String, TrafficLightWrapper> TrafficLightList = new HashMap<>();
    protected final HashMap<String, VehicleWrapper> VehicleList = new HashMap<>();
    protected List<String> RouteList = new ArrayList<String>(); // a list of available route IDs in the simulation
    // Constructor 1
    public SimulationWrapper(String sumocfg, double step_length, String sumo_bin){
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true"); //start sumo immediately
        System.out.println("Simulation created");
    }
    // Constructor 2
    public SimulationWrapper(String sumocfg){
        String sumo_bin = "sumo";
        double step_length = 1;
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true"); //start sumo immediately
        System.out.println("Simulation created");
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
            System.out.println("Started successfully.");
        }
        catch(Exception e) {System.out.println("Failed to start.");}
    }
    // do a simulation's time step
    public void Step(){
        if(!isPaused) {
            try {
                Thread.sleep(delay);
                conn.do_timestep();
            }
            catch(Exception e) {System.out.println("Failed to step.");}
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
            if (po == 1) {System.out.println("Current Time: " + time);}
            return time;
        }
        catch(Exception e) {System.out.println("Can't get the time.");}
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
                            // start subscription of the vehicle
                            conn.do_subscription(vs);
                        } 
                        catch (Exception ex) {System.err.println("subscription to " + vehID + " failed");}
                    }
                }
            }
            else if (so.variable == Constants.VAR_ARRIVED_VEHICLES_IDS) {// when a vehicle has reached its end point
                SumoStringList ssl = (SumoStringList) so.object;
                if (ssl.size() > 0) {
                    for (String vehID : ssl) {
                        try {
                            VehicleList.remove(vehID);
                            System.out.println("Delete " + vehID + " from the hashmap");
                        }
                        catch (Exception ex) {
                            System.err.println("Unable to delete " + vehID + " from hashmap");
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
    }
    // set delay
    public void setDelay(int input) {
        delay = input;
    } 
//===== TRAFFIC LIGHT STUFF ===============================
//===== GETTER ============================================
    // get a list of traffic light IDs
    public List<String> getTLIDsList() {
        List<String> returnTrafficLightList = new ArrayList<>(TrafficLightList.keySet());
        return returnTrafficLightList;
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
    // get the number of controlled links
    public int getTLControlledLinksNum(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        int linkNum = x.getControlledLinksNum();
        return linkNum;
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
    // get a list of current controlled links
    public void getTLControlledLinks(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.getControlledLinks(this, 0);
    }
//===== MAKE COPY =========================================
    public DataType.TrafficLightData makeTLCopy(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        return x.makeCopy();
    }
//===== SETTER ============================================
    // set the phase definition of a traffic light
    public void setTLPhaseDef(String inputID, String inputDef) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.setPhaseDef(this, inputDef);
    }
    // set the phase definition of a traffic light in a range of time, then set back to previous phase definition
    public void setTLPhaseDefWithPhaseTime(String inputID, String inputDef, int inputTime) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.setPhaseDefWithPhaseTime(this, inputDef, inputTime);
    }
    // set phase definition of a traffic light to the origin
    public void setTLPhaseDefOrigin(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.setPhaseDefOrigin(this);
    }
    public void setTLPhaseNext(String inputID) {
        TrafficLightWrapper x = TrafficLightList.get(inputID);
        x.setPhaseNext(this);
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
    // get average speed of all vehicle
    public double getVehicleAverageSpeed(int po) {
        double result = 0;
        for (VehicleWrapper x : VehicleList.values()) {result += x.speed;}
        result /= VehicleList.size();
        if (po == 1) {System.out.println("Average speed is " + result);}
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
        else {System.out.println("Vehicle not exist");}
    }
    // set Vehicle's color
    public void setVehicleColor(String inputID, int r, int b, int g, int a) {
        if (VehicleList.containsKey(inputID)) {  
            VehicleWrapper x = VehicleList.get(inputID);
            x.setColor(this, r, g, b, a);
        }
        else {System.out.println("Vehicle not exist");}
    }
//===== ADDER =============================================
    // add a vehicle into the 1st route in RouteList
    public void addVehicleBasic(String inputID) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (RouteList.size() == 0) {System.out.println("No available route");}
            else {VehicleWrapper.addVehicle(this, inputID, RouteList.get(0));}
        }
        catch (Exception e) {System.out.println("hmm");}
    }
    //add a vehicle into selected route
    public void addVehicleNormal(String inputID, int inputRoute) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (RouteList.size() == 0 || inputRoute >= RouteList.size()) {System.out.println("Invalid injection");}
            else {VehicleWrapper.addVehicle(this, inputID, RouteList.get(inputRoute));}
        }
        catch (Exception e) {System.out.println("Error when adding vehicle normally");}
    }
    public void addVehicleNormalx(String inputID, int inputRoute) {
        try {
            RouteWrapper.updateRouteIDs(this);
            if (RouteList.size() == 0) {System.out.println("No available route");}
            else {VehicleWrapper.addVehicle(this, inputID, "r_1");}
        }
        catch (Exception e) {System.out.println("Error when adding vehicle normally");}
    }
    public void testRoute() {
        try {
            RouteWrapper.updateRouteIDs(this);
            System.out.println(RouteList);
            List<String> temp1 = (List<String>)conn.do_job_get(Route.getEdges(RouteList.get(0)));
            //List<String> temp2 = (List<String>)conn.do_job_get(Route.getEdges(RouteList.get(1)));
            System.out.println(temp1);
            //System.out.println(temp2);
        }
        catch (Exception e) {System.out.println("test_Route failed");}
    }
//===== ROUTE STUFF ========================================
    //get number of available route
    public int getRouteNum(int po) {
        RouteWrapper.updateRouteIDs(this);
        int routeNum = RouteList.size();
        if (po == 1) {System.out.println(routeNum);}
        return routeNum;
    }
}
