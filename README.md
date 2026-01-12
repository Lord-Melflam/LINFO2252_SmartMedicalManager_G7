# SmartMedicalManager

**A Healthcare Appointment Management System**  
Built with Java, Swing, and design patterns for maintainability and extensibility.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](.)
[![Tests](https://img.shields.io/badge/tests-14%2F14%20passing-brightgreen)](.)
[![Java](https://img.shields.io/badge/Java-21-orange)](.)
[![License](https://img.shields.io/badge/license-Educational-blue)](.)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Design Decisions](#design-decisions)
- [Testing](#testing)
- [Documentation](#documentation)
- [Demo Guide](#demo-guide)
- [Contributors](#contributors)

---

## 🔍 Overview

SmartMedicalManager is a comprehensive healthcare appointment management system designed with **software quality** and **maintainability** in mind. The project demonstrates professional software engineering practices including:

- **SOLID principles** for clean architecture
- **Design patterns** (Singleton, Observer, Strategy, State, etc.)
- **MVC separation** for loose coupling
- **Feature management** with dynamic toggling
- **Time simulation** for testing and demonstration
- **Comprehensive testing** with 14 passing unit tests

### Key Highlights

✓ **Observer Pattern** - Real-time UI updates when model changes  
✓ **Feature Gating** - Dynamic feature toggling based on insurance level  
✓ **Time Simulation** - Controllable time for testing and demos  
✓ **Insurance-Based Constraints** - Features adapt to user's insurance plan  
✓ **Notification System** - Appointment and medication reminders  
✓ **Testable Architecture** - 14 unit tests covering core business logic  

---

## ✨ Features

### Core Functionality

**Appointment Management**
- Create, modify, and cancel medical appointments
- Automatic status updates (Scheduled → Completed) based on time
- Search and filter appointments by date, doctor, location
- Support for specific time slots
- Rescheduling support for cancelled appointments

**Notification & Reminders**
- 24-hour appointment reminders
- Daily medication reminders
- Home feed with recent notifications
- Configurable notification channels (In-App, Email, SMS)

**Feature Management**
- Dynamic feature toggling via Admin panel
- Insurance-based feature constraints (MINIMAL, NORMAL, PREMIUM)
- 20+ toggleable features including:
  - Search, DarkMode, Reminders, BillingInformation
  - RoomType (NORMAL+), Personel selection (PREMIUM)

**Patient Profile**
- Personal information management
- Medical history tracking
- Current medication and vaccine records
- Insurance level configuration

**Time System**
- Simulated time for testing and demonstration
- Time-based event scheduling
- Automatic appointment status transitions
- Admin panel time controls

### UI Components

**Home Page**
- Upcoming appointments display
- Recent notifications feed
- Quick access to appointment details
- Toggle between upcoming/past appointments

**Appointments Tab**
- Full appointment table with sorting
- Search and filter controls
- CRUD operations (Create, Read, Update, Delete)
- Visual separation of operations

**Admin Panel**
- Feature toggle administration
- Insurance level configuration
- Time system controls
- System-wide settings

**Patient Profile**
- Personal information
- Medical history
- Current medications
- Vaccination records

---

## 🏗️ Architecture

### Design Pattern: MVC + Observer

```
┌─────────────────────────────────────────────────────────┐
│                        VIEW LAYER                        │
│  MainFrame.java (implements 6 observer interfaces)      │
│  - Observes: Appointments, Features, Notifications,     │
│              Time, Patient                               │
│  - Updates UI automatically on model changes            │
└─────────────────────────────────────────────────────────┘
                            ▲
                            │ Observer Callbacks
                            │
┌─────────────────────────────────────────────────────────┐
│                       MODEL LAYER                        │
│  Singleton Managers (9 total):                          │
│  ├─ AppointmentManager      (appointment CRUD)          │
│  ├─ FeatureManager           (feature toggles)          │
│  ├─ TimeEventManager         (time simulation)          │
│  ├─ NotificationManager      (notification dispatch)    │
│  ├─ MedicationManager        (medication reminders)     │
│  ├─ PatientManager           (patient data)             │
│  ├─ AppointmentNotificationManager (reminders)          │
│  ├─ Logger                   (system logging)           │
│  └─ DataProvider             (data access)              │
│                                                          │
│  Observer Interfaces (6 total):                         │
│  ├─ AppointmentObserver      ├─ NotificationObserver    │
│  ├─ FeatureObserver          ├─ TimeChangeObserver      │
│  ├─ TimeEventObserver        └─ PatientObserver         │
└─────────────────────────────────────────────────────────┘
```

### Package Structure

```
src/main/java/com/mycompany/
├── data/                 # Data models
│   ├── Appointment.java
│   ├── Notification.java
│   ├── TimeEvent.java
│   └── AppointmentTableModel.java
├── model/                # Business logic
│   ├── *Manager.java     (9 singleton managers)
│   └── *Observer.java    (6 observer interfaces)
└── ui/                   # User interface
    ├── MainFrame.java    (main window)
    └── components/       (reusable UI components)
```

### Key Design Patterns

| Pattern | Usage | Benefit |
|---------|-------|---------|
| **Singleton** | All 9 managers | Single source of truth |
| **Observer** | 6 observer types | MVC separation, loose coupling |
| **Strategy** | Insurance constraints | Variable feature availability |
| **State** | Appointment status | Clear state transitions |
| **Adapter** | TimeChangeObserver | Adapt time changes to components |
| **Template Method** | Manager initialization | Consistent setup across managers |
| **Chain of Responsibility** | Feature validation | Sequential constraint checking |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **Git** (for cloning)

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/Lord-Melflam/LINFO2252_SmartMedicalManager_G7.git
cd LINFO2252_SmartMedicalManager_G7
```

2. **Build the project**
```bash
mvn clean compile
```

3. **Run tests**
```bash
mvn test
```

4. **Launch the application**
```bash
mvn exec:java
```

### Quick Start

Once the application launches:

1. **Home Tab**: View upcoming appointments and notifications
2. **Appointments Tab**: Create, modify, or cancel appointments
3. **Patient Profile Tab**: Update personal and medical information
4. **Admin Panel Tab**: Toggle features and adjust time simulation

---

## 📁 Project Structure

```
LINFO2252_SmartMedicalManager_G7/
├── src/
│   ├── main/java/com/mycompany/
│   │   ├── data/              # Data models
│   │   ├── model/             # Business logic & observers
│   │   └── ui/                # User interface
│   └── test/java/com/mycompany/
│       ├── data/              # Data model tests
│       ├── model/             # Manager tests
│       └── testsupport/       # Test utilities
├── pom.xml                    # Maven configuration
├── rewrite.yml                # Java 21 migration recipe
├── TESTING.md                 # Testing guide
├── DESIGNH.md                 # Design heuristics analysis
├── DESIGNP.md                 # Design patterns analysis
├── BAD.md                     # Code smells & improvements
├── YESNO.md                   # Framework analysis
├── IMPROVEMENTS.md            # Feature mapping
├── PRESENTATION_GUIDE.md      # Oral exam guide
├── INDEX.md                   # Navigation index
└── README.md                  # This file
```

---

## 💡 Design Decisions

### SOLID Principles Applied

**Single Responsibility Principle (SRP)**
- Each manager handles one concern (appointments, features, time, etc.)
- Observers focus on specific event types

**Open/Closed Principle (OCP)**
- Features can be added declaratively without modifying code
- New observers can be registered without modifying subjects

**Liskov Substitution Principle (LSP)**
- Any observer implementation is substitutable for the interface
- Manager singletons follow consistent patterns

**Interface Segregation Principle (ISP)**
- 6 focused observer interfaces instead of one monolithic interface
- Clients implement only needed methods

**Dependency Inversion Principle (DIP)**
- Managers depend on observer abstractions, not concrete classes
- UI implements observer interfaces, not manager dependencies

See [DESIGNH.md](DESIGNH.md) for detailed analysis with code examples.

### Why These Patterns?

**Singleton for Managers**
- Ensures single source of truth for each concern
- Global access without parameter passing
- Thread-safe access via synchronized getInstance()

**Observer for MVC**
- Decouples model from view (MainFrame doesn't call managers directly)
- Multiple observers can react independently
- Easy to add new observers without modifying subjects

**Strategy for Insurance**
- Feature availability adapts to insurance level
- New insurance plans can be added easily
- Constraints are declarative, not hard-coded

See [DESIGNP.md](DESIGNP.md) for detailed pattern analysis.

---

## 🧪 Testing

### Test Coverage

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**Test Classes:**
- `AppointmentTest` - Data model validation (3 tests)
- `AppointmentManagerTest` - Appointment CRUD (2 tests)
- `AppointmentNotificationManagerTest` - Reminder scheduling (2 tests)
- `FeatureManagerTest` - Feature toggling (2 tests)
- `MedicationManagerTest` - Medication reminders (1 test)
- `NotificationManagerTest` - Notification dispatch (1 test)
- `TimeEventManagerTest` - Event scheduling (3 tests)

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AppointmentManagerTest

# Run with detailed output
mvn test -X
```

See [TESTING.md](TESTING.md) for comprehensive testing guide.

---

## 📖 Documentation

### Analysis Documents

| Document | Purpose | Lines |
|----------|---------|-------|
| [DESIGNH.md](DESIGNH.md) | Design heuristics & SOLID principles | ~1,100 |
| [DESIGNP.md](DESIGNP.md) | Design patterns analysis | ~900 |
| [BAD.md](BAD.md) | Code smells & refactoring | ~700 |
| [YESNO.md](YESNO.md) | Framework potential analysis | ~700 |
| [IMPROVEMENTS.md](IMPROVEMENTS.md) | Feature mapping | ~600 |
| [TESTING.md](TESTING.md) | Testing philosophy & guide | ~500 |
| [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md) | Oral exam preparation | ~700 |
| [INDEX.md](INDEX.md) | Quick navigation | ~400 |

**Total**: 5,200+ lines of comprehensive analysis

### Quick Links

- **For understanding architecture**: [DESIGNH.md](DESIGNH.md)
- **For understanding patterns**: [DESIGNP.md](DESIGNP.md)
- **For finding features**: [IMPROVEMENTS.md](IMPROVEMENTS.md)
- **For presentation prep**: [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md)
- **For navigation**: [INDEX.md](INDEX.md)

---

## 🎬 Demo Guide

### Presentation Script (10 minutes)

**1. Architecture Overview (2 min)**
- Show package structure: data, model, ui
- Explain Observer pattern with diagram
- Point out 9 managers and 6 observer types

**2. Feature Demonstration (3 min)**
- **Home Tab**: Show upcoming appointments, notifications
- **Appointments Tab**: Create, modify, cancel appointment
- **Admin Panel**: Toggle features (Search, DarkMode)
- **Time System**: Change time, watch status updates

**3. Design Quality (3 min)**
- **SOLID**: Explain SRP with AppointmentManager example
- **Patterns**: Show Observer pattern in action
- **Testing**: Run tests, show 14/14 passing

**4. Insurance Demo (2 min)**
- Change insurance level: MINIMAL → NORMAL → PREMIUM
- Show features enabling/disabling (RoomType, Personel)
- Explain Strategy pattern implementation

### Interactive Demo Steps

1. **Launch Application**
```bash
mvn exec:java
```

2. **Home Page**
   - View upcoming appointments
   - Click "View Details" on an appointment
   - Toggle to past appointments
   - Show notification feed

3. **Appointments Tab**
   - Search for appointments by doctor
   - Create new appointment
   - Modify existing appointment
   - Cancel appointment (note reschedule option)

4. **Admin Panel**
   - Toggle "Search" feature (watch tab change)
   - Toggle "DarkMode" (watch theme change)
   - Change time (watch appointments auto-update to "Completed")

5. **Patient Profile**
   - Change insurance level
   - Show how features enable/disable
   - View medical history

6. **Time System**
   - Set time to tomorrow
   - Watch notification fire
   - Set time to past
   - Watch appointment status change to "Completed"

---

## 🎓 Educational Context

This project was developed as part of **LINFO2252 - Software Maintenance and Evolution** at UCL.

### Learning Objectives Demonstrated

✓ **Design Principles**: All SOLID principles applied  
✓ **Design Patterns**: 7+ patterns identified and implemented  
✓ **Code Quality**: Continuous refactoring from v1 to v2  
✓ **Testing**: Comprehensive unit test coverage  
✓ **Architecture**: Clean separation of concerns (MVC)  
✓ **Maintainability**: Extensible feature system  
✓ **Documentation**: Complete analysis (5,200+ lines)  

### Improvements from Version 1

**v1 Issues**:
- ❌ Monolithic MainFrame (blob/god object)
- ❌ Hard-coded features, no feature management
- ❌ Poor separation of concerns
- ❌ Minimal testing

**v2 Solutions**:
- ✅ Extracted managers with single responsibilities
- ✅ Dynamic feature management with constraints
- ✅ Clean MVC + Observer architecture
- ✅ 14 comprehensive unit tests
- ✅ Time simulation for testing
- ✅ Insurance-based feature gating

---

## 👥 Contributors

**Development Team**: Lord-Melflam + NaiJii on github (LINFO2252 Group 7)

**Course**: LINFO2252 - Software Maintenance and Evolution  
**Institution**: Université catholique de Louvain (UCL)  
**Academic Year**: 2025-2026

---

## 📝 License

This project is developed for educational purposes as part of university coursework.

---

## 🔗 Additional Resources

### For Developers

- **Architecture**: See [DESIGNH.md](DESIGNH.md) for SOLID principles
- **Patterns**: See [DESIGNP.md](DESIGNP.md) for design patterns
- **Testing**: See [TESTING.md](TESTING.md) for test strategy
- **Features**: See [IMPROVEMENTS.md](IMPROVEMENTS.md) for feature locations

### For Reviewers/Examiners

- **Presentation**: See [PRESENTATION_GUIDE.md](PRESENTATION_GUIDE.md)
- **Navigation**: See [INDEX.md](INDEX.md)
- **Code Quality**: See [BAD.md](BAD.md) for honest assessment
- **Framework**: See [YESNO.md](YESNO.md) for evolution potential

---

## 📊 Project Statistics

```
Java Files:          23 production + 8 test
Lines of Code:       ~4,500 (production)
Design Patterns:     7 identified
SOLID Principles:    All 5 applied
Test Coverage:       14 tests, 100% passing
Observer Types:      6 interfaces
Manager Classes:     9 singletons
Features:            20+ toggleable
Documentation:       5,200+ lines of analysis
```

---

## 🎯 Quick Start Commands

```bash
# Clone repository
git clone https://github.com/Lord-Melflam/LINFO2252_SmartMedicalManager_G7.git

# Navigate to project
cd LINFO2252_SmartMedicalManager_G7

# Compile
mvn clean compile

# Run tests
mvn test

# Launch application
mvn exec:java
```

---

**Built with ❤️ for Software Quality and Maintainability**
   - Reminder system: show a reminder being sent and received. (Make sure to modify date after time, because TimePicker does not have change events).
   - Show an appointment being created, modified, cancelled or completed and how the home page updates accordingly.
   - Show that an appointment can't be booked or rescheduled to a time in the past.
   - Show that UX is intuitive by adding shortcuts (double clicks on appointments, time picker will open on current date/time, etc.)
   - Show sorting on columns.
4. Mention architecture:
   - Managers + Observers (Model), `MainFrame` UI and handlers (View/Controller), reusable UI components.
   - Talk about time type handling struggles.
   - Talk about god class `MainFrame` and possible refactoring.

## Future Work
- Data persistence (JSON/SQLite) — see guide not committed.
- Email sending for reminders — currently preferences only.

## Key Files (Quick Links)
- Model: [AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java), [FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java), [PatientManager.java](src/main/java/com/mycompany/model/PatientManager.java)
- Observers: [AppointmentObserver.java](src/main/java/com/mycompany/model/AppointmentObserver.java), [FeatureObserver.java](src/main/java/com/mycompany/model/FeatureObserver.java), [PatientObserver.java](src/main/java/com/mycompany/model/PatientObserver.java)
- View/Controller: [MainFrame.java](src/main/java/com/mycompany/ui/MainFrame.java)
- Table Adapter: [AppointmentTableModel.java](src/main/java/com/mycompany/ui/model/AppointmentTableModel.java)
- UI Components: [TimePickerPanel.java](src/main/java/com/mycompany/ui/components/TimePickerPanel.java), [AppointmentFilterPanel.java](src/main/java/com/mycompany/ui/components/AppointmentFilterPanel.java)
