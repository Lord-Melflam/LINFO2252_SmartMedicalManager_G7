import Controller.SmartMedicalController;
import Logger.Logger;
import Model.Feature;
import View.MainFrame;

import java.util.Scanner;

public class Main {
    private static Logger logger = Logger.getInstance();
    private static SmartMedicalController controller = SmartMedicalController.getInstance();

    private static void printInfo() {
        logger.log("System", "System Ready. Enter commands:");
        logger.log("System", "UI commands: title <text>, add, remove, activate <feature1...>, deactivate <feature1...>, dark, light");
        logger.log("System", "TES commands: day, week, event <name>");
        logger.log("System", "System command: stop");
        logger.log("System", "Features available:");
        for (Feature f : Feature.values()) {
            logger.log("System", "Feature: " + f.name());
        }
    }

    public static void main(String[] args) {
        MainFrame.main(args);

        controller.enableUIView();

        Scanner in = new Scanner(System.in);
        printInfo();

        while (true) {
            String line = in.nextLine();

            if (line.equals("stop")) {
                logger.log("System", "Application stopped by user.");
                in.close();
                System.exit(0);
            }

            controller.handleCommand(line);
        }
    }
}