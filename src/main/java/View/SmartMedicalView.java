package View;
// SmartMedicalView.java

import Logger.Logger;

import javax.swing.*;
import java.awt.*;

public class SmartMedicalView {
    private static final Logger logger = Logger.getInstance();
    private static SmartMedicalView instance;

    public SmartMedicalView() {
        MainFrame.main(null);
    }

    public static SmartMedicalView getInstance() {
        if (instance == null) {
            synchronized (SmartMedicalView.class) {
                if (instance == null) instance = new SmartMedicalView();
            }
        }
        return instance;
    }

    public void show() {
        frame.setVisible(true);
        logger.log("View", "Displaying the Smart Medical Appointment Manager UI.");
    }

    public void hide() {
        frame.setVisible(false);
        logger.log("View", "Hiding the UI (System running in headless/testing mode).");
    }

    public void setTitle(String title) {
        frame.setTitle(title);
        this.updateUI();
    }

    public void toggleDynamicButton(boolean isEnabled) {
        if (isEnabled) {
            if (darkModeButton.getParent() == null) {
                frame.add(darkModeButton);
            }
        } else {
            frame.remove(darkModeButton);
        }
        this.updateUI();
    }

    public void setDarkMode(boolean isDark) {
        logger.log("View", "Setting Dark Mode to " + isDark);
        panel.setBackground(isDark ? Color.BLACK : Color.WHITE);
        darkModeButton.setBackground(isDark ? Color.BLACK : Color.WHITE);
        this.updateUI();
    }

    private void updateUI() {
        panel.repaint();
        panel.revalidate();
    }

    public void updateDisplay(String[] stateLog) {
        logger.log("View", "UPDATING DISPLAY based on new configuration:");
        for (String log : stateLog) {
            logger.log("View", "  " + log);
        }
    }
}