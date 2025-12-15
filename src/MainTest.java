import wrapper.*;

import java.util.List;

public class MainTest { 
public static void main ( String [] args){ 
        // config_file path is based on this class path
        String config_file = "../resource/test_2_traffic.sumocfg"; 
        double step_length = 1;
        String sumo_bin = "sumo-gui";
        SimulationWrapper A = new SimulationWrapper(config_file, step_length, sumo_bin);
        try {
            A.Start();
            for (String id : A.getTLIDsList()) {
                System.out.println(id);
            }

            for (int i = 0; i < 100; i++) {
                A.Step();
                A.getTime(1);
                A.getTLPhaseNum("clusterJ10_J7_J8_J9");

                //A.getTLPhaseDef(0);
                //A.getTLControlledLinks(0);
                if(i == 10){
                    //A.setTLPhaseDef2(0,"rrrrrrrrrrrr");
                    class Test2 extends Thread {
                        public void run() {
                            A.setTLPhaseDef(String.valueOf(0),"GGGGGGGGGGGG");
                        }
                    }
                    Test2 hmm = new Test2();
                    hmm.start();
                }
                // Test Vehicle Stuff
                List<String> vehID = A.getVehicleIDsList();
                // Get IDs list of all current vehicles in the current simulation
                if (!vehID.isEmpty()) { // Check if there is at least one vehicle in the simulation
                    String firstVehID = vehID.get(0); // Choose the first vehicle in the list to test
                    System.out.println("Testing for the vehicle: " + firstVehID);

                }
                else  {
                    System.out.println("No vehicles found");
                }
                System.out.println("-----------------------------------------------");
            }
            A.End();
        }
        catch (Exception e) {
            System.out.println("Error in Main");
        }
    }
} 
