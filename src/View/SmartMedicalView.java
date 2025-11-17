package View;
// SmartMedicalView.java
import java.awt.Color;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import Logger.Logger;   

public class SmartMedicalView {
    private static SmartMedicalView instance;
    private static final Logger logger = Logger.getInstance();
    
    private final JFrame frame;
    private final JPanel panel;
    private final JButton darkModeButton;
    private final JTextField inputField;

    public SmartMedicalView() {
        // --- UI Setup ---
        frame = new JFrame("Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        frame.add(panel);
        frame.setSize(800, 600);

        darkModeButton = new JButton("Dynamic click me");
        darkModeButton.addActionListener((finalactionEvent) -> {
            logger.log("View", "Dynamic button clicked.");
        });
        frame.add(darkModeButton);

        inputField = new JTextField(16);
        panel.add(inputField);
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