package com.mycompany.model;

import java.util.*;

/**
 * Singleton provider for UI dropdown data and options.
 * Centralizes all hardcoded options to make them maintainable and configurable.
 * Replaces scattered hardcoded data throughout the UI.
 */
public class DataProvider {
    private static DataProvider instance;
    
    private final Map<String, List<String>> optionsMap;
    
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
        optionsMap.put("consultationTypes", Arrays.asList(
            "General Consultation",
            "Surgery",
            "Dentist",
            "Cardiology",
            "Dermatology",
            "Preventive Checkup"
        ));
        
        // Hospital/Center options
        optionsMap.put("locations", Arrays.asList(
            "Hospital Dav",
            "Hospital Helen",
            "Dental Clinic",
            "Medical Center A",
            "Specialist Center B"
        ));
        
        // Personnel/Doctor options
        optionsMap.put("personnel", Arrays.asList(
            "Dr. Angst",
            "Dr. Stuckov",
            "Dr. Smith",
            "Dr. Johnson",
            "Dr. Williams"
        ));
        
        // Room type options
        optionsMap.put("roomTypes", Arrays.asList(
            "Shared",
            "Private",
            "Semi-Private"
        ));
        
        // Equipment options
        optionsMap.put("equipment", Arrays.asList(
            "CT Scanner",
            "X-Ray",
            "Ultrasound",
            "MRI Machine",
            "None"
        ));
        
        // Payment method options
        optionsMap.put("paymentMethods", Arrays.asList(
            "Card",
            "Cash",
            "Insurance Billing"
        ));
        
        // Theme options
        optionsMap.put("themes", Arrays.asList(
            "Light",
            "Dark",
            "System Default"
        ));
        
        // Language options
        optionsMap.put("languages", Arrays.asList(
            "English",
            "French",
            "German",
            "Spanish"
        ));
        
        // Notification options
        optionsMap.put("notificationTypes", Arrays.asList(
            "In-App",
            "Email",
            "SMS"
        ));
        
        // Contact method options
        optionsMap.put("contactMethods", Arrays.asList(
            "Email",
            "Phone",
            "SMS"
        ));
    }
    
    /**
     * Gets options by category.
     */
    public List<String> getOptions(String category) {
        return new ArrayList<>(optionsMap.getOrDefault(category, new ArrayList<>()));
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
     * Adds custom options (for runtime extensions).
     */
    public void addOption(String category, String option) {
        optionsMap.computeIfAbsent(category, k -> new ArrayList<>()).add(option);
    }
    
    /**
     * Adds multiple options to a category.
     */
    public void addOptions(String category, List<String> options) {
        optionsMap.computeIfAbsent(category, k -> new ArrayList<>()).addAll(options);
    }
    
    /**
     * Gets all available categories.
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(optionsMap.keySet());
    }
}
