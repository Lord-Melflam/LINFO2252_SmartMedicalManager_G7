package com.mycompany.model;

import com.mycompany.data.TimeEvent;
import com.mycompany.testsupport.SingletonReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeEventManagerTest {

    @BeforeEach
    void reset() {
        SingletonReset.resetSingleton(TimeEventManager.class);
    }

    @Test
    void schedule_firesImmediatelyWhenDue() throws Exception {
        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("11-01-2026 10:00"));

        List<String> fired = new ArrayList<>();
        tm.registerListener(event -> fired.add(event.getId()));

        tm.schedule(new TimeEvent("past", parse("11-01-2026 09:59"), "past"));
        tm.schedule(new TimeEvent("future", parse("11-01-2026 10:01"), "future"));

        assertEquals(List.of("past"), fired);

        tm.setDate(parse("11-01-2026 10:01"));
        assertEquals(List.of("past", "future"), fired);
    }

    @Test
    void cancelScheduledEvent_preventsFiring() throws Exception {
        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("11-01-2026 10:00"));

        List<String> fired = new ArrayList<>();
        tm.registerListener(event -> fired.add(event.getId()));

        tm.schedule(new TimeEvent("toCancel", parse("11-01-2026 10:05"), "x"));
        assertTrue(tm.cancelScheduledEvent("toCancel"));

        tm.setDate(parse("11-01-2026 10:06"));
        assertTrue(fired.isEmpty());
    }

    @Test
    void rewind_allowsPreviouslyFiredEventToFireAgain() throws Exception {
        TimeEventManager tm = TimeEventManager.getInstance();
        tm.setDate(parse("11-01-2026 10:00"));

        List<String> fired = new ArrayList<>();
        tm.registerListener(event -> fired.add(event.getId()));

        tm.schedule(new TimeEvent("e", parse("11-01-2026 10:01"), "x"));

        tm.setDate(parse("11-01-2026 10:02"));
        assertEquals(List.of("e"), fired);

        tm.setDate(parse("11-01-2026 10:00"));
        tm.setDate(parse("11-01-2026 10:02"));

        assertEquals(List.of("e", "e"), fired);
    }

    private static Date parse(String s) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        fmt.setLenient(false);
        return fmt.parse(s);
    }
}
