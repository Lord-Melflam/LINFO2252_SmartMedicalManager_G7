package com.mycompany.model;

import com.mycompany.data.Notification;

public interface NotificationObserver {
    void onNotification(Notification notification);
}
