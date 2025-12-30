package com.mycompany.model;

import com.mycompany.data.TimeEvent;

public interface TimeEventObserver {
    void onTimeEvent(TimeEvent event);
}
