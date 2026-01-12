package tracker;

import wrapper.SimulationWrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.print.PrinterJob;
import javafx.print.PageLayout;
import javafx.print.Paper;
import javafx.print.PageOrientation;
import javafx.print.Printer;
import javafx.print.Printer.MarginType;

import java.util.List;
import java.util.ArrayList;

public class Statistic {
    private static SimulationWrapper sim = null;
    private static final Object fileLock = new Object(); // Lock object for file operations
    
    public static boolean initialize(SimulationWrapper input) {
        sim = input;
        synchronized (fileLock) {
            try {
                // Ensure tracker directory exists
                File trackerDir = new File("tracker");
                if (!trackerDir.exists()) {
                    trackerDir.mkdirs();
                }
                
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("tracker/data.csv", false))) {
                    writer.write("Simulation Time, Edge, Density, Travel Time, Congested\n");
                    writer.flush();
                }
                return true;
            } catch (Exception e) {
                System.err.println("An error occurred while initializing the file: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public static synchronized boolean addNewData() {
        synchronized (fileLock) {
            // Retry logic in case file is temporarily locked
            int maxRetries = 3;
            int retryDelay = 100; // milliseconds
            
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    // Ensure tracker directory exists
                    File trackerDir = new File("tracker");
                    if (!trackerDir.exists()) {
                        trackerDir.mkdirs();
                    }
                    
                    try (BufferedWriter writer = new BufferedWriter(
                            new FileWriter("tracker/data.csv", true))) {
                        double currentTime = sim.getTime(0);
                        List<String> edges = sim.getEdgeIDsList();
                        
                        if (edges != null) {
                            for (String e : edges) {
                                int density = sim.getEdgeDensity(e);
                                if(density > 0) {
                                    double travelTime = sim.getEdgeTravelTime(e);
                                    boolean congested = sim.getEdgeCongested(e);
                                    writer.write(currentTime + ",");
                                    writer.write(" " + e + ",");
                                    writer.write(density + ",");
                                    writer.write(travelTime + ",");
                                    writer.write(congested + "\n");
                                }
                            }
                        }
                        writer.flush(); // Ensure data is written immediately
                        return true;
                    }
                } catch (IOException e) {
                    if (attempt < maxRetries - 1) {
                        // File might be locked, wait and retry
                        try {
                            Thread.sleep(retryDelay);
                            retryDelay *= 2; // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    } else {
                        // Last attempt failed
                        System.err.println("An error occurred while writing to the file after " + 
                            maxRetries + " attempts: " + e.getMessage());
                        // Don't print full stack trace for file lock errors to reduce noise
                        if (!e.getMessage().contains("being used by another process")) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("An unexpected error occurred: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }
    /**
     * Export CSV file to user-selected location with optional filter
     * @param stage JavaFX stage for file chooser dialog
     * @param filterType "ALL" to export all data, "CONGESTED_ONLY" to export only congested edges
     * @return true if export successful, false otherwise
     */
    public static boolean exportCSV(Stage stage, String filterType) {
        if (stage == null) {
            System.err.println("Stage is null, cannot show file chooser");
            return false;
        }

        try {
            File sourceFile = new File("tracker/data.csv");
            if (!sourceFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("CSV file not found");
                alert.setContentText("The data.csv file does not exist in the tracker folder.");
                alert.showAndWait();
                return false;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export CSV File");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            
            // Set default filename with timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            fileChooser.setInitialFileName("traffic_statistics_" + timestamp + ".csv");

            File destinationFile = fileChooser.showSaveDialog(stage);
            if (destinationFile != null) {
                // Read, filter, and write CSV data
                try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
                     BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile))) {
                    
                    String header = reader.readLine();
                    if (header == null) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Export Error");
                        alert.setHeaderText("Empty CSV file");
                        alert.setContentText("The source CSV file is empty.");
                        alert.showAndWait();
                        return false;
                    }
                    
                    // Write header
                    writer.write(header);
                    writer.newLine();
                    
                    // Filter and write data
                    String line;
                    int totalRows = 0;
                    int exportedRows = 0;
                    
                    while ((line = reader.readLine()) != null) {
                        totalRows++;
                        boolean shouldExport = true;
                        
                        // Apply filter if CONGESTED_ONLY
                        if ("CONGESTED_ONLY".equals(filterType)) {
                            // Parse CSV line: "Time, Edge, Density, Travel Time, Congested"
                            String[] parts = line.split(",");
                            if (parts.length >= 5) {
                                String congestedStr = parts[4].trim();
                                // Check if congested is true
                                if (!congestedStr.equalsIgnoreCase("true")) {
                                    shouldExport = false;
                                }
                            }
                        }
                        
                        if (shouldExport) {
                            writer.write(line);
                            writer.newLine();
                            exportedRows++;
                        }
                    }
                    
                    writer.flush();
                    
                    String message = String.format("File saved to: %s\n\nTotal rows: %d\nExported rows: %d", 
                        destinationFile.getAbsolutePath(), totalRows, exportedRows);
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export Successful");
                    alert.setHeaderText("CSV file exported");
                    alert.setContentText(message);
                    alert.showAndWait();
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error exporting CSV file: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText("Failed to export CSV file");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
        return false;
    }

    /**
     * Export graphs to PDF using PrinterJob
     * @param stage JavaFX stage for print dialog
     * @param speedChart Speed area chart
     * @param travelTimeChart Travel time bar chart
     * @param densityChart Density bar chart
     * @return true if export successful, false otherwise
     */
    public static boolean exportPDF(Stage stage, AreaChart<Number, Number> speedChart, 
                                    BarChart<String, Number> travelTimeChart, 
                                    BarChart<String, Number> densityChart) {
        if (stage == null) {
            System.err.println("Stage is null, cannot show print dialog");
            return false;
        }

        try {
            // Create a container to hold all charts - căn giữa và vừa với A4
            VBox chartContainer = new VBox(20);  // Spacing giữa các charts
            chartContainer.setStyle("-fx-padding: 30; -fx-background-color: white; -fx-alignment: center;");
            // Giảm width để có margin đều 2 bên và không bị cắt
            chartContainer.setPrefWidth(600);
            // Chiều cao vừa với A4: 3 charts x 240px + 2 spacing x 20px + padding 60px = 820px
            chartContainer.setPrefHeight(820);
            chartContainer.setAlignment(Pos.CENTER);
            
            // Add charts to container - kích thước vừa với A4, căn giữa
            if (speedChart != null) {
                speedChart.setPrefSize(550, 240);  // Width x Height - giảm để fit A4 với margin
                chartContainer.getChildren().add(speedChart);
            }
            if (travelTimeChart != null) {
                travelTimeChart.setPrefSize(550, 240);
                chartContainer.getChildren().add(travelTimeChart);
            }
            if (densityChart != null) {
                densityChart.setPrefSize(550, 240);
                chartContainer.getChildren().add(densityChart);
            }

            if (chartContainer.getChildren().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Export Warning");
                alert.setHeaderText("No charts available");
                alert.setContentText("No charts are available to export.");
                alert.showAndWait();
                return false;
            }

            // Create a temporary scene for printing - kích thước vừa với A4
            Scene printScene = new Scene(chartContainer, 600, 820);
            
            // Create printer job
            PrinterJob printerJob = PrinterJob.createPrinterJob();
            if (printerJob == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Print Error");
                alert.setHeaderText("No printer available");
                alert.setContentText("Please ensure a printer (including PDF printer) is available.");
                alert.showAndWait();
                return false;
            }

            // Show print dialog
            boolean showDialog = printerJob.showPrintDialog(stage);
            if (showDialog) {
                // Configure page layout for better PDF output
                Printer printer = printerJob.getPrinter();
                PageLayout pageLayout = printer.createPageLayout(Paper.A4, 
                    PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
                
                // Print the charts
                boolean success = printerJob.printPage(pageLayout, chartContainer);
                if (success) {
                    printerJob.endJob();
                    
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export Successful");
                    alert.setHeaderText("PDF export completed");
                    alert.setContentText("The charts have been exported to PDF. " +
                        "If you selected a PDF printer, the file should be saved.");
                    alert.showAndWait();
                    return true;
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Export Error");
                    alert.setHeaderText("Failed to export PDF");
                    alert.setContentText("The print job failed. Please try again.");
                    alert.showAndWait();
                }
            }
        } catch (Exception e) {
            System.err.println("Error exporting PDF: " + e.getMessage());
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText("Failed to export PDF");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
        return false;
    }

    /**
     * Export CSV with default filter (ALL)
     * @param stage JavaFX stage for file chooser dialog
     * @return true if export successful, false otherwise
     */
    public static boolean exportCSV(Stage stage) {
        return exportCSV(stage, "ALL");
    }

    /**
     * Legacy export method - exports CSV if stage is provided
     * @param stage Optional JavaFX stage for file chooser
     */
    public static boolean export(Stage stage) {
        if (stage != null) {
            return exportCSV(stage, "ALL");
        }
        System.out.println("Export called but no stage provided. Use exportCSV(Stage) or exportPDF(Stage, ...) for full functionality.");
        return false;
    }

    /**
     * Export method without parameters (for backward compatibility)
     */
    public static boolean export() {
        System.out.println("Export called but no stage provided. Use exportCSV(Stage) or exportPDF(Stage, ...) for full functionality.");
        return false;
    }
}