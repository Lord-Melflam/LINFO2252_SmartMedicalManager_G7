package com.mycompany.model;

import com.mycompany.ui.model.Appointment;
import java.util.*;

/**
 * Singleton manager for all appointment operations.
 * Handles creation, modification, cancellation, and querying of appointments.
 * Implements Observer pattern to notify Views of changes.
 */
public class AppointmentManager {
    private static AppointmentManager instance;
    
    private final List<Appointment> allAppointments;
    private final List<AppointmentObserver> observers;
    
    private AppointmentManager() {
        this.allAppointments = new ArrayList<>();
        this.observers = new ArrayList<>();
        initializeSampleData();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized AppointmentManager getInstance() {
        if (instance == null) {
            instance = new AppointmentManager();
        }
        return instance;
    }
    
    /**
     * Initialize with sample data.
     * TODO: Replace with database/file loading in production.
     */
    private void initializeSampleData() {
        addAppointment(new Appointment(
            "22-12-2025", "10:00", "Dr. Angst", "Hospital Dav", "Stomach pain", "Confirmed",
            new HashMap<>(Map.of(
                "consultationType", "General Consultation",
                "price", "100 EUR",
                "paymentMethod", "Card"
            ))
        ));
        addAppointment(new Appointment(
            "06-01-2025", "14:30", "Dr. Stuckov", "Hospital Helen", "Vaccine", "Cancelled",
            new HashMap<>(Map.of(
                "consultationType", "Preventive",
                "price", "50 EUR",
                "paymentMethod", "Cash"
            ))
        ));
        addAppointment(new Appointment(
            "15-01-2025", "11:00", "Dr. Smith", "Dental Clinic", "Checkup", "Scheduled",
            new HashMap<>(Map.of(
                "consultationType", "Dental",
                "price", "75 EUR",
                "paymentMethod", "Insurance"
            ))
        ));
    }
    
    /**
     * Adds an appointment and notifies observers.
     */
    public synchronized void addAppointment(Appointment appointment) {
        allAppointments.add(appointment);
        notifyObserversAppointmentAdded(appointment);
    }
    
    /**
     * Removes an appointment and notifies observers.
     */
    public synchronized boolean removeAppointment(Appointment appointment) {
        boolean removed = allAppointments.remove(appointment);
        if (removed) {
            notifyObserversAppointmentRemoved(appointment);
        }
        return removed;
    }
    
    /**
     * Updates an appointment and notifies observers.
     */
    public synchronized void updateAppointment(Appointment appointment) {
        // Remove and re-add to ensure list consistency
        int index = allAppointments.indexOf(appointment);
        if (index >= 0) {
            allAppointments.set(index, appointment);
            notifyObserversAppointmentUpdated(appointment);
        }
    }
    
    /**
     * Gets all appointments.
     */
    public List<Appointment> getAllAppointments() {
        return new ArrayList<>(allAppointments);
    }
    
    /**
     * Gets appointments by status.
     */
    public List<Appointment> getAppointmentsByStatus(String status) {
        return allAppointments.stream()
            .filter(a -> a.getStatus().equalsIgnoreCase(status))
            .toList();
    }
    
    /**
     * Gets upcoming appointments using current system date.
     * Filters appointments that are scheduled and occur on or after today.
     */
    public List<Appointment> getUpcomingAppointments() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
        
        return allAppointments.stream()
            .filter(a -> a.getStatus().equalsIgnoreCase("Scheduled"))
            .filter(a -> {
                try {
                    java.time.LocalDate aptDate = java.time.LocalDate.parse(a.getDate(), formatter);
                    return !aptDate.isBefore(today); // Today or later
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted((a, b) -> {
                try {
                    java.time.LocalDate dateA = java.time.LocalDate.parse(a.getDate(), formatter);
                    java.time.LocalDate dateB = java.time.LocalDate.parse(b.getDate(), formatter);
                    return dateA.compareTo(dateB);
                } catch (Exception e) {
                    return 0;
                }
            })
            .toList();
    }
    
    /**
     * Gets past appointments using current system date.
     * Filters appointments that are completed or scheduled before today.
     */
    public List<Appointment> getPastAppointments() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
        
        return allAppointments.stream()
            .filter(a -> a.getStatus().equalsIgnoreCase("Completed") || 
                   (a.getStatus().equalsIgnoreCase("Scheduled") && 
                    java.time.LocalDate.parse(a.getDate(), formatter).isBefore(today)))
            .sorted((a, b) -> {
                try {
                    java.time.LocalDate dateA = java.time.LocalDate.parse(a.getDate(), formatter);
                    java.time.LocalDate dateB = java.time.LocalDate.parse(b.getDate(), formatter);
                    return dateB.compareTo(dateA); // Reverse order (most recent first)
                } catch (Exception e) {
                    return 0;
                }
            })
            .toList();
    }
    
    /**
     * Cancels an appointment by changing its status.
     */
    public synchronized boolean cancelAppointment(Appointment appointment) {
        appointment.setStatus("Cancelled");
        updateAppointment(appointment);
        return true;
    }
    
    /**
     * Registers an observer for appointment changes.
     */
    public void registerObserver(AppointmentObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /**
     * Unregisters an observer.
     */
    public void unregisterObserver(AppointmentObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifies all observers of a new appointment.
     */
    private void notifyObserversAppointmentAdded(Appointment appointment) {
        for (AppointmentObserver observer : observers) {
            observer.onAppointmentAdded(appointment);
        }
    }
    
    /**
     * Notifies all observers of appointment removal.
     */
    private void notifyObserversAppointmentRemoved(Appointment appointment) {
        for (AppointmentObserver observer : observers) {
            observer.onAppointmentRemoved(appointment);
        }
    }
    
    /**
     * Notifies all observers of appointment update.
     */
    private void notifyObserversAppointmentUpdated(Appointment appointment) {
        for (AppointmentObserver observer : observers) {
            observer.onAppointmentUpdated(appointment);
        }
    }
    
    /**
     * Gets total number of appointments.
     */
    public int getTotalAppointmentCount() {
        return allAppointments.size();
    }
    
    /**
     * Finds an appointment by its reference.
     */
    public Appointment findAppointment(Appointment appointment) {
        for (Appointment a : allAppointments) {
            if (a.getDate().equals(appointment.getDate()) && 
                a.getTime().equals(appointment.getTime()) &&
                a.getDoctor().equals(appointment.getDoctor())) {
                return a;
            }
        }
        return null;
    }
}
