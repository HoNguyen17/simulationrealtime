package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap; // Cần dùng HashMap để quản lý theo ID

public class SimulationWrapper {
    public SumoTraciConnection conn;
    protected int delay = 200;

    // SỬA: Dùng HashMap thay vì List để lấy đèn bằng tên (ID)
    protected final HashMap<String, TrafficLightWrapper> TrafficLightList = new HashMap<>();

    // Constructor 1
    public SimulationWrapper(String sumocfg, double step_length, String sumo_bin){
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true");
        System.out.println("Simulation created");
    }
    // Constructor 2
    public SimulationWrapper(String sumocfg){
        String sumo_bin = "sumo";
        double step_length = 1;
        conn = new SumoTraciConnection(sumo_bin, sumocfg);
        conn.addOption("step-length", step_length + "");
        conn.addOption("start", "true");
        System.out.println("Simulation created");
    }

    //===== SIMULATION STUFF ==================================
    public void Start(){
        try {
            conn.runServer();
            conn.setOrder(1);
            // Cập nhật danh sách đèn vào HashMap
            TrafficLightWrapper.updateTrafficLightIDs(this);
            System.out.println("Started successfully.");
        }
        catch(Exception e) {System.out.println("Failed to start."); e.printStackTrace();}
    }

    public void Step(){
        try {
            Thread.sleep(delay);
            conn.do_timestep();
        }
        catch(Exception e) {System.out.println("Failed to step.");}
    }

    public void End() {
        conn.close();
    }

    // SỬA: Thêm hàm test() mà MainTest đang gọi
    public void test() {
        System.out.println("Test method called. Traffic Lights count: " + TrafficLightList.size());
    }

    // SỬA: Thêm hàm setDelay
    public void setDelay(int delay) {
        this.delay = delay;
    }

    public double getTime(int po) {
        try {
            double time = (double)conn.do_job_get(Simulation.getTime());
            if (po == 1) {System.out.println("Current Time: " + time);}
            return time;
        }
        catch(Exception e) {System.out.println("Can't get the time.");}
        return -1;
    }

    //===== TRAFFIC LIGHT STUFF ===============================
//===== GETTER ============================================
    public void printTrafficLightList() {
        System.out.println("List of Traffic Light IDs:");
        for (TrafficLightWrapper x : TrafficLightList.values()) {
            x.getID(1);
        }
        System.out.println("");
    }

    // SỬA: Đổi tham số từ int temp -> String id
    public int getTLPhaseNum(String id) {
        if (TrafficLightList.containsKey(id)) {
            TrafficLightWrapper x = TrafficLightList.get(id);
            return x.getPhaseNum(this, 1);
        }
        return -1;
    }

    // SỬA: Đổi tham số từ int temp -> String id
    public String getTLPhaseDef(String id) {
        if (TrafficLightList.containsKey(id)) {
            TrafficLightWrapper x = TrafficLightList.get(id);
            return x.getPhaseDef(this, 1);
        }
        return null;
    }

    // SỬA: Đổi tham số từ int temp -> String id
    public List<String[][]> getTLControlledLinks(String id) {
        if (TrafficLightList.containsKey(id)) {
            TrafficLightWrapper x = TrafficLightList.get(id);
            return x.getControlledLinks(this, 1);
        }
        return null;
    }

    //===== SETTER ============================================
    // SỬA: Đổi tham số int temp -> String id
    public void setTLPhaseDef(String id, String input) {
        if (TrafficLightList.containsKey(id)) {
            TrafficLightWrapper x = TrafficLightList.get(id);
            // Gọi hàm setPhaseDef (không có WPT) cho đúng logic
            x.setPhaseDef(this, input);
        } else {
            System.out.println("Traffic Light ID " + id + " not found!");
        }
    }

    // SỬA: Thêm hàm bị thiếu này
    public void setTLPhaseDefWithPhaseTime(String id, String input, int time) {
        if (TrafficLightList.containsKey(id)) {
            TrafficLightWrapper x = TrafficLightList.get(id);
            x.setPhaseDefWithPhaseTime(this, input, (double)time);
        } else {
            System.out.println("Traffic Light ID " + id + " not found!");
        }
    }

//===== VEHICLE STUFF =====================================
    // Giữ nguyên phần Vehicle như code cũ của bạn (tạm thời)
    // Nếu VehicleWrapper có phương thức static thì code này chạy ổn

    public SumoPosition2D getVehiclePosition(String ID) {
        try {
            return (SumoPosition2D) conn.do_job_get(Vehicle.getPosition(ID));
        } catch (Exception e) { return null; }
    }

    public double getVehicleSpeed(String ID) {
        try {
            return (double) conn.do_job_get(Vehicle.getSpeed(ID));
        } catch (Exception e) { return -1; }
    }

    public List<String> getIDList() {
        try {
            SumoStringList ssl = (SumoStringList) conn.do_job_get(Vehicle.getIDList());
            return ssl;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public String getTypeID(String ID) {
        try {
            return (String) conn.do_job_get(Vehicle.getTypeID(ID));
        } catch (Exception e) { return ""; }
    }

    public SumoColor getColor(String ID) {
        try {
            return (SumoColor) conn.do_job_get(Vehicle.getColor(ID));
        } catch (Exception e) { return null; }
    }

    public void setSpeed(String ID, double speed) {
        try {
            conn.do_job_set(Vehicle.setSpeed(ID, speed));
        } catch (Exception e) {}
    }

    public void setColor(String ID, int r, int b, int g, int a) {
        try {
            conn.do_job_set(Vehicle.setColor(ID, new SumoColor(r, g, b, a)));
        } catch (Exception e) {}
    }
}