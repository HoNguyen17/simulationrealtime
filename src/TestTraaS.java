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
String sumo_bin = "sumo-gui";
        String config_file = "../resource/test_5_wrapper.sumocfg";
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

            for (int i = 0; i < 1000; i++) {
                Thread.sleep(200);
                conn.do_timestep();
                List<String> temp = (List<String>)conn.do_job_get(Trafficlight.getIDList());
                System.out.println(temp);
                if(i == 1){
                    conn.do_job_set(Vehicle.add("x_0", "DEFAULT_VEHTYPE", "r_0", 0, 0, 0, (byte)0));
                }
            }

            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
} 