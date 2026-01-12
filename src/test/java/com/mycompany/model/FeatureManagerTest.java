package com.mycompany.model;

import com.mycompany.testsupport.SingletonReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureManagerTest {

    @BeforeEach
    void reset() {
        SingletonReset.resetSingleton(FeatureManager.class);
    }

    @Test
    void mandatoryFeaturesCannotBeDeactivated() {
        FeatureManager fm = FeatureManager.getInstance();
        assertThrows(IllegalStateException.class, () -> fm.deactivateFeatures("Book"));
    }

    @Test
    void insuranceBillingDeactivatesAndRestoresRoomType() {
        FeatureManager fm = FeatureManager.getInstance();

        assertTrue(fm.isFeatureActive("RoomType"));

        fm.setFeatureAttribute("InsuranceBilling", "value", "MINIMAL");
        assertFalse(fm.isFeatureActive("RoomType"));

        fm.setFeatureAttribute("InsuranceBilling", "value", "NORMAL");
        assertTrue(fm.isFeatureActive("RoomType"));
    }
}
