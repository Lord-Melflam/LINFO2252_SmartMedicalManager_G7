package com.mycompany.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mycompany.data.Notification;

public class NotificationManager {
    private static NotificationManager instance;
    private final List<NotificationObserver> listeners = new CopyOnWriteArrayList<>();
    private final Logger logger = Logger.getInstance();

    private NotificationManager() {
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void registerObserver(NotificationObserver listener) {
        listeners.add(listener);
        logger.log("NotificationManager", "Listener registered.");
    }

    public void unregister(NotificationObserver listener) {
        listeners.remove(listener);
        logger.log("NotificationManager", "Listener unregistered.");
    }

    public void send(Notification notification) {
        logger.log("NotificationManager", "Dispatching notification: " + notification.getTitle());
        for (NotificationObserver listener : listeners) {
            listener.onNotification(notification);
        }
    }
}
