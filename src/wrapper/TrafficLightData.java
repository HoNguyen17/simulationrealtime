package wrapper;

import java.util.List;
import java.util.ArrayList;

public class TrafficLightData implements Identifiable {
    protected String ID;
    protected String lightDef;
    protected List<String> fromLaneID;
    protected List<String> toLaneID;
    TrafficLightData(String inputID, List<String> inputFrom, List<String> inputTo) {
        this.ID = inputID;
        this.fromLaneID = inputFrom;
        this.toLaneID = inputTo;
    }
    // get ID
    public String getID(int po) {
        if (po == 1) {System.out.print(" " + ID);}
        return ID;
    }
}