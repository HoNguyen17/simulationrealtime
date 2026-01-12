
package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Route;

import java.util.List;
import java.util.ArrayList;

class RouteWrapper extends DataType.RouteData {
    RouteWrapper(String inputID, String inputEdgeID) {
        super(inputID, inputEdgeID);
    }
    // make copy of the object for MapCanvas
    public DataType.RouteData makeCopy() {
        DataType.RouteData copy = new DataType.RouteData(this.ID, this.firstEdgeID);
        return copy;
    }
    // static method because a "route" in the context of SUMO is a network-level entity, and the purpose of this wrapper is just to manage the list of their IDs, not the state of any single route
    static void updateRouteIDs(SimulationWrapper temp) { // refresh the list of routes available for vehicle injection in the simulation
        try {
            List<String> newRouteList = (List<String>) temp.conn.do_job_get(Route.getIDList());
            for (String x : newRouteList) {
                if (x.charAt(0) != '!') {
                    String firstEdge = ((List<String>) temp.conn.do_job_get(Route.getEdges(x))).get(0);
                    RouteWrapper y = new RouteWrapper(x, firstEdge);
                    temp.RouteList.put(x, y);
                }
            }
        }
        catch(Exception e) {System.out.println("Unable to update route list");}
    }
}
