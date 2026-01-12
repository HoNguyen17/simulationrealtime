package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Route;

import java.util.List;
import java.util.ArrayList;

class RouteWrapper extends DataType.RouteData {

    RouteWrapper(String inputID, String inputEdgeID) {
        super(inputID, inputEdgeID);
    }
    // update RouteList of SimulationWrapper
    static void updateRouteIDs(SimulationWrapper temp) {
        try {
            List<String> newRouteList = (List<String>) temp.conn.do_job_get(Route.getIDList());
            for (String id : newRouteList) {
                if (id.charAt(0) != '!') {
                    String firstEdge = ((List<String>) temp.conn.do_job_get(Route.getEdges(id))).get(0);
                    RouteWrapper route = new RouteWrapper(id, firstEdge);
                    temp.RouteList.put(id, route);
                }  
            }
        }
        catch(Exception e) {System.out.println("Unable to update route list");}
    }
    // make copy of the object for rendering in MapCanvas
    public DataType.RouteData makeCopy() {
        DataType.RouteData copy = new DataType.RouteData(this.ID, this.firstEdgeID);
        return copy;
    }
}