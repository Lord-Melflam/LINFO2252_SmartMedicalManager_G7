package com.mycompany.model;

import com.mycompany.data.Appointment;
import com.mycompany.testsupport.SingletonReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentManagerTest {

    @BeforeEach
    void reset() {
        SingletonReset.resetSingleton(TimeEventManager.class);
        SingletonReset.resetSingleton(AppointmentManager.class);
    }

    @Test
    void refreshStatuses_marksPastScheduledAsCompleted() throws Exception {
        // Important: AppointmentManager refreshes statuses when it receives time-change events.
        // So create it first (so it can register as observer), then advance simulated time.
        AppointmentManager am = AppointmentManager.getInstance();

        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("23-12-2025 00:00"));

        // Sample data includes a Scheduled appointment on 22-12-2025 10:00.
        Appointment target = am.getAllAppointments().stream()
            .filter(a -> "22-12-2025".equals(a.getDate()) && "10:00".equals(a.getTime()))
            .findFirst()
            .orElseThrow();

        assertEquals("Completed", target.getStatus());
    }

    @Test
    void canCancelAppointment_falseForCompletedOrCancelled() throws Exception {
        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("01-01-2025 00:00"));

        AppointmentManager am = AppointmentManager.getInstance();

        Appointment cancelled = am.getAllAppointments().stream()
            .filter(a -> "Cancelled".equalsIgnoreCase(a.getStatus()))
            .findFirst()
            .orElseThrow();

        assertFalse(am.canCancelAppointment(cancelled));
    }

    private static Date parse(String s) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        fmt.setLenient(false);
        return fmt.parse(s);
    }
}
