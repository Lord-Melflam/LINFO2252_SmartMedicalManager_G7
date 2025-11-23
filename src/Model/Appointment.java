package Model;

// Appointment.java
public class Appointment {
    private final int day;
    private final String patient;
    private boolean cancelled = false;
    private boolean isHistory = false;
    private String result = "";

    public Appointment(int day, String patient) {
        this.day = day;
        this.patient = patient;
    }

    @Override
    public String toString() {
        return "Appointment{day=" + day + ", patient=" + patient
                + (cancelled ? ", CANCELLED" : "") + (isHistory ? ", HISTORY" : "") + "}";
    }

    public int getDay() {
        return day;
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
