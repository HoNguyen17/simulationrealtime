package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Vehicletype;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;

import javafx.scene.paint.Color;

import java.util.List;
import java.util.ArrayList;

class VehicleWrapper extends DataType.VehicleData {

    VehicleWrapper(String inputID, Color inputColor){
        super(inputID);
        color = inputColor;
        System.out.println("Added vehicle " + inputID + ".");
    }
    //=================ADDER=================================
    protected static void addVehicle(SimulationWrapper temp, String inputID, String inputRoute) { 
        try {
            temp.conn.do_job_set(Vehicle.add(inputID, "DEFAULT_VEHTYPE", inputRoute, 0, 0, 0, (byte)0)); // default vehicle type has the initial departure time, position, and speed = 0
        }
        catch (Exception e) {System.out.println("add vehicle fail");}
    }
    //=================GETTER================================
    public static List<String> getIDList(SimulationWrapper temp, int po) { // the method should be static, because it returns all vehicles, not one
        try {
            List<String> idList = (List<String>) temp.conn.do_job_get(Vehicle.getIDList());
            if (po==1) {System.out.println(String.format("ID list of all vehicle in the current simulation: %s", idList));}
            return idList;
        }
        catch(Exception e) {
            System.out.println("Cannot get vehicle ID list." + e.getMessage());
            return null;
        }
    }
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
    public void setSpeed(SimulationWrapper temp, double inputSpeed, int po) {
        try {
            temp.conn.do_job_set(Vehicle.setSpeed(ID, inputSpeed));
            if (po==1) System.out.println(String.format("Set the speed of the vehicle that has the ID %s into %.3f m/s", ID, speed));
        }
        catch(Exception e) {
            System.out.println("Cannot set the speed of the vehicle that has the ID " + ID + e.getMessage());
        }
    }
    public void setColor(SimulationWrapper temp, double r, double g, double b, double a) {
        try {
            Color dataColor = new Color(r, g, b, a);
            this.color = dataColor;
            SumoColor inputColor = DataType.convertColor(dataColor);
            temp.conn.do_job_set(Vehicle.setColor(ID, inputColor));    
        }
        catch(Exception e) {
            System.out.println("Cannot set the color of the vehicle that has the ID " + ID + e.getMessage());
        }
    }
}