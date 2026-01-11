package com.mycompany.model;

import java.util.Date;
import java.time.Duration;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mycompany.data.TimeEvent;

public class TimeEventManager {
    private final PriorityQueue<TimeEvent> queue = new PriorityQueue<>();


    private final PriorityQueue<TimeEvent> firedEvents =
        new PriorityQueue<>((a, b) -> b.getScheduledAt().compareTo(a.getScheduledAt()));

    private final List<TimeEventObserver> listeners = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getInstance();
    private Date currentDate = new Date(System.currentTimeMillis());

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

    public synchronized void setDate(Date newDate) {
        Date oldDate = this.currentDate;

        this.currentDate = (newDate == null) ? new Date(System.currentTimeMillis()) : new Date(newDate.getTime());
        logger.log("TimeEventManager", "Current date set to " + this.currentDate);

        if (this.currentDate.before(oldDate)) {
            rewindFiredEventsTo(this.currentDate);
        }

        processDueEvents();
    }

    public synchronized void advance(Duration duration) {
        if (duration == null) return;

        currentDate = new Date(currentDate.getTime() + duration.toMillis());
        logger.log("TimeEventManager", "Advanced time by " + duration + " to " + currentDate);
        processDueEvents();
    }

    public synchronized void schedule(TimeEvent event) {
        queue.offer(event);
        logger.log("TimeEventManager", "Scheduled event '" + event.getId() + "' at " + event.getScheduledAt());
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

    private void rewindFiredEventsTo(Date newNow) {
        while (!firedEvents.isEmpty() && firedEvents.peek().getScheduledAt().after(newNow)) {
            queue.offer(firedEvents.poll());
        }
        logger.log("TimeEventManager", "Rewound fired events to align with new date " + newNow);
    }

    private synchronized void processDueEvents() {
        while (!queue.isEmpty() && !queue.peek().getScheduledAt().after(currentDate)) {
            TimeEvent event = queue.poll();

            firedEvents.offer(event);

            logger.log("TimeEventManager", "Firing event '" + event.getId() + "': " + event.getDescription());
            for (TimeEventObserver listener : listeners) {
                listener.onTimeEvent(event);
            }
        }
    }
}
