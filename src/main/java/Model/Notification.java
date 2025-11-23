package Model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notification {
    private final String id;
    private final LocalDateTime timestamp;
    private final String message;
    private boolean read = false;

    public Notification(String message) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead() {
        this.read = true;
    }

    @Override
    public String toString() {
        return "Notification{" + "id=" + id + ", ts=" + timestamp + ", read=" + read + ", msg='" + message + "'}";
    }
}
