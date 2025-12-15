package Model;

// Appointment.java
import java.time.LocalDate;
import java.util.UUID;

public class Appointment {
    private LocalDate date; // preferred source-of-truth for scheduling
    private final UUID id; // immutable unique identifier
    private final String patient;
    private String staff;
    private boolean cancelled = false;
    private boolean isHistory = false;
    private String result = "";

    public Appointment(LocalDate date, String patient, String staff) {
        this.date = date;
        this.id = UUID.randomUUID();
        this.patient = patient;
        this.staff = staff;
    }

    public Appointment(Appointment a) {
        this.date = a.date;
        this.id = a.id;
        this.patient = a.patient;
        this.staff = a.staff;
        this.cancelled = a.cancelled;
        this.isHistory = a.isHistory;
        this.result = a.result;
    }

    @Override
    public String toString() {
        return "Appointment{id=" + id + ", date=" + date + ", patient=" + patient + ", staff=" + staff
                + (cancelled ? ", CANCELLED" : "") + (isHistory ? ", HISTORY" : "") + "}";
    }

    /**
     * Preferred: get the appointment date as a LocalDate.
     */
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate newDate) {
        date = newDate;
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

    public String getStaff() { return staff; }

    public void setStaff(String staff) { this.staff = staff; }

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
