package com.mycompany.model;

import com.mycompany.data.Notification;
import com.mycompany.testsupport.SingletonReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NotificationManagerTest {

    @BeforeEach
    void reset() {
        SingletonReset.resetSingleton(NotificationManager.class);
    }

    @Test
    void send_dispatchesToObservers() {
        NotificationManager nm = NotificationManager.getInstance();
        AtomicInteger count = new AtomicInteger();

        NotificationObserver obs = n -> {
            assertEquals("t", n.getTitle());
            count.incrementAndGet();
        };

        nm.registerObserver(obs);
        nm.send(new Notification("t", "m", 123L));

        assertEquals(1, count.get());
        nm.unregister(obs);
    }
}
