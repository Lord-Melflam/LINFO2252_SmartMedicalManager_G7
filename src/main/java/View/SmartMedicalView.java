package View;
// SmartMedicalView.java

import Logger.Logger;

import javax.swing.*;
import java.awt.*;
import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.Theme;

public class SmartMedicalView {
    private static final Logger logger = Logger.getInstance();
    private static SmartMedicalView instance;
    private final MainFrame mainFrame = new MainFrame();

    public SmartMedicalView() {
        createForm();
    }

    public static SmartMedicalView getInstance() {
        if (instance == null) {
            synchronized (SmartMedicalView.class) {
                if (instance == null) instance = new SmartMedicalView();
            }
        }
        return instance;
    }

    private void createForm() {
        SwingUtilities.invokeLater(LafManager::install);

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                // System.out.println(info.getName());
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log("View", "Failed to set Look and Feel: " + ex.getMessage());
        }

        /* Create and display the form */
        SwingUtilities.invokeLater(() -> {
            LafManager.install();
            mainFrame.setVisible(true);
            for (Theme theme : LafManager.getRegisteredThemes()) {
                System.out.println("Available theme: " + theme.getName());
            }
        });
    }

    public void show() {
        SwingUtilities.invokeLater(() -> mainFrame.setVisible(true));
        logger.log("View", "Displaying the Smart Medical Appointment Manager UI.");
    }

    public void hide() {
        SwingUtilities.invokeLater(() -> mainFrame.setVisible(false));
        logger.log("View", "Hiding the UI (System running in headless/testing mode).");
    }

    public void setTitle(String title) {
        SwingUtilities.invokeLater(() -> mainFrame.setTitle(title));
        this.updateUI();
    }

    public void setDarkMode(boolean isDark) {
        logger.log("View", "Setting Dark Mode to " + isDark);


        //panel.setBackground(isDark ? Color.BLACK : Color.WHITE);
        //darkModeButton.setBackground(isDark ? Color.BLACK : Color.WHITE);
        this.updateUI();
    }

    private void updateUI() {
        //panel.repaint();
        //panel.revalidate();
        mainFrame.repaint();
        mainFrame.revalidate();
    }

    public void updateDisplay(String[] stateLog) {
        logger.log("View", "UPDATING DISPLAY based on new configuration:");
        for (String log : stateLog) {
            logger.log("View", "  " + log);
        }
    }
}