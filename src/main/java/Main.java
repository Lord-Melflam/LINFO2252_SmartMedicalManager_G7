import Controller.SmartMedicalController;
import Logger.Logger;
import Model.Feature;

import java.util.Scanner;

public class Main {
    private static Logger logger = Logger.getInstance();
    private static SmartMedicalController controller = SmartMedicalController.getInstance();

    private static void printInfo() {
        logger.log("System", "System Ready. Enter commands:");
        SmartMedicalController.printHelp(logger);
    }

    private static void commandLoop() {
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

    public static void main(String[] args) {
        controller.enableUIView();
        commandLoop();
    }
}