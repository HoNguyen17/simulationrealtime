package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Vehicletype;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;

import java.util.List;
import java.util.ArrayList;

class VehicleWrapper {
    String ID;
    SumoColor color;
    double speed;
    SumoPosition2D position; 
    double angle; // current angle (heading) of the vehicle in degrees, updated via subscription
    // constructor
    VehicleWrapper(String inputID, SumoColor inputColor){
        ID = inputID;
        color = inputColor;
        System.out.println("Added vehicle " + inputID + ".");
    }
    //=================GETTER================================
    // get Vehicle's ID
    public String getID(int po) {
        if (po == 1) {System.out.println(ID);}
        return ID;
    }

    // get Vehicle's current position as a SumoPosition2D object
    public SumoPosition2D getPosition(int po) {
        if (po == 1 && position != null) {System.out.println("Position of "+ ID +" is " + position.x + " " + position.y);}
        return position;
    }

    // get Vehicle's current speed
    public double getSpeed(int po) {
        if (po == 1) {System.out.println("Speed of "+ ID +" is " + speed);}
        return speed;
    }

    // get Vehicle's ID list
    public static List<String> getIDList(SimulationWrapper temp, int po) { // the method should be static, because it returns all vehicles, not one
        try {
            @SuppressWarnings("unchecked")
            List<String> idList = (List<String>) temp.conn.do_job_get(Vehicle.getIDList());

            if (po==1) {
                System.out.println(String.format("ID list of all vehicle in the current simulation: %s", idList));
            }
            return idList;
        }

        catch(Exception e) {
            System.out.println("Cannot get vehicle ID list." + e.getMessage());
            return null;
        }
    }

    // get Vehicle's type ID (each type of vehicle gets the different ID)
    public String getTypeID(SimulationWrapper temp, int po) {
        try {
            String typeID = (String) temp.conn.do_job_get(Vehicle.getTypeID(ID));
            if (po==1) {System.out.println(String.format("Type ID of vehicle %s: %s", typeID, ID));}
            return typeID;
        }
        catch(Exception e) {
            System.out.println("Cannot get type ID list of vehicle " + ID + e.getMessage());
        }
        return null;
    }

    // get Vehicle's color as a SumoColor object
    public SumoColor getColor(int po) {
        // SUMO default color (undefined)
        if (color.r == -1 && color.g == -1 && color.b == 0 && color.a == -1 && po == 1) {
            System.out.println("Vehicle " + ID + " has no custom color (using SUMO default which has the format r#g#b#a): " + color);
        }
        else if (po == 1) {System.out.println(String.format("Color of vehicle " + ID + ": " + color));}
        return color;
    }

    // get Vehicle's current angle/heading in degrees
    public double getAngle(int po) {
        if (po == 1) {System.out.println("Vehicle " + ID + " is facing " + angle);}
        return angle;
    }
    //=================SETTER================================
    // set Vehicle's speed -> override the vehicle's internal speed logic until another command is sent or the simulation takes over
    public void setSpeed(SimulationWrapper temp, double inputSpeed, int po) {
        try {
            temp.conn.do_job_set(Vehicle.setSpeed(ID, inputSpeed));
            if  (po==1) {System.out.println(String.format("Set the speed of the vehicle that has the ID %s into %.3f m/s", ID, speed));}
        }
        catch(Exception e) {
            System.out.println("Cannot set the speed of the vehicle that has the ID " + ID + e.getMessage());
        }
    }

    // set Vehicle's color, also update the local (color) variable in the wrapper object
    public void setColor(SimulationWrapper temp, int r, int g, int b, int a) {
        try {
            SumoColor inputColor = new SumoColor(r, g, b, a);
            temp.conn.do_job_set(Vehicle.setColor(ID, inputColor));
            color = inputColor;
        }
        catch(Exception e) {
            System.out.println("Cannot set the color of the vehicle that has the ID " + ID + e.getMessage());
        }
    }
    //=================STATIC================================
    protected static void addVehicle(SimulationWrapper temp, String inputID, String inputRoute) { // injecting a new vehicle into the simulation
        try {
            temp.conn.do_job_set(Vehicle.add(inputID, "DEFAULT_VEHTYPE", inputRoute, 0, 0, 0, (byte)0)); // default vehicle type has the initial departure time, position, and speed = 0
        }
        catch(Exception e){System.out.println("add vehicle fail");}
    }
}