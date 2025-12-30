package com.mycompany.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Singleton manager for feature activation/deactivation.
 * Implements Observer pattern to notify Views of feature changes.
 * Core component for dynamic, adaptive system behavior.
 */
public class FeatureManager {
    public enum InsuranceLevel {
        MINIMAL, NORMAL, PREMIUM
    }

    public enum NotificationMethod {
        IN_APP, EMAIL, SMS
    }

    private static FeatureManager instance;

    private final Set<String> activeFeatures;
    private final List<FeatureObserver> observers;
    private final Map<String, Map<String, Object>> featureAttributes; // Store feature-specific attributes

    private InsuranceLevel insuranceLevel;
    
    // Available features as per feature diagram
    private static final Set<String> VALID_FEATURES = Set.of(
        // Appointment Features
        "Book", "Modify", "Cancel", "Complete", "Personel",
        "ConsultationType", "ConsultationLocation", "RoomType", "SharedRoom", "PrivateRoom",
        "Prepay", "DelayedPayment", "Card", "Cash", "InsuranceBilling",
        
        // Medical History Features
        "PastConsultations", "Prescriptions",
        "Sort", "SortByDate", "SortByType", "SortByService", "SearchByStaff",
        "BasicSearch", "AdvancedSearch",
        
        // Adaptation Features
        "Reminders", "AppointmentReminders", "MedicationReminders", "OtherReminders",
        "Fast", "AutoDoctor", "AutoTimeslot",
        "AutoReschedule", "NotifyOnReschedule",
        "Notification",
        "DarkMode",
        
        // User Profile
        "ContactMethod", "CurrentMedication", "Vaccines"
    );

    private static final Set<String> MANDATORY_FEATURES = Set.of(
        "Book", "Cancel", "Complete", "PastConsultations", "Personel",
            "ConsultationType", "ConsultationLocation", "RoomType", "SharedRoom", "PrivateRoom"
    );
    
    private FeatureManager() {
        this.activeFeatures = new HashSet<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.featureAttributes = new ConcurrentHashMap<>();
        this.insuranceLevel = InsuranceLevel.NORMAL;
        
        // Initialize default active features (mandatory ones)
        initializeDefaultFeatures();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized FeatureManager getInstance() {
        if (instance == null) {
            instance = new FeatureManager();
        }
        return instance;
    }
    
    /**
     * Initialize mandatory features that are always active.
     */
    private void initializeDefaultFeatures() {
        activeFeatures.addAll(new ArrayList<>(MANDATORY_FEATURES));
        
        // Mandatory medical history
        activeFeatures.add("PastConsultations");
        
        // Default optional features
        activeFeatures.add("BasicSearch");
        activeFeatures.add("Reminders");
    }
    
    /**
     * Activates one or more features.
     * Notifies observers of changes.
     */
    public synchronized void activateFeatures(String... featureNames) {
        List<String> activated = new ArrayList<>();
        for (String feature : featureNames) {
            if (!VALID_FEATURES.contains(feature)) {
                throw new IllegalArgumentException("Unknown feature: " + feature);
            }
            if (activeFeatures.add(feature)) {
                activated.add(feature);
            }
        }
        if (!activated.isEmpty()) {
            notifyObserversFeatureActivated(activated);
        }
    }
    
    /**
     * Deactivates one or more features.
     * Cannot deactivate mandatory features.
     * Notifies observers of changes.
     */
    public synchronized void deactivateFeatures(String... featureNames) {
        List<String> deactivated = new ArrayList<>();
        for (String feature : featureNames) {
            if (!VALID_FEATURES.contains(feature)) {
                throw new IllegalArgumentException("Unknown feature: " + feature);
            }
            // Prevent deactivation of mandatory features
            if (isMandatory(feature)) {
                throw new IllegalStateException("Cannot deactivate mandatory feature: " + feature);
            }
            if (activeFeatures.remove(feature)) {
                deactivated.add(feature);
            }
        }
        if (!deactivated.isEmpty()) {
            notifyObserversFeatureDeactivated(deactivated);
        }
    }
    
    /**
     * Checks if a feature is currently active.
     */
    public boolean isFeatureActive(String featureName) {
        return activeFeatures.contains(featureName);
    }
    
    /**
     * Gets all active features.
     */
    public Set<String> getActiveFeatures() {
        return new HashSet<>(activeFeatures);
    }
    
    /**
     * Gets all available features.
     */
    public static Set<String> getAvailableFeatures() {
        return new HashSet<>(VALID_FEATURES);
    }
    
    /**
     * Checks if a feature is mandatory (cannot be deactivated).
     */
    public boolean isMandatory(String feature) {
        return Set.of("Book", "Cancel", "Complete", "PastConsultations").contains(feature);
    }
    
    /**
     * Sets the insurance level and adjusts available features.
     * Insurance level affects feature availability and behavior.
     */
    public synchronized void setInsuranceLevel(InsuranceLevel level) {
        this.insuranceLevel = level;
        notifyObserversInsuranceLevelChanged(level);
    }
    
    /**
     * Gets the current insurance level.
     */
    public InsuranceLevel getInsuranceLevel() {
        return insuranceLevel;
    }
    
    /**
     * Gets features that should be available for current insurance level.
     * Override this for feature interactions based on insurance.
     */
    public Set<String> getAvailableFeaturesForInsurance() {
        Set<String> available = new HashSet<>(VALID_FEATURES);
        // Future: Remove features not available for current insurance level
        return available;
    }
    
    private void forEachObserver(Consumer<FeatureObserver> action) {
        for (FeatureObserver observer : observers) {
            action.accept(observer);
        }
    }

    /**
     * Registers an observer for feature changes.
     */
    public void registerObserver(FeatureObserver observer) {
        if (observer != null) {
            ((CopyOnWriteArrayList<FeatureObserver>) observers).addIfAbsent(observer);
        }
    }
    
    /**
     * Unregisters an observer.
     */
    public void unregisterObserver(FeatureObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifies observers of feature activation.
     */
    private void notifyObserversFeatureActivated(List<String> features) {
        forEachObserver(o -> o.onFeaturesActivated(features));
    }
    
    /**
     * Notifies observers of feature deactivation.
     */
    private void notifyObserversFeatureDeactivated(List<String> features) {
        forEachObserver(o -> o.onFeaturesDeactivated(features));
    }
    
    /**
     * Notifies observers of insurance level change.
     */
    private void notifyObserversInsuranceLevelChanged(InsuranceLevel level) {
        forEachObserver(o -> o.onInsuranceLevelChanged(level));
    }
    
    /**
     * Sets an attribute for a feature (e.g., reminder type, email address).
     */
    public void setFeatureAttribute(String featureName, String attributeName, Object attributeValue) {
        if (!VALID_FEATURES.contains(featureName)) {
            throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
        featureAttributes
            .computeIfAbsent(featureName, k -> new ConcurrentHashMap<>())
            .put(attributeName, attributeValue);
    }
    
    /**
     * Gets an attribute for a feature.
     */
    public Object getFeatureAttribute(String featureName, String attributeName) {
        if (!VALID_FEATURES.contains(featureName)) {
            throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
        Map<String, Object> attrs = featureAttributes.get(featureName);
        return (attrs == null) ? null : attrs.get(attributeName);
    }
    
    /**
     * Gets all attributes for a feature.
     */
    public Map<String, Object> getFeatureAttributes(String featureName) {
        if (!VALID_FEATURES.contains(featureName)) {
            throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
        Map<String, Object> attrs = featureAttributes.get(featureName);
        return (attrs == null) ? Collections.emptyMap() : Map.copyOf(attrs);
    }
}
