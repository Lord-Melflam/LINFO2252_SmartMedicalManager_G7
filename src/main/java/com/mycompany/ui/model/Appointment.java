package com.mycompany.ui.model;

/**
 * Model class representing a medical appointment.
 * Follows JavaBean conventions for data encapsulation.
 */
public class Appointment {
    private String date;
    private String doctor;
    private String location;
    private String reason;
    private String status;

    public Appointment(String date, String doctor, String location, String reason, String status) {
        this.date = date;
        this.doctor = doctor;
        this.location = location;
        this.reason = reason;
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Checks if this appointment matches the given search text.
     * Searches across all fields in a case-insensitive manner.
     *
     * @param searchText the text to search for
     * @return true if any field contains the search text
     */
    public boolean matches(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }
        
        String lowerSearch = searchText.toLowerCase();
        return (date != null && date.toLowerCase().contains(lowerSearch)) ||
               (doctor != null && doctor.toLowerCase().contains(lowerSearch)) ||
               (location != null && location.toLowerCase().contains(lowerSearch)) ||
               (reason != null && reason.toLowerCase().contains(lowerSearch)) ||
               (status != null && status.toLowerCase().contains(lowerSearch));
    }

    /**
     * Converts appointment to object array for table display.
     *
     * @return array of appointment data
     */
    public Object[] toArray() {
        return new Object[]{date, doctor, location, reason, status};
    }
}
