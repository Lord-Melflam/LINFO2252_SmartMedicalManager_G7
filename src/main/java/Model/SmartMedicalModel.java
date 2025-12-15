package Model;
// SmartMedicalModel.java

import Logger.Logger;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.Theme;

import java.util.*;
import java.time.LocalDate;

public class SmartMedicalModel implements TimeEventListener {
    private static SmartMedicalModel instance;
    private final Set<Feature> activeFeatures = new HashSet<>();
    private final ArrayList<Appointment> futureAppointments = new ArrayList<>();
    private final ArrayList<Appointment> pastAppointments = new ArrayList<>();
    private final ArrayList<Notification> notifications = new ArrayList<>();
    private boolean darkTheme;
    private Theme activeTheme = new DarculaTheme();

    private final Logger logger = Logger.getInstance();
    private final TimeEventSystem tes = TimeEventSystem.getInstance();

    public SmartMedicalModel() {
        activeFeatures.addAll(Feature.getMandatoryFeatures());
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

    public synchronized Appointment addAppointment(String patient, String staff, LocalDate date) {
        Appointment a = new Appointment(date, patient, staff);
        futureAppointments.add(a);
        futureAppointments.sort(Comparator.comparing(Appointment::getDate));

        logger.log("Model", "Appointment added: " + a);
        addNotification("Appointment added: " + a);
        return a;
    }

    /**
     * Return a copy of the current future appointments. Caller may inspect but
     * must not modify the returned list to avoid concurrent modification.
     */
    public synchronized java.util.List<Appointment> getFutureAppointments() {
        return new ArrayList<>(futureAppointments);
    }

    /**
     * Return a copy of past appointments.
     */
    public synchronized java.util.List<Appointment> getPastAppointments() {
        return new ArrayList<>(pastAppointments);
    }

    /**
     * Reschedule a future appointment by index to a new day.
     * Returns true if successful.
     */
    public synchronized boolean rescheduleAppointment(int index, LocalDate date) {
        if (index < 0 || index >= futureAppointments.size()) {
            logger.error("Model", "rescheduleAppointment: invalid index " + index);
            return false;
        }

        Appointment a = futureAppointments.get(index);
        if (a.isCancelled()) {
            logger.error("Model", "rescheduleAppointment: appointment is cancelled " + a);
            return false;
        }

        // create a new Appointment with the new day but keep patient and result/history/cancel state
        Appointment newA = new Appointment(a);
        newA.setDate(date);

        // preserve cancelled/history flags if applicable
        if (a.isHistory()) newA.setHistory(true);
        if (a.isCancelled()) newA.setCancelled(true);

        futureAppointments.remove(index);
        futureAppointments.add(newA);
        futureAppointments.sort(Comparator.comparing(Appointment::getDate));

        logger.log("Model", "Rescheduled appointment: " + a + " -> " + newA);
        addNotification("Appointment for " + a.getPatient() + " rescheduled to " + date);
        return true;
    }

    /**
     * Cancel a future appointment by index (as returned by {@link #getFutureAppointments()}).
     * Returns true if cancellation succeeded.
     */
    public synchronized boolean cancelAppointment(int index) {
        if (index < 0 || index >= futureAppointments.size()) {
            logger.error("Model", "cancelAppointment: invalid index " + index);
            return false;
        }
        Appointment a = futureAppointments.get(index);
        if (a.isCancelled()) {
            logger.log("Model", "cancelAppointment: appointment already cancelled: " + a);
            return false;
        }
        a.setCancelled(true);
        logger.log("Model", "Cancelled appointment: " + a);
        addNotification("Appointment cancelled for " + a.getPatient() + " on " + a.getDate());
        return true;
    }

    /**
     * Cancel appointment by UUID. Returns true if found and cancelled.
     */
    public synchronized boolean cancelAppointmentById(java.util.UUID id) {
        for (Appointment a : futureAppointments) {
            if (a.getId().equals(id)) {
                if (a.isCancelled()) {
                    logger.log("Model", "cancelAppointmentById: already cancelled " + a);
                    return false;
                }
                a.setCancelled(true);
                logger.log("Model", "Cancelled appointment: " + a);
                addNotification("Appointment cancelled for " + a.getPatient() + " on " + a.getDate());
                return true;
            }
        }
        logger.error("Model", "cancelAppointmentById: not found " + id);
        return false;
    }

    /**
     * Reschedule appointment by UUID to a new LocalDate.
     */
    public synchronized boolean rescheduleAppointmentById(java.util.UUID id, LocalDate newDate) {
        for (int i = 0; i < futureAppointments.size(); i++) {
            Appointment a = futureAppointments.get(i);
            if (a.getId().equals(id)) {
                if (a.isCancelled()) {
                    logger.error("Model", "rescheduleAppointmentById: appointment is cancelled " + a);
                    return false;
                }
                Appointment newA = new Appointment(a);
                newA.setDate(newDate);

                if (a.isHistory()) newA.setHistory(true);
                if (a.isCancelled()) newA.setCancelled(true);

                futureAppointments.set(i, newA);
                futureAppointments.sort(Comparator.comparing(Appointment::getDate));
                logger.log("Model", "Rescheduled appointment: " + a + " -> " + newA);
                addNotification("Appointment for " + a.getPatient() + " rescheduled to " + newDate);
                return true;
            }
        }
        logger.error("Model", "rescheduleAppointmentById: not found " + id);
        return false;
    }

    /**
     * Add a notification to the system.
     */
    public synchronized void addNotification(String message) {
        Notification n = new Notification(message);
        notifications.add(0, n); // newest first
        logger.log("Model", "Notification added: " + n);
    }

    /**
     * Return a copy of notifications (newest first).
     */
    public synchronized java.util.List<Notification> getNotifications() {
        return new ArrayList<>(notifications);
    }

    /**
     * Return only unread notifications.
     */
    public synchronized java.util.List<Notification> getUnreadNotifications() {
        java.util.List<Notification> out = new ArrayList<>();
        for (Notification n : notifications) if (!n.isRead()) out.add(n);
        return out;
    }

    /**
     * Mark a notification read by id. Returns true if found.
     */
    public synchronized boolean markNotificationRead(String id) {
        for (Notification n : notifications) {
            if (n.getId().equals(id)) {
                if (!n.isRead()) n.markRead();
                logger.log("Model", "Notification marked read: " + id);
                return true;
            }
        }
        logger.error("Model", "markNotificationRead: not found " + id);
        return false;
    }

    /**
     * Clear (remove) all notifications.
     */
    public synchronized void clearNotifications() {
        notifications.clear();
        logger.log("Model", "All notifications cleared.");
    }

    /**
     * Attempts to change the set of active features, enforcing feature constraints.
     *
     * @param toDeactivate Features to turn off.
     * @param toActivate   Features to turn on.
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
        LocalDate today = tes.getCurrentDate();
        List<Appointment> toMove = new ArrayList<>();
        for (Appointment a : futureAppointments) {
            if (a.getDate().isBefore(today) && !a.isCancelled()) {
                a.setHistory(true);
                a.setResult("Completed (time advanced)");
                toMove.add(a);
            } else if (a.isCancelled()) {
                // cancelled appointments older than today move to history as cancelled
                if (a.getDate().isBefore(today)) {
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

        Appointment earliest = futureAppointments.get(0);
        // futureAppointments is sorted by day by default so no need to search
        // for (Appointment a : futureAppointments) if (a.getDay() < earliest.getDay()) earliest = a;

        earliest.setCancelled(true);
        logger.log("Model", "Doctor unavailable: cancelled appointment " + earliest);
        addNotification("Doctor unavailable: cancelled appointment for " + earliest.getPatient() + " on day " + earliest.getDate().getDayOfMonth());

        if (activeFeatures.contains(Feature.AUTOMATIC_RESCHEDULING)) {
            // reschedule to next day
            LocalDate newDay = earliest.getDate().plusDays(1);
            Appointment res = new Appointment(earliest);
            res.setDate(newDay);
            futureAppointments.add(res);
            logger.log("Model", "AUTOMATIC_RESCHEDULING: rescheduled to day " + newDay);
            addNotification("Appointment rescheduled to day " + newDay + " for " + earliest.getPatient());
        }
    }

    private synchronized void handleUserIll() {
        if (futureAppointments.isEmpty()) {
            logger.log("Model", "User ill event: no future appointments to update.");
            return;
        }

        Appointment target = futureAppointments.get(0);
        target.setResult("Patient reported illness");
        addNotification("Patient reported illness for appointment on day " + target.getDate().getDayOfMonth() + " for " + target.getPatient());
        if (activeFeatures.contains(Feature.AUTOMATIC_RESCHEDULING)) {
            target.setCancelled(true);
            LocalDate newDay = target.getDate().plusDays(7);
            futureAppointments.remove(target);
            Appointment followUp = new Appointment(target);
            followUp.setDate(newDay);
            futureAppointments.add(followUp);
            logger.log("Model", "USER_ILL: created follow-up appointment on day " + newDay);
            addNotification("USER_ILL: created follow-up appointment on day " + newDay + " for " + target.getPatient());
        } else {
            logger.log("Model", "USER_ILL: annotated appointment: " + target);
        }
    }
}