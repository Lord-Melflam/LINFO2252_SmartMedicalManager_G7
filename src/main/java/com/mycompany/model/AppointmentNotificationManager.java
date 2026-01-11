package com.mycompany.model;

import com.mycompany.data.Appointment;
import com.mycompany.data.Notification;
import com.mycompany.data.TimeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer component that bridges appointments with notifications and time events.
 * Handles scheduling of reminders and cancellation notices.
 * Implements TimeEventListener to respond to scheduled events via observer pattern.
 */
public class AppointmentNotificationManager implements TimeEventObserver {
    private static final String TAG = "AppointmentNotificationService";

    private static AppointmentNotificationManager instance;
    
    private final NotificationManager notificationManager;
    private final TimeEventManager timeEventManager;
    private final FeatureManager featureManager;
    private final Logger logger = Logger.getInstance();

    // Maps event IDs to their associated appointments and metadata
    private final Map<String, AppointmentEventData> eventRegistry = new ConcurrentHashMap<>();

    /**
     * Returns upcoming appointments that currently have a scheduled reminder in the registry.
     * This is primarily used by the Home screen to render a clickable "Upcoming reminders" list.
     */
    public List<Appointment> getUpcomingReminderAppointments(int limit) {
        if (!isAppointmentRemindersEnabled()) {
            return java.util.Collections.emptyList();
        }

        int effectiveLimit = (limit <= 0) ? Integer.MAX_VALUE : limit;
        long now = nowMillis();

        Set<Appointment> unique = new HashSet<>();
        for (AppointmentEventData data : eventRegistry.values()) {
            if (data == null || data.eventType != EventType.REMINDER || data.appointment == null) {
                continue;
            }
            Date dt = data.appointment.getDateAsDate();
            if (dt == null) {
                continue;
            }
            if (dt.getTime() >= now) {
                unique.add(data.appointment);
            }
        }

        List<Appointment> result = new ArrayList<>(unique);
        result.sort(Comparator.comparing(a -> {
            Date d = a.getDateAsDate();
            return (d == null) ? new Date(Long.MAX_VALUE) : d;
        }));

        if (result.size() > effectiveLimit) {
            return new ArrayList<>(result.subList(0, effectiveLimit));
        }
        return result;
    }

    public static synchronized AppointmentNotificationManager getInstance() {
        if (instance == null) {
            instance = new AppointmentNotificationManager();
        }
        return instance;
    }

    private AppointmentNotificationManager() {
        this.notificationManager = NotificationManager.getInstance();
        this.timeEventManager = TimeEventManager.getInstance();
        this.featureManager = FeatureManager.getInstance();

        this.timeEventManager.registerListener(this);
        logger.log(TAG, "AppointmentNotificationService initialized and registered as listener.");
    }

    private boolean isNotificationsEnabled() {
        try {
            return featureManager != null && featureManager.isFeatureActive("Notification");
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean isAppointmentRemindersEnabled() {
        try {
            if (featureManager == null) {
                return true;
            }

            if (!featureManager.isFeatureActive("Reminders")) {
                return false;
            }

            return featureManager.isFeatureActive("AppointmentReminders");
        } catch (Exception ignored) {
            return true;
        }
    }

    private long nowMillis() {
        try {
            Date d = (timeEventManager == null) ? null : timeEventManager.getDate();
            return (d == null) ? System.currentTimeMillis() : d.getTime();
        } catch (Exception ignored) {
            return System.currentTimeMillis();
        }
    }

    /**
     * Handles fired time events - dispatches to appropriate notification handler.
     */
    @Override
    public void onTimeEvent(TimeEvent event) {
        AppointmentEventData eventData = eventRegistry.get(event.getId());
        if (eventData == null) {
            logger.log(TAG, "No registered handler for event: " + event.getId());
            return;
        }

        switch (eventData.eventType) {
            case REMINDER:
                sendAppointmentReminder(eventData.appointment, eventData.hoursBeforeAppointment);
                break;
            case CANCELLATION:
                if (eventData.reason != null && eventData.reason.equalsIgnoreCase("Doctor Unavailable")) {
                    sendDoctorUnavailableNotice(eventData.appointment, eventData.reason);
                } else {
                    sendCancellationNotice(eventData.appointment, eventData.cancelledBy, eventData.reason);
                }
                break;
            default:
                logger.log(TAG, "Unknown event type for: " + event.getId());
        }

        // Clean up processed event
        eventRegistry.remove(event.getId());
    }

    /**
     * Schedules a reminder for an appointment.
     *
     * @param appointment              The appointment to remind about
     * @param hoursBeforeAppointment How many hours before the appointment to send reminder
     */
    public void scheduleAppointmentReminder(Appointment appointment, int hoursBeforeAppointment) {
        try {
            if (!isAppointmentRemindersEnabled()) {
                return;
            }

            if (hoursBeforeAppointment < 0) {
                logger.logError(TAG, "hoursBeforeAppointment must be >= 0");
                return;
            }

            Date appointmentDateTime = appointment.getDateAsDate();
            long reminderMillis = appointmentDateTime.getTime() - (hoursBeforeAppointment * 3600_000L);
            Date reminderTime = new Date(reminderMillis);

            String eventId = "reminder_" + appointment.getDate() + "_" + appointment.getTime() + "_" + hoursBeforeAppointment + "h";
            String description = "Reminder for appointment with " + appointment.getDoctor();

            TimeEvent reminderEvent = new TimeEvent(eventId, reminderTime, description);

            // Register event metadata
            AppointmentEventData eventData = new AppointmentEventData();
            eventData.eventType = EventType.REMINDER;
            eventData.appointment = appointment;
            eventData.hoursBeforeAppointment = hoursBeforeAppointment;
            eventRegistry.put(eventId, eventData);

            timeEventManager.schedule(reminderEvent);
            logger.log(TAG, "Scheduled reminder for appointment " + appointment.getDate() + " " + appointment.getTime() +
                    " at " + reminderTime + " (" + hoursBeforeAppointment + "h before)");
        } catch (Exception e) {
            logger.logError(TAG, "Failed to schedule reminder for appointment " + appointment.getDate() + " " + appointment.getTime() + ": " + e.getMessage());
        }
    }

    /**
     * Sends an immediate reminder notification for an appointment.
     */
    private void sendAppointmentReminder(Appointment appointment, int hoursBeforeAppointment) {
        if (!isNotificationsEnabled() || !isAppointmentRemindersEnabled()) {
            return;
        }

        String title = "Appointment Reminder";
        String message = String.format(
                "Your appointment with %s is in %d hour(s).\nDate: %s at %s\nLocation: %s",
                appointment.getDoctor(),
                hoursBeforeAppointment,
                appointment.getDate(),
                appointment.getTime(),
                appointment.getLocation()
        );

        Notification notification = new Notification(title, message, nowMillis());
        notificationManager.send(notification);
        logger.log(TAG, "Sent appointment reminder: " + appointment.getDate() + " " + appointment.getTime() +
                " with " + appointment.getDoctor());
    }

    /**
     * Sends a cancellation notice for an appointment (doctor/user can't make it).
     *
     * @param appointment The cancelled appointment
     * @param cancelledBy Who cancelled (doctor/user)
     * @param reason     Optional reason for cancellation
     */
    public void sendCancellationNotice(Appointment appointment, String cancelledBy, String reason) {
        if (!isNotificationsEnabled()) {
            return;
        }

        String title = "Appointment Cancelled";
        String message = String.format(
                "Your appointment with %s on %s at %s has been cancelled.\nCancelled by: %s%s",
                appointment.getDoctor(),
                appointment.getDate(),
                appointment.getTime(),
                cancelledBy,
                reason != null && !reason.isEmpty() ? "\nReason: " + reason : ""
        );

        Notification notification = new Notification(title, message, nowMillis());
        notificationManager.send(notification);
        logger.log(TAG, "Sent cancellation notice for appointment " + appointment.getDate() + " " + appointment.getTime() +
                " (cancelled by " + cancelledBy + ")");
    }

    /**
     * Schedules a cancellation notice to be sent at a specific time.
     * Useful for advance notice of unavailability.
     */
    public void scheduleCancellationNotice(Appointment appointment, String cancelledBy, String reason, Date noticeTime) {
        if (!isNotificationsEnabled()) {
            return;
        }

        long when = (noticeTime == null) ? nowMillis() : noticeTime.getTime();
        String eventId = "cancellation_" + appointment.getDate() + "_" + appointment.getTime() + "_" + when;
        String description = "Cancellation notice for appointment with " + appointment.getDoctor();

        TimeEvent cancellationEvent = new TimeEvent(eventId, noticeTime, description);

        // Register event metadata
        AppointmentEventData eventData = new AppointmentEventData();
        eventData.eventType = EventType.CANCELLATION;
        eventData.appointment = appointment;
        eventData.cancelledBy = cancelledBy;
        eventData.reason = reason;
        eventRegistry.put(eventId, eventData);

        timeEventManager.schedule(cancellationEvent);
        logger.log(TAG, "Scheduled cancellation notice for appointment " + appointment.getDate() + " " + appointment.getTime() +
                " at " + noticeTime);
    }

    /**
     * Sends an immediate unavailability notice (doctor can't make it on short notice).
     */
    public void sendDoctorUnavailableNotice(Appointment appointment, String reason) {
        if (!isNotificationsEnabled()) {
            return;
        }

        String title = "Doctor Unavailable";
        String message = String.format(
                "Dr. %s is unable to attend your appointment on %s at %s.\n%s\nPlease contact the office to reschedule.",
                appointment.getDoctor(),
                appointment.getDate(),
                appointment.getTime(),
                reason != null && !reason.isEmpty() ? "Reason: " + reason : "Please reschedule at your earliest convenience."
        );

        Notification notification = new Notification(title, message, nowMillis());
        notificationManager.send(notification);
        logger.log(TAG, "Sent doctor unavailable notice for appointment " + appointment.getDate() + " " + appointment.getTime());
    }

    /**
     * Sends a rescheduling suggestion notification.
     */
    public void sendRescheduleNotification(Appointment appointment, String suggestedDate, String suggestedTime) {
        if (!isNotificationsEnabled()) {
            return;
        }

        if (featureManager != null && !featureManager.isFeatureActive("NotifyOnReschedule")) {
            return;
        }

        String title = "Reschedule Appointment";
        String message = String.format(
                "Your appointment with %s on %s at %s needs to be rescheduled.\nSuggested new time: %s at %s\nPlease confirm or contact the office.",
                appointment.getDoctor(),
                appointment.getDate(),
                appointment.getTime(),
                suggestedDate,
                suggestedTime
        );

        Notification notification = new Notification(title, message, nowMillis());
        notificationManager.send(notification);
        logger.log(TAG, "Sent reschedule notification for appointment " + appointment.getDate() + " " + appointment.getTime());
    }

    private enum EventType {
        REMINDER,
        CANCELLATION
    }

    private static class AppointmentEventData {
        EventType eventType;
        Appointment appointment;
        int hoursBeforeAppointment;
        String cancelledBy;
        String reason;
    }
}
