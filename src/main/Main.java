import wrapper.SimulationWrapper;
import wrapper.TrafficLightWrapper;
public class Main { 
public static void main ( String [] args){ 
        String config_file = "../../resource/test_2_traffic.sumocfg";
        double step_length = 1;
        SimulationWrapper A = new SimulationWrapper();
        A.Test();
        A.End();
    }
} 
