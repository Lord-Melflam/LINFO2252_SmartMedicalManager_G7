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

        boolean success = model.applyFeatureChange(deactivations, activations);
        if (success) {
            if (isUIViewEnabled) {
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
        String arguments = (cutLine.length > 1) ? cutLine[1] : "";

        switch (command) {
            case "title":
                String title = (cutLine.length > 1) ? cutLine[1] : "Application";
                view.setTitle(title);
                System.out.println("[Controller] Title set to: " + title);
                break;

            case "add":
                activate(new String[]{}, new String[]{"DYNAMIC_BUTTON"});
                break;

            case "remove":
                activate(new String[]{"DYNAMIC_BUTTON"}, new String[]{});
                break;

            case "activate":
                String toActivate[] = arguments.isEmpty() ? new String[]{} : arguments.split(" ");
                activate(new String[]{}, toActivate);
                break;

            case "deactivate":
                String toDeactivate[] = arguments.isEmpty() ? new String[]{} : arguments.split(" ");
                activate(toDeactivate, new String[]{});
                break;

            case "dark":
                activate(new String[]{}, new String[]{"DARK_MODE"});
                break;

            case "light":
                activate(new String[]{"DARK_MODE"}, new String[]{});
                break;

            case "day":
                TimeEventSystem.getInstance().advanceDays(1);
                break;

            case "week":
                TimeEventSystem.getInstance().advanceDays(7);
                break;

            case "event":
                if (!arguments.isEmpty()) {
                    try {
                        TimeEvent ev = TimeEvent.valueOf(arguments.trim());
                        TimeEventSystem.getInstance().triggerEvent(ev);
                    } catch (IllegalArgumentException e) {
                        System.out.println("[Controller] Unknown event: " + arguments);
                    }
                } else {
                    System.out.println("[Controller] event requires an argument: DOCTOR_UNAVAILABLE | USER_ILL");
                }
                break;

            // small helper for testing appointments: dayaddappt <patient> <offsetDays>
            case "dayaddappt":
                if (!arguments.isEmpty()) {
                    String[] parts = arguments.split(" ");
                    if (parts.length >= 2) {
                        String patient = parts[0];
                        try {
                            int offset = Integer.parseInt(parts[1]);
                            int day = TimeEventSystem.getInstance().getCurrentDay() + offset;
                            model.addAppointment(patient, day);
                        } catch (NumberFormatException nfe) {
                            System.out.println("[Controller] Invalid offset: " + parts[1]);
                        }
                    } else {
                        System.out.println("[Controller] Usage: dayaddappt <patient> <offsetDays>");
                    }
                } else {
                    System.out.println("[Controller] Usage: dayaddappt <patient> <offsetDays>");
                }
                break;

            default:
                System.out.println("[Controller] Unknown command " + command);
                break;
        }
    }
}