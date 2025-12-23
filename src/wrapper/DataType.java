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
    // not yet implements
    public static Color convertColor(SumoColor inputColor) {
        double tempR = ((double)(inputColor.r & 0xFF))/255;
        double tempG = ((double)(inputColor.g & 0xFF))/255;
        double tempB = ((double)(inputColor.b & 0xFF))/255;
        double tempA = ((double)(inputColor.a & 0xFF))/255;
        Color result =  new Color(tempR, tempG, tempB, tempA);
        return result;
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