// SmartMedicalModel.java
import java.util.*;

public class SmartMedicalModel implements TimeEventListener {
    private final Set<Feature> activeFeatures = new HashSet<>();
    private final List<Appointment> futureAppointments = new ArrayList<>();
    private final List<Appointment> pastAppointments = new ArrayList<>();

    public SmartMedicalModel() {
        activeFeatures.add(Feature.APPOINTMENTS);
        activeFeatures.add(Feature.MEDICAL_HISTORY);
        activeFeatures.add(Feature.INSURANCE_LEVELS);

        TimeEventSystem.getInstance().registerListener(this);
    }

    public synchronized void addAppointment(String patient, int day) {
        Appointment a = new Appointment(day, patient);
        futureAppointments.add(a);
        System.out.println("[Model] Appointment added: " + a);
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
        List<String> lines = new ArrayList<>();
        lines.add("System Status: OPERATIONAL");
        lines.add("Active Features:");
        for (Feature f : activeFeatures) {
            lines.add("- " + f.name() + " (" + f.toString() + ")");
        }
        lines.add("Future Appointments:");
        for (Appointment a : futureAppointments) {
            lines.add("- " + a.toString());
        }
        lines.add("Past Appointments:");
        for (Appointment a : pastAppointments) {
            lines.add("- " + a.toString());
        }
        return lines.toArray(new String[0]);
    }

    @Override
    public void onTimeEvent(TimeEvent event, int daysAdvanced) {
        switch (event) {
            case DAY_PASSED:
            case WEEK_PASSED:
                handleAdvanceTime();
                break;
            case DOCTOR_UNAVAILABLE:
                handleDoctorUnavailable();
                break;
            case USER_ILL:
                handleUserIll();
                break;
            case MANUAL_TRIGGER:
                System.out.println("[Model] Manual TES trigger received.");
                break;
        }
    }

    private synchronized void handleAdvanceTime() {
        int today = TimeEventSystem.getInstance().getCurrentDay();
        List<Appointment> toMove = new ArrayList<>();
        for (Appointment a : futureAppointments) {
            if (a.day <= today && !a.cancelled) {
                a.isHistory = true;
                a.result = "Completed (time advanced)";
                toMove.add(a);
            } else if (a.cancelled) {
                // cancelled appointments older than today move to history as cancelled
                if (a.day <= today) {
                    a.isHistory = true;
                    toMove.add(a);
                }
            }
        }
        futureAppointments.removeAll(toMove);
        pastAppointments.addAll(toMove);
        if (!toMove.isEmpty()) {
            System.out.println("[Model] Moved " + toMove.size() + " appointment(s) to history due to time advance.");
        }
    }

    private synchronized void handleDoctorUnavailable() {
        if (futureAppointments.isEmpty()) {
            System.out.println("[Model] Doctor unavailable but no future appointments.");
            return;
        }
        Appointment earliest = futureAppointments.get(0);
        for (Appointment a : futureAppointments) if (a.day < earliest.day) earliest = a;

        earliest.cancelled = true;
        System.out.println("[Model] Doctor unavailable: cancelled appointment " + earliest);

        if (activeFeatures.contains(Feature.AUTOMATIC_RESCHEDULING)) {
            int newDay = earliest.day + 1;
            Appointment res = new Appointment(newDay, earliest.patient);
            futureAppointments.add(res);
            System.out.println("[Model] AUTOMATIC_RESCHEDULING: rescheduled to day " + newDay);
        }
    }

    private synchronized void handleUserIll() {
        if (futureAppointments.isEmpty()) {
            System.out.println("[Model] User ill event: no future appointments to update.");
            return;
        }

        Appointment target = futureAppointments.get(0);
        target.result = "Patient reported illness";

        if (activeFeatures.contains(Feature.AUTOMATIC_RESCHEDULING)) {
            target.cancelled = true;
            int newDay = target.day + 7;
            futureAppointments.remove(target);
            Appointment followUp = new Appointment(newDay, target.patient);
            futureAppointments.add(followUp);
            System.out.println("[Model] USER_ILL: created follow-up appointment on day " + newDay);
        } else {
            System.out.println("[Model] USER_ILL: annotated appointment: " + target);
        }
    }
}