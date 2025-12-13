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

class TrafficLightWrapper {
    String ID;
    String originProgramID;
    String lightDef;
    List<String> from;
    List<String> to;
    int controlledLinksNum;
    // constructor
    TrafficLightWrapper(String inputID, String startProgram, List<String> inputFrom, List<String> inputTo){
        ID = inputID;
        originProgramID = startProgram;
        from = inputFrom;
        to = inputTo;
        controlledLinksNum = inputFrom.size();
        System.out.println("Added " + ID + " with program " + originProgramID);
    }
//=================GETTER================================
    // get ID
    public String getID(int po) {
        if (po == 1) {System.out.print(" " + ID);}
        return ID;
    }
    // get phase number
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
    // get phase definition (Red-Green-Yellow)
    public String getPhaseDef(int po) {
        if (po == 1) {System.out.println(String.format("Current phase definition of %s: %s", ID, lightDef));}
        return lightDef;
    }
    // get controlled links
    public int getControlledLinksNum(int po) {
        if (po == 1) {System.out.println(controlledLinksNum);}
        return controlledLinksNum;
    }
    public List<String> getDefFromTo(int index, int po) {
        List<String> result = new ArrayList<String>();
        result.add("" + lightDef.charAt(index));
        result.add(from.get(index));
        result.add(to.get(index));
        if (po == 1) {System.out.println(result);}
        return result;
    }
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
    // set phase definition with phase time (Red-Green-Yellow with time)  
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
    // set next phase
    public boolean setPhaseNext(SimulationWrapper temp) {
        try {
            String program = (String)temp.conn.do_job_get(Trafficlight.getProgram(ID));
            SumoTLSController TLController = (SumoTLSController) temp.conn.do_job_get(Trafficlight.getCompleteRedYellowGreenDefinition("J1"));
            int phaseNumLimit = TLController.programs.get(program).phases.size();
            int currentPhaseNum = (int)temp.conn.do_job_get(Trafficlight.getPhase(ID));
            if(currentPhaseNum < phaseNumLimit) {temp.conn.do_job_set(Trafficlight.setPhase(ID, currentPhaseNum + 1));}
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
            List<String> IDsList = (List<String>)temp.conn.do_job_get(Trafficlight.getIDList());
            for (String x : IDsList) {
                // set up base variable
                String program = (String)temp.conn.do_job_get(Trafficlight.getProgram(x));
                List<String> inputFrom = new ArrayList<String>();
                List<String> inputTo = new ArrayList<String>();
                List<SumoLink> controlledLinks = (List<SumoLink>)temp.conn.do_job_get(Trafficlight.getControlledLinks(x));
                for (int i = 0; i < controlledLinks.size(); i++) {
                    SumoLink link = controlledLinks.get(i);
                    inputFrom.add(link.from);
                    inputTo.add(link.to);
                }
                TrafficLightWrapper y = new TrafficLightWrapper(x, program, inputFrom, inputTo);
                temp.TrafficLightList.put(x, y);

                // set up subscription for traffic light
                VariableSubscription vs = new VariableSubscription(SubscribtionVariable.trafficlight, 0, 100000 * 60, x);
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