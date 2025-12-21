package wrapper;

public class VehicleData implements Identifiable {
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