package com.mycompany.ui.components;

import javax.swing.*;

/**
 * Simple time picker for appointment booking.
 * Provides hour and minute selection via spinners.
 */
public class TimePickerPanel extends JPanel {
    private final JSpinner hourSpinner;
    private final JSpinner minuteSpinner;
    
    public TimePickerPanel() {
        this.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        
        // Hour spinner (0-23)
        hourSpinner = new JSpinner(new SpinnerNumberModel(9, 0, 23, 1));
        hourSpinner.setPreferredSize(new java.awt.Dimension(50, 24));
        
        // Minute spinner (0-59, increment by 15)
        minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 59, 15));
        minuteSpinner.setPreferredSize(new java.awt.Dimension(50, 24));
        
        // Add components
        this.add(new JLabel("Time:"));
        this.add(hourSpinner);
        this.add(new JLabel(":"));
        this.add(minuteSpinner);
    }
    
    /**
     * Gets the selected time in HH:mm format.
     */
    public String getSelectedTime() {
        int hour = (Integer) hourSpinner.getValue();
        int minute = (Integer) minuteSpinner.getValue();
        return String.format("%02d:%02d", hour, minute);
    }
    
    /**
     * Sets the time (expects HH:mm format).
     */
    public void setSelectedTime(String time) {
        if (time != null && time.contains(":")) {
            try {
                String[] parts = time.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                hourSpinner.setValue(hour);
                minuteSpinner.setValue(minute);
            } catch (NumberFormatException e) {
                // Keep defaults
            }
        }
    }
}
