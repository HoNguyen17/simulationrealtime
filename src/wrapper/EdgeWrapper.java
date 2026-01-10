package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Edge;

import de.tudresden.sumo.config.Constants;

import de.tudresden.sumo.subscription.VariableSubscription;
import de.tudresden.sumo.subscription.SubscribtionVariable;
import de.tudresden.sumo.subscription.SubscriptionObject;
import de.tudresden.sumo.subscription.ResponseType;

import java.util.List;
import java.util.ArrayList;

class EdgeWrapper extends DataType.EdgeData {
    EdgeWrapper(String inputID) {
        super(inputID);
    }
//=================STATIC================================VAR_EDGE_TRAVELTIME, LAST_STEP_VEHICLE_NUMBER, VAR_WAITING_TIME
    public static void updateEdgeIDs(SimulationWrapper temp) {
        try {
            List<String> IDsList = (List<String>)temp.conn.do_job_get(Edge.getIDList());
            for (String x : IDsList) {
                EdgeWrapper y = new EdgeWrapper(x);
                temp.EdgeList.put(x, y);
                VariableSubscription vs = new VariableSubscription(SubscribtionVariable.edge, 0, 100000 * 60, x); 
                vs.addCommand(Constants.LAST_STEP_VEHICLE_NUMBER);
                vs.addCommand(Constants.VAR_CURRENT_TRAVELTIME);
                vs.addCommand(Constants.VAR_WAITING_TIME);
                temp.conn.do_subscription(vs);
                System.out.println("subscribe " + x);
            }
        }
        catch (Exception A) {
            System.out.println("Set up edge data failed.");
        }
    }
}