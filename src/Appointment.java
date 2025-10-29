// Appointment.java
public class Appointment {
    public final int day;
    public final String patient;
    public boolean cancelled = false;
    public boolean isHistory = false;
    public String result = "";

    public Appointment(int day, String patient) {
        this.day = day;
        this.patient = patient;
    }

    @Override
    public String toString() {
        return "Appointment{day=" + day + ", patient=" + patient
                + (cancelled ? ", CANCELLED" : "") + (isHistory ? ", HISTORY" : "") + "}";
    }
}
