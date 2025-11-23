package Model;

import java.util.List;

// Feature.java
public enum Feature {
    // Core Mandatory Features (from Lab 1)
    APPOINTMENTS("Appointment Management"),
    MEDICAL_HISTORY("Personal Medical History"),
    INSURANCE_LEVELS("Varying Insurance Levels"),
    PAYMENTS("Payment Management"),

    // Optional/Adaptive Features (from Lab 1 case study)
    REMINDERS("Medication/Appointment Reminders"),
    FAST_SCHEDULING("Fast Appointment Scheduling"),
    AUTOMATIC_RESCHEDULING("Automatic Rescheduling on Doctor Unavailability"),
    PREMIUM_SERVICE_ACCESS("Access to Better/Premium Services"),

    // New UI Features for Lab 3 Demonstration
    DYNAMIC_BUTTON("Dynamic Button Display"),
    DARK_MODE("Dark Mode UI Theme");

    private final String description;

    Feature(String description) {
        this.description = description;
    }

    public static List<Feature> getAllFeatures() {
        return List.of(Feature.values());
    }

    public static List<Feature> getMandatoryFeatures() {
        return List.of(APPOINTMENTS, MEDICAL_HISTORY, INSURANCE_LEVELS);
    }

    public static boolean isMandatory(Feature feature) {
        return getMandatoryFeatures().contains(feature);
    }

    @Override
    public String toString() {
        return description;
    }

    public boolean isMandatory() {
        return isMandatory(this);
    }

    // TODO: cross-tree constraints can be added here as needed
}
