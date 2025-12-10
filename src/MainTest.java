import wrapper.*;

import java.util.List;

public class MainTest { 
public static void main ( String [] args){ 
        // config_file path is based on this class path
        String config_file = "../resource/test_5_wrapper.sumocfg"; 
        double step_length = 1;
        String sumo_bin = "sumo-gui";
        SimulationWrapper A = new SimulationWrapper(config_file, step_length, sumo_bin);
        try {
            A.Start();
            for (int i = 1; i < 1000; i++) {
                A.Step();
                A.getTime(1);
                System.out.println("list: " +A.getVehicleIDsList());
                System.out.println("listTL: " +A.getTLIDsList());
                // A.getTLPhaseNum("J5");
                // A.getTLPhaseDef("J5");
                // A.getTLControlledLinks("J1");
                // if(i == 11){
                //     //A.setTLPhaseDef2(0,"rrrrrrrrrrrr");
                //     class Test2 extends Thread {
                //         public void run() {
                //             try {
                //                 A.setTLPhaseDef("J1","GGGGGGGGGGGG");
                //                 A.setTLPhaseDefWithPhaseTime("J3","gggggg", 5);
                //                 Thread.sleep(200); 
                //                 A.setTLPhaseDefOrigin("J1");
                //                 A.setTLPhaseNext("J5");
                //             }
                //             catch(Exception a) {System.out.println("work");}
                //         }
                //     }
                //     Test2 hmm = new Test2();
                //     hmm.start();
                // //     A.setDelay(50);
                // }
                // if (i > 10 && i < 30){
                //     System.out.println("test" + i);
                //     A.getVehicleSpeed("f_0.1");
                //     A.getVehiclePosition("f_0.1");
                //     A.getVehicleAngle("f_0.1");
                // }
                // Test Vehicle Stuff
                // List<String> vehID = A.getIDList(); // Get IDs list of all current vehicles in the current simulation
                // if (!vehID.isEmpty()) { // Check if there is at least one vehicle in the simulation
                //     String firstVehID = vehID.get(0); // Choose the first vehicle in the list to test
                //     System.out.println("Testing for the vehicle: " + firstVehID);
                //     A.getTypeID(firstVehID);
                //     A.getColor(firstVehID);
                //     A.getPosition(firstVehID);
                //     A.getSpeed(firstVehID);
                //     A.setSpeed(firstVehID, 36.369);
                //     A.setColor(firstVehID, 255, 0, 0, 255); // red
                //     A.setColor(firstVehID, 0, 255, 0, 255); // green
                //     A.setColor(firstVehID, 0, 0, 255, 255); // blue
                // }
                // else  {
                //     System.out.println("No vehicles found");
                // }
                // System.out.println("-----------------------------------------------");
                // if(i >= 10 && i <= 40 && 1 == 1) {
                //     A.getVehicleColor("f_0.1");
                //     A.getVehicleColor("f_0.0");
                //     A.getVehiclePosition("f_0.1");
                //     A.getVehicleSpeed("f_0.1");
                //     A.getVehicleSpeed("f_0.0");
                // }

                // if(i == 10) {
                //     A.setVehicleColor("f_0.1",255,255,255,255);
                //     A.setVehicleColor("f_0.0",0,255,255,255);
                //     A.setSpeed("f_0.0", 40);
                // }
                if(i == 99) {
                    A.test();
                    // A.addVehicleBasic("x0");
                }
            }
            A.End();
        }
        catch (Exception e) {
            System.out.println("Error in Main");
        }
    }
} 
