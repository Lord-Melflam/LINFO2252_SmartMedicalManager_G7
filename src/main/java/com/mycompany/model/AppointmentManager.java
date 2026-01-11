package com.mycompany.model;

import com.mycompany.data.Appointment;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Singleton manager for all appointment operations.
 * Handles creation, modification, cancellation, and querying of appointments.
 * Implements Observer pattern to notify Views of changes.
 */
public class AppointmentManager implements TimeChangeObserver {
    private static AppointmentManager instance;
    private final TimeEventManager timeEventManager = TimeEventManager.getInstance();
    
    private final List<Appointment> allAppointments;
    private final List<AppointmentObserver> observers;
    
    private AppointmentManager() {
        this.allAppointments = new ArrayList<>();
        this.observers = new ArrayList<>();
        initializeSampleData();

        // Keep appointment statuses consistent with the simulated time.
        timeEventManager.registerTimeObserver(this);
    }

    @Override
    public void onTimeChanged(Date newNow) {
        refreshStatusesBasedOnNow(newNow);
    }

    public synchronized void refreshStatusesBasedOnNow(Date now) {
        if (now == null) {
            return;
        }

        java.util.List<Appointment> changed = new java.util.ArrayList<>();
        for (Appointment appointment : allAppointments) {
            if (appointment == null) continue;
            if (!"Scheduled".equalsIgnoreCase(appointment.getStatus())) continue;

            Date appointmentDate = appointment.getDateAsDate();
            if (appointmentDate != null && appointmentDate.before(now)) {
                appointment.setStatus("Completed");
                changed.add(appointment);
            }
        }

        for (Appointment appointment : changed) {
            notifyObserversAppointmentUpdated(appointment);
        }
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
     * TODO: Replace with db in prod!
     */
    private void initializeSampleData() {
        addAppointment(new Appointment(
            "22-12-2025", "10:00", "Dr. Angst", "Hospital Dav", "Stomach pain", "Scheduled",
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
     * Gets upcoming appointments using time-event "now".
     * Filters appointments that are scheduled and occur on or after now.
     */
    public List<Appointment> getUpcomingAppointments() {
        final java.util.Date now = timeEventManager.getDate();
        refreshStatusesBasedOnNow(now);

        return allAppointments.stream()
            .filter(a -> a.getStatus().equalsIgnoreCase("Scheduled"))
            .filter(a -> {
                Date apt = a.getDateAsDate();
                return apt != null && !apt.before(now);
            })
            .sorted((a, b) -> {
                Date da = a.getDateAsDate();
                Date db = b.getDateAsDate();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return da.compareTo(db);
            })
            .toList();
    }
    
    /**
     * Gets past appointments using time-event "now".
     * Filters appointments that are completed or scheduled before now.
     */
    public List<Appointment> getPastAppointments() {
        final java.util.Date now = timeEventManager.getDate();
        refreshStatusesBasedOnNow(now);

        return allAppointments.stream()
            .filter(a -> {
                if (a.getStatus().equalsIgnoreCase("Completed")) return true;
                if (!a.getStatus().equalsIgnoreCase("Scheduled")) return false;

                Date apt = a.getDateAsDate();
                return apt != null && apt.before(now);
            })
            .sorted((a, b) -> {
                Date da = a.getDateAsDate();
                Date db = b.getDateAsDate();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da); // most recent first
            })
            .toList();
    }
    
    /**
     * Cancels an appointment by changing its status.
     */
    public synchronized boolean cancelAppointment(Appointment appointment) {
        if (!canCancelAppointment(appointment)) {
            return false;
        }
        appointment.setStatus("Cancelled");
        updateAppointment(appointment);
        return true;
    }

    /**
     * Returns true if the appointment is eligible for cancellation.
     * Rule: only upcoming (date-time >= simulated now) and not already Cancelled/Completed.
     */
    public synchronized boolean canCancelAppointment(Appointment appointment) {
        if (appointment == null) {
            return false;
        }

        String status = appointment.getStatus();
        if (status == null) {
            return false;
        }

        if ("Cancelled".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            return false;
        }

        // Only allow cancelling upcoming appointments.
        Date now = timeEventManager.getDate();
        Date appointmentDate = appointment.getDateAsDate();
        if (appointmentDate == null || now == null) {
            return false;
        }

        return !appointmentDate.before(now);
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