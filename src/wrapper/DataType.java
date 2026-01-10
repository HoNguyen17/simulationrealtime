package wrapper;

import de.tudresden.sumo.objects.SumoColor;

import java.util.List;
import java.util.ArrayList;

import javafx.scene.paint.Color;

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
        protected double pos_x = Double.NaN, pos_y = Double.NaN;
        protected double angle;
        protected Color color;
        protected boolean valid = true;

        VehicleData(String inputID) {
            this.ID = inputID;
        }
        // get ID
        public String getID(int po) {
            return ID;
        }
        // check vehicle validity
        public boolean getValidity() {
            return valid;
        }
        // get Vehicle's current x position
        public double getPositionX(int po) {
            if (po == 1) {System.out.println("Position x of "+ ID +" is " + pos_x);}
            return pos_x;
        }

        // get Vehicle's current y position
        public double getPositionY(int po) {
            if (po == 1) {System.out.println("Position x of "+ ID +" is " + pos_y);}
            return pos_y;
        }
        // get Vehicle's current angle/heading in degrees
        public double getAngle(int po) {
            if (po == 1) {System.out.println("Vehicle " + ID + " is facing " + angle);}
            return angle;
        }
        // get Vehicle's current speed
        public double getSpeed(int po) {
            if (po == 1) {System.out.println("Speed of "+ ID +" is " + speed);}
            return speed;
        }
        // get Vehicle's color
        public Color getColor(int po) {
            if (po == 1) {System.out.println("Color of "+ ID +" is " + color);}
            return color;
        }
    }
    public static class RouteData implements Identifiable {
        protected String ID;
        protected String firstEdgeID;
        RouteData(String inputID, String inputEdgeID) {
            this.ID = inputID;
            this.firstEdgeID = inputEdgeID;
        } 
        //
        public String getID(int po) {
            if(po == 1){System.out.println(ID);}
            return ID;
        }
        // get first edge ID of the route
        public String getFirstEdgeID(int po) {
            if (po == 1) {System.out.println("First edge " + this.firstEdgeID);}
            return firstEdgeID;
        }
    }
    public static class EdgeData implements Identifiable {
        protected String ID; 
        protected int density;
        protected double travelTime;
        protected double waitingTime;
        EdgeData(String inputID) {
            this.ID = inputID;
        }
        public String getID(int po) {
            if (po == 1) {System.out.println(ID);}
            return ID;
        }
        public int getDensity(int po) {
            if (po == 1) {System.out.println(ID);}
            return density;
        }
        public double getTravelTime(int po) {
            if (po == 1) {System.out.println(ID);}
            return travelTime;
        }
        public double getwaitingTime(int po) {
            if (po == 1) {System.out.println(ID);}
            return waitingTime;
        }
    }
    // color converts
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
}