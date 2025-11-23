package View;
// SmartMedicalView.java

import Logger.Logger;

import javax.swing.*;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.Theme;

public class SmartMedicalView {
    private static final Logger logger = Logger.getInstance();
    private static SmartMedicalView instance;
    private MainFrame mainFrame = new MainFrame();
    private final Theme darkTheme = new DarculaTheme();
    private final Theme lightTheme = new IntelliJTheme();

    public SmartMedicalView() {
        SwingUtilities.invokeLater(this::createForm);
    }

    public static SmartMedicalView getInstance() {
        if (instance == null) {
            synchronized (SmartMedicalView.class) {
                if (instance == null) instance = new SmartMedicalView();
            }
        }
        return instance;
    }

    private void logAvailableThemes() {
        StringBuilder builder = new StringBuilder("Registered Themes:\n");
        for (Theme theme : LafManager.getRegisteredThemes()) {
            builder.append(" - ").append(theme.getName()).append("\n");
        }
        logger.log("View", builder.toString());
    }

    private void createForm() {
        logAvailableThemes();
        // Install default theme (dark mode)
        setDarkMode(true);

        // Do not call show() here; allow caller (controller.enableUIView)
        // to show the UI explicitly to avoid multiple windows during
        // circular initialization.
        setTitle("SmartMedicalView");
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            if (mainFrame == null) mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
        logger.log("View", "Displaying the Smart Medical Appointment Manager UI.");
    }

    public void hide() {
        SwingUtilities.invokeLater(() -> {
            if (mainFrame != null) mainFrame.setVisible(false);
        });
        logger.log("View", "Hiding the UI (System running in headless/testing mode).");
    }

    public void setTitle(String title) {
        SwingUtilities.invokeLater(() -> mainFrame.setTitle(title));
        logger.log("View", "Setting Title: " + title);
    }

    public void setDarkMode(boolean isDark) {
        SwingUtilities.invokeLater(() -> {
            if (isDark) {
                LafManager.setTheme(darkTheme);
            } else {
                LafManager.setTheme(lightTheme);
            }
            LafManager.install();
        });
        logger.log("View", "Setting Dark Mode to " + isDark);
    }

    private void updateUI() {
        //panel.repaint();
        //panel.revalidate();
        mainFrame.repaint();
        mainFrame.revalidate();
        logger.log("View", "Updating UI.");
    }

    public void updateDisplay(String[] stateLog) {
        logger.log("View", "UPDATING DISPLAY based on new configuration:");
        for (String log : stateLog) {
            logger.log("View", "  " + log);
        }
    }
}