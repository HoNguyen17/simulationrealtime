// Code taken from https://github.com/eclipse-sumo/sumo/blob/main/tests/complex/traas/simple/data/Main.java
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Inductionloop;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.objects.SumoVehicleData;
import de.tudresden.sumo.cmd.Vehicletype;

import java.util.List;

public class TestTraaS {
public static void main ( String [] args){ 
String sumo_bin = "sumo";
        String config_file = "../resource/test_2_traffic.sumocfg";
        double step_length = 1;

        if (args.length > 0) {
            sumo_bin = args[0];
        }
        if (args.length > 1) {
            config_file = args[1];
        }

        try {
            SumoTraciConnection conn = new SumoTraciConnection(sumo_bin, config_file);
            conn.addOption("step-length", step_length + "");
            conn.addOption("start", "true"); //start sumo immediately

            //start Traci Server
            conn.runServer();
            conn.setOrder(1);
            List<String> vehicletype = (List<String>)conn.do_job_get(Vehicletype.getIDList());
            System.out.println("Vehicle type: " + vehicletype);

            for (int i = 0; i < 100; i++) {
                Thread.sleep(1000);
                conn.do_timestep();

                // --- Traffic light & simulation info ---
                String indexProgram = (String) conn.do_job_get(Trafficlight.getProgram("J1"));
                double timeSeconds = (double) conn.do_job_get(Simulation.getTime());
                int tlsPhase = (int) conn.do_job_get(Trafficlight.getPhase("J1"));
                double phaseDuration = (double) conn.do_job_get(Trafficlight.getPhaseDuration("J1"));
                int vehicleCount = (int) conn.do_job_get(Trafficlight.getIDCount());
                String lightState = (String) conn.do_job_get(Trafficlight.getRedYellowGreenState("J1"));
                double nextSwitch = (double) conn.do_job_get(Trafficlight.getNextSwitch("J1"));
                String tlsPhaseName = (String) conn.do_job_get(Trafficlight.getPhaseName("J1"));
                int indexPhase = (int) conn.do_job_get(Trafficlight.getPhase("J1"));

                @SuppressWarnings("unchecked")
                List<String> controlledLanes = (List<String>) conn.do_job_get(Trafficlight.getControlledLanes("J1"));

                @SuppressWarnings("unchecked")
                List<Object> controlledLinks = (List<Object>) conn.do_job_get(Trafficlight.getControlledLinks("J1"));

                @SuppressWarnings("unchecked")
                List<String> controlledJunctions = (List<String>) conn.do_job_get(Trafficlight.getControlledJunctions("J1"));

                @SuppressWarnings("unchecked")
                List<String> IDsList = (List<String>) conn.do_job_get(Trafficlight.getIDList());

                // --- Print core information ---
                //System.out.println(String.format("Index of the current program: %s", indexProgram));
                //System.out.println(String.format("Step %.2f, TLS Phase %d (%s): ", timeSeconds, tlsPhase, tlsPhaseName));
                //System.out.println(String.format("Phase %d duration: %.2f seconds", tlsPhase, phaseDuration));
                //System.out.println(String.format("Index of the current phase: %d", indexPhase));
                //System.out.println(String.format("Number of traffic lights in the network: %d", vehicleCount));
                //System.out.println(String.format("Controlled Junctions: %s", controlledJunctions));
                //System.out.println(String.format("IDs of all traffic lights: %s", IDsList));
                //System.out.println(String.format("Next switch at: %.2f seconds", nextSwitch));
                //System.out.println(String.format("Current simulation time: %.2f", timeSeconds));
                // --- Print controlled lanes and their signal states ---
                System.out.println("Controlled Lanes and their Red-Yellow-Green (RYG) states:");
                for (int j = 0; j < controlledLanes.size() && j < lightState.length(); j++) {
                    char stateChar = lightState.charAt(j);
                    String meaning;
                    switch (stateChar) {
                        case 'r': meaning = "RED"; break;
                        case 'y': meaning = "YELLOW"; break;
                        case 'g': meaning = "GREEN"; break;
                        case 'G': meaning = "GREEN (priority)"; break;
                        default:  meaning = "UNKNOWN"; break;
                    }
                    System.out.println(String.format("  Lane %s -> %s", controlledLanes.get(j), meaning));
                }

                // --- Print controlled links for completeness ---
                System.out.println(String.format("Controlled Links: %s", controlledLinks));

                System.out.println("-----------------------------------------------------------------------");

                // Optional: Uncomment to show induction loop data
                // SumoVehicleData vehData = (SumoVehicleData) conn.do_job_get(Inductionloop.getVehicleData("loop1"));
                // for (SumoVehicleData.VehicleData d : vehData.ll) {
                //     System.out.println(String.format("  veh=%s len=%s entry=%s leave=%s type=%s",
                //         d.vehID, d.length, d.entry_time, d.leave_time, d.typeID));
                // }
            }

            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
} 