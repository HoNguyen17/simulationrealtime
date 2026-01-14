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

// logging
import java.util.logging.Logger;
import java.util.logging.Level;

class EdgeWrapper extends DataType.EdgeData {
    // logging
    private final static Logger LOG = Logger.getLogger(EdgeWrapper.class.getName());
    EdgeWrapper(String inputID) {
        super(inputID);
        // Log individual edge wrapping
        LOG.log(Level.INFO, "Edge wrapper initialized for ID: {0}", inputID);
    }
//=================STATIC================================VAR_EDGE_TRAVELTIME, LAST_STEP_VEHICLE_NUMBER, VAR_WAITING_TIME
    public static void updateEdgeIDs(SimulationWrapper temp) {
        try {
            List<String> IDsList = (List<String>)temp.conn.do_job_get(Edge.getIDList());
            int edgeCount = 0;
            for (String x : IDsList) {
                EdgeWrapper y = new EdgeWrapper(x);
                temp.EdgeList.put(x, y);
                VariableSubscription vs = new VariableSubscription(SubscribtionVariable.edge, 0, 100000 * 60, x); 
                vs.addCommand(Constants.LAST_STEP_VEHICLE_NUMBER);
                vs.addCommand(Constants.VAR_CURRENT_TRAVELTIME);
                vs.addCommand(Constants.VAR_WAITING_TIME);
                temp.conn.do_subscription(vs);
                edgeCount++;
            }
            LOG.log(Level.INFO, "Edge list updated. Subscribed to {0} edges.", edgeCount);
        }
        catch (Exception A) {
            LOG.log(Level.SEVERE, "Cannot update the edge IDs", A);
        }
    }
}