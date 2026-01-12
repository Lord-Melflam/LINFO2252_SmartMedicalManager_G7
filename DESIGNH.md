# Design Heuristics Analysis

This document analyzes the design principles and heuristics applied throughout the SmartMedicalManager system.

## 1. Encapsulate What Varies

**Principle**: Identify aspects of code that are likely to change and isolate them.

### Implementation

#### Feature Management System
The system encapsulates varying business features within the `FeatureManager`:

**Location**: [src/main/java/com/mycompany/model/FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java)

```java
// Features are encapsulated in sets - easy to modify without changing logic
private static final Set<String> VALID_FEATURES = Set.of(
    "Book", "Modify", "Cancel", "Re-scheduling",
    "Personel", "ConsultationType", "ConsultationLocation", "RoomType",
    "InsuranceBilling",
    "PastConsultations", "Sort", "SortByDate", "SortByType", "SortByService",
    "Search",
    "Reminders", "AppointmentReminders", "MedicationReminders", "OtherReminders",
    "Notification", "NotifyOnReschedule",
    "DarkMode",
    "BillingInformation", "CurrentMedication", "Vaccines"
);

private static final Map<String, ChoiceDefinition> FEATURE_CHOICES = Map.of(
    "InsuranceBilling", new ChoiceDefinition(List.of("MINIMAL", "NORMAL", "PREMIUM")),
    "Notification", new ChoiceDefinition(List.of("IN_APP", "EMAIL", "SMS"))
);
```

**Why**: Features can be added/removed without changing the core logic that manages them. Insurance constraints are encoded separately from the feature set.

---

#### Time Management
The time system is encapsulated in `TimeEventManager`, isolating all time-related logic:

**Location**: [src/main/java/com/mycompany/model/TimeEventManager.java](src/main/java/com/mycompany/model/TimeEventManager.java)

```java
public class TimeEventManager {
    private Date currentDate = new Date(System.currentTimeMillis());
    private final PriorityQueue<TimeEvent> queue = new PriorityQueue<>();
    private final List<TimeEventObserver> listeners = new CopyOnWriteArrayList<>();
    
    public synchronized Date getDate() {
        return new Date(currentDate.getTime()); 
    }
    
    public synchronized void setCurrentDate(Date date) {
        // All time logic is contained here
    }
}
```

**Why**: Time simulation can be modified (for testing, speed-up, etc.) without affecting appointment, medication, or notification logic.

---

#### Appointment Attributes
The `Appointment` class encapsulates flexible attributes for extensibility:

**Location**: [src/main/java/com/mycompany/data/Appointment.java](src/main/java/com/mycompany/data/Appointment.java)

```java
public class Appointment {
    private String date;
    private String time;
    private String doctor;
    private String location;
    private String reason;
    private String status;
    private final Map<String, Object> attributes;  // Encapsulated variation
    
    public Appointment(String date, String doctor, String location, String reason, 
                       String status, Map<String, Object> attributes) {
        // ... initialization
        if (attributes != null) {
            this.attributes.putAll(attributes);
        }
    }
}
```

**Why**: New appointment properties (price, insurance info, room type, etc.) can be added without modifying the core structure.

---

## 2. Program to an Interface, not an Implementation

**Principle**: Depend on abstractions (interfaces) rather than concrete classes.

### Implementation

#### Observer Pattern Interfaces
The system defines multiple observer interfaces for different concerns:

**Location**: [src/main/java/com/mycompany/model/](src/main/java/com/mycompany/model/)

```java
// AppointmentObserver.java
public interface AppointmentObserver {
    void onAppointmentAdded(Appointment appointment);
    void onAppointmentRemoved(Appointment appointment);
    void onAppointmentUpdated(Appointment appointment);
}

// FeatureObserver.java
public interface FeatureObserver {
    void onFeatureToggled(String featureName, boolean enabled);
    void onFeatureAttributeChanged(String featureName, String attributeName, Object value);
}

// TimeEventObserver.java
public interface TimeEventObserver {
    void onEvent(TimeEvent event);
}

// NotificationObserver.java
public interface NotificationObserver {
    void onNotification(Notification notification);
}
```

**Why**: UI components, managers, and services depend on these interfaces, not concrete implementations. This allows:
- Multiple implementations of the same observer
- Easy mocking for tests
- Loose coupling between model and view

---

#### Manager Dependency Injection
Managers depend on interfaces rather than concrete implementations:

**Location**: [src/main/java/com/mycompany/model/AppointmentNotificationManager.java](src/main/java/com/mycompany/model/AppointmentNotificationManager.java)

```java
public class AppointmentNotificationManager implements TimeEventObserver {
    // Depends on abstractions
    private final NotificationManager notificationManager;
    private final TimeEventManager timeEventManager;
    private final FeatureManager featureManager;
    
    // Uses manager interfaces, not concrete classes in method signatures
    public void onEvent(TimeEvent event) {
        // Polymorphic behavior
    }
}
```

**Why**: Managers can be replaced, mocked, or extended without changing dependent code.

---

#### UI Layer Integration
The `MainFrame` implements multiple observer interfaces:

**Location**: [src/main/java/com/mycompany/ui/MainFrame.java](src/main/java/com/mycompany/ui/MainFrame.java#L34)

```java
public class MainFrame extends javax.swing.JFrame 
    implements FeatureObserver, PatientObserver, AppointmentObserver, 
               NotificationObserver, TimeChangeObserver {
    
    @Override
    public void onAppointmentAdded(Appointment appointment) {
        // Polymorphic implementation for UI updates
    }
    
    @Override
    public void onFeatureToggled(String featureName, boolean enabled) {
        // Polymorphic implementation for UI changes
    }
}
```

**Why**: The view depends on observer abstractions, not the model directly. This enables the MVC pattern.

---

## 3. Favor Composition Over Inheritance

**Principle**: Use object composition instead of extending classes.

### Implementation

#### Manager Composition
Managers are composed rather than inherited through a hierarchy:

**Location**: [src/main/java/com/mycompany/model/AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java)

```java
public class AppointmentManager implements TimeChangeObserver {
    private final TimeEventManager timeEventManager = TimeEventManager.getInstance();
    // Composition: delegate to managers rather than inherit
    
    private final List<Appointment> allAppointments;
    private final List<AppointmentObserver> observers;
    
    @Override
    public void onTimeChanged(Date newNow) {
        refreshStatusesBasedOnNow(newNow);
    }
}
```

**Why**: 
- No fragile base class problem
- Managers are independently testable
- Features can be combined flexibly

---

#### Medication Manager Composition
`MedicationManager` composes multiple services rather than inheriting:

**Location**: [src/main/java/com/mycompany/model/MedicationManager.java](src/main/java/com/mycompany/model/MedicationManager.java)

```java
public class MedicationManager implements TimeEventObserver, FeatureObserver, TimeChangeObserver {
    // Composition: not inheritance
    private final NotificationManager notificationManager;
    private final TimeEventManager timeEventManager;
    private final FeatureManager featureManager;
    
    private MedicationManager() {
        this.notificationManager = NotificationManager.getInstance();
        this.timeEventManager = TimeEventManager.getInstance();
        this.featureManager = FeatureManager.getInstance();
        
        this.timeEventManager.registerListener(this);
        this.timeEventManager.registerTimeObserver(this);
        this.featureManager.registerObserver(this);
    }
}
```

**Why**: 
- Medication reminders are decoupled from time management
- Easy to test each component in isolation
- Flexible feature composition

---

#### UI Component Composition
`MainFrame` composes custom panels rather than inheriting from them:

**Location**: [src/main/java/com/mycompany/ui/MainFrame.java](src/main/java/com/mycompany/ui/MainFrame.java#L44)

```java
public class MainFrame extends javax.swing.JFrame implements ... {
    private TimePickerPanel timePicker;
    private TimePickerPanel adminTimePicker;
    
    private void initializeUI() {
        // Composition: create and use components
        this.timePicker = new TimePickerPanel();
        this.adminTimePicker = new TimePickerPanel();
        // Add to frame instead of extending
    }
}
```

**Why**: Different UI sections have different behaviors; composition allows reusing `TimePickerPanel` twice with different purposes.

---

## SOLID Principles

### 1. Single Responsibility Principle (SRP)

**Definition**: A class should have only one reason to change.

#### Implementation

**AppointmentManager** - Responsibility: Manage appointments
- Location: [src/main/java/com/mycompany/model/AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java)
- Only handles: CRUD operations on appointments, appointment queries, status management
- Does NOT handle: Notifications, time events, payments

**TimeEventManager** - Responsibility: Manage timed events
- Location: [src/main/java/com/mycompany/model/TimeEventManager.java](src/main/java/com/mycompany/model/TimeEventManager.java)
- Only handles: Event scheduling, time simulation, observer notifications
- Does NOT handle: Appointment data, feature toggles

**AppointmentNotificationManager** - Responsibility: Bridge appointments with notifications
- Location: [src/main/java/com/mycompany/model/AppointmentNotificationManager.java](src/main/java/com/mycompany/model/AppointmentNotificationManager.java)
- Only handles: Scheduling reminders, handling event triggers, notification dispatch
- Does NOT handle: Core appointment logic, core notification logic

**FeatureManager** - Responsibility: Manage feature toggles
- Location: [src/main/java/com/mycompany/model/FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java)
- Only handles: Feature activation/deactivation, insurance-based constraints, feature attributes
- Does NOT handle: Time events, notifications, appointment logic

---

### 2. Open/Closed Principle (OCP)

**Definition**: Classes should be open for extension but closed for modification.

#### Implementation

**Feature System is Open for Extension**

The `FeatureManager` uses declarative feature definitions that allow adding new features without code changes:

**Location**: [src/main/java/com/mycompany/model/FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java)

```java
private static final Set<String> VALID_FEATURES = Set.of(
    // Existing features...
    "Search",  // New feature - no code change needed
    "DarkMode" // New feature - no code change needed
);

private static final Map<String, ChoiceDefinition> FEATURE_CHOICES = Map.of(
    "InsuranceBilling", new ChoiceDefinition(List.of("MINIMAL", "NORMAL", "PREMIUM")),
    "Notification", new ChoiceDefinition(List.of("IN_APP", "EMAIL", "SMS"))
    // New notification options can be added here
);
```

**Why**: New features can be added to the set without modifying the feature management logic.

---

**Observer Pattern is Open for Extension**

New observers can be added without modifying existing managers:

**Location**: [src/main/java/com/mycompany/model/AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java)

```java
public synchronized void registerObserver(AppointmentObserver observer) {
    observers.add(observer);
}

// The manager doesn't know or care about specific observer implementations
// It's closed to modification (the registration code doesn't change)
// But open to extension (new observers can be added at runtime)
```

**Why**: New UI panels or features can observe appointment changes without modifying the `AppointmentManager`.

---

**Flexible Appointment Attributes**

The `Appointment` class is open for extension through its flexible attribute map:

**Location**: [src/main/java/com/mycompany/data/Appointment.java](src/main/java/com/mycompany/data/Appointment.java)

```java
public class Appointment {
    private final Map<String, Object> attributes;  // Extensible
    
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}
```

**Why**: New appointment properties (insurance coverage %, payment method, etc.) can be added without modifying the class structure.

---

### 3. Liskov Substitution Principle (LSP)

**Definition**: Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.

#### Implementation

**Observer Interface Contracts**

All observers follow the same substitutability contract:

**Location**: [src/main/java/com/mycompany/model/](src/main/java/com/mycompany/model/)

```java
// Any class implementing these interfaces can be substituted for each other
public interface AppointmentObserver {
    void onAppointmentAdded(Appointment appointment);
    void onAppointmentRemoved(Appointment appointment);
    void onAppointmentUpdated(Appointment appointment);
}

// MainFrame can be substituted for AppointmentObserver
public class MainFrame extends javax.swing.JFrame implements AppointmentObserver {
    @Override
    public void onAppointmentAdded(Appointment appointment) {
        // Implementation specific to UI
    }
}

// Future: Another observer could be added (logging, persistence)
public class AppointmentLogger implements AppointmentObserver {
    @Override
    public void onAppointmentAdded(Appointment appointment) {
        // Implementation specific to logging
    }
}

// Both can be registered and called identically:
appointmentManager.registerObserver(mainFrame);
appointmentManager.registerObserver(appointmentLogger);
```

**Why**: The manager doesn't need to know about specific implementations; any observer is treated the same.

---

**Singleton Pattern Consistency**

All singletons follow the same contract:

**Locations**: 
- [NotificationManager](src/main/java/com/mycompany/model/NotificationManager.java)
- [TimeEventManager](src/main/java/com/mycompany/model/TimeEventManager.java)
- [AppointmentManager](src/main/java/com/mycompany/model/AppointmentManager.java)

```java
public class NotificationManager {
    private static NotificationManager instance;
    
    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
}

// Any singleton can be used the same way:
NotificationManager.getInstance().send(notification);
TimeEventManager.getInstance().scheduleEvent(event);
```

**Why**: All singletons are replaceable in their usage patterns; no manager needs to know the specific type.

---

### 4. Interface Segregation Principle (ISP)

**Definition**: Clients should not be forced to depend on methods they don't use.

#### Implementation

**Separate Observer Interfaces for Separate Concerns**

Instead of one monolithic observer, the system defines multiple focused interfaces:

**Location**: [src/main/java/com/mycompany/model/](src/main/java/com/mycompany/model/)

```java
// Separate interfaces - clients implement only what they need
public interface AppointmentObserver {
    void onAppointmentAdded(Appointment appointment);
    void onAppointmentRemoved(Appointment appointment);
    void onAppointmentUpdated(Appointment appointment);
}

public interface FeatureObserver {
    void onFeatureToggled(String featureName, boolean enabled);
    void onFeatureAttributeChanged(String featureName, String attributeName, Object value);
}

public interface PatientObserver {
    void onPatientUpdated(Patient patient);
}

public interface TimeChangeObserver {
    void onTimeChanged(Date newNow);
}

// Client only implements what it needs:
// - A time-aware component
public class MedicationManager implements TimeEventObserver, FeatureObserver, TimeChangeObserver {
    // Only handles time events, feature changes, and time changes
}

// - A feature-aware component
public class AppointmentNotificationManager implements TimeEventObserver {
    // Only cares about time events
}
```

**Why**: Components only depend on the methods they actually use. If `TimeEventObserver` changes, `AppointmentObserver` is not affected.

---

**Segregated Manager Interfaces**

Managers expose only the methods relevant to their clients:

**AppointmentManager**:
```java
public synchronized void addAppointment(Appointment appointment);
public synchronized boolean removeAppointment(Appointment appointment);
public synchronized void updateAppointment(Appointment appointment);
public List<Appointment> getAllAppointments();
public List<Appointment> getUpcomingAppointments();
```

**NotificationManager**:
```java
public void registerObserver(NotificationObserver listener);
public void unregister(NotificationObserver listener);
public void send(Notification notification);
```

**FeatureManager**:
```java
public synchronized boolean isFeatureActive(String featureName);
public synchronized void activateFeature(String featureName);
public synchronized void deactivateFeature(String featureName);
```

**Why**: Clients see only relevant methods. A UI component doesn't need to know about internal manager state management.

---

### 5. Dependency Inversion Principle (DIP)

**Definition**: High-level modules should not depend on low-level modules; both should depend on abstractions.

#### Implementation

**Managers Depend on Abstractions (Observer Interfaces)**

**Location**: [src/main/java/com/mycompany/model/AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java)

```java
public class AppointmentManager implements TimeChangeObserver {
    // High-level module (AppointmentManager)
    private final List<AppointmentObserver> observers;  // Depends on ABSTRACTION
    
    public synchronized void registerObserver(AppointmentObserver observer) {
        observers.add(observer);
    }
    
    private void notifyObserversAppointmentAdded(Appointment appointment) {
        // Calls abstraction, not concrete classes
        for (AppointmentObserver observer : observers) {
            observer.onAppointmentAdded(appointment);
        }
    }
}

// Low-level modules (UI, Logging, etc.) also depend on the SAME ABSTRACTION
public class MainFrame extends javax.swing.JFrame implements AppointmentObserver {
    @Override
    public void onAppointmentAdded(Appointment appointment) {
        // Implementation
    }
}
```

**Why**: Both high-level (manager) and low-level (UI) modules depend on the same abstraction, inverting the dependency direction.

---

**Feature System Uses Abstraction**

**Location**: [src/main/java/com/mycompany/model/FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java)

```java
// FeatureManager (high-level) depends on FeatureObserver interface
private final List<FeatureObserver> observers;

// Uses depend on the abstraction
public synchronized void activateFeature(String featureName) {
    if (activeFeatures.add(featureName)) {
        notifyObserversFeatureToggled(featureName, true);
    }
}

// Low-level components also depend on the same interface
public class MedicationManager implements FeatureObserver {
    @Override
    public void onFeatureToggled(String featureName, boolean enabled) {
        if ("MedicationReminders".equals(featureName)) {
            refreshSchedule();
        }
    }
}
```

**Why**: Adding new features or observers doesn't require modifying the core feature management logic.

---

**Manager Singleton Access Inverted**

Managers are accessed through their public interfaces, not through concrete implementations:

**Location**: [src/main/java/com/mycompany/model/TimeEventManager.java](src/main/java/com/mycompany/model/TimeEventManager.java)

```java
public class TimeEventManager {
    // Provides abstract access through getInstance()
    public static synchronized TimeEventManager getInstance() {
        if (instance == null) {
            instance = new TimeEventManager();
        }
        return instance;
    }
    
    // Methods are implementation-agnostic
    public synchronized Date getDate() { ... }
    public synchronized void scheduleEvent(String id, Date time, String description) { ... }
}

// Clients don't depend on the concrete constructor
// They depend on the abstraction provided through getInstance()
MedicationManager medicationManager = new MedicationManager() {
    // Uses TimeEventManager through its public interface
    this.timeEventManager = TimeEventManager.getInstance();
}
```

**Why**: Concrete dependencies are isolated at creation time; all interactions use abstractions.

---

## Summary Table

| Principle | Where Applied | Benefit |
|-----------|---------------|---------|
| **Encapsulate What Varies** | FeatureManager, TimeEventManager, Appointment attributes | New features/time sources can be added without core logic changes |
| **Program to Interface** | Observer pattern (multiple interfaces) | UI/managers decoupled; easy testing and mocking |
| **Favor Composition** | Manager composition in MedicationManager, AppointmentNotificationManager | More flexible than inheritance; independent testability |
| **SRP** | Each manager has one responsibility | Easy to understand, modify, and test individual components |
| **OCP** | Feature system, observer registration | New features/observers added without modifying existing code |
| **LSP** | Observer interface implementations | Any observer can substitute for another |
| **ISP** | Multiple focused observer interfaces | Components depend only on methods they use |
| **DIP** | Managers/UI depend on abstractions | High and low-level modules both depend on interfaces |

---

## Evolution Example: From First Version to Current

The application demonstrates these principles through evolution:

1. **First version**: Monolithic `MainFrame` (blob problem) ❌
2. **Current version**: Separated concerns with observers ✓
   - Managers handle business logic (SRP)
   - UI implements observers, not extends managers (Composition, DIP)
   - Features defined declaratively (OCP, Encapsulation)
   - Multiple observer interfaces (ISP)

This structure allows the system to evolve with new features (search, admin panel, notifications) without breaking existing code.
