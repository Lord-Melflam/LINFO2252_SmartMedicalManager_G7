// SmartMedicalModel.java
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SmartMedicalModel {
    private final Set<Feature> activeFeatures = new HashSet<>();

    // Initialize with mandatory core features
    public SmartMedicalModel() {
        activeFeatures.add(Feature.APPOINTMENTS);
        activeFeatures.add(Feature.MEDICAL_HISTORY);
        activeFeatures.add(Feature.INSURANCE_LEVELS);
    }

    /**
     * Attempts to change the set of active features, enforcing feature constraints.
     * @param toDeactivate Features to turn off.
     * @param toActivate Features to turn on.
     * @return true if the configuration change is valid and applied, false otherwise.
     */
    public boolean applyFeatureChange(String[] toDeactivate, String[] toActivate) {
        Set<Feature> newActiveFeatures = new HashSet<>(activeFeatures);

        // 1. Process Deactivations
        for (String name : toDeactivate) {
            try {
                Feature feature = Feature.valueOf(name);
                if (isMandatory(feature)) {
                    System.out.println("[Model] ERROR: Cannot deactivate mandatory feature: " + feature);
                    return false; // Constraint violation
                }
                newActiveFeatures.remove(feature);
            } catch (IllegalArgumentException e) {
                System.out.println("[Model] ERROR: Feature not found: " + name);
                // Return true here, as the model should only enforce constraints on valid features
                // The Controller should catch this and return an error code if necessary.
            }
        }

        // 2. Process Activations
        for (String name : toActivate) {
            try {
                Feature feature = Feature.valueOf(name);
                newActiveFeatures.add(feature);
            } catch (IllegalArgumentException e) {
                System.out.println("[Model] ERROR: Feature not found: " + name);
                // Return true, allowing valid changes to pass if they exist
            }
        }

        // 3. Enforce Cross-Tree Constraints (Simplified Example: FAST_SCHEDULING REQUIRES PREMIUM_SERVICE_ACCESS)
        if (newActiveFeatures.contains(Feature.FAST_SCHEDULING) &&
                !newActiveFeatures.contains(Feature.PREMIUM_SERVICE_ACCESS)) {
            System.out.println("[Model] ERROR: Fast Scheduling REQUIRES Premium Service Access (Simulated constraint).");
            return false;
        }

        // 4. Apply Change
        activeFeatures.clear();
        activeFeatures.addAll(newActiveFeatures);
        return true;
    }

    // Helper to check mandatory features
    private boolean isMandatory(Feature feature) {
        return feature == Feature.APPOINTMENTS || feature == Feature.MEDICAL_HISTORY || feature == Feature.INSURANCE_LEVELS;
    }

    // Helper method to check if a feature is currently active
    public boolean isFeatureActive(Feature feature) {
        return activeFeatures.contains(feature);
    }

    /**
     * Reports the current state of the system in a log format (used by automated testing tools).
     */
    public String[] getCurrentStateLog() {
        String[] log = new String[activeFeatures.size() + 2];
        log[0] = "System Status: OPERATIONAL";
        log[1] = "Active Features:";
        int i = 2;
        for (Feature f : activeFeatures) {
            log[i++] = "- " + f.name() + " (" + f.toString() + ")";
        }
        return log;
    }
}