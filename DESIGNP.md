# Design Patterns Analysis

This document identifies and explains the design patterns implemented in the SmartMedicalManager system.

## Creational Design Patterns

### 1. Singleton Pattern

**Definition**: Ensures a class has only one instance and provides a global point of access to it.

#### Implementation

The system extensively uses the Singleton pattern for all manager classes to ensure single instances across the application.

**AppointmentManager**
- Location: [src/main/java/com/mycompany/model/AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java)

```java
public class AppointmentManager implements TimeChangeObserver {
    private static AppointmentManager instance;
    
    private AppointmentManager() {
        // Private constructor prevents direct instantiation
        this.allAppointments = new ArrayList<>();
        this.observers = new ArrayList<>();
        initializeSampleData();
        timeEventManager.registerTimeObserver(this);
    }
    
    public static synchronized AppointmentManager getInstance() {
        if (instance == null) {
            instance = new AppointmentManager();
        }
        return instance;
    }
}
```

**NotificationManager**
- Location: [src/main/java/com/mycompany/model/NotificationManager.java](src/main/java/com/mycompany/model/NotificationManager.java)

```java
public class NotificationManager {
    private static NotificationManager instance;
    
    private NotificationManager() {
        // Private constructor
    }
    
    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
    
    public void send(Notification notification) {
        // Centralized notification dispatch
        logger.log("NotificationManager", "Dispatching notification: " + notification.getTitle());
        for (NotificationObserver listener : listeners) {
            listener.onNotification(notification);
        }
    }
}
```

**TimeEventManager**
- Location: [src/main/java/com/mycompany/model/TimeEventManager.java](src/main/java/com/mycompany/model/TimeEventManager.java)

```java
public class TimeEventManager {
    private static TimeEventManager instance;
    private final PriorityQueue<TimeEvent> queue = new PriorityQueue<>();
    
    private TimeEventManager() {
        // Private constructor
    }
    
    public static synchronized TimeEventManager getInstance() {
        if (instance == null) {
            instance = new TimeEventManager();
        }
        return instance;
    }
}
```

**Other Singletons**:
- [FeatureManager](src/main/java/com/mycompany/model/FeatureManager.java)
- [Logger](src/main/java/com/mycompany/model/Logger.java)
- [PatientManager](src/main/java/com/mycompany/model/PatientManager.java)
- [MedicationManager](src/main/java/com/mycompany/model/MedicationManager.java)
- [AppointmentNotificationManager](src/main/java/com/mycompany/model/AppointmentNotificationManager.java)
- [DataProvider](src/main/java/com/mycompany/data/DataProvider.java)

**Why This Pattern**:
- Ensures single source of truth for each concern (appointments, features, time, etc.)
- Allows global access without parameter passing through method chains
- Simplifies state management (all appointment changes go through one manager)
- Thread-safe access with synchronized getInstance()

---

## Structural Design Patterns

### 2. Observer Pattern

**Definition**: Defines a one-to-many dependency between objects so that when one object changes state, all dependents are notified automatically.

#### Implementation

The Observer pattern is fundamental to the system's architecture, used for MVC separation and feature adaptation.

**Appointment Observation**
- Location: [src/main/java/com/mycompany/model/AppointmentObserver.java](src/main/java/com/mycompany/model/AppointmentObserver.java)

```java
// Observer Interface
public interface AppointmentObserver {
    void onAppointmentAdded(Appointment appointment);
    void onAppointmentRemoved(Appointment appointment);
    void onAppointmentUpdated(Appointment appointment);
}

// Subject (AppointmentManager)
public class AppointmentManager implements TimeChangeObserver {
    private final List<AppointmentObserver> observers;
    
    public synchronized void registerObserver(AppointmentObserver observer) {
        observers.add(observer);
    }
    
    public synchronized void addAppointment(Appointment appointment) {
        allAppointments.add(appointment);
        notifyObserversAppointmentAdded(appointment);
    }
    
    private void notifyObserversAppointmentAdded(Appointment appointment) {
        for (AppointmentObserver observer : observers) {
            observer.onAppointmentAdded(appointment);
        }
    }
}

// Concrete Observer (MainFrame)
public class MainFrame extends javax.swing.JFrame implements AppointmentObserver, ... {
    @Override
    public void onAppointmentAdded(Appointment appointment) {
        // Update UI with new appointment
        appointmentModel.addRow(/* ... */);
    }
    
    @Override
    public void onAppointmentRemoved(Appointment appointment) {
        // Remove from UI
    }
    
    @Override
    public void onAppointmentUpdated(Appointment appointment) {
        // Update in UI
    }
}
```

**Feature Observation**
- Location: [src/main/java/com/mycompany/model/FeatureObserver.java](src/main/java/com/mycompany/model/FeatureObserver.java) & [FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java)

```java
// Observer Interface
public interface FeatureObserver {
    void onFeatureToggled(String featureName, boolean enabled);
    void onFeatureAttributeChanged(String featureName, String attributeName, Object value);
}

// Subject (FeatureManager)
public class FeatureManager {
    private final List<FeatureObserver> observers;
    
    public synchronized void activateFeature(String featureName) {
        if (activeFeatures.add(featureName)) {
            notifyObserversFeatureToggled(featureName, true);
        }
    }
    
    private void notifyObserversFeatureToggled(String featureName, boolean enabled) {
        for (FeatureObserver observer : observers) {
            observer.onFeatureToggled(featureName, enabled);
        }
    }
}

// Concrete Observers
public class MedicationManager implements FeatureObserver {
    @Override
    public void onFeatureToggled(String featureName, boolean enabled) {
        if ("MedicationReminders".equals(featureName)) {
            refreshSchedule();  // Adapt behavior based on feature state
        }
    }
}
```

**Time Event Observation**
- Location: [src/main/java/com/mycompany/model/TimeEventObserver.java](src/main/java/com/mycompany/model/TimeEventObserver.java) & [TimeEventManager.java](src/main/java/com/mycompany/model/TimeEventManager.java)

```java
// Observer Interface
public interface TimeEventObserver {
    void onEvent(TimeEvent event);
}

// Subject (TimeEventManager)
public class TimeEventManager {
    private final List<TimeEventObserver> listeners = new CopyOnWriteArrayList<>();
    
    public synchronized void scheduleEvent(String id, Date time, String description) {
        TimeEvent event = new TimeEvent(id, time, description);
        queue.add(event);
    }
    
    private void fireEvent(TimeEvent event) {
        for (TimeEventObserver listener : listeners) {
            listener.onEvent(event);
        }
    }
}

// Concrete Observer (AppointmentNotificationManager)
public class AppointmentNotificationManager implements TimeEventObserver {
    @Override
    public void onEvent(TimeEvent event) {
        // Handle scheduled reminders when time events fire
        handleScheduledReminder(event);
    }
}
```

**Notification Observation**
- Location: [src/main/java/com/mycompany/model/NotificationObserver.java](src/main/java/com/mycompany/model/NotificationObserver.java)

```java
// Observer Interface
public interface NotificationObserver {
    void onNotification(Notification notification);
}

// Subject (NotificationManager)
public class NotificationManager {
    private final List<NotificationObserver> listeners = new CopyOnWriteArrayList<>();
    
    public void send(Notification notification) {
        for (NotificationObserver listener : listeners) {
            listener.onNotification(notification);
        }
    }
}

// Concrete Observer (MainFrame)
public class MainFrame implements NotificationObserver {
    @Override
    public void onNotification(Notification notification) {
        // Display notification in UI
        addNotificationToHomeFeed(notification);
    }
}
```

**Why This Pattern**:
- Decouples model from view (MainFrame doesn't call managers; managers notify MainFrame)
- Enables feature-based adaptation (features observe feature toggles and adapt behavior)
- Multiple observers can react to the same event independently
- Easy to add new observers without modifying existing code

---

### 3. Adapter Pattern

**Definition**: Converts the interface of a class into another interface clients expect.

#### Implementation

**TimeEventObserver to TimeChangeObserver Adaptation**

Different parts of the system need to be notified of time changes in different ways:

- Location: [src/main/java/com/mycompany/model/TimeChangeObserver.java](src/main/java/com/mycompany/model/TimeChangeObserver.java)

```java
// Target Interface (used by managers that need time-based status updates)
public interface TimeChangeObserver {
    void onTimeChanged(Date newNow);
}

// Adapter: AppointmentManager adapts to TimeChangeObserver
public class AppointmentManager implements TimeChangeObserver {
    @Override
    public void onTimeChanged(Date newNow) {
        refreshStatusesBasedOnNow(newNow);  // Adapts generic time change to specific behavior
    }
}

// Adapter: MedicationManager adapts to TimeChangeObserver
public class MedicationManager implements TimeChangeObserver {
    @Override
    public void onTimeChanged(Date newNow) {
        // Reschedules medication reminders when time changes
    }
}

// Adapter: MainFrame adapts to TimeChangeObserver
public class MainFrame implements TimeChangeObserver {
    @Override
    public void onTimeChanged(Date newNow) {
        // Updates UI with new time and refreshes displays
    }
}

// TimeEventManager uses the adapted interface
public class TimeEventManager {
    private final List<TimeChangeObserver> timeChangeObservers = new CopyOnWriteArrayList<>();
    
    public synchronized void setCurrentDate(Date newNow) {
        currentDate = newNow;
        for (TimeChangeObserver observer : timeChangeObservers) {
            observer.onTimeChanged(newNow);  // Calls adapted interface
        }
    }
}
```

**Why This Pattern**:
- Allows components to adapt to time changes without direct coupling
- Each component can handle time changes according to its own needs
- Separates time simulation concerns from business logic

---

## Behavioral Design Patterns

### 4. Strategy Pattern

**Definition**: Defines a family of algorithms, encapsulates each one, and makes them interchangeable.

#### Implementation

**Feature Constraint Strategies**

Different insurance levels apply different strategies for feature availability:

- Location: [src/main/java/com/mycompany/model/FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java)

```java
public class FeatureManager {
    // Strategy: Insurance-based feature constraints
    private static final Map<String, Integer> FEATURE_MIN_INSURANCE_INDEX = Map.of(
        "RoomType", InsuranceLevel.NORMAL.ordinal(),
        "Personel", InsuranceLevel.PREMIUM.ordinal()
    );
    
    // Strategy implementation: check if feature is allowed for insurance level
    private boolean isFeatureAllowedForInsurance(String featureName, String insuranceBillingValue) {
        Integer minIdx = FEATURE_MIN_INSURANCE_INDEX.get(featureName);
        if (minIdx == null) {
            return true;  // Strategy: feature is always available
        }
        return insuranceIndexFromValue(insuranceBillingValue) >= minIdx;
    }
    
    private synchronized void enforceInsuranceConstraints(String insuranceBillingValue) {
        for (String feature : new HashSet<>(activeFeatures)) {
            if (isMandatory(feature)) {
                continue;  // Strategy: mandatory features always available
            }
            // Apply constraint strategy
            if (!isFeatureAllowedForInsurance(feature, insuranceBillingValue)) {
                activeFeatures.remove(feature);
                insuranceDisabledFeatures.add(feature);
            }
        }
    }
}

// Different insurance strategies:
// - MINIMAL: Only mandatory features
// - NORMAL: Mandatory + RoomType
// - PREMIUM: Mandatory + RoomType + Personel selection
```

**Why This Pattern**:
- Insurance constraints can be changed without modifying core feature logic
- New insurance levels and strategies can be added easily
- Each strategy (insurance level) encapsulates its own feature availability rules

---

### 5. State Pattern

**Definition**: Allows an object to alter its behavior when its internal state changes.

#### Implementation

**Appointment Status States**

Appointments transition through different states, and behavior changes with each state:

- Location: [src/main/java/com/mycompany/data/Appointment.java](src/main/java/com/mycompany/data/Appointment.java)

```java
public class Appointment {
    private String status;  // State
    
    // Behavior depends on state
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}

// State Management in AppointmentManager
public class AppointmentManager {
    public synchronized void refreshStatusesBasedOnNow(Date now) {
        for (Appointment appointment : allAppointments) {
            if (!"Scheduled".equalsIgnoreCase(appointment.getStatus())) 
                continue;
            
            Date appointmentDate = appointment.getDateAsDate();
            // State transition: Scheduled -> Completed when time passes
            if (appointmentDate != null && appointmentDate.before(now)) {
                appointment.setStatus("Completed");  // State change
                changed.add(appointment);
            }
        }
        
        if (!changed.isEmpty()) {
            notifyObserversAppointmentUpdated(changed);
        }
    }
}

// Different behaviors for different states:
// - "Scheduled": Can be modified, cancelled, or rescheduled
// - "Completed": Read-only, shows in history
// - "Cancelled": Read-only, shows in history
// - "Rescheduled": Transitional state
```

**Why This Pattern**:
- Appointment behavior changes based on state without complex if-else chains
- New states can be added easily
- State transitions are explicit and traceable

---

### 6. Chain of Responsibility Pattern

**Definition**: Passes requests along a chain of handlers where each handler decides to process or pass it on.

#### Implementation

**Feature Constraint Chain**

Feature activation goes through multiple checks in sequence:

- Location: [src/main/java/com/mycompany/model/FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java)

```java
public class FeatureManager {
    public synchronized void activateFeature(String featureName) {
        // Chain of responsibility:
        
        // Handler 1: Check if feature is valid
        if (!VALID_FEATURES.contains(featureName)) {
            throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
        
        // Handler 2: Check if feature is already active
        if (activeFeatures.contains(featureName)) {
            return;
        }
        
        // Handler 3: Check insurance constraints
        String insuranceValue = (String) getFeatureAttribute("InsuranceBilling", "value");
        if (!isFeatureAllowedForInsurance(featureName, insuranceValue)) {
            insuranceDisabledFeatures.add(featureName);
            return;  // Pass fails at this handler
        }
        
        // Handler 4: Activate the feature
        if (activeFeatures.add(featureName)) {
            notifyObserversFeatureToggled(featureName, true);
        }
    }
}
```

**Why This Pattern**:
- Each constraint check is independent and can be modified separately
- New constraint handlers can be added without changing existing ones
- Clear separation of validation logic

---

### 7. Template Method Pattern

**Definition**: Defines the skeleton of an algorithm, letting subclasses fill in the steps.

#### Implementation

**Manager Initialization Template**

All managers follow a similar initialization template:

- Location: [src/main/java/com/mycompany/model/AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java)

```java
public class AppointmentManager implements TimeChangeObserver {
    private AppointmentManager() {
        // Template step 1: Initialize data structures
        this.allAppointments = new ArrayList<>();
        this.observers = new ArrayList<>();
        
        // Template step 2: Load initial data
        initializeSampleData();
        
        // Template step 3: Register for time changes
        timeEventManager.registerTimeObserver(this);
        
        // Result: Fully initialized manager ready for use
    }
}

// Same template in other managers:
private MedicationManager() {
    // Step 1: Initialize dependencies
    this.notificationManager = NotificationManager.getInstance();
    this.timeEventManager = TimeEventManager.getInstance();
    this.featureManager = FeatureManager.getInstance();
    
    // Step 2: Register listeners
    this.timeEventManager.registerListener(this);
    this.timeEventManager.registerTimeObserver(this);
    this.featureManager.registerObserver(this);
    
    // Step 3: Initialize state
    refreshSchedule();
}
```

**Why This Pattern**:
- Ensures consistent initialization across all managers
- Template can be extended for new managers
- Clear sequence of initialization steps

---

## Summary Table

| Pattern | Location | Purpose |
|---------|----------|---------|
| **Singleton** | All managers (AppointmentManager, TimeEventManager, etc.) | Single source of truth; global access point |
| **Observer** | AppointmentObserver, FeatureObserver, TimeEventObserver, NotificationObserver | MVC separation; loose coupling; feature adaptation |
| **Adapter** | TimeChangeObserver interface implementations | Adapt time changes to component-specific behavior |
| **Strategy** | FeatureManager insurance constraints | Variable feature availability based on insurance level |
| **State** | Appointment status management | Different behaviors for different appointment states |
| **Chain of Responsibility** | Feature activation validation | Sequential constraint checking |
| **Template Method** | Manager initialization | Consistent initialization sequence across managers |

---

## Design Pattern Evolution

### First Version Problems
- Limited observer pattern usage (monolithic MainFrame)
- No feature management strategy
- Poor separation of concerns

### Current Version Improvements
1. **Observer Pattern Everywhere**: Multiple focused observer interfaces enable clean MVC
2. **Singleton for Consistency**: Each concern has single manager instance
3. **Strategy for Insurance**: Features adapt dynamically based on insurance level
4. **State for Appointments**: Clear state transitions instead of scattered status logic
5. **Chain of Responsibility**: Extensible validation for features

### Example: Adding New Feature with Patterns

To add a new feature (e.g., "VideoConsultation"):

```java
// 1. Add to VALID_FEATURES (no code change needed - OCP principle)
private static final Set<String> VALID_FEATURES = Set.of(
    // ... existing features ...
    "VideoConsultation"  // New!
);

// 2. Add constraint if needed (Strategy pattern)
private static final Map<String, Integer> FEATURE_MIN_INSURANCE_INDEX = Map.of(
    "VideoConsultation", InsuranceLevel.NORMAL.ordinal()  // New!
);

// 3. Create observer to handle feature toggle (Observer pattern)
public class VideoConsultationManager implements FeatureObserver {
    @Override
    public void onFeatureToggled(String featureName, boolean enabled) {
        if ("VideoConsultation".equals(featureName)) {
            updateVideoOptions(enabled);
        }
    }
}

// 4. Register observer (Singleton pattern)
FeatureManager.getInstance().registerObserver(new VideoConsultationManager());

// No existing code needs modification!
```

This demonstrates how the design patterns work together to support the open-closed principle.
