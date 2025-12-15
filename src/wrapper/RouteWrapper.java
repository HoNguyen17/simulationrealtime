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
            List<String> newRouteList = (List<String>) temp.conn.do_job_get(Route.getIDList()); // retrieve a list of all route IDs defined in the loaded network configuration
            List<String> validRoute = new ArrayList<String>();
            // filter invalid routes, because SUMO may return temporary or internal route IDs like those beginning with !
            for (String x : newRouteList) {
                if (x.charAt(0) != '!') {validRoute.add(x);}
            }
            temp.RouteList = newRouteList; // assign the filtered list of route IDs to the RouteList field of the provided SimulationWrapper instance
        }
        catch(Exception e) {System.out.println("Unable to update route list");}
    }
}