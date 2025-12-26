package com.mycompany.ui.components;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Appointment filter panel for filtering by date range and status.
 * Provides UI for "Today", "This Week", "All Upcoming", "Past Appointments" filters.
 */
public class AppointmentFilterPanel extends JPanel {
    private final JComboBox<String> filterComboBox;
    private FilterChangedListener filterListener;
    
    public interface FilterChangedListener {
        void onFilterChanged(AppointmentFilter filter);
    }
    
    public static class AppointmentFilter {
        public enum FilterType {
            TODAY, THIS_WEEK, ALL_UPCOMING, PAST, ALL
        }
        
        private FilterType type;
        private LocalDate startDate;
        private LocalDate endDate;
        
        public AppointmentFilter(FilterType type) {
            this.type = type;
            calculateDateRange();
        }
        
        private void calculateDateRange() {
            LocalDate now = LocalDate.now();
            switch (type) {
                case TODAY:
                    startDate = now;
                    endDate = now;
                    break;
                case THIS_WEEK:
                    startDate = now;
                    endDate = now.plusDays(7);
                    break;
                case ALL_UPCOMING:
                    startDate = now;
                    endDate = now.plusYears(1);
                    break;
                case PAST:
                    startDate = now.minusYears(1);
                    endDate = now;
                    break;
                case ALL:
                    startDate = null;
                    endDate = null;
                    break;
            }
        }
        
        public boolean matches(String appointmentDateStr) {
            if (type == FilterType.ALL) return true;
            
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDate appointmentDate = LocalDate.parse(appointmentDateStr, formatter);
                return !appointmentDate.isBefore(startDate) && !appointmentDate.isAfter(endDate);
            } catch (Exception e) {
                return false;
            }
        }
        
        public FilterType getType() { return type; }
    }
    
    public AppointmentFilterPanel() {
        this.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        
        // Create filter dropdown
        String[] filterOptions = {"All Appointments", "Today", "This Week", "All Upcoming", "Past Appointments"};
        filterComboBox = new JComboBox<>(filterOptions);
        filterComboBox.addActionListener(e -> notifyFilterChanged());
        
        this.add(new JLabel("Filter:"));
        this.add(filterComboBox);
    }
    
    /**
     * Gets the currently selected filter.
     */
    public AppointmentFilter getSelectedFilter() {
        int selectedIndex = filterComboBox.getSelectedIndex();
        return switch (selectedIndex) {
            case 1 -> new AppointmentFilter(AppointmentFilter.FilterType.TODAY);
            case 2 -> new AppointmentFilter(AppointmentFilter.FilterType.THIS_WEEK);
            case 3 -> new AppointmentFilter(AppointmentFilter.FilterType.ALL_UPCOMING);
            case 4 -> new AppointmentFilter(AppointmentFilter.FilterType.PAST);
            default -> new AppointmentFilter(AppointmentFilter.FilterType.ALL);
        };
    }
    
    /**
     * Sets the filter change listener.
     */
    public void setFilterChangeListener(FilterChangedListener listener) {
        this.filterListener = listener;
    }
    
    /**
     * Notifies listener of filter change.
     */
    private void notifyFilterChanged() {
        if (filterListener != null) {
            filterListener.onFilterChanged(getSelectedFilter());
        }
    }
}
