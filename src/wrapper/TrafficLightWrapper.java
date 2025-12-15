package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Trafficlight;

import de.tudresden.sumo.config.Constants;

import de.tudresden.sumo.objects.SumoTLSController;
import de.tudresden.sumo.objects.SumoTLSProgram;
import de.tudresden.sumo.objects.SumoLink;

import de.tudresden.sumo.subscription.VariableSubscription;
import de.tudresden.sumo.subscription.SubscribtionVariable;
import de.tudresden.sumo.subscription.SubscriptionObject;
import de.tudresden.sumo.subscription.ResponseType;

import java.util.List;
import java.util.ArrayList;

// This entire class serves to encapsulate the complexities of TraCI commands and organize traffic light data for easy manipulation and monitoring from the main SimulationWrapper
class TrafficLightWrapper { // helper class to simplify the interaction with single traffic light entity within a SUMO simulation
    String ID;
    String originProgramID;
    String lightDef; // current state of the traffic light signals as a sequence of characters like "rGGryg"
    // lists of the edge IDs that lead to (from) and lead away from (to) the traffic light, corresponding to the controlled links
    List<String> from;
    List<String> to;
    int controlledLinksNum;
    // constructor
    TrafficLightWrapper(String inputID, String startProgram, List<String> inputFrom, List<String> inputTo){
        ID = inputID;
        originProgramID = startProgram;
        from = inputFrom;
        to = inputTo;
        controlledLinksNum = inputFrom.size(); // the number of controlled links by the traffic light, equal to the size of the (from) and (to) lists
        System.out.println("Added " + ID + " with program " + originProgramID);
    }
//=================GETTER================================
    // get the traffic light ID
    public String getID(int po) {
        if (po == 1) {System.out.print(" " + ID);}
        return ID;
    }
    // get the current phase index (number) of the traffic light's program from SUMO
    public int getPhaseNum(SimulationWrapper temp, int po) {
        try {
            int tlsPhase = (int)temp.conn.do_job_get(Trafficlight.getPhase(ID));
            if (po == 1) {System.out.println(String.format("tlsPhase of %s: %d", ID, tlsPhase));}
            return tlsPhase;
        }
        catch(Exception A) {
            System.out.println("Failed to get phase number.");
        }
        return -1;
    }
    // get phase definition (Red-Green-Yellow) stored in the wrapper object, updated asynchronously via subscription
    public String getPhaseDef(int po) {
        if (po == 1) {System.out.println(String.format("Current phase definition of %s: %s", ID, lightDef));}
        return lightDef;
    }
    // get the number of controlled links
    public int getControlledLinksNum(int po) {
        if (po == 1) {System.out.println(controlledLinksNum);}
        return controlledLinksNum;
    }

    // get a list containing the light state, the (from) edge ID, and the (to) edge ID for a specific controlled link index
    public List<String> getDefFromTo(int index, int po) {
        List<String> result = new ArrayList<String>();
        result.add("" + lightDef.charAt(index));
        result.add(from.get(index));
        result.add(to.get(index));
        if (po == 1) {System.out.println(result);}
        return result;
    }

    // get a summary of the traffic light's controlled links and current light state
    public void getControlledLinks(SimulationWrapper temp, int po) {
        try {
            if (po == 1){
                System.out.println("Number of links of " + ID + ":" + controlledLinksNum);
                System.out.println("From of " + ID + ":" + to);
                System.out.println("To of " + ID + ":" + from);
                System.out.println("Current light of " + ID + ":" + lightDef);
            }
        }   
        catch (Exception C) {
            System.out.println("Cannot get controlled links of traffic light");
        }
    }
//=================SETTER================================
    // set phase definition (Red-Green-Yellow)
    public boolean setPhaseDef(SimulationWrapper temp, String input) {
        try {               //maybe need check??
            temp.conn.do_job_set(Trafficlight.setRedYellowGreenState(ID, input));
            return true;
        }
        catch (Exception D) {
            System.out.println("Unable to set controlled links of traffic light");
        }
        return false;
    // set phase definition within a specified time, then set back to the originProgramID  (Red-Green-Yellow with time)
    }
    public boolean setPhaseDefWithPhaseTime(SimulationWrapper temp, String inputDef, double inputTime) {
        try {               //maybe need check??
            long roundedTime = Math.round(inputTime * temp.delay);
            temp.conn.do_job_set(Trafficlight.setRedYellowGreenState(ID, inputDef));
            Thread.sleep(roundedTime); 
            temp.conn.do_job_set(Trafficlight.setProgram(ID, originProgramID));
        }
        catch (Exception D) {
            System.out.println("Unable to set phase with time");
        }
    return false;
    }
    // set phase definition to origin (auto)
    public boolean setPhaseDefOrigin(SimulationWrapper temp) {
        try {
            temp.conn.do_job_set(Trafficlight.setProgram(ID, originProgramID));
            return true;
        }
        catch (Exception F) {
            System.out.println("Unable to set light definition back to auto");
        }
        return false;
    }
    // set the traffic light program to the next phase
    public boolean setPhaseNext(SimulationWrapper temp) {
        try {
            String program = (String)temp.conn.do_job_get(Trafficlight.getProgram(ID));
            SumoTLSController TLController = (SumoTLSController) temp.conn.do_job_get(Trafficlight.getCompleteRedYellowGreenDefinition("J1"));
            int phaseNumLimit = TLController.programs.get(program).phases.size();
            int currentPhaseNum = (int)temp.conn.do_job_get(Trafficlight.getPhase(ID)); // get the current phase index
            if(currentPhaseNum < phaseNumLimit) {temp.conn.do_job_set(Trafficlight.setPhase(ID, currentPhaseNum + 1));} // increment the phase index
            else {temp.conn.do_job_set(Trafficlight.setPhase(ID, 0));}
            return true;
        }
        catch (Exception G) {
            System.out.println("Unable to set to next phase");
        }
        return false;
    }
//=================STATIC================================
    // update all traffic light IDs of simulation
    protected static void updateTrafficLightIDs(SimulationWrapper temp) {
        try {
            @SuppressWarnings("unchecked")
            List<String> IDsList = (List<String>)temp.conn.do_job_get(Trafficlight.getIDList()); // fetch a list of all traffic light IDs in the network
            for (String x : IDsList) { // for each ID (x)
                // set up base variable
                String program = (String)temp.conn.do_job_get(Trafficlight.getProgram(x)); // retrieve the current program ID
                List<String> inputFrom = new ArrayList<String>();
                List<String> inputTo = new ArrayList<String>();
                List<SumoLink> controlledLinks = (List<SumoLink>)temp.conn.do_job_get(Trafficlight.getControlledLinks(x)); // retrieve the list of controlled links, which are then parsed to populate the (inputFrom) and (inputTo) lists
                for (int i = 0; i < controlledLinks.size(); i++) {
                    SumoLink link = controlledLinks.get(i);
                    inputFrom.add(link.from);
                    inputTo.add(link.to);
                }
                // create a new TrafficLightWrapper object with the gathered data and add to the temp.TrafficLightList HashMap in the SimulationWrapper
                TrafficLightWrapper y = new TrafficLightWrapper(x, program, inputFrom, inputTo);
                temp.TrafficLightList.put(x, y);

                // set up subscription for traffic light
                VariableSubscription vs = new VariableSubscription(SubscribtionVariable.trafficlight, 0, 100000 * 60, x); // initiates a variable subscription for each traffic light ID
                vs.addCommand(Constants.TL_RED_YELLOW_GREEN_STATE);
                temp.conn.do_subscription(vs);
                System.out.println("subscribe " + x);
            }
        }
        catch (Exception A) {
            System.out.println("Set up traffic lights failed.");
        }
    }
}