// Feature.java
public enum Feature {
    // Core Mandatory Features (from Lab 1)
    APPOINTMENTS("Appointment Management"),
    MEDICAL_HISTORY("Personal Medical History"),
    INSURANCE_LEVELS("Varying Insurance Levels"),

    // Optional/Adaptive Features (from Lab 1 case study)
    PAYMENT("Payment Feature"),
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

    @Override
    public String toString() {
        return description;
    }
}