package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Vehicletype;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;

import javafx.scene.paint.Color;

import java.util.List;
import java.util.ArrayList;

// logging
import java.util.logging.Logger;
import java.util.logging.Level;

class VehicleWrapper extends DataType.VehicleData {
    // logging
    private final static Logger LOG = Logger.getLogger(VehicleWrapper.class.getName());

    // constructor
    VehicleWrapper(String inputID, Color inputColor){
        super(inputID);
        color = inputColor;
        LOG.log(Level.INFO, "Add vehicle: {0}", inputID);
    }
    //=================GETTER================================
    // get Vehicle's ID list
    public static List<String> getIDList(SimulationWrapper temp, int po) { // the method should be static, because it returns all vehicles, not one
        try {
            @SuppressWarnings("unchecked")
            List<String> idList = (List<String>) temp.conn.do_job_get(Vehicle.getIDList());
            if (po==1) {LOG.log(Level.INFO, "ID list of all vehicle in the current simulation: {0}", idList);}
            return idList;
        }
        catch(Exception e) {
            LOG.log(Level.SEVERE, "Error while getting vehicle ids from the simulation. ", e);
            return null;
        }
    }

    // get Vehicle's type ID (each type of vehicle gets the different ID)
    public String getTypeID(SimulationWrapper temp, int po) {
        try {
            String typeID = (String) temp.conn.do_job_get(Vehicle.getTypeID(ID));
            if (po==1) {LOG.log(Level.INFO, "Type ID of vehicle {0}: {1}", new Object[]{ID, typeID});}
            return typeID;
        }
        catch(Exception e) {
            LOG.log(Level.SEVERE, "Cannot get type ID list of vehicle {0}", new Object[]{ID,e});
        }
        return null;
    }

    // get Vehicle's color as a SumoColor object
    // public SumoColor getColor(int po) {
    //     // SUMO default color (undefined)
    //     if (color.r == -1 && color.g == -1 && color.b == 0 && color.a == -1 && po == 1) {
    //         System.out.println("Vehicle " + ID + " has no custom color (using SUMO default which has the format r#g#b#a): " + color);
    //     }
    //     else if (po == 1) {System.out.println(String.format("Color of vehicle " + ID + ": " + color));}
    //     return color;
    // }
    //=================MAKE COPY=============================
    public DataType.VehicleData makeCopy() {
        DataType.VehicleData copy = new DataType.VehicleData(ID);
        copy.speed = this.speed;
        copy.pos_x = this.pos_x;
        copy.pos_y = this.pos_y;
        copy.angle = this.angle;
        copy.color = this.color;
        return copy;
    }
    //=================SETTER================================
    // set Vehicle's speed -> override the vehicle's internal speed logic until another command is sent or the simulation takes over
    public void setSpeed(SimulationWrapper temp, double inputSpeed, int po) {
        try {
            temp.conn.do_job_set(Vehicle.setSpeed(ID, inputSpeed));
            if  (po==1) {LOG.log(Level.INFO, "Set the speed of vehicle that has the ID {0} into {1}", new Object[]{ID, inputSpeed});}
        }
        catch(Exception e) {
            LOG.log(Level.SEVERE, "Cannot set the speed of the vehicle that has the ID {0}", new Object[]{ID,e});
        }
    }

    // set Vehicle's color, also update the local (color) variable in the wrapper object
    public void setColor(SimulationWrapper temp, double r, double g, double b, double a) {
        try {
            Color dataColor = new Color(r, g, b, a);
            this.color = dataColor;
            SumoColor inputColor = DataType.convertColor(dataColor);
            temp.conn.do_job_set(Vehicle.setColor(ID, inputColor));
            LOG.log(Level.INFO, "Set the color for vehicle with ID {0} into {1}", new Object[]{ID, inputColor});
            
            
        }
        catch(Exception e) {
            LOG.log(Level.SEVERE, "Cannot set the color of the vehicle that has the ID {0}", new Object[]{ID,e});
        }
    }
    public void set_test() {
        LOG.log(Level.INFO, "Still work");
    }
    //=================STATIC================================
    // injecting a new vehicle into the simulation
    protected static void addVehicle(SimulationWrapper temp, String inputID, String inputRoute) { 
        try {
            temp.conn.do_job_set(Vehicle.add(inputID, "DEFAULT_VEHTYPE", inputRoute, 0, 0, 0, (byte)0)); // default vehicle type has the initial departure time, position, and speed = 0
            LOG.log(Level.INFO, "Vehicle {0} injected into route {1}", new Object[]{inputID, inputRoute});
        }
        catch (Exception e) {LOG.log(Level.SEVERE, "Failed to add vehicle {0} to simulation", new Object[]{inputID, e});}
    }
}