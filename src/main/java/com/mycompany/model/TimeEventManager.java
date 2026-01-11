package com.mycompany.model;

import java.util.Date;
import java.util.Calendar;
import java.time.Duration;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mycompany.data.TimeEvent;

public class TimeEventManager {
    private final PriorityQueue<TimeEvent> queue = new PriorityQueue<>();


    private final PriorityQueue<TimeEvent> firedEvents =
        new PriorityQueue<>((a, b) -> b.getScheduledAt().compareTo(a.getScheduledAt()));

    private final List<TimeEventObserver> listeners = new CopyOnWriteArrayList<>();
    private final List<TimeChangeObserver> timeChangeObservers = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getInstance();
    private Date currentDate = new Date(System.currentTimeMillis());
    private static final String DEFAULT_DATE_TIME_PATTERN = "dd-MM-yyyy HH:mm";
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_PATTERN);

    private static TimeEventManager instance;

    private TimeEventManager() {
    }

    public static synchronized TimeEventManager getInstance() {
        if (instance == null) {
            instance = new TimeEventManager();
        }
        return instance;
    }

    public synchronized Date getDate() {
        return new Date(currentDate.getTime()); 
    }

    public synchronized Date getCurrentDate() {
        return getDate();
    }

    /**
     * Formats the given date using the TimeEvent system's default display format.
     */
    public synchronized String format(Date date) {
        if (date == null) {
            return "";
        }
        return displayDateFormat.format(date);
    }

    /**
     * Formats a millisecond timestamp using the TimeEvent system's default display format.
     */
    public synchronized String formatMillis(long millis) {
        return format(new Date(millis));
    }

    /**
     * Returns the simulated "now" as a human-readable string.
     */
    public synchronized String nowString() {
        return format(currentDate);
    }

    public synchronized void setTime(int hour24, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDate);
        cal.set(Calendar.HOUR_OF_DAY, hour24);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        setDate(cal.getTime());
    }

    public synchronized void setDateTime(Date date, int hour24, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date == null ? currentDate : date);
        cal.set(Calendar.HOUR_OF_DAY, hour24);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        setDate(cal.getTime());
    }

    public synchronized void setDate(Date newDate) {
        Date oldDate = this.currentDate;

        this.currentDate = (newDate == null) ? new Date(System.currentTimeMillis()) : new Date(newDate.getTime());

        logger.log("TimeEventManager", "Current date set to " + nowString());

        if (this.currentDate.before(oldDate)) {
            rewindFiredEventsTo(this.currentDate);
        }

        notifyTimeChanged();
        processDueEvents();
    }

    public synchronized void advance(Duration duration) {
        if (duration == null) return;

        currentDate = new Date(currentDate.getTime() + duration.toMillis());
        logger.log("TimeEventManager", "Advanced time by " + duration + " to " + nowString());

        notifyTimeChanged();
        processDueEvents();
    }

    public synchronized void schedule(TimeEvent event) {
        queue.offer(event);
        logger.log("TimeEventManager", "Scheduled event '" + event.getId() + "' at " + format(event.getScheduledAt()));
        processDueEvents();
    }

    public synchronized void registerListener(TimeEventObserver listener) {
        listeners.add(listener);
        logger.log("TimeEventManager", "Listener registered.");
    }

    public synchronized void unregisterListener(TimeEventObserver listener) {
        listeners.remove(listener);
        logger.log("TimeEventManager", "Listener unregistered.");
    }

    public synchronized void registerTimeObserver(TimeChangeObserver observer) {
        if (observer != null) {
            timeChangeObservers.add(observer);
        }
    }

    public synchronized void unregisterTimeObserver(TimeChangeObserver observer) {
        timeChangeObservers.remove(observer);
    }

    private void notifyTimeChanged() {
        Date nowCopy = getDate();
        for (TimeChangeObserver observer : timeChangeObservers) {
            try {
                observer.onTimeChanged(nowCopy);
            } catch (Exception ignored) {
                // Don't let a broken observer take down the time system
            }
        }
    }

    private void rewindFiredEventsTo(Date newNow) {
        while (!firedEvents.isEmpty() && firedEvents.peek().getScheduledAt().after(newNow)) {
            queue.offer(firedEvents.poll());
        }
        logger.log("TimeEventManager", "Rewound fired events to align with new date " + format(newNow));
    }

    private synchronized void processDueEvents() {
        while (!queue.isEmpty() && !queue.peek().getScheduledAt().after(currentDate)) {
            TimeEvent event = queue.poll();

            firedEvents.offer(event);

            logger.log("TimeEventManager",
                "Firing event '" + event.getId() + "' at " + format(event.getScheduledAt()) + ": " + event.getDescription());
            for (TimeEventObserver listener : listeners) {
                listener.onTimeEvent(event);
            }
        }
    }
}
