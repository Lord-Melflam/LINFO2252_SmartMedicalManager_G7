package Model;
// TimeEventListener.java
public interface TimeEventListener {
    /**
     * Called when the TES advances time or emits an event.
     * @param event the kind of event
     * @param daysAdvanced number of days advanced (useful for DAY_PASSED/WEEK_PASSED)
     */
    void onTimeEvent(TimeEvent event, int daysAdvanced);
}
