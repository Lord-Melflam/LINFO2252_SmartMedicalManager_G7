package com.mycompany.model;

import com.mycompany.data.Appointment;
import com.mycompany.testsupport.SingletonReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentNotificationManagerTest {

    @BeforeEach
    void reset() {
        SingletonReset.resetSingleton(NotificationManager.class);
        SingletonReset.resetSingleton(TimeEventManager.class);
        SingletonReset.resetSingleton(FeatureManager.class);
        SingletonReset.resetSingleton(AppointmentNotificationManager.class);
    }

    @Test
    void scheduleAppointmentReminder_registersUpcomingReminderAppointment() throws Exception {
        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("10-01-2026 10:00"));

        FeatureManager fm = FeatureManager.getInstance();
        fm.activateFeatures("Reminders", "AppointmentReminders");

        AppointmentNotificationManager anm = AppointmentNotificationManager.getInstance();

        Appointment appointment = new Appointment("12-01-2026", "10:00", "Dr", "Loc", "Reason", "Scheduled");
        anm.scheduleAppointmentReminder(appointment, 24);

        assertEquals(1, anm.getUpcomingReminderAppointments(10).size());
    }

    @Test
    void scheduleAppointmentReminder_noopWhenAppointmentRemindersDisabled() throws Exception {
        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("10-01-2026 10:00"));

        FeatureManager fm = FeatureManager.getInstance();
        // Reminders enabled, but AppointmentReminders disabled.
        fm.activateFeatures("Reminders");
        fm.deactivateFeatures("AppointmentReminders");

        AppointmentNotificationManager anm = AppointmentNotificationManager.getInstance();
        Appointment appointment = new Appointment("12-01-2026", "10:00", "Dr", "Loc", "Reason", "Scheduled");
        anm.scheduleAppointmentReminder(appointment, 24);

        assertTrue(anm.getUpcomingReminderAppointments(10).isEmpty());
    }

    private static Date parse(String s) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        fmt.setLenient(false);
        return fmt.parse(s);
    }
}
