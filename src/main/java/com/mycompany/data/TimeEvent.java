package com.mycompany.data;

import java.util.Date;

/**
 * Represents a scheduled event with a specific time.
 */
public class TimeEvent implements Comparable<TimeEvent> {
    private final String id;
    private final Date scheduledAt;
    private final String description;

    public TimeEvent(String id, Date scheduledAt, String description) {
        this.id = id;
        this.scheduledAt = scheduledAt;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public Date getScheduledAt() {
        return scheduledAt;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int compareTo(TimeEvent other) {
        return this.scheduledAt.compareTo(other.scheduledAt);
    }
}
