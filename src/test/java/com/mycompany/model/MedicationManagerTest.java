package com.mycompany.model;

import com.mycompany.testsupport.SingletonReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicationManagerTest {

    @BeforeEach
    void reset() {
        SingletonReset.resetSingleton(NotificationManager.class);
        SingletonReset.resetSingleton(TimeEventManager.class);
        SingletonReset.resetSingleton(FeatureManager.class);
        SingletonReset.resetSingleton(MedicationManager.class);
    }

    @Test
    void dailyReminderFiresOnlyWhenEnabled() throws Exception {
        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("11-01-2026 08:59"));

        FeatureManager fm = FeatureManager.getInstance();
        // Ensure enabled.
        fm.activateFeatures("Reminders", "MedicationReminders", "CurrentMedication", "Notification");

        List<String> received = new ArrayList<>();
        NotificationManager.getInstance().registerObserver(n -> received.add(n.getTitle()));

        MedicationManager.getInstance();

        tm.setDate(parse("11-01-2026 09:00"));
        assertTrue(received.contains("Medication Reminder"));

        // Disable notifications -> should not send even if event fires.
        received.clear();
        fm.deactivateFeatures("Notification");

        tm.setDate(parse("12-01-2026 09:00"));
        assertFalse(received.contains("Medication Reminder"));
    }

    private static Date parse(String s) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        fmt.setLenient(false);
        return fmt.parse(s);
    }
}
