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
    public static final class ChoiceDefinition {
        private final List<String> choices;

        public ChoiceDefinition(List<String> choices) {
            this.choices = (choices == null) ? List.of() : List.copyOf(choices);
        }

        public List<String> getChoices() {
            return choices;
        }
    }

    private static FeatureManager instance;
    private final Set<String> activeFeatures;
    private final List<FeatureObserver> observers;
    private final Map<String, Map<String, Object>> featureAttributes;
    private final Set<String> insuranceDisabledFeatures = ConcurrentHashMap.newKeySet();

    private static final Set<String> VALID_FEATURES = Set.of(
        // Appointment Features
        "Book", "Modify", "Cancel", "Re-scheduling",
        "Personel", "ConsultationType", "ConsultationLocation", "RoomType",
        "InsuranceBilling",
        
        // Medical History Features
        "PastConsultations",
        "Sort", "SortByDate", "SortByType", "SortByService",
        "Search",
        
        // Adaptation Features
        "Reminders", "AppointmentReminders", "MedicationReminders", "OtherReminders",
        "Notification", "NotifyOnReschedule",
        "DarkMode",
        
        // User Profile
        "BillingInformation", "CurrentMedication", "Vaccines"
    );
    
    private static final Set<String> MANDATORY_FEATURES = Set.of(
        "Book", "Modify", "Cancel", "PastConsultations", "InsuranceBilling"
    );

    private static final Set<String> DEFAULT_FEATURES = Set.of(
            "Personel", "ConsultationType", "ConsultationLocation", "RoomType",
        "Sort", "SortByDate", "SortByType", "SortByService",
        "Search",

        "Reminders", "AppointmentReminders", "MedicationReminders", "OtherReminders",
        "Notification", "NotifyOnReschedule", "Re-scheduling",
        "DarkMode", "BillingInformation", "CurrentMedication", "Vaccines"
    );

    /**
     * Declarative configuration schema for the Admin panel.
     * Map: featureName -> choice definition.
     */
    private static final Map<String, ChoiceDefinition> FEATURE_CHOICES = Map.of(
        "InsuranceBilling", new ChoiceDefinition(List.of("MINIMAL", "NORMAL", "PREMIUM")),
        "Notification", new ChoiceDefinition(List.of("IN_APP", "EMAIL", "SMS"))
    );

    public enum InsuranceLevel {
        MINIMAL,
        NORMAL,
        PREMIUM
    }

    /**
     * Feature gating based on InsuranceBilling value.
     * The integer is the minimum required index in the InsuranceBilling choices list.
     * Example: if InsuranceBilling choices are [MINIMAL, NORMAL, PREMIUM], then:
     */
    private static final Map<String, Integer> FEATURE_MIN_INSURANCE_INDEX = Map.of(
        // Room choice is only available from NORMAL and above
        "RoomType", InsuranceLevel.NORMAL.ordinal(),
        "SharedRoom", InsuranceLevel.NORMAL.ordinal(),
        // Private room is only available for PREMIUM
        "PrivateRoom", InsuranceLevel.PREMIUM.ordinal()
    );

    /**
     * Returns the choice definition for a feature, or null if the feature is free-text.
     */
    public static ChoiceDefinition getChoiceDefinition(String featureName) {
        if (!VALID_FEATURES.contains(featureName)) {
            throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
        return FEATURE_CHOICES.get(featureName);
    }

    private static int insuranceIndexFromValue(String insuranceBillingValue) {
        ChoiceDefinition def = FEATURE_CHOICES.get("InsuranceBilling");
        if (def == null || def.getChoices().isEmpty()) {
            return 0;
        }

        // return the index of the value, or default to NORMAL (1) if not found
        for (int i = 0; i < def.getChoices().size(); i++) {
            if (def.getChoices().get(i).equalsIgnoreCase(insuranceBillingValue)) {
                return i;
            }
        }

        return Math.min(InsuranceLevel.NORMAL.ordinal(), def.getChoices().size() - 1);
    }

    private boolean isFeatureAllowedForInsurance(String featureName, String insuranceBillingValue) {
        Integer minIdx = FEATURE_MIN_INSURANCE_INDEX.get(featureName);
        if (minIdx == null) {
            return true;
        }
        return insuranceIndexFromValue(insuranceBillingValue) >= minIdx;
    }

    private synchronized void enforceInsuranceConstraints(String insuranceBillingValue) {
        List<String> deactivated = new ArrayList<>();
        List<String> activated = new ArrayList<>();

        for (String feature : new HashSet<>(activeFeatures)) {
            if (isMandatory(feature)) {
                continue;
            }
            if (!isFeatureAllowedForInsurance(feature, insuranceBillingValue)) {
                if (activeFeatures.remove(feature)) {
                    deactivated.add(feature);
                    insuranceDisabledFeatures.add(feature);
                }
            }
        }

        // If insurance increased, restore any features we disabled only due to insurance.
        for (String feature : new HashSet<>(insuranceDisabledFeatures)) {
            if (isMandatory(feature)) {
                insuranceDisabledFeatures.remove(feature);
                continue;
            }
            if (isFeatureAllowedForInsurance(feature, insuranceBillingValue) && !activeFeatures.contains(feature)) {
                activeFeatures.add(feature);
                insuranceDisabledFeatures.remove(feature);
                activated.add(feature);
            }
        }

        if (!deactivated.isEmpty()) {
            notifyObserversFeatureDeactivated(deactivated);
        }
        if (!activated.isEmpty()) {
            notifyObserversFeatureActivated(activated);
        }
    }
    
    private FeatureManager() {
        this.activeFeatures = new HashSet<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.featureAttributes = new ConcurrentHashMap<>();
        
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
        activeFeatures.addAll(new ArrayList<>(DEFAULT_FEATURES));
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
                insuranceDisabledFeatures.remove(feature);
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
        return MANDATORY_FEATURES.contains(feature);
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
    private void notifyObserversInsuranceLevelChanged(String level) {
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

        // Insurance level is stored as a plain string in the InsuranceBilling feature attribute.
        if ("InsuranceBilling".equals(featureName) && "value".equals(attributeName) && attributeValue != null) {
            String raw = String.valueOf(attributeValue).trim();
            if (!raw.isEmpty()) {
                notifyObserversInsuranceLevelChanged(raw);
                enforceInsuranceConstraints(raw);
            }
        }
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
