
import wrapper.SimulationWrapper;

public class Main {
    public static void main ( String [] args){
        String config = "../resource/test_2_traffic.sumocfg";
        double step = 1.0;
        String sumo_bin = "sumo-gui";

        SimulationWrapper A = new SimulationWrapper(config, step, sumo_bin);


        A.End();
    }
} 
