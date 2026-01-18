package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Vehicletype;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoColor;

import javafx.scene.paint.Color;

import java.util.List;
import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

class VehicleWrapper extends DataType.VehicleData {
    private final static Logger LOG = Logger.getLogger(VehicleWrapper.class.getName());
    protected double departTime = Double.NaN;

    VehicleWrapper(String inputID, Color inputColor){
        super(inputID);
        color = inputColor;
    }
    //=================ADDER=================================
    protected static void addVehicle(SimulationWrapper temp, String inputID, String inputRoute) { 
        try {
            temp.conn.do_job_set(Vehicle.add(inputID, "DEFAULT_VEHTYPE", inputRoute, 0, 0, 0, (byte)0)); 
        }
        catch (Exception e) {
            LOG.log(Level.WARNING, "Add vehicle failed.", e);
        }
    }
    //=================GETTER================================
    public static List<String> getIDList(SimulationWrapper temp, int po) { 
        try {
            List<String> idList = (List<String>) temp.conn.do_job_get(Vehicle.getIDList());
            if (po == 1) {LOG.info("ID list of all vehicle in the current simulation: %s" + idList);}
            return idList;
        }
        catch(Exception e) {
            LOG.log(Level.SEVERE, "Cannot get vehicle ID list.", e);
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
            if (po == 1) {LOG.info("Set the speed of the vehicle " + ID +" to " + speed);}
        }
        catch(Exception e) {
            LOG.log(Level.WARNING, "Cannot set the speed of the vehicle" + ID, e);
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
            LOG.log(Level.WARNING, "Cannot set the color of the vehicle" + ID, e);
        }
    }
}