package logger;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

public class AppLog { 
    private final static Logger rootLog = Logger.getLogger("");

    public static void initialize() {
        Handler fileHandler;
        Handler consoleHandler;

        for (Handler h : rootLog.getHandlers()) {rootLog.removeHandler(h);}

        try {
            fileHandler = new java.util.logging.FileHandler("logfile.log");
            rootLog.addHandler(fileHandler);

            java.util.logging.Formatter textFormat = new java.util.logging.SimpleFormatter();
            fileHandler.setFormatter(textFormat);
            fileHandler.setLevel(Level.INFO);

            consoleHandler = new java.util.logging.ConsoleHandler();
            rootLog.addHandler(consoleHandler);

            consoleHandler.setLevel(Level.WARNING);
            consoleHandler.setFormatter(textFormat);

            rootLog.setLevel(Level.INFO);
            // Disable default console logging to avoid duplicate messages
            rootLog.setUseParentHandlers(false);
            // Register a shutdown hook to close handlers properly
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (Handler h : rootLog.getHandlers()) {
                    h.close(); // Ensures all XML tags are closed and file is saved
                }
            }));

        } catch (java.io.IOException | SecurityException e) {
            System.err.println("Logging setup failed: " + e.getMessage());
        }
    }
} 
