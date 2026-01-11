package com.mycompany.model;

import com.mycompany.data.Notification;
import com.mycompany.data.TimeEvent;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Medication manager (singleton) that:
 * - Owns the current hardcoded medication + vaccine lists (requested).
 * - Schedules a daily medication reminder using the simulated TimeEvent system.
 *
 * Feature gating:
 * - Requires Reminders + MedicationReminders to be active to schedule.
 * - Requires Notification to be active to actually send notifications.
 */
public class MedicationManager implements TimeEventObserver, FeatureObserver, TimeChangeObserver {
    private static final String TAG = "MedicationManager";
    private static final int DEFAULT_REMINDER_HOUR = 9;
    private static final int DEFAULT_REMINDER_MINUTE = 0;

    private static final String DAILY_EVENT_ID = "medication_daily";

    private static MedicationManager instance;

    private final NotificationManager notificationManager;
    private final TimeEventManager timeEventManager;
    private final FeatureManager featureManager;
    private final Logger logger = Logger.getInstance();

    private Date scheduledAt;

    private final List<String> currentMedications = List.of(
        "Paracetamol 500mg",
        "Vitamin D"
    );

    private final List<String> vaccines = List.of(
        "COVID-19",
        "Influenza"
    );

    public static synchronized MedicationManager getInstance() {
        if (instance == null) {
            instance = new MedicationManager();
        }
        return instance;
    }

    private MedicationManager() {
        this.notificationManager = NotificationManager.getInstance();
        this.timeEventManager = TimeEventManager.getInstance();
        this.featureManager = FeatureManager.getInstance();

        this.timeEventManager.registerListener(this);
        this.timeEventManager.registerTimeObserver(this);
        this.featureManager.registerObserver(this);

        refreshSchedule();
    }

    public List<String> getCurrentMedications() {
        return currentMedications;
    }

    public List<String> getVaccines() {
        return vaccines;
    }

    public String formatMedicationSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Medication:\n");
        for (String med : currentMedications) {
            sb.append("- ").append(med).append("\n");
        }
        return sb.toString();
    }

    public String formatVaccinesSection() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vaccines:\n");
        for (String v : vaccines) {
            sb.append("- ").append(v).append("\n");
        }
        return sb.toString();
    }

    private String buildDailyReminderMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Daily medication reminder:\n");
        for (String med : currentMedications) {
            sb.append("- ").append(med).append("\n");
        }
        return sb.toString().trim();
    }

    private boolean isMedicationReminderFeatureEnabled() {
        return featureManager.isFeatureActive("Reminders") && featureManager.isFeatureActive("MedicationReminders") && featureManager.isFeatureActive("CurrentMedication");
    }

    private boolean isNotificationFeatureEnabled() {
        return featureManager.isFeatureActive("Notification");
    }

    private Date computeNextFireTime(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now == null ? new Date() : now);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Calendar candidate = (Calendar) cal.clone();
        candidate.set(Calendar.HOUR_OF_DAY, DEFAULT_REMINDER_HOUR);
        candidate.set(Calendar.MINUTE, DEFAULT_REMINDER_MINUTE);

        if (now != null && !candidate.getTime().after(now)) {
            candidate.add(Calendar.DAY_OF_MONTH, 1);
        }

        return candidate.getTime();
    }

    private void cancelScheduledEventIfAny() {
        try {
            timeEventManager.cancelScheduledEvent(DAILY_EVENT_ID);
        } catch (Exception ignored) {
            // best effort
        } finally {
            scheduledAt = null;
        }
    }

    private void scheduleNext() {
        Date now = timeEventManager.getDate();
        Date fireAt = computeNextFireTime(now);
        String description = "Daily medication reminder";

        // Ensure we never keep stale reminders around when the admin jumps time.
        timeEventManager.cancelScheduledEvent(DAILY_EVENT_ID);

        scheduledAt = fireAt;
        timeEventManager.schedule(new TimeEvent(DAILY_EVENT_ID, fireAt, description));
        logger.log(TAG, "Scheduled daily medication reminder at " + timeEventManager.format(fireAt));
    }

    private void refreshSchedule() {
        if (!isMedicationReminderFeatureEnabled()) {
            cancelScheduledEventIfAny();
            return;
        }

        Date now = timeEventManager.getDate();
        if (scheduledAt == null || (now != null && !scheduledAt.after(now))) {
            scheduleNext();
        }
    }

    @Override
    public void onTimeEvent(TimeEvent event) {
        if (event == null || event.getId() == null) {
            return;
        }

        if (!DAILY_EVENT_ID.equals(event.getId())) {
            return;
        }

        // Clear scheduled time first so we can re-schedule even if sending fails.
        scheduledAt = null;

        if (isMedicationReminderFeatureEnabled() && isNotificationFeatureEnabled()) {
            String title = "Medication Reminder";
            String message = buildDailyReminderMessage();
            long ts = timeEventManager.getDate().getTime();

            try {
                notificationManager.send(new Notification(title, message, ts));
            } catch (Exception e) {
                logger.logError(TAG, "Failed to send medication reminder: " + e.getMessage());
            }
        }

        // Schedule next occurrence if still enabled.
        refreshSchedule();
    }

    @Override
    public void onTimeChanged(Date newNow) {
        // IMPORTANT: TimeEventManager notifies time observers before it processes due events.
        // So we must NOT cancel/reschedule here when we've jumped past the scheduled reminder,
        // otherwise the reminder will never fire.
        if (!isMedicationReminderFeatureEnabled()) {
            cancelScheduledEventIfAny();
            return;
        }

        // If we don't currently have a reminder scheduled, create one.
        if (scheduledAt == null) {
            scheduleNext();
            return;
        }

        // If time jumped forward beyond the scheduled reminder time, do nothing here and let
        // TimeEventManager.processDueEvents() fire the reminder, then onTimeEvent() will
        // schedule the next occurrence.
    }

    @Override
    public void onFeaturesActivated(java.util.List<String> features) {
        refreshSchedule();
    }

    @Override
    public void onFeaturesDeactivated(java.util.List<String> features) {
        refreshSchedule();
    }

    @Override
    public void onInsuranceLevelChanged(String level) {
        // no-op
    }
}
