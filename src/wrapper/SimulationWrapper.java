package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import it.polito.appeal.traci.TraCIException;

import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Inductionloop;

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

public class SimulationWrapper implements Observer {
    protected static SumoTraciConnection conn;
    protected int delay = 200;

    protected final HashMap<String, TrafficLightWrapper> TrafficLightList = new HashMap<>();
    protected HashMap<String, VehicleWrapper> VehicleList = new HashMap<>();
    //protected final List<TrafficLightWrapper> TrafficLightList = new ArrayList<TrafficLightWrapper>();
    //protected final List<EdgeWrapper> EdgeList = new ArrayList<EdgeWrapper>();
    //protected List<VehicleWrapper> VehicleList = new ArrayList<VehicleWrapper>();
    // Constructor 1
    public SimulationWrapper(String sumocfg, double step_length, String sumo_bin){
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true"); //start sumo immediately
        System.out.println("Simulation created");
    }
    // Constructor 2
    SimulationWrapper(String sumocfg){
        String sumo_bin = "sumo";
        double step_length = 1;
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true"); //start sumo immediately
        System.out.println("Simulation created");
    }
//===== SIMULATION STUFF ==================================
    // Start simulation, update TrafficLightList, more will be implemented
    public void test() {
        int a = VehicleList.size();
        System.out.println("Size of hash map of vehicle is " + a);
    }
    public void Start(){
        try {
            conn.runServer();
            conn.setOrder(1);
            conn.addObserver(this);// add observer
            //start subscription to look out for departed (spawn in) and arrived (despawn) vehicle
            VariableSubscription vs = new VariableSubscription(SubscribtionVariable.simulation, 0, 100000 * 60, "");//set up the variable subscriptoion
            vs.addCommand(Constants.VAR_DEPARTED_VEHICLES_IDS);//choose when
            vs.addCommand(Constants.VAR_ARRIVED_VEHICLES_IDS);
            conn.do_subscription(vs);//start the subscription

            System.out.println("this still work");
            TrafficLightWrapper.updateTrafficLightIDs(this);
            System.out.println("Started successfully.");
        }
        catch(Exception e) {System.out.println("Failed to start.");}
    }
    // Do a simulation's time step
    public void Step(){
        try {
            Thread.sleep(delay);
            conn.do_timestep();
        }
        catch(Exception e) {System.out.println("Failed to step.");}
    }
    // Close simulation
    public void End() {
        conn.close();
    }
    // Get simulation time
    public double getTime(int po) {
        try {
            double time = (double)conn.do_job_get(Simulation.getTime());
            if (po == 1) {System.out.println("Current Time: " + time);}
            return time;
        }
        catch(Exception e) {System.out.println("Can't get the time.");}
        return -1;
    }
    //(new) update from subscription Constants.VAR_STOPPED_VEHICLES_IDS
    public void update(Observable arg0, SubscriptionObject so) {
        if (so.response == ResponseType.SIM_VARIABLE) { 
            if (so.variable == Constants.VAR_DEPARTED_VEHICLES_IDS) {//when new vehicle detect?
                SumoStringList ssl = (SumoStringList) so.object;
                if (ssl.size() > 0) {
                    for (String vehID : ssl) {
                        //System.out.println("Subscription Departed vehicles: " + vehID);
                        //set up the subscription for the vehicle (1 vehicle)
                        VariableSubscription vs = new VariableSubscription(SubscribtionVariable.vehicle, 0, 100000 * 60, vehID);
                        vs.addCommand(Constants.VAR_POSITION);
                        vs.addCommand(Constants.VAR_SPEED);
                        
                        try {
                            // create a vehicle wrapper object and add to hash map
                            SumoColor color = (SumoColor)conn.do_job_get(Vehicle.getColor(vehID));
                            VehicleWrapper y = new VehicleWrapper(vehID, color);
                            VehicleList.put(vehID, y);
                            // start subscription of the vehicle
                            conn.do_subscription(vs);
                        } 
                        catch (Exception ex) {System.err.println("subscription to " + vehID + " failed");}
                    }
                }
            }
            else if (so.variable == Constants.VAR_ARRIVED_VEHICLES_IDS) {
                SumoStringList ssl = (SumoStringList) so.object;
                if (ssl.size() > 0) {
                    for (String vehID : ssl) {
                        try {
                            VehicleList.remove(vehID);
                            //System.out.println("Delete " + vehID + " from the hashmap");
                        }
                        catch (Exception ex) {
                            System.err.println("Unable to delete " + vehID + " from hashmap");
                        }
                    }
                }
            }
        } 
        else if (so.response == ResponseType.VEHICLE_VARIABLE) {
            if (so.variable == Constants.VAR_SPEED) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                VehicleWrapper x = VehicleList.get(so.id);
                x.speed = (double) sp.val;
            } 
            else if (so.variable == Constants.VAR_POSITION) {
                SumoPosition2D sc = (SumoPosition2D) so.object;
                VehicleWrapper x = VehicleList.get(so.id);
                x.position = sc;
            }
        }
        else if (so.response == ResponseType.TL_VARIABLE) {
            if (so.variable == Constants.TL_RED_YELLOW_GREEN_STATE) {
                SumoPrimitive sp = (SumoPrimitive) so.object;
                TrafficLightWrapper x = TrafficLightList.get(so.id);
                x.lightDef = (String) sp.val;
                System.out.println("traffic light "+ so.id + " "+ sp.val);
            }
        }
    }
    // set delay
    public void setDelay(int input) {
        delay = input;
    }



//===== TRAFFIC LIGHT STUFF ===============================
//===== GETTER ============================================
    // print all traffic light IDs
    // public void printTrafficLightList() {
    //     System.out.println("List of Traffic Light IDs:");
    //     for (TrafficLightWrapper x : TrafficLightList) {
    //         x.getID(1);
    //     }
    //     System.out.println("");
    // }
    // get phase number of a traffic light
    public int getTLPhaseNum(String ID) {
        TrafficLightWrapper x = TrafficLightList.get(ID);
        int phaseNum = x.getPhaseNum(this, 1);
        return phaseNum;
    }
    // get phase definition of a traffic light (current light state)
    public String getTLPhaseDef(String ID) {
        TrafficLightWrapper x = TrafficLightList.get(ID);
        String phaseDef = x.getPhaseDef(this, 1);
        return phaseDef;
    }
    public List<String[][]> getTLControlledLinks(String ID) {
        TrafficLightWrapper x = TrafficLightList.get(ID);
        List<String[][]> controlledLinks = x.getControlledLinks(this, 1);
        return null;
    }
//===== SETTER ============================================
    public void setTLPhaseDef(String ID, String input) {
        TrafficLightWrapper x = TrafficLightList.get(ID);
        x.setPhaseDefWPT(this, input, 10);
    }
//===== VEHICLE STUFF =====================================
//===== GETTER ============================================
    // get position of the vehicle
    public SumoPosition2D getVehiclePosition(String ID) {
        VehicleWrapper x = VehicleList.get(ID);
        SumoPosition2D VehiclePosition = x.getPosition(this, 1);
        return VehiclePosition;
    }
    // get Vehicle speed
    public double getVehicleSpeed(String ID) {
        VehicleWrapper x = VehicleList.get(ID);
        double vehicleSpeed = x.getSpeed(this, 1);
        return vehicleSpeed;
    }

//     // get Vehicle's ID list
//     public List<String> getIDList() {
//         return wrapper.VehicleWrapper.getIDList(this, 1);
//     }

//     // get Vehicle's type ID
//     public String getTypeID(String ID) {
//         VehicleWrapper v = new wrapper.VehicleWrapper(ID);
//         return v.getTypeID(this, 1);
//     }

//     // get Vehicle's color
//     public SumoColor getColor(String ID) {
//         VehicleWrapper v = new wrapper.VehicleWrapper(ID);
//         return v.getColor(this, 1);
//     }
// //===== SETTER ============================================
//     // set Vehicle's speed
//     public void setSpeed(String ID, double speed) {
//         VehicleWrapper v = new wrapper.VehicleWrapper(ID);
//         v.setSpeed(this, speed, 1);
//     }

//     // set Vehicle's color
//     public void setColor(String ID, int r, int b, int g, int a) {
//         VehicleWrapper v = new wrapper.VehicleWrapper(ID);
//         v.setColor(this, r, g, b, a, 1);
//     }
}