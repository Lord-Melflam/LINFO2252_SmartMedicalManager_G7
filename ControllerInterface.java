import java.io.FileWriter;
import java.io.IOException;

public interface ControllerInterface {
    /** The "activate" method is responsible for (de)activating features.
     A feature *must* be mentioned in the feature model (which can be updated), and only features in the feature model can be (de)activated.
     @pre: "deactivations" is a list of features' names to deactivate.
     @pre: "activations" is a list of features' names to activate.
     @return: 0 if the (de)activations were successful, or another integer indicating which problem occurred (e.g. if the feature name is not in the feature model).
     **/
    public int activate(String[] deactivations, String[] activations);

    /** The "enableUIView" method makes the UI View visible and usable.
     * @return: 0 if the UI View was successfully established, or if it was already established, else returns another integer indicating which problem occurred (your choice).
     */
    public boolean enableUIView();

    /** The "disableUIView" method makes the UI invisible and unusable.
     * Important: the Controller methods should be non-blocking when the UI View is disabled,
     * i.e. no interaction from the user is needed for it to (de)activate features.
     * @return: 0 if the UI View was successfully established, or if it was already established, else returns another integer indicating which problem occurred (your choice).
     */
    public boolean disableUIView();

    /**
     * The "getStateAsLog" method is used to get the system's state in the form of logs.
     * It must only contain the description of the current state (not of the previous states).
     * @return returns a list of String that represents all of the system's current state in text. The order does not matter in the list.
     */
    public String[] getStateAsLog();
}