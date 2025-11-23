package Model;
// TimeEventSystem.java

import Logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TimeEventSystem {
    private static TimeEventSystem instance;
    private final List<TimeEventListener> listeners = new ArrayList<>();
    private final Random rng = new Random();
    private int currentDay = 0;

    private Logger logger = Logger.getInstance();

    private TimeEventSystem() {
    }

    public static TimeEventSystem getInstance() {
        if (instance == null) {
            synchronized (TimeEventSystem.class) {
                if (instance == null) instance = new TimeEventSystem();
            }
        }
        return instance;
    }

    public synchronized void registerListener(TimeEventListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public synchronized void unregisterListener(TimeEventListener l) {
        listeners.remove(l);
    }

    public int getCurrentDay() {
        return currentDay;
    }

    /**
     * Advance time by given number of days. For each day advanced we:
     * - increment currentDay
     * - notify DAY_PASSED with daysAdvanced = 1
     * - possibly generate random events (doctor unavailable, user ill)
     * After multiple days we also emit WEEK_PASSED if 7 days passed in a single call.
     */
    public void advanceDays(int days) {
        if (days <= 0) return;
        for (int i = 0; i < days; i++) {
            synchronized (this) {
                currentDay++;
            }
            notifyListeners(TimeEvent.DAY_PASSED, 1);

            if (rng.nextDouble() < 0.10) notifyListeners(TimeEvent.DOCTOR_UNAVAILABLE, 0);
            if (rng.nextDouble() < 0.06) notifyListeners(TimeEvent.USER_ILL, 0);
        }
        if (days >= 7) {
            notifyListeners(TimeEvent.WEEK_PASSED, days / 7);
        }
    }

    public void advanceWeek() {
        advanceDays(7);
    }

    /**
     * Manually trigger a named event (DOCTOR_UNAVAILABLE, USER_ILL, etc.)
     */
    public void triggerEvent(TimeEvent event) {
        notifyListeners(event, 0);
    }

    private void notifyListeners(TimeEvent event, int daysAdvanced) {
        List<TimeEventListener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(listeners);
        }
        for (TimeEventListener l : snapshot) {
            try {
                l.onTimeEvent(event, daysAdvanced);
            } catch (Exception e) {
                logger.log("TES", "listener error: " + e.getMessage());
            }
        }
    }
}
