package com.mycompany.model;

public final class Logger {
    private static Logger instance;

    private Logger() {}

    /**
     * Gets the singleton instance.
    */
   public static synchronized Logger getInstance() {
       if (instance == null) {
           instance = new Logger();
        }
        return instance;
    }
    
    public synchronized void log(String component, String message) {
        System.out.println("[" + component + "] " + message);
    }
    
    public synchronized void logError(String component, String message) {
        System.err.println("[" + component + "] ERROR: " + message);
    }
}