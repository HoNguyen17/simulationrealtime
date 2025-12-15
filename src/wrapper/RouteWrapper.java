package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Route;

import java.util.List;
import java.util.ArrayList;

// This small helper class solely focused on retrieving and managing the list of available traffic routes within a SUMO simulation
class RouteWrapper {
    // static method because a "route" in the context of SUMO is a network-level entity, and the purpose of this wrapper is just to manage the list of their IDs, not the state of any single route
    static void updateRouteIDs(SimulationWrapper temp) { // refresh the list of routes available for vehicle injection in the simulation
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