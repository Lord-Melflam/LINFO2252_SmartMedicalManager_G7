package com.mycompany.ui.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a medical appointment.
 * Follows JavaBean conventions for data encapsulation.
 * Supports flexible attributes for feature-oriented extensions.
 */
public class Appointment {
    private String date;
    private String time;  // NEW: Time slot (HH:mm format)
    private String doctor;
    private String location;
    private String reason;
    private String status;
    private final Map<String, Object> attributes;

    /**
     * Basic constructor with core appointment information.
     */
    public Appointment(String date, String doctor, String location, String reason, String status) {
        this(date, "09:00", doctor, location, reason, status);
    }
    
    /**
     * Constructor with time slot.
     */
    public Appointment(String date, String time, String doctor, String location, String reason, String status) {
        this.date = date;
        this.time = time;
        this.doctor = doctor;
        this.location = location;
        this.reason = reason;
        this.status = status;
        this.attributes = new HashMap<>();
    }

    /**
     * Extended constructor supporting flexible attributes.
     * Allows feature-oriented additions (price, payment method, etc.)
     */
    public Appointment(String date, String doctor, String location, String reason, String status, Map<String, Object> attributes) {
        this(date, doctor, location, reason, status);
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }
    
    /**
     * Extended constructor with time and attributes.
     */
    public Appointment(String date, String time, String doctor, String location, String reason, String status, Map<String, Object> attributes) {
        this(date, time, doctor, location, reason, status);
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
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
     * Gets an attribute value by key.
     * Used for feature-oriented extensions (price, payment method, etc.)
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Sets an attribute value.
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Checks if an attribute exists.
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
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
               (time != null && time.toLowerCase().contains(lowerSearch)) ||
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
        return new Object[]{date, time, doctor, location, reason, status};
    }
}
