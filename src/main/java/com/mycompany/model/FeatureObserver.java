package com.mycompany.model;

import java.util.List;

/**
 * Observer interface for feature changes.
 * Implements Observer pattern for MVC separation.
 * Views can observe feature activation/deactivation.
 */
public interface FeatureObserver {
    /**
     * Called when features are activated.
     */
    void onFeaturesActivated(List<String> features);
    
    /**
     * Called when features are deactivated.
     */
    void onFeaturesDeactivated(List<String> features);
    
    /**
     * Called when insurance level changes (affects feature availability).
     */
    void onInsuranceLevelChanged(FeatureManager.InsuranceLevel level);
}
