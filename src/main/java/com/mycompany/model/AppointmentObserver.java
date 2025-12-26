package com.mycompany.model;

import com.mycompany.ui.model.Appointment;

/**
 * Observer interface for appointment changes.
 * Implements Observer pattern for MVC separation.
 */
public interface AppointmentObserver {
    /**
     * Called when a new appointment is added.
     */
    void onAppointmentAdded(Appointment appointment);
    
    /**
     * Called when an appointment is removed.
     */
    void onAppointmentRemoved(Appointment appointment);
    
    /**
     * Called when an appointment is updated.
     */
    void onAppointmentUpdated(Appointment appointment);
}
