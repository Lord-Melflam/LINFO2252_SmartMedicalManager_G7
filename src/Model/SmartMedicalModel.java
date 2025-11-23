package Model;
// SmartMedicalModel.java
import java.util.*;
import Logger.Logger;

public class SmartMedicalModel implements TimeEventListener {
    private static SmartMedicalModel instance;
    private final Set<Feature> activeFeatures = new HashSet<>();
    private final List<Appointment> futureAppointments = new ArrayList<>();
    private final List<Appointment> pastAppointments = new ArrayList<>();

    private final Logger logger = Logger.getInstance();
    private final TimeEventSystem tes = TimeEventSystem.getInstance();

    public SmartMedicalModel() {
        activeFeatures.add(Feature.APPOINTMENTS);
        activeFeatures.add(Feature.MEDICAL_HISTORY);
        activeFeatures.add(Feature.INSURANCE_LEVELS);

        tes.registerListener(this);
    }

    public static SmartMedicalModel getInstance() {
        if (instance == null) {
            synchronized (SmartMedicalModel.class) {
                if (instance == null) instance = new SmartMedicalModel();
            }
        }
        return instance;
    }

    public synchronized void addAppointment(String patient, int day) {
        Appointment a = new Appointment(day, patient);
        futureAppointments.add(a);
        futureAppointments.sort(Comparator.comparingInt(app -> app.getDay()));
        
        logger.log("Model", "Added appointment: " + a);
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
            if (name.isEmpty()) continue;
            try {
                Feature feature = Feature.valueOf(name);
                if (feature.isMandatory()) {
                    logger.error("Model", "Cannot deactivate mandatory feature: " + feature);
                    return false;
                }
                newActiveFeatures.remove(feature);
            } catch (IllegalArgumentException e) {
                logger.error("Model", "Feature not found: " + name);
                return false;
            }
        }

        // 2. Process Activations
        for (String name : toActivate) {
            if (name.isEmpty()) continue;
            try {
                Feature feature = Feature.valueOf(name);
                newActiveFeatures.add(feature);
            } catch (IllegalArgumentException e) {
                logger.error("Model", "Feature not found: " + name);
            }
        }

        // 3. Enforce Cross-Tree Constraints (Simplified Example: FAST_SCHEDULING REQUIRES PREMIUM_SERVICE_ACCESS)
        if (newActiveFeatures.contains(Feature.FAST_SCHEDULING) &&
                !newActiveFeatures.contains(Feature.PREMIUM_SERVICE_ACCESS)) {
            logger.error("Model", "Fast Scheduling REQUIRES Premium Service Access (Simulated constraint).");
            return false;
        }

        // 4. Apply Change
        activeFeatures.clear();
        activeFeatures.addAll(newActiveFeatures);
        return true;
    }

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
                logger.log("Model", "Number of days passed: " + daysAdvanced);
                handleAdvanceTime();
                break;
            case DOCTOR_UNAVAILABLE:
                handleDoctorUnavailable();
                break;
            case USER_ILL:
                handleUserIll();
                break;
            case MANUAL_TRIGGER:
                logger.log("Model", "Manual TES trigger received.");
                break;
        }
    }

    private synchronized void handleAdvanceTime() {
        int today = tes.getCurrentDay();
        List<Appointment> toMove = new ArrayList<>();
        for (Appointment a : futureAppointments) {
            if (a.getDay() <= today && !a.isCancelled()) {
                a.setHistory(true);
                a.setResult("Completed (time advanced)");
                toMove.add(a);
            } else if (a.isCancelled()) {
                // cancelled appointments older than today move to history as cancelled
                if (a.getDay() <= today) {
                    a.setHistory(true);
                    toMove.add(a);
                }
            }
        }
        futureAppointments.removeAll(toMove);
        pastAppointments.addAll(toMove);
        if (!toMove.isEmpty()) {
            logger.log("Model", "Moved " + toMove.size() + " appointment(s) to history due to time advance.");
        }
    }

    private synchronized void handleDoctorUnavailable() {
        if (futureAppointments.isEmpty()) {
            logger.log("Model", "Doctor unavailable but no future appointments.");
            return;
        }

        Appointment earliest = futureAppointments.getFirst();
        // futureAppointments is sorted by day by default so no need to search
        // for (Appointment a : futureAppointments) if (a.getDay() < earliest.getDay()) earliest = a;

        earliest.setCancelled(true);
        logger.log("Model", "Doctor unavailable: cancelled appointment " + earliest);

        if (activeFeatures.contains(Feature.AUTOMATIC_RESCHEDULING)) {
            int newDay = earliest.getDay() + 1;
            Appointment res = new Appointment(newDay, earliest.getPatient());
            futureAppointments.add(res);
            logger.log("Model", "AUTOMATIC_RESCHEDULING: rescheduled to day " + newDay);
        }
    }

    private synchronized void handleUserIll() {
        if (futureAppointments.isEmpty()) {
            logger.log("Model", "User ill event: no future appointments to update.");
            return;
        }

        Appointment target = futureAppointments.get(0);
        target.setResult("Patient reported illness");

        if (activeFeatures.contains(Feature.AUTOMATIC_RESCHEDULING)) {
            target.setCancelled(true);
            int newDay = target.getDay() + 7;
            futureAppointments.remove(target);
            Appointment followUp = new Appointment(newDay, target.getPatient());
            futureAppointments.add(followUp);
            logger.log("Model", "USER_ILL: created follow-up appointment on day " + newDay);
        } else {
            logger.log("Model", "USER_ILL: annotated appointment: " + target);
        }
    }
}