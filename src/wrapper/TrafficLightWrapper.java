package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Trafficlight;

import de.tudresden.sumo.config.Constants;

import de.tudresden.sumo.subscription.VariableSubscription;
import de.tudresden.sumo.subscription.SubscribtionVariable;
import de.tudresden.sumo.subscription.SubscriptionObject;
import de.tudresden.sumo.subscription.ResponseType;

import java.util.List;
import java.util.ArrayList;

public class TrafficLightWrapper {
    String ID;
    String originProgramID;
    String lightDef;
    // constructor
    TrafficLightWrapper(String inputID, String startProgram){
        ID = inputID;
        originProgramID = startProgram;
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
    public String getPhaseDef(SimulationWrapper temp, int po) {
        try {
            String lightState = (String)temp.conn.do_job_get(Trafficlight.getRedYellowGreenState(ID));
            if (po == 1) {System.out.println(String.format("Current phase definition of %s: %s", ID, lightState));}
            return lightState;
        }
        catch (Exception B) {
            System.out.println("Failed to get TL phase definition.");
        }
        return null;
    }
    // get controlled links
    public List<String[][]> getControlledLinks(SimulationWrapper temp, int po) {
        try {
            List<String[][]> controlledLinks = (List<String[][]>)temp.conn.do_job_get(Trafficlight.getControlledLinks(ID));
            if (po == 1){System.out.println("Current controlled links of " + ID + ":" + controlledLinks.get(0));}
            return controlledLinks;
        }   
        catch (Exception C) {
            System.out.println("Cannot get controlled links of traffic light");
        }
        return null;
    }
//=================SETTER================================
    // set phase definition (Red-Green-Yellow)
    public boolean setPhaseDef(SimulationWrapper temp, String input) {
        try {               //maybe need check??
            temp.conn.do_job_set(Trafficlight.setRedYellowGreenState(ID, input));
            Thread.sleep(2000); 
            temp.conn.do_job_set(Trafficlight.setProgram(ID, originProgramID));
        }
        catch (Exception D) {
            System.out.println("Unable to set controlled links of traffic light");
        }
        return false;
    // set phase definition with phase time (Red-Green-Yellow with time)  
    }
        public boolean setPhaseDefWPT(SimulationWrapper temp, String input, double time) {
        try {               //maybe need check??
            long roundedTime = Math.round(time * 200);
            temp.conn.do_job_set(Trafficlight.setRedYellowGreenState(ID, input));
            Thread.sleep(roundedTime); 
            temp.conn.do_job_set(Trafficlight.setProgram(ID, "0"));
        }
        catch (Exception D) {
            System.out.println("Unable to set controlled links of traffic light");
        }
        return false;
    }
    //     public boolean setPhaseDef2(SimulationWrapper temp, String input) {
    //     class TempThread extends Thread {
    //             boolean debugFlag = false;
    //         public void run() {
    //             try {               //maybe need check??
    //                 temp.conn.do_job_set(Trafficlight.setRedYellowGreenState(ID, input));
    //                 Thread.sleep(2000); 
    //                 temp.conn.do_job_set(Trafficlight.setProgram(ID, "0"));
    //                 debugFlag = true;
    //             }
    //             catch (Exception D) {
    //                 System.out.println("Unable to set controlled links of traffic light");
    //             }
    //         }
    //     }
    //     TempThread T = new TempThread();
    //     T.start();
    //     return T.debugFlag;
    // }
//=================STATIC================================
    // update all traffic light IDs of simulation
    public static void updateTrafficLightIDs(SimulationWrapper temp) {
        try {
            @SuppressWarnings("unchecked")
            List<String> IDsList = (List<String>)temp.conn.do_job_get(Trafficlight.getIDList());
            for (String x : IDsList) {
                String program = (String)temp.conn.do_job_get(Trafficlight.getProgram(x));
                TrafficLightWrapper y = new TrafficLightWrapper(x, program);
                temp.TrafficLightList.put(x, y);
                TrafficLightWrapper z = temp.TrafficLightList.get(x);
                //set up subscription for traffic light
                VariableSubscription vs2 = new VariableSubscription(SubscribtionVariable.trafficlight, 0, 100000 * 60, x);
                vs2.addCommand(Constants.TL_RED_YELLOW_GREEN_STATE);
                temp.conn.do_subscription(vs2);
                System.out.println("subscribe " + x);
            }
        }
        catch (Exception A) {
            System.out.println("Set up traffic lights failed.");
        }
    }
}