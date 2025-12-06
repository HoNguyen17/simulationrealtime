package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Vehicle;

import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoPosition2D;
import java.util.List;
import java.util.ArrayList;

public class SimulationWrapper {
    protected SumoTraciConnection conn;
    protected int delay = 200;
    protected final List<TrafficLightWrapper> TrafficLightList = new ArrayList<TrafficLightWrapper>();
    //protected final List<EdgeWrapper> EdgeList = new ArrayList<EdgeWrapper>();
    //List<String> VehicleList = new ArrayList<VehicleWrapper>();
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
    public void Start(){
        try {
            conn.runServer();
            conn.setOrder(1);
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


//===== TRAFFIC LIGHT STUFF ===============================
//===== GETTER ============================================
    // print all traffic light IDs
    public void printTrafficLightList() {
        System.out.println("List of Traffic Light IDs:");
        for (TrafficLightWrapper x : TrafficLightList) {
            x.getID(1);
        }
        System.out.println("");
    }
    // get phase number of a traffic light
    public int getTLPhaseNum(int temp) {
        TrafficLightWrapper x = TrafficLightList.get(temp);
        int phaseNum= x.getPhaseNum(this, 1);
        return phaseNum;
    }
    // get phase definition of a traffic light (current light state)
    public String getTLPhaseDef(int temp) {
        TrafficLightWrapper x = TrafficLightList.get(temp);
        String phaseDef = x.getPhaseDef(this, 1);
        return phaseDef;
    }
    public List<String[][]> getTLControlledLinks(int temp) {
        TrafficLightWrapper x = TrafficLightList.get(temp);
        List<String[][]> controlledLinks = x.getControlledLinks(this, 1);
        return null;
    }
//===== SETTER ============================================
    public void setTLPhaseDef(int temp, String input) {
        TrafficLightWrapper x = TrafficLightList.get(temp);
        x.setPhaseDefWPT(this, input, 10);
    }
//===== VEHICLE STUFF =====================================
//===== GETTER ============================================
    public SumoPosition2D getPosition(String ID) {
        VehicleWrapper v = new wrapper.VehicleWrapper(ID);
        return v.getPosition(this, 1);
    }

    // get Vehicle speed
    public double getSpeed(String ID) {
        VehicleWrapper v = new wrapper.VehicleWrapper(ID);
        return v.getSpeed(this, 1);
    }

    // get Vehicle's ID list
    public List<String> getIDList() {
        return wrapper.VehicleWrapper.getIDList(this, 1);
    }

    // get Vehicle's type ID
    public String getTypeID(String ID) {
        VehicleWrapper v = new wrapper.VehicleWrapper(ID);
        return v.getTypeID(this, 1);
    }

    // get Vehicle's color
    public SumoColor getColor(String ID) {
        VehicleWrapper v = new wrapper.VehicleWrapper(ID);
        return v.getColor(this, 1);
    }
//===== SETTER ============================================
    // set Vehicle's speed
    public void setSpeed(String ID, double speed) {
        VehicleWrapper v = new wrapper.VehicleWrapper(ID);
        v.setSpeed(this, speed, 1);
    }

    // set Vehicle's color
    public void setColor(String ID, int r, int b, int g, int a) {
        VehicleWrapper v = new wrapper.VehicleWrapper(ID);
        v.setColor(this, r, g, b, a, 1);
    }

}