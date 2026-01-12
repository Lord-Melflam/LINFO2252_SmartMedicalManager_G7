package com.mycompany.model;

import java.util.Date;

/**
 * Observer for changes to the simulated "current time" managed by TimeEventManager.
 */
public interface TimeChangeObserver {
    void onTimeChanged(Date newNow);
}
