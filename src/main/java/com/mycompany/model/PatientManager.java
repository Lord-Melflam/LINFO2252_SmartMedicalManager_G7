package com.mycompany.model;

import java.util.*;

/**
 * Singleton manager for patient profile data.
 * Handles user information, medical history, and billing information.
 * Implements Observer pattern for MVC separation.
 */
public class PatientManager {
    private static PatientManager instance;
    
    private Patient currentPatient;
    private final List<PatientObserver> observers;
    
    private PatientManager() {
        this.observers = new ArrayList<>();
        this.currentPatient = createDefaultPatient();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static synchronized PatientManager getInstance() {
        if (instance == null) {
            instance = new PatientManager();
        }
        return instance;
    }
    
    /**
     * Creates a default patient with sample data.
     * TODO: Replace with actual patient loading/creation.
     */
    private Patient createDefaultPatient() {
        Patient patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setAge(31);
        patient.setSex("M");
        patient.setContactMethod("Email");
        patient.setInsuranceLevel(FeatureManager.InsuranceLevel.NORMAL);
        patient.setCurrentMedication("Aspirin 100mg daily");
        patient.setVaccines("COVID-19, Flu");
        
        // Medical history
        patient.addMedicalHistoryEntry("2025-12-20", "Checkup", "General consultation, all good");
        patient.addMedicalHistoryEntry("2025-11-15", "Lab Test", "Blood test results normal");
        
        return patient;
    }
    
    /**
     * Gets the current patient.
     */
    public Patient getCurrentPatient() {
        return currentPatient;
    }
    
    /**
     * Sets the current patient and notifies observers.
     */
    public synchronized void setCurrentPatient(Patient patient) {
        this.currentPatient = patient;
        notifyObserversPatientChanged(patient);
    }
    
    /**
     * Updates patient information and notifies observers.
     */
    public synchronized void updatePatient(String firstName, String lastName, int age, String sex) {
        currentPatient.setFirstName(firstName);
        currentPatient.setLastName(lastName);
        currentPatient.setAge(age);
        currentPatient.setSex(sex);
        notifyObserversPatientUpdated();
    }
    
    /**
     * Updates insurance level and notifies observers.
     */
    public synchronized void setInsuranceLevel(FeatureManager.InsuranceLevel level) {
        currentPatient.setInsuranceLevel(level);
        notifyObserversPatientUpdated();
    }
    
    /**
     * Adds a medical history entry.
     */
    public synchronized void addMedicalHistoryEntry(String date, String type, String notes) {
        currentPatient.addMedicalHistoryEntry(date, type, notes);
        notifyObserversPatientUpdated();
    }
    
    /**
     * Registers an observer for patient changes.
     */
    public void registerObserver(PatientObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /**
     * Unregisters an observer.
     */
    public void unregisterObserver(PatientObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifies observers of patient change.
     */
    private void notifyObserversPatientChanged(Patient patient) {
        for (PatientObserver observer : observers) {
            observer.onPatientChanged(patient);
        }
    }
    
    /**
     * Notifies observers of patient update.
     */
    private void notifyObserversPatientUpdated() {
        for (PatientObserver observer : observers) {
            observer.onPatientUpdated(currentPatient);
        }
    }
    
    /**
     * Inner class representing patient data.
     */
    public static class Patient {
        private String firstName;
        private String lastName;
        private int age;
        private String sex;
        private String contactMethod;
        private String currentMedication;
        private String vaccines;
        private FeatureManager.InsuranceLevel insuranceLevel;
        private final List<MedicalHistoryEntry> medicalHistory;
        private final Map<String, Object> attributes; // For feature extensions
        
        public Patient() {
            this.medicalHistory = new ArrayList<>();
            this.attributes = new HashMap<>();
        }
        
        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public String getSex() { return sex; }
        public void setSex(String sex) { this.sex = sex; }
        
        public String getContactMethod() { return contactMethod; }
        public void setContactMethod(String contactMethod) { this.contactMethod = contactMethod; }
        
        public String getCurrentMedication() { return currentMedication; }
        public void setCurrentMedication(String medication) { this.currentMedication = medication; }
        
        public String getVaccines() { return vaccines; }
        public void setVaccines(String vaccines) { this.vaccines = vaccines; }
        
        public FeatureManager.InsuranceLevel getInsuranceLevel() { return insuranceLevel; }
        public void setInsuranceLevel(FeatureManager.InsuranceLevel level) { this.insuranceLevel = level; }
        
        public List<MedicalHistoryEntry> getMedicalHistory() {
            return new ArrayList<>(medicalHistory);
        }
        
        public void addMedicalHistoryEntry(String date, String type, String notes) {
            medicalHistory.add(new MedicalHistoryEntry(date, type, notes));
        }
        
        public Object getAttribute(String key) { return attributes.get(key); }
        public void setAttribute(String key, Object value) { attributes.put(key, value); }
    }
    
    /**
     * Inner class representing a medical history entry.
     */
    public static class MedicalHistoryEntry {
        private final String date;
        private final String type;
        private final String notes;
        
        public MedicalHistoryEntry(String date, String type, String notes) {
            this.date = date;
            this.type = type;
            this.notes = notes;
        }
        
        public String getDate() { return date; }
        public String getType() { return type; }
        public String getNotes() { return notes; }
    }
}
