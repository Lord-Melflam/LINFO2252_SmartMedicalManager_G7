package Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static Logger instance;
    private PrintWriter fileWriter;
    private boolean logToFile = false;
    private boolean logToConsole = true;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Logger() {
        enableFileLogging("app.log");
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    /**
     * Enable logging to a file
     * @param filepath Path to the log file
     */
    public synchronized void enableFileLogging(String filepath) {
        try {
            fileWriter = new PrintWriter(new FileWriter(filepath, true), true);
            logToFile = true;
            log("Logger", "File logging enabled: " + filepath);
        } catch (IOException e) {
            System.err.println("Failed to enable file logging: " + e.getMessage());
        }
    }

    /**
     * Disable logging to file
     */
    public synchronized void disableFileLogging() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
        logToFile = false;
    }

    /**
     * Enable/disable console logging
     */
    public void setConsoleLogging(boolean enabled) {
        this.logToConsole = enabled;
    }

    /**
     * Log a message with a component tag
     * @param component The component generating the log (e.g., "Controller", "Model", "View", "TES")
     * @param message The message to log
     */
    public synchronized void log(String component, String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String formattedMessage = String.format("[%s] [%s] %s", timestamp, component, message);
        
        if (logToConsole) {
            System.out.println(formattedMessage);
        }
        
        if (logToFile && fileWriter != null) {
            fileWriter.println(formattedMessage);
        }
    }

    /**
     * Log an error message
     */
    public synchronized void error(String component, String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String formattedMessage = String.format("[%s] [%s] ERROR: %s", timestamp, component, message);
        
        if (logToConsole) {
            System.err.println(formattedMessage);
        }
        
        if (logToFile && fileWriter != null) {
            fileWriter.println(formattedMessage);
        }
    }

    /**
     * Flush the file writer
     */
    public synchronized void flush() {
        if (fileWriter != null) {
            fileWriter.flush();
        }
    }
}