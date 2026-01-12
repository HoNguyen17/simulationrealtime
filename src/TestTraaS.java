// Code taken from https://github.com/eclipse-sumo/sumo/blob/main/tests/complex/traas/simple/data/Main.java
import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Inductionloop;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.objects.SumoVehicleData;
import de.tudresden.sumo.cmd.Vehicletype;
import de.tudresden.sumo.util.SumoCommand;
import java.util.List;

public class TestTraaS {
    public static void main ( String [] args){
        String sumo_bin = "sumo-gui";
        String config_file = "../resource/test_7_huge.sumocfg";
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
                double tempTime = (double)conn.do_job_get(Simulation.getTime());
                System.out.println(tempTime);
                List<String> time = (List<String>)conn.do_job_get(Trafficlight.getControlledJunctions("J28"));
                if(i == 2) conn.do_job_set(Trafficlight.setPhaseDuration("J28", 7));
                System.out.println(time);
                if(0 == 1){
                    conn.do_job_set(Vehicle.add("x_0", "DEFAULT_VEHTYPE", "r_0", 0, 0, 0, (byte)0));
                    conn.do_job_set(Vehicle.changeTarget("x_0", "E0"));
                    conn.do_job_set(Vehicle.rerouteEffort("f_0.0"));
                    conn.do_job_set(Vehicle.rerouteEffort("f_1.0"));
                    //conn.do_job_set(Vehicle.rerouteEffort("x_0"));

                    conn.do_job_set(Vehicle.add("i2d", "DEFAULT_VEHTYPE", "r_0", 0, 0, 0, (byte)0));
                    //conn.do_job_set(Vehicle.changeTarget("i2d", "E0"));
                    conn.do_job_set(Vehicle.rerouteEffort("i2d"));
                    conn.do_job_set(Vehicle.rerouteEffort("i2d"));
                    conn.do_job_set(Vehicle.add("idx", "DEFAULT_VEHTYPE", "r_0", 0, 0, 0, (byte)0));
                    //conn.do_job_set(Vehicle.changeTarget("idx", "E0"));
                    conn.do_job_set(Vehicle.add("idk", "DEFAULT_VEHTYPE", "r_0", 0, 0, 0, (byte)0));
                    conn.do_job_set(Vehicle.changeTarget("idk", "E0"));
                    conn.do_job_set(Vehicle.add("idz", "DEFAULT_VEHTYPE", "r_0", 0, 0, 0, (byte)0));
                    conn.do_job_set(Vehicle.changeTarget("idz", "E0"));
                    //String test = conn.do_job_get(Vehicle.getRoutingMode("x_0"));
                    //System.out.println(test);
                }
                if(i > 0){
                    double testTime = (double)conn.do_job_get(Edge.getTraveltime("-E18"));
                    System.out.println(testTime);
                }
            }

            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
} 