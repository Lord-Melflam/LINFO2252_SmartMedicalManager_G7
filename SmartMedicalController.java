// SmartMedicalController.java
import java.util.Arrays;

public class SmartMedicalController implements ControllerInterface {
    private final SmartMedicalModel model;
    private final SmartMedicalView view;
    private boolean isUIViewEnabled = false;

    public SmartMedicalController(SmartMedicalModel model, SmartMedicalView view) {
        this.model = model;
        this.view = view;
    }

    /**
     * @return: 0 if successful, 2 if constraint violation/other error.
     */
    @Override
    public int activate(String[] deactivations, String[] activations) {
        System.out.println("\n[Controller] Attempting feature change...");
        System.out.println("  Deactivate: " + Arrays.toString(deactivations));
        System.out.println("  Activate: " + Arrays.toString(activations));

        // Attempt to apply change in the model
        boolean success = model.applyFeatureChange(deactivations, activations);

        if (success) {
            // SUCCESS: Adapt the View based on the new active features
            if (isUIViewEnabled) {
                // UI Feature Adaptation (Directly tells the View how to change)
                for (String name : activations) {
                    if (name.equals("DYNAMIC_BUTTON")) view.toggleDynamicButton(true);
                    if (name.equals("DARK_MODE")) view.setDarkMode(true);
                }
                for (String name : deactivations) {
                    if (name.equals("DYNAMIC_BUTTON")) view.toggleDynamicButton(false);
                    if (name.equals("DARK_MODE")) view.setDarkMode(false);
                }

                // Update the state log display
                view.updateDisplay(model.getCurrentStateLog());
            }
            System.out.println("[Controller] Feature activation successful (Code 0).");
            return 0;
        } else {
            // FAILURE: Log the failure and return an error code
            System.out.println("[Controller] Feature activation FAILED.");
            return 2;
        }
    }

    @Override
    public boolean enableUIView() {
        if (isUIViewEnabled) {
            System.out.println("[Controller] UI View is already enabled.");
            return true;
        }
        isUIViewEnabled = true;
        view.show();
        System.out.println("[Controller] UI View enabled.");
        return true;
    }

    @Override
    public boolean disableUIView() {
        if (!isUIViewEnabled) {
            System.out.println("[Controller] UI View is already disabled.");
            return true;
        }
        isUIViewEnabled = false;
        view.hide();
        System.out.println("[Controller] UI View disabled (Non-blocking).");
        return true;
    }

    @Override
    public String[] getStateAsLog() {
        return model.getCurrentStateLog();
    }

    /**
     * Handles command line input and delegates logic to the activate method or the view.
     */
    public void handleCommand(String commandLine) {
        String[] cutLine = commandLine.split(" ", 2);
        String command = cutLine[0];

        switch (command) {
            case "title":
                String title = (cutLine.length > 1) ? cutLine[1] : "Application";
                view.setTitle(title);
                System.out.println("[Controller] Title set to: " + title);
                break;

            case "add": // Corresponds to activating DYNAMIC_BUTTON
                activate(new String[]{}, new String[]{"DYNAMIC_BUTTON"});
                break;

            case "remove": // Corresponds to deactivating DYNAMIC_BUTTON
                activate(new String[]{"DYNAMIC_BUTTON"}, new String[]{});
                break;

            case "dark": // Corresponds to activating DARK_MODE
                activate(new String[]{}, new String[]{"DARK_MODE"});
                break;

            case "light": // Corresponds to deactivating DARK_MODE
                activate(new String[]{"DARK_MODE"}, new String[]{});
                break;

            case "day":
            case "week":
            case "event":
                // Placeholder for Lab 3 Time Event System (TES) commands
                System.out.println("[Controller] Time/Event command received: " + commandLine);
                // Here you would call a model.advanceTime(command) or model.triggerEvent()
                // and then call model.getCurrentStateLog() to check system reaction.
                break;

            default:
                System.out.println("[Controller] Unknown command " + command);
                break;
        }
    }
}