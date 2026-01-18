package tracker;

import wrapper.SimulationWrapper;

import javafx.print.PageLayout;
import javafx.print.Paper;
import javafx.print.PageOrientation;
import javafx.print.Printer;
import javafx.print.Printer.MarginType;
import javafx.print.PrinterJob;

import javafx.scene.layout.VBox;
import javafx.scene.Scene;

import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.List;
import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Statistic {
    private static SimulationWrapper sim = null;
    private final static Logger LOG = Logger.getLogger(Statistic.class.getName());

    public static boolean initialize(SimulationWrapper input) {
        sim = input;
        try (FileWriter writer = new FileWriter("tracker/trackedData.csv")) {
            writer.write("Simulation Time, Overall Average Speed of Time Step, Edge, Density, Estimate Travel Time, Congested\n");
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "An error occurred while opening the file.", e);
        }
        return false;
    }
    public static void addNewEdgeData() {
        try (FileWriter writer = new FileWriter("tracker/trackedData.csv", true)) {
            double currentTime = sim.getTime(0);
            List<String> edges = sim.getEdgeIDsList();
            for (String e : edges) {
                int density = sim.getEdgeDensity(e);
                if(density > 0) {
                    double travelTime = sim.getEdgeTravelTime(e);
                    boolean congested = sim.getEdgeCongested(e);
                    double averageSpeed = sim.getVehicleAverageSpeed(0);
                    writer.write(currentTime + ",");
                    writer.write(averageSpeed + ",");
                    writer.write(" " + e + ",");
                    writer.write(density + ",");
                    writer.write(travelTime + ",");
                    writer.write(congested + "\n");
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "An error occurred while opening the file.", e);
        }
    }

    public static String exportCSV(File destinationFile, String filterType) {
        try {
            File sourceFile = new File("tracker/trackedData.csv");
            if (!sourceFile.exists()) {
                // throw exception here
                return null;
            }
            if (destinationFile != null) {
                try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
                     BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile))) {
                    
                    String header = reader.readLine();
                    if (header == null) return null;
                    
                    writer.write(header);
                    writer.newLine();
                    
                    String line;
                    int totalRows = 0;
                    int exportedRows = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        totalRows++;
                        boolean shouldExport = true;
                        // Apply filter if CONGESTED_ONLY
                        if ("CONGESTED_ONLY".equals(filterType)) {
                            // Parse CSV line: "Time, Average, Edge, Density, Travel Time, Congested"
                            String[] parts = line.split(",");
                            if (parts.length >= 6) {
                                String congestedStr = parts[5].trim();
                                if (!congestedStr.equalsIgnoreCase("true")) shouldExport = false;
                            }
                        }
                        if (shouldExport) {
                            writer.write(line);
                            writer.newLine();
                            exportedRows++;
                        }
                    }
                    writer.flush();
                    return destinationFile.getAbsolutePath();
                }

            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error exporting CSV file: ", e);
        }
        return null;
    }

    public static boolean exportPDF(VBox chartContainer, Stage stage) {
        Scene printScene = new Scene(chartContainer, 480, 656); //600 820
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob == null) return false;
        
        if (printerJob.showPrintDialog(stage)) {
            Printer printer = printerJob.getPrinter();
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, 
                PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        
            if (printerJob.printPage(pageLayout, chartContainer)) {
                printerJob.endJob();
                return true;
            }
        }
        return false;
    }
}