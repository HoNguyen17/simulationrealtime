package wrapper;

import de.tudresden.sumo.objects.SumoColor;

import java.util.List;
import java.util.ArrayList;

public class DataType {
    // interface for object with id
    public static interface Identifiable {
        public String getID(int po);
    }
    // basic traffic light data class
    public static class TrafficLightData implements Identifiable {
        protected String ID;
        protected String lightDef;
        protected List<String> fromLaneID;
        protected List<String> toLaneID;
        protected int controlledLinksNum;
        TrafficLightData(String inputID, List<String> inputFrom, List<String> inputTo) {
            this.ID = inputID;
            this.fromLaneID = inputFrom;
            this.toLaneID = inputTo;
            this.controlledLinksNum = inputFrom.size();
        }
        // get ID
        public String getID(int po) {
            if (po == 1) {System.out.print(" " + ID);}
            return ID;
        }
        // get the number of controlled links
        public int getControlledLinksNum() {
            return controlledLinksNum;
        }
        // get a list containing the light state, the (from) edge ID, and the (to) edge ID for a specific controlled link index
        public List<String> getDefFromTo(int index) {
            if (lightDef == null) {return null;}
            List<String> result = new ArrayList<String>();
            result.add("" + lightDef.charAt(index));
            result.add(fromLaneID.get(index));
            result.add(toLaneID.get(index));
            return result;
        }
    }
    //
    public static class VehicleData implements Identifiable {
        protected String ID;
        protected double speed;
        protected double x, y;
        protected double angle;
        VehicleData(String inputID) {
            this.ID = inputID;
        }
        // get ID
        public String getID(int po) {
            return ID;
        }
    }
    // not yet implements
    public static void convertColor(SumoColor inputColor) {
        double tempR = ((double)(inputColor.r & 0xFF))/255;
        double tempG = ((double)(inputColor.g & 0xFF))/255;
        double tempB = ((double)(inputColor.b & 0xFF))/255;
        double tempA = ((double)(inputColor.a & 0xFF))/255;

    }
    // should do polymorphism, convert to javafx equivalent
    public static void convertColor(int inputColor) {
        // double tempR = ((double)(inputColor.r & 0xFF))/255;
        // double tempG = ((double)(inputColor.g & 0xFF))/255;
        // double tempB = ((double)(inputColor.b & 0xFF))/255;
        // double tempA = ((double)(inputColor.a & 0xFF))/255;
        System.out.println("placeholder");
    }
}