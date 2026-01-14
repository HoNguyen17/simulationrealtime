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

// logging
import java.util.logging.Logger;
import java.util.logging.Level;

class TrafficLightWrapper extends DataType.TrafficLightData { 
    String originProgramID;

    private final static Logger LOG = Logger.getLogger(TrafficLightWrapper.class.getName());
    // constructor
    TrafficLightWrapper(String inputID, String startProgram, List<String> inputFrom, List<String> inputTo){
        super(inputID, inputFrom, inputTo);
        this.originProgramID = startProgram;
        LOG.log(Level.INFO, "Added {0} with program {1}", new Object[]{ID, originProgramID});
    }
//=================GETTER================================
    // get the current phase index (number) of the traffic light's program from SUMO
    public int getPhaseNum(SimulationWrapper temp, int po) {
        try {
            int tlsPhase = (int)temp.conn.do_job_get(Trafficlight.getPhase(ID));
            if (po == 1) {LOG.log(Level.INFO, "tlsPhase of {0}: {1}", new Object[]{ID, tlsPhase});}
            return tlsPhase;
        }
        catch(Exception A) {
            LOG.log(Level.WARNING, "Failed to get phase number.", A);
        }
        return -1;
    }
    // get phase definition (Red-Green-Yellow) stored in the wrapper object, updated asynchronously via subscription
    public String getPhaseDef(int po) {
        if (po == 1) {LOG.log(Level.INFO, "Current phase definition of {0}: {1}", new Object[]{ID, lightDef});}
        return lightDef;
    }
    // get controlled junctions of traffic light (usually 1)
    public List<String> getControlledJunctions(SimulationWrapper temp, int po) {
        try {
            List<String> junctions = (List<String>)temp.conn.do_job_get(Trafficlight.getControlledJunctions(ID));
            return junctions;
        }   
        catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to get controlled junctions.", e);
        }
        return null;
    }
//=================MAKE COPY=============================
    public DataType.TrafficLightData makeCopy() {
        List<String> fromLaneIDCopy = new ArrayList<>();
        List<String> toLaneIDCopy = new ArrayList<>();
        for (int i = 0; i < this.controlledLinksNum; i++) {
            fromLaneIDCopy.add(fromLaneID.get(i));
            toLaneIDCopy.add(toLaneID.get(i));
        }
        DataType.TrafficLightData copy = new DataType.TrafficLightData(ID, fromLaneIDCopy, toLaneIDCopy);
        copy.lightDef = this.lightDef;
        return copy;
    }
//=================SETTER================================
    // set phase definition to origin (auto)
    public boolean setPhaseDefOrigin(SimulationWrapper temp) {
        try {
            temp.conn.do_job_set(Trafficlight.setProgram(ID, originProgramID));
            return true;
        }
        catch (Exception F) {
            LOG.log(Level.WARNING, "Unable to set light definition back to auto", F);
        }
        return false;
    }
    // set the traffic light program to the next phase
    public boolean setPhaseNext(SimulationWrapper temp) {
        try {
            String program = (String)temp.conn.do_job_get(Trafficlight.getProgram(ID));
            SumoTLSController TLController = (SumoTLSController) temp.conn.do_job_get(Trafficlight.getCompleteRedYellowGreenDefinition(ID));
            int phaseNumLimit = TLController.programs.get(program).phases.size();
            int currentPhaseNum = (int)temp.conn.do_job_get(Trafficlight.getPhase(ID));
            if (currentPhaseNum < phaseNumLimit - 1) {temp.conn.do_job_set(Trafficlight.setPhase(ID, currentPhaseNum + 1));}
            else {temp.conn.do_job_set(Trafficlight.setPhase(ID, 0));}
            return true;
        }
        catch (Exception G) {
            LOG.log(Level.WARNING, "Unable to set to next phase", G);
        }
        return false;
    }
    // set current phase duration
    public boolean setPhaseDuration(SimulationWrapper temp, double inputTime) {
        try {
            temp.conn.do_job_set(Trafficlight.setPhaseDuration(ID, inputTime));
            return true;
        }
        catch (Exception E) {
            LOG.log(Level.WARNING, "Unable to set duration for {0}", new Object[]{ID, E});
            return false;
        }
    }
//=================STATIC================================
    // update all traffic light IDs of simulation
    protected static void updateTrafficLightIDs(SimulationWrapper temp) {
        try {
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
                // create a new TrafficLightWrapper object with the gathered data and add to the temp.TrafficLightList HashMap in the SimulationWrapper
                TrafficLightWrapper y = new TrafficLightWrapper(x, program, inputFrom, inputTo);
                temp.TrafficLightList.put(x, y);

                // set up subscription for traffic light
                VariableSubscription vs = new VariableSubscription(SubscribtionVariable.trafficlight, 0, 100000 * 60, x); // initiates a variable subscription for each traffic light ID
                vs.addCommand(Constants.TL_RED_YELLOW_GREEN_STATE);
                temp.conn.do_subscription(vs);
            }
        }
        catch (Exception A) {
            LOG.log(Level.SEVERE, "Set up traffic lights failed.", A);
        }
    }
}