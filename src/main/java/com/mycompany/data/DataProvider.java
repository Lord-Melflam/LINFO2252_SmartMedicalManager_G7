package com.mycompany.data;

import com.mycompany.model.Logger;

import java.util.*;

/**
 * Singleton provider for UI dropdown data and options.
 * Centralizes all hardcoded options to make them maintainable and configurable.
 * Replaces scattered hardcoded data throughout the UI.
 */
public class DataProvider {
    public static final String CATEGORY_CONSULTATION_TYPES = "consultationTypes";
    public static final String CATEGORY_LOCATIONS = "locations";
    public static final String CATEGORY_PERSONNEL = "personnel";
    public static final String CATEGORY_ROOM_TYPES = "roomTypes";
    public static final String CATEGORY_EQUIPMENT = "equipment";
    public static final String CATEGORY_PAYMENT_METHODS = "paymentMethods";
    public static final String CATEGORY_THEMES = "themes";
    public static final String CATEGORY_LANGUAGES = "languages";
    public static final String CATEGORY_NOTIFICATION_TYPES = "notificationTypes";
    public static final String CATEGORY_CONTACT_METHODS = "contactMethods";

    private static DataProvider instance;
    
    private final Map<String, List<String>> optionsMap;
    private final Logger logger = Logger.getInstance();
    
    private DataProvider() {
        this.optionsMap = new HashMap<>();
        initializeOptions();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized DataProvider getInstance() {
        if (instance == null) {
            instance = new DataProvider();
        }
        return instance;
    }
    
    /**
     * Initialize all available options.
     * These should be loaded from configuration in production.
     */
    private void initializeOptions() {
        // Consultation types - from feature diagram
        optionsMap.put(CATEGORY_CONSULTATION_TYPES, Arrays.asList(
            "General Consultation",
            "Surgery",
            "Dentist",
            "Cardiology",
            "Dermatology",
            "Preventive Checkup"
        ));
        
        // Hospital/Center options
        optionsMap.put(CATEGORY_LOCATIONS, Arrays.asList(
            "Hospital Dav",
            "Hospital Helen",
            "Dental Clinic",
            "Medical Center A",
            "Specialist Center B"
        ));
        
        // Personnel/Doctor options
        optionsMap.put(CATEGORY_PERSONNEL, Arrays.asList(
            "Dr. Angst",
            "Dr. Stuckov",
            "Dr. Smith",
            "Dr. Johnson",
            "Dr. Williams"
        ));
        
        // Room type options
        optionsMap.put(CATEGORY_ROOM_TYPES, Arrays.asList(
            "Shared",
            "Private",
            "Semi-Private"
        ));
        
        // Equipment options
        optionsMap.put(CATEGORY_EQUIPMENT, Arrays.asList(
            "CT Scanner",
            "X-Ray",
            "Ultrasound",
            "MRI Machine",
            "None"
        ));
        
        // Payment method options
        optionsMap.put(CATEGORY_PAYMENT_METHODS, Arrays.asList(
            "Card",
            "Cash",
            "Insurance Billing"
        ));
        
        // Theme options
        optionsMap.put(CATEGORY_THEMES, Arrays.asList(
            "Light",
            "Dark",
            "System Default"
        ));
        
        // Language options
        optionsMap.put(CATEGORY_LANGUAGES, Arrays.asList(
            "English",
            "French",
            "German",
            "Spanish"
        ));
        
        // Notification options
        optionsMap.put(CATEGORY_NOTIFICATION_TYPES, Arrays.asList(
            "In-App",
            "Email",
            "SMS"
        ));
        
        // Contact method options
        optionsMap.put(CATEGORY_CONTACT_METHODS, Arrays.asList(
            "Email",
            "Phone",
            "SMS"
        ));
        logger.log("DataProvider", "Options initialized.");
    }
    
    /**
     * Gets options as array (for UI components).
     */
    public String[] getOptionsArray(String category) {
        List<String> options = optionsMap.get(category);
        if (options == null) {
            return new String[0];
        }
        return options.toArray(new String[0]);
    }
    
    /**
     * Checks if a category has options.
     */
    public boolean hasCategory(String category) {
        return optionsMap.containsKey(category);
    }

    /**
     * Removes all options for a category.
     */
    public void removeCategory(String category) {
        optionsMap.remove(category);
        logger.log("DataProvider", "Cleared options for category '" + category + "'.");
    }
    
    /**
     * Gets all available categories.
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(optionsMap.keySet());
    }
}
