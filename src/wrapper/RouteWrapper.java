package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Route;

import java.util.List;
import java.util.ArrayList;

class RouteWrapper {
    static void updateRouteIDs(SimulationWrapper temp) {
        try {
            List<String> newRouteList = (List<String>) temp.conn.do_job_get(Route.getIDList());
            temp.RouteList = newRouteList;
        }
        catch(Exception e) {System.out.println("Unable to update route list");}
    }
}