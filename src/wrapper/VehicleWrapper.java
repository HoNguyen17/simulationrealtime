package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Vehicletype;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;


// import javax.swing.text.Position;
import java.util.List;
import java.util.ArrayList;

public class VehicleWrapper {
    String ID;
    SumoColor color;
    double speed;               
    SumoPosition2D position;
    // constructor
    VehicleWrapper(String inputID, SumoColor inputColor){
        ID = inputID;
        color = inputColor;
        System.out.println("Added vehicle " + inputID + ".");
    }

    // get Vehicle ID
    public String getID(int po) {
        if (po == 1) {System.out.println(ID);}
        return ID;
    }

    // get Vehicle position
    public SumoPosition2D getPosition(wrapper.SimulationWrapper temp, int po) {
        if (po == 1) {System.out.println("Position of "+ ID +" is " + position.x + " " + position.y);}
        return position;
    }

    // get Vehicle speed
    public double getSpeed(SimulationWrapper temp, int po) {
        if (po == 1) {System.out.println("Speed of "+ ID +" is " + speed);}
        return speed;
    }

    // get Vehicle's ID list
    public static List<String> getIDList(wrapper.SimulationWrapper temp, int po) { // the method should be static, because it returns all vehicles, not one.
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

    // get Vehicle's type ID
    public String getTypeID(wrapper.SimulationWrapper temp, int po) {
        try {
            String typeID = (String) temp.conn.do_job_get(Vehicle.getTypeID(ID));

            if (po==1) {
                System.out.println(String.format("Type ID of vehicle %s: %s", typeID, ID));
            }
            return typeID;
        }

        catch(Exception e) {
            System.out.println("Cannot get type ID list of vehicle " + ID + e.getMessage());
            return null;
        }
    }

    // get Vehicle's color
    public SumoColor getColor(wrapper.SimulationWrapper temp, int po) {
        try {
            SumoColor color = (SumoColor) temp.conn.do_job_get(Vehicle.getColor(ID));

            // SUMO default color (undefined)
            if (color.r == -1 && color.g == -1 && color.b == 0 && color.a == -1) {
                if (po == 1) {
                    System.out.println("Vehicle " + ID + " has no custom color (using SUMO default which has the format r#g#b#a): " + color);
                }
                return color;
            }

            if (po==1) {
                System.out.println(String.format("Color of vehicle " + ID + ": " + color));
            }
            return color;
        }

        catch (Exception e) {
            System.out.println("Cannot get color of vehicle " + ID + e.getMessage());
            return null;
        }
    }

    // set Vehicle's speed
    public void setSpeed(wrapper.SimulationWrapper temp, double speed, int po) {
        try {
            temp.conn.do_job_set(Vehicle.setSpeed(ID, speed));

            if  (po==1) {
                System.out.println(String.format("Set the speed of the vehicle that has the ID %s into %.3f m/s", ID, speed));
            }
        }

        catch(Exception e) {
            System.out.println("Cannot set the speed of the vehicle that has the ID " + ID + e.getMessage());
        }
    }

    // set Vehicle's color
    public void setColor(wrapper.SimulationWrapper temp, int r, int g, int b, int a, int po) {
        try {
            SumoColor color = new SumoColor(r, g, b, a);
            temp.conn.do_job_set(Vehicle.setColor(ID, color));
            if  (po==1) {
                System.out.println(String.format("Set the color of the vehicle %s into SumoColor format (RGBA format): %d %d %d %d", ID, color.r, color.g, color.b, color.a));
            }
        }

        catch(Exception e) {
            System.out.println("Cannot set the color of the vehicle that has the ID " + ID + e.getMessage());
        }
    }
}
