package wrapper;

import de.tudresden.sumo.objects.SumoColor;

import java.util.List;
import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.scene.paint.Color;

public class DataType {
    private final static Logger LOG = Logger.getLogger(DataType.class.getName());
    public static String[] colorOptions = {"Default", "Red", "Orange",  "Yellow", "Green", "Blue", "Indigo", "Violet", "Pink"};
    
    public static Color convertColor(SumoColor inputColor) {
        double tempR = ((double)(inputColor.r & 0xFF))/255;
        double tempG = ((double)(inputColor.g & 0xFF))/255;
        double tempB = ((double)(inputColor.b & 0xFF))/255;
        double tempA = ((double)(inputColor.a & 0xFF))/255;
        Color result =  new Color(tempR, tempG, tempB, tempA);
        return result;
    }
    public static SumoColor convertColor(Color inputColor) {
        int tempR = (int) (inputColor.getRed() * 255);
        int tempG = (int) (inputColor.getGreen() * 255);
        int tempB = (int) (inputColor.getBlue() * 255);
        int tempA = (int) (inputColor.getOpacity() * 255);
        SumoColor result =  new SumoColor(tempR, tempG, tempB, tempA);
        return result;
    }
    public static Color convertColor(String inputColor) {
        switch (inputColor) {
            case "Red":
                return Color.RED;
            case "Orange":
                return Color.ORANGE;
            case "Yellow":
                return Color.YELLOW;
            case "Green":
                return Color.GREEN;
            case "Blue":
                return Color.BLUE;
            case "Indigo":
                return Color.INDIGO;
            case "Violet":
                return Color.VIOLET;
            case "Pink":
                return Color.PINK;
        }
        return Color.RED;
    }
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
        public String getID(int po) {
            if (po == 1) {LOG.info(" " + ID);}
            return ID;
        }
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
    // basic vehicle data class
    public static class VehicleData implements Identifiable {
        protected String ID;
        protected double speed;
        protected double pos_x = Double.NaN, pos_y = Double.NaN;
        protected double angle;
        protected Color color;

        VehicleData(String inputID) {
            this.ID = inputID;
        }
        public String getID(int po) {
            return ID;
        }
        public double getPositionX(int po) {
            if (po == 1) {LOG.info("Position x of "+ ID +" is " + pos_x);}
            return pos_x;
        }
        public double getPositionY(int po) {
            if (po == 1) {LOG.info("Position x of "+ ID +" is " + pos_y);}
            return pos_y;
        }
        public double getAngle(int po) {
            if (po == 1) {LOG.info("Vehicle " + ID + " is facing " + angle);}
            return angle;
        }
        public double getSpeed(int po) {
            if (po == 1) {LOG.info("Speed of "+ ID +" is " + speed);}
            return speed;
        }
        public Color getColor(int po) {
            if (po == 1) {LOG.info("Color of "+ ID +" is " + color);}
            return color;
        }
    }
    // basic route data class
    public static class RouteData implements Identifiable {
        protected String ID;
        protected String firstEdgeID;
        RouteData(String inputID, String inputEdgeID) {
            this.ID = inputID;
            this.firstEdgeID = inputEdgeID;
        } 
        public String getID(int po) {
            if(po == 1){LOG.info(ID);}
            return ID;
        }
        public String getFirstEdgeID(int po) {
            if (po == 1) {LOG.info("First edge " + this.firstEdgeID);}
            return firstEdgeID;
        }
    }
    // basic edge data class
    public static class EdgeData implements Identifiable {
        protected String ID; 
        protected int density;
        protected double travelTime;
        protected double waitingTime;
        protected boolean congested = false;
        EdgeData(String inputID) {
            this.ID = inputID;
        }
        public String getID(int po) {
            if (po == 1) {LOG.info(ID);}
            return ID;
        }
        public int getDensity(int po) {
            if (po == 1) {LOG.info(ID);}
            return density;
        }
        public double getTravelTime(int po) {
            if (po == 1) {LOG.info(ID);}
            return travelTime;
        }
        public double getwaitingTime(int po) {
            if (po == 1) {LOG.info(ID);}
            return waitingTime;
        }
    }
}