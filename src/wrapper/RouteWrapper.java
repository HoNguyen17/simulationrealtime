package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Route;

import java.util.List;
import java.util.ArrayList;

class RouteWrapper {
    static void updateRouteIDs(SimulationWrapper temp) {
        try {
            List<String> newRouteList = (List<String>) temp.conn.do_job_get(Route.getIDList());
            List<String> validRoute = new ArrayList<String>();
            for (String x : newRouteList) {
                if (x.charAt(0) != '!') {validRoute.add(x);}
            }
            temp.RouteList = validRoute;
        }
        catch(Exception e) {System.out.println("Unable to update route list");}
    }
}