package Controller;
// SmartMedicalController.java

import Logger.Logger;
import Model.SmartMedicalModel;
import Model.TimeEvent;
import Model.TimeEventSystem;
import View.SmartMedicalView;

import java.util.Arrays;

public class SmartMedicalController implements ControllerInterface {
    private static SmartMedicalController instance;
    // singleton instances
    private final Logger logger = Logger.getInstance();
    private final SmartMedicalView view = SmartMedicalView.getInstance();
    private final SmartMedicalModel model = SmartMedicalModel.getInstance();
    private final TimeEventSystem tes = TimeEventSystem.getInstance();
    private boolean isUIViewEnabled = false;

    public SmartMedicalController() {
    }

    public static SmartMedicalController getInstance() {
        if (instance == null) {
            synchronized (SmartMedicalController.class) {
                if (instance == null) instance = new SmartMedicalController();
            }
        }
        return instance;
    }

    /**
     * @return: 0 if successful, 2 if constraint violation/other error.
     */
    @Override
    public int activate(String[] deactivations, String[] activations) {
        logger.log("Controller", "Attempting feature change...\n  Deactivate: " + Arrays.toString(deactivations) + "\n  Activate: " + Arrays.toString(activations));

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

                view.updateDisplay(model.getCurrentStateLog());
            }
            logger.log("Controller", "Feature change successful.");
            return 0;
        } else {
            logger.error("Controller", "Feature activation FAILED.");
            return 2;
        }
    }

    @Override
    public boolean enableUIView() {
        if (isUIViewEnabled) {
            logger.log("Controller", "UI View is already enabled.");
            return true;
        }
        isUIViewEnabled = true;
        view.show();
        logger.log("Controller", "UI View enabled.");
        return true;
    }

    @Override
    public boolean disableUIView() {
        if (!isUIViewEnabled) {
            logger.log("Controller", "UI View is already disabled.");
            return true;
        }
        isUIViewEnabled = false;
        view.hide();
        logger.log("Controller", "UI View disabled (Non-blocking).");
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
                logger.log("Controller", "Title set to: " + title);
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
                tes.advanceDays(1);
                break;

            case "week":
                tes.advanceDays(7);
                break;

            case "event":
                if (!arguments.isEmpty()) {
                    try {
                        TimeEvent ev = TimeEvent.valueOf(arguments.trim());
                        tes.triggerEvent(ev);
                    } catch (IllegalArgumentException e) {
                        logger.error("Controller", "Unknown event: " + arguments);
                    }
                } else {
                    logger.error("Controller", "event requires an argument: DOCTOR_UNAVAILABLE | USER_ILL");
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
                            int day = tes.getCurrentDay() + offset;
                            model.addAppointment(patient, day);
                        } catch (NumberFormatException nfe) {
                            logger.error("Controller", "Invalid offset: " + parts[1]);
                        }
                    } else {
                        logger.error("Controller", "Usage: dayaddappt <patient> <offsetDays>");
                    }
                } else {
                    logger.error("Controller", "Usage: dayaddappt <patient> <offsetDays>");
                }
                break;

            default:
                logger.error("Controller", "Unknown command " + command);
                break;
        }
    }
}