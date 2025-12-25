package com.mycompany.ui.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom table model for appointments with built-in filtering capability.
 * Separates data management logic from UI presentation.
 */
public class AppointmentTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {"Date", "Doctor", "Location", "Reason", "Status"};
    private static final Class<?>[] COLUMN_TYPES = {String.class, String.class, String.class, String.class, String.class};
    
    private final List<Appointment> allAppointments;
    private final List<Appointment> filteredAppointments;
    private String currentFilter = "";

    public AppointmentTableModel() {
        this.allAppointments = new ArrayList<>();
        this.filteredAppointments = new ArrayList<>();
    }

    /**
     * Adds an appointment to the model.
     *
     * @param appointment the appointment to add
     */
    public void addAppointment(Appointment appointment) {
        allAppointments.add(appointment);
        applyFilter(currentFilter);
    }

    /**
     * Removes an appointment at the specified index in the filtered view.
     *
     * @param rowIndex the row index in the filtered view
     */
    public void removeAppointment(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < filteredAppointments.size()) {
            Appointment appointment = filteredAppointments.get(rowIndex);
            allAppointments.remove(appointment);
            applyFilter(currentFilter);
        }
    }

    /**
     * Gets the appointment at the specified row in the filtered view.
     *
     * @param rowIndex the row index
     * @return the appointment at that row
     */
    public Appointment getAppointmentAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < filteredAppointments.size()) {
            return filteredAppointments.get(rowIndex);
        }
        return null;
    }

    /**
     * Applies a filter to the appointments.
     * Updates the filtered list and notifies listeners.
     *
     * @param filterText the text to filter by (case-insensitive, searches all fields)
     */
    public void applyFilter(String filterText) {
        this.currentFilter = filterText == null ? "" : filterText.trim();
        filteredAppointments.clear();
        
        for (Appointment appointment : allAppointments) {
            if (appointment.matches(currentFilter)) {
                filteredAppointments.add(appointment);
            }
        }
        
        fireTableDataChanged();
    }

    /**
     * Clears the current filter, showing all appointments.
     */
    public void clearFilter() {
        applyFilter("");
    }

    /**
     * Clears all appointments from the model.
     */
    public void clear() {
        allAppointments.clear();
        filteredAppointments.clear();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return filteredAppointments.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return COLUMN_TYPES[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Appointment appointment = filteredAppointments.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> appointment.getDate();
            case 1 -> appointment.getDoctor();
            case 2 -> appointment.getLocation();
            case 3 -> appointment.getReason();
            case 4 -> appointment.getStatus();
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // Make table read-only
    }

    /**
     * Gets the total number of appointments (including filtered out ones).
     *
     * @return total appointment count
     */
    public int getTotalAppointmentCount() {
        return allAppointments.size();
    }

    /**
     * Gets the number of visible (filtered) appointments.
     *
     * @return filtered appointment count
     */
    public int getFilteredAppointmentCount() {
        return filteredAppointments.size();
    }
}
