// SmartMedicalView.java
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class SmartMedicalView {
    private final JFrame frame;
    private final JPanel panel;
    private final JButton buttonTemp;

    public SmartMedicalView() {
        // --- UI Setup ---
        frame = new JFrame("Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        frame.add(panel);
        frame.setSize(300, 200);

        buttonTemp = new JButton("Dynamic click me");
        buttonTemp.addActionListener((actionEvent) -> {
            System.out.println("dynamic click");
        });
    }

    public void show() {
        frame.setVisible(true);
        System.out.println("[View] Displaying the Smart Medical Appointment Manager UI.");
    }

    public void hide() {
        frame.setVisible(false);
        System.out.println("[View] Hiding the UI (System running in headless/testing mode).");
    }

    // Handles the "title" command - not feature-dependent
    public void setTitle(String title) {
        frame.setTitle(title);
        this.updateUI();
    }

    // Adapts the UI based on DYNAMIC_BUTTON feature state
    public void toggleDynamicButton(boolean isEnabled) {
        if (isEnabled) {
            if (buttonTemp.getParent() == null) {
                panel.add(buttonTemp);
            }
        } else {
            panel.remove(buttonTemp);
        }
        this.updateUI();
    }

    // Adapts the UI based on DARK_MODE feature state
    public void setDarkMode(boolean isDark) {
        panel.setBackground(isDark ? Color.BLACK : Color.WHITE);
        this.updateUI();
    }

    // Method to force UI refresh after changes
    private void updateUI() {
        // These calls are essential for Swing to reflect changes dynamically [cite: 24]
        frame.repaint();
        frame.revalidate();
    }

    public void updateDisplay(String[] stateLog) {
        // In a real application, the View would use the Model's state to render the appropriate UI elements.
        System.out.println("\n[View] UPDATING DISPLAY based on new configuration:");
        for (String log : stateLog) {
            System.out.println("  " + log);
        }
    }
}