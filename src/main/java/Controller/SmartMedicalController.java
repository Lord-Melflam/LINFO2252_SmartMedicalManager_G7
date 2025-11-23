package Controller;
// SmartMedicalController.java

import Logger.Logger;
import Model.Feature;
import Model.SmartMedicalModel;
import Model.Appointment;
import Model.Notification;
import Model.TimeEvent;
import Model.TimeEventSystem;
import View.SmartMedicalView;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

import com.github.weisj.darklaf.theme.Theme;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SmartMedicalController implements ControllerInterface {
    private static SmartMedicalController instance;
    // singleton instances
    private final Logger logger = Logger.getInstance();
    private final SmartMedicalView view = SmartMedicalView.getInstance();
    private final SmartMedicalModel model = SmartMedicalModel.getInstance();
    private final TimeEventSystem tes = TimeEventSystem.getInstance();

    private boolean isUIViewEnabled = false;
    private final Map<String, Command> commands = new HashMap<>();

    public SmartMedicalController() {
        initCommands();
    }

    public static SmartMedicalController getInstance() {
        if (instance == null) {
            synchronized (SmartMedicalController.class) {
                if (instance == null) instance = new SmartMedicalController();
            }
        }
        return instance;
    }

    private void initCommands() {
        commands.put("title", args -> {
            String title = args.isEmpty() ? "Application" : args;
            view.setTitle(title);
            logger.log("Controller", "Title set to: " + title);
        });

        commands.put("add", args -> activate(new String[]{}, new String[]{"DYNAMIC_BUTTON"}));

        commands.put("remove", args -> activate(new String[]{"DYNAMIC_BUTTON"}, new String[]{}));

        commands.put("activate", args -> {
            String[] toActivate = args.isEmpty() ? new String[]{} : args.split(" ");
            activate(new String[]{}, toActivate);
        });

        commands.put("deactivate", args -> {
            String[] toDeactivate = args.isEmpty() ? new String[]{} : args.split(" ");
            activate(toDeactivate, new String[]{});
        });

        commands.put("dark", args -> activate(new String[]{}, new String[]{"DARK_MODE"}));

        commands.put("light", args -> activate(new String[]{"DARK_MODE"}, new String[]{}));

        commands.put("day", args -> tes.advanceDays(1));

        commands.put("week", args -> tes.advanceDays(7));

        commands.put("event", args -> {
            if (!args.isEmpty()) {
                try {
                    TimeEvent ev = TimeEvent.valueOf(args.trim());
                    tes.triggerEvent(ev);
                } catch (IllegalArgumentException e) {
                    logger.error("Controller", "Unknown event: " + args);
                }
            } else {
                logger.error("Controller", "event requires an argument: DOCTOR_UNAVAILABLE | USER_ILL");
            }
        });

        commands.put("dayaddappt", args -> {
            if (!args.isEmpty()) {
                String[] parts = args.split(" ");
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
        });

        commands.put("help", args -> {
            printHelp(logger);
        });
        
        commands.put("notifications", args -> {
            java.util.List<Notification> list = getNotifications();
            if (list.isEmpty()) {
                logger.log("Controller", "No notifications.");
            } else {
                for (Notification n : list) logger.log("Controller", n.toString());
            }
        });

        commands.put("clearnotifs", args -> {
            clearNotifications();
        });
    }

    public static void printHelp(Logger logger) {
        logger.log("System", "UI commands: title <text>, add, remove, activate <feature1...>, deactivate <feature1...>, dark, light");
        logger.log("System", "TES commands: day, week, event <name>");
        logger.log("System", "System command: stop");
        logger.log("System", "Features available:");
        for (Feature f : Feature.values()) {
            logger.log("System", "Feature: " + f.name());
        }
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
                    //if (name.equals("DYNAMIC_BUTTON")) view.toggleDynamicButton(true);
                    if (name.equals("DARK_MODE")) view.setDarkMode(true);
                }
                for (String name : deactivations) {
                    //if (name.equals("DYNAMIC_BUTTON")) view.toggleDynamicButton(false);
                    if (name.equals("DARK_MODE")) view.setDarkMode(false);
                }

                view.updateDisplay(model.getCurrentStateLog());
            }
            logger.log("Controller", "Feature change successful, activated : " + Arrays.toString(activations) + ", deactivated: " + Arrays.toString(deactivations));
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

    public boolean addAppointmentDirect(String patient, int day) {
        try {
            model.addAppointment(patient, day);
            if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
            logger.log("Controller", "Directly added appointment for " + patient + " on day " + day);
            return true;
        } catch (Exception e) {
            logger.error("Controller", "addAppointmentDirect failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * New API: add an appointment by LocalDate, returning the UUID of the created appointment.
     */
    public UUID addAppointment(LocalDate date, String patient) {
        try {
            UUID id = model.addAppointment(date, patient);
            if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
            logger.log("Controller", "Added appointment (by date) for " + patient + " on " + date + " id=" + id);
            return id;
        } catch (Exception e) {
            logger.error("Controller", "addAppointment failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Return a copy of future appointments for UI display.
     */
    public List<Appointment> getFutureAppointments() {
        java.util.List<Appointment> list = model.getFutureAppointments();
        logger.log("Controller", "getFutureAppointments: returning " + list.size() + " appointments");
        return list;
    }

    /**
     * Return past appointments from model.
     */
    public java.util.List<Appointment> getPastAppointments() {
        java.util.List<Appointment> list = model.getPastAppointments();
        logger.log("Controller", "getPastAppointments: returning " + list.size() + " appointments");
        return list;
    }

    /**
     * Notifications API
     */
    public java.util.List<Notification> getNotifications() {
        java.util.List<Notification> list = model.getNotifications();
        logger.log("Controller", "getNotifications: returning " + list.size() + " notifications");
        return list;
    }

    public java.util.List<Notification> getUnreadNotifications() {
        java.util.List<Notification> list = model.getUnreadNotifications();
        logger.log("Controller", "getUnreadNotifications: returning " + list.size() + " notifications");
        return list;
    }

    public boolean markNotificationRead(String id) {
        logger.log("Controller", "markNotificationRead requested: " + id);
        boolean res = model.markNotificationRead(id);
        if (res) {
            if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
            logger.log("Controller", "Marked notification read: " + id);
        }
        return res;
    }

    public void clearNotifications() {
        model.clearNotifications();
        if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
    }

    /* Themes API */
    public List<Theme> getAvailableThemes() {
        java.util.List<Theme> list = model.getAvailableThemes();
        logger.log("Controller", "getAvailableThemes: returning " + list.size() + " themes");
        return list;
    }

    public boolean addTheme(Theme theme) {
        logger.log("Controller", "addTheme requested: " + theme);
        boolean res = model.addTheme(theme);
        if (res) {
            if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
            logger.log("Controller", "Theme added: " + theme);
        }
        return res;
    }

    public Theme getActiveTheme() {
        return model.getActiveTheme();
    }

    public boolean setActiveTheme(Theme theme) {
        logger.log("Controller", "setActiveTheme requested: " + theme);
        boolean res = model.setActiveTheme(theme);
        if (res) {
            if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
            logger.log("Controller", "Active theme set to: " + theme);
        }
        return res;
    }

    /**
     * Cancel appointment by UUID.
     */
    public boolean cancelAppointmentById(UUID id) {
        logger.log("Controller", "cancelAppointmentById requested: " + id);
        boolean res = model.cancelAppointmentById(id);
        if (res) {
            if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
            logger.log("Controller", "Cancelled appointment id: " + id);
        } else {
            logger.error("Controller", "Failed to cancel appointment id: " + id);
        }
        return res;
    }

    

    /**
     * Reschedule appointment by UUID to a new LocalDate.
     */
    public boolean rescheduleAppointment(UUID id, LocalDate newDate) {
        logger.log("Controller", "rescheduleAppointment requested: " + id + " -> " + newDate);
        boolean res = model.rescheduleAppointmentById(id, newDate);
        if (res) {
            if (isUIViewEnabled) view.updateDisplay(model.getCurrentStateLog());
            logger.log("Controller", "Rescheduled appointment id " + id + " -> " + newDate);
        } else {
            logger.error("Controller", "Failed to reschedule appointment id: " + id);
        }
        return res;
    }

    /**
     * Handles command line input and delegates logic to the activate method or the view.
     */
    public void handleCommand(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            logger.error("Controller", "Empty command received.");
            return;
        }

        String[] cutLine = commandLine.split(" ", 2);
        String command = cutLine[0];
        String arguments = (cutLine.length > 1) ? cutLine[1] : "";

        Command handler = commands.get(command);
        if (handler != null) {
            handler.execute(arguments);
        }
        else {
            logger.error("Controller", "Unknown command " + command);
        }
    }
}