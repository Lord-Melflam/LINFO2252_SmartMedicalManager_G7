package com.mycompany.ui.components;

import javax.swing.*;

import com.mycompany.model.TimeEventManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Appointment filter panel for filtering by date range and status.
 * Provides UI for "Today", "This Week", "All Upcoming", "Past Appointments" filters.
 */
public class AppointmentFilterPanel extends JPanel {
    private final JComboBox<String> filterComboBox;
    private FilterChangedListener filterListener;
    private final static TimeEventManager timeEventManager = TimeEventManager.getInstance();
    
    public interface FilterChangedListener {
        void onFilterChanged(AppointmentFilter filter);
    }
    
    public static class AppointmentFilter {
        public enum FilterType {
            TODAY, THIS_WEEK, ALL_UPCOMING, PAST, ALL
        }
        
        private final FilterType type;
        private Date startDate;
        private Date endDate;
        
        public AppointmentFilter(FilterType type) {
            this.type = type;
            calculateDateRange();
        }
        
        private static Date atStartOfDay(Date d) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        }
        
        private static Date atEndOfDay(Date d) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTime();
        }
        
        private static Date addDays(Date d, int days) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.add(Calendar.DAY_OF_YEAR, days);
            return cal.getTime();
        }
        
        private static Date addYears(Date d, int years) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.add(Calendar.YEAR, years);
            return cal.getTime();
        }
        
        private void calculateDateRange() {
            Date now = timeEventManager.getDate(); 
            Date todayStart = atStartOfDay(now);
            Date todayEnd = atEndOfDay(now);
            
            switch (type) {
                case TODAY:
                    startDate = todayStart;
                    endDate = todayEnd;
                    break;
                case THIS_WEEK:
                    startDate = todayStart;
                    endDate = atEndOfDay(addDays(todayStart, 7));
                    break;
                case ALL_UPCOMING:
                    startDate = todayStart;
                    endDate = atEndOfDay(addYears(todayStart, 1));
                    break;
                case PAST:
                    startDate = atStartOfDay(addYears(todayStart, -1));
                    endDate = todayEnd;
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
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                sdf.setLenient(false);
                
                Date apptDay = sdf.parse(appointmentDateStr);
                Date apptStart = atStartOfDay(apptDay);
                Date apptEnd = atEndOfDay(apptDay);
                
                return !apptEnd.before(startDate) && !apptStart.after(endDate); // overlap with range
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
