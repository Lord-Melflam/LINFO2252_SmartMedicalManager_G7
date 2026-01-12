package com.mycompany.model;

/**
 * Observer interface for patient changes.
 * Implements Observer pattern for MVC separation.
 */
public interface PatientObserver {
    /**
     * Called when the current patient is changed.
     */
    void onPatientChanged(PatientManager.Patient patient);
    
    /**
     * Called when patient information is updated.
     */
    void onPatientUpdated(PatientManager.Patient patient);
}
