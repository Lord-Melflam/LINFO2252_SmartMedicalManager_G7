package Model;

// Appointment.java
import java.time.LocalDate;
import java.util.UUID;

public class Appointment {
    private final int day; // legacy field (kept for compatibility)
    private final LocalDate date; // preferred source-of-truth for scheduling
    private final UUID id; // immutable unique identifier
    private final String patient;
    private boolean cancelled = false;
    private boolean isHistory = false;
    private String result = "";

    public Appointment(int day, String patient) {
        this.day = day;
        this.patient = patient;
        this.id = UUID.randomUUID();
        // Map the integer day into a LocalDate relative to the TES currentDay.
        // We assume that `LocalDate.now()` corresponds to the current TES day at runtime.
        int currentDay = TimeEventSystem.getInstance().getCurrentDay();
        this.date = LocalDate.now().plusDays(day - currentDay);
    }

    // New constructor to create Appointment directly from a LocalDate
    public Appointment(LocalDate date, String patient) {
        this.date = date;
        this.patient = patient;
        this.id = UUID.randomUUID();
        // compute legacy day relative to TES currentDay
        int currentDay = TimeEventSystem.getInstance().getCurrentDay();
        this.day = currentDay + (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    @Override
    public String toString() {
        return "Appointment{id=" + id + ", date=" + date + ", day=" + day + ", patient=" + patient
                + (cancelled ? ", CANCELLED" : "") + (isHistory ? ", HISTORY" : "") + "}";
    }

    public int getDay() {
        return day;
    }

    /**
     * Preferred: get the appointment date as a LocalDate.
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Unique identifier for this appointment.
     */
    public UUID getId() {
        return id;
    }

    public String getPatient() {
        return patient;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isHistory() {
        return isHistory;
    }

    public void setHistory(boolean history) {
        isHistory = history;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
