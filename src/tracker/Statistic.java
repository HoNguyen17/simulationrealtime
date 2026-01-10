package tracker;

import wrapper.SimulationWrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;
import java.util.ArrayList;

public class Statistic {
    private static SimulationWrapper sim = null;
    public static boolean initialize(SimulationWrapper input) {
        sim = input;
        try (FileWriter writer = new FileWriter("tracker/data.csv")) {
            writer.write("Simulation Time, Edge, Density, Travel Time\n");
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while opening the file.");
            e.printStackTrace();
        }
        return false;
    }
    public static boolean addNewData() {
        try (FileWriter writer = new FileWriter("tracker/data.csv", true)) {
            double currentTime = sim.getTime(0);
            List<String> edges = sim.getEdgeIDsList();
            for (String e : edges) {
                int density = sim.getEdgeDensity(e);
                double travelTime = sim.getEdgeTravelTime(e);
                if(density > 0) {
                    writer.write(currentTime + ",");
                    writer.write(" " + e + ",");
                    writer.write(density + ",");
                    writer.write(travelTime + "\n");
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("An error occurred while opening the file.");
            e.printStackTrace();
        }
        return false;
    }
    public static boolean export() {
        System.out.println("Do export and stuff");
        return false;
    }
}