package wrapper;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Trafficlight;

import java.util.List;
import java.util.ArrayList;

public class TrafficLightWrapper {
    String ID;
    String originProgramID;

    // SỬA LỖI: Thêm biến này để SimulationWrapper có thể truy cập
    public String lightDef;

    TrafficLightWrapper(String temp){
        ID = temp;
        System.out.println("Added " + temp + ".");
    }

    //=================GETTER================================
    public String getID(int po) {
        if (po == 1) {System.out.print(" " + ID);}
        return ID;
    }

    public int getPhaseNum(SimulationWrapper temp, int po) {
        try {
            int tlsPhase = (int)temp.conn.do_job_get(Trafficlight.getPhase(ID));
            if (po == 1) {System.out.println(String.format("tlsPhase of %s: %d", ID, tlsPhase));}
            return tlsPhase;
        }
        catch(Exception A) {
            System.out.println("Failed to get phase number.");
        }
        return -1;
    }

    public String getPhaseDef(SimulationWrapper temp, int po) {
        try {
            String lightState = (String)temp.conn.do_job_get(Trafficlight.getRedYellowGreenState(ID));
            if (po == 1) {System.out.println(String.format("Current phase definition of %s: %s", ID, lightState));}
            return lightState;
        }
        catch (Exception B) {
            System.out.println("Failed to get TL phase definition.");
        }
        return null;
    }

    public List<String[][]> getControlledLinks(SimulationWrapper temp, int po) {
        try {
            List<String[][]> controlledLinks = (List<String[][]>)temp.conn.do_job_get(Trafficlight.getControlledLinks(ID));
            if (po == 1){System.out.println("Current controlled links of " + ID + ":" + controlledLinks.get(0));}
            return controlledLinks;
        }
        catch (Exception C) {
            System.out.println("Cannot get controlled links of traffic light");
        }
        return null;
    }

    //=================SETTER================================
    public boolean setPhaseDef(SimulationWrapper temp, String input) {
        try {
            temp.conn.do_job_set(Trafficlight.setRedYellowGreenState(ID, input));
            // Lưu ý: Thread.sleep ở đây sẽ làm dừng cả mô phỏng, hãy cân nhắc khi dùng
            Thread.sleep(2000);
            temp.conn.do_job_set(Trafficlight.setProgram(ID, "0"));
            return true;
        }
        catch (Exception D) {
            System.out.println("Unable to set phase definition");
        }
        return false;
    }

    // SỬA LỖI: Đổi tên hàm cho khớp với SimulationWrapper và logic chung
    public boolean setPhaseDefWithPhaseTime(SimulationWrapper temp, String input, double time) {
        try {
            long roundedTime = Math.round(time * 200);
            temp.conn.do_job_set(Trafficlight.setRedYellowGreenState(ID, input));
            Thread.sleep(roundedTime);
            temp.conn.do_job_set(Trafficlight.setProgram(ID, "0"));
            return true;
        }
        catch (Exception D) {
            System.out.println("Unable to set phase definition with time");
        }
        return false;
    }

    //=================STATIC================================
    public static void updateTrafficLightIDs(SimulationWrapper temp) {
        try {
            @SuppressWarnings("unchecked")
            List<String> IDsList = (List<String>)temp.conn.do_job_get(Trafficlight.getIDList());
            for (String x : IDsList) {
                TrafficLightWrapper y = new TrafficLightWrapper(x);
                // SỬA LỖI: TrafficLightList là HashMap nên dùng .put(key, value) thay vì .add()
                temp.TrafficLightList.put(x, y);
            }
        }
        catch (Exception A) {
            System.out.println("Set up traffic lights failed.");
            A.printStackTrace(); // In lỗi chi tiết để dễ debug
        }
    }
}