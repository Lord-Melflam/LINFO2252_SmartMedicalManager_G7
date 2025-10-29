// SmartMedicalView.java
import java.awt.Color;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SmartMedicalView {
    private final JFrame frame;
    private final JPanel panel;
    private final JButton darkModeButton;
    private final JTextField inputField;
    private boolean isDark = false;

    public SmartMedicalView() {
        // --- UI Setup ---
        frame = new JFrame("Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        frame.add(panel);
        frame.setSize(300, 200);

        darkModeButton = new JButton("Dynamic click me");
        darkModeButton.addActionListener((actionEvent) -> {
            isDark = !isDark;
            setDarkMode(isDark);
            System.out.println("dynamic click");
        });
        frame.add(darkModeButton);

        inputField = new JTextField(16);
        panel.add(inputField);
    }

    public void show() {
        frame.setVisible(true);
        System.out.println("[View] Displaying the Smart Medical Appointment Manager UI.");
    }

    public void hide() {
        frame.setVisible(false);
        System.out.println("[View] Hiding the UI (System running in headless/testing mode).");
    }

    public void setTitle(String title) {
        frame.setTitle(title);
        this.updateUI();
    }

    public void toggleDynamicButton(boolean isEnabled) {
        if (isEnabled) {
            if (darkModeButton.getParent() == null) {
                panel.add(darkModeButton);
            }
        } else {
            panel.remove(darkModeButton);
        }
        this.updateUI();
    }

    public void setDarkMode(boolean isDark) {
        System.out.println("[View] Setting Dark Mode to " + isDark);
        panel.setBackground(isDark ? Color.BLACK : Color.WHITE);
        this.updateUI();
    }

    private void updateUI() {
        frame.repaint();
        frame.revalidate();
    }

    public void updateDisplay(String[] stateLog) {
        System.out.println("\n[View] UPDATING DISPLAY based on new configuration:");
        for (String log : stateLog) {
            System.out.println("  " + log);
        }
    }
}