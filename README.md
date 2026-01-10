# SmartMedicalManager — End-to-End Implementation & Git Workflow (Mel)

## Overview
This document summarizes the end-to-end work done to prepare the Smart Medical Appointment Manager for presentation: architectural mapping (MVC + Observer), feature implementations, build/run steps, and the Git workflow used to deliver changes on a clean branch (`dev2`) while excluding non-code docs.

## Goals
- Implement accurate appointment categorization using current date + time.
- Improve Home page listing with dynamic scaling and proper notifications behavior.
- Add clear separation of search/filters from appointment operations.
- Support rescheduling of cancelled appointments in the modify flow.
- Keep a clean, explainable Git history on a dedicated branch.

## Architecture (MVC + Observer)
- **Model (Managers + Observers)**
  - [src/main/java/com/mycompany/model/AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java): Stores, queries, updates appointments; notifies observers.
  - [src/main/java/com/mycompany/model/FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java): Feature flags and attributes.
  - [src/main/java/com/mycompany/model/PatientManager.java](src/main/java/com/mycompany/model/PatientManager.java): Patient data.
  - Observers: [AppointmentObserver.java](src/main/java/com/mycompany/model/AppointmentObserver.java), [FeatureObserver.java](src/main/java/com/mycompany/model/FeatureObserver.java), [PatientObserver.java](src/main/java/com/mycompany/model/PatientObserver.java).

- **View (Swing UI)**
  - [src/main/java/com/mycompany/ui/MainFrame.java](src/main/java/com/mycompany/ui/MainFrame.java): Main UI; renders tabs and updates based on observer callbacks.
  - Table adapter (part of View): [src/main/java/com/mycompany/ui/model/AppointmentTableModel.java](src/main/java/com/mycompany/ui/model/AppointmentTableModel.java).
  - UI components: [src/main/java/com/mycompany/ui/components/TimePickerPanel.java](src/main/java/com/mycompany/ui/components/TimePickerPanel.java), [src/main/java/com/mycompany/ui/components/AppointmentFilterPanel.java](src/main/java/com/mycompany/ui/components/AppointmentFilterPanel.java).

- **Controller (in View handlers)**
  - In `MainFrame.java`: `bookBtnActionPerformed()`, `modifyBtnActionPerformed()`, `cancelAppBtnActionPerformed()`, `applyFilterBtnActionPerformed()`, `toggleHomePageAppointments()`.

## Feature Implementations
1. **Accurate Past vs Upcoming (Time-of-Day aware)**
   - Updated categorization to use `LocalDateTime` with pattern `dd-MM-yyyy HH:mm`.
   - Implemented in [AppointmentManager](src/main/java/com/mycompany/model/AppointmentManager.java):
     - `getUpcomingAppointments()` — scheduled at or after now; sorted ascending.
     - `getPastAppointments()` — completed or scheduled before now; sorted descending.

2. **Home Page: Dynamic List + Proper Notifications**
   - Rebuilt the home list dynamically so all appointments show with a "View Details" button; panel auto-scales and scrolls.
   - Notifications & Reminders show only for upcoming; they are hidden for past.
   - Implemented in [MainFrame](src/main/java/com/mycompany/ui/MainFrame.java): `updateHomePageAppointments()`.

3. **Appointments Tab: Clear Separation**
   - Added a "Search & Filters" heading and a separator above the search bar and time range list.
   - Added an "Appointment Operations" heading above New/Modify/Cancel buttons.
   - Implemented in [MainFrame](src/main/java/com/mycompany/ui/MainFrame.java) layout groups.

4. **Reschedule Cancelled Appointments**
   - When modifying a cancelled appointment, prompt the user to reschedule; if yes, status switches back to `Scheduled` before editing.
   - Implemented in [MainFrame](src/main/java/com/mycompany/ui/MainFrame.java): `modifyBtnActionPerformed()`.

## Build & Run
- Compile:
```bash
mvn compile -q
```
- Run:
```bash
mvn -q exec:java
```
- Notes:
  - Exit code `130` during `exec:java` usually indicates the app was manually terminated; compilation success confirms the code is valid.

## Git Workflow (Branching, Exclusions, Commits)
1. **Exclude non-code docs**
   - `.gitignore` excludes the following from commits:
     - `labs_and_final_guidelines/`
     - `feature_diagram/`
     - `LATEST_UPDATES_SUMMARY.md`
     - `DATA_PERSISTENCE_GUIDE.md`

2. **Create and work on `dev2` branch**
```bash
git checkout -b dev2
```

3. **Stage and commit changes with detailed messages**
- Examples of commit messages used:
  - feat(home): auto-scale home list; hide past notifications
  - feat(appointments): classify using exact time-of-day
  - ux(appointments): add operations header; reschedule prompt

4. **Push branch to remote**
```bash
git push -u origin dev2
```

5. **Optional: Open PR to `main`**
- Create a pull request from `dev2` to `main` describing the architecture and changes.

## Demo Script (Presentation)
1. Launch the app (`mvn -q exec:java`).
2. Show Home tab:
   - Toggle between upcoming/past; note dynamic list and per-item "View Details" buttons.
   - Confirm notifications list only appears for upcoming.
3. Show Appointments tab:
   - Demonstrate search + filters under the "Search & Filters" section.
   - Demonstrate create/modify/cancel under "Appointment Operations".
   - Modify a cancelled appointment → confirm reschedule prompt.
4. Mention architecture:
   - Managers + Observers (Model), `MainFrame` UI and handlers (View/Controller), reusable UI components.

## Future Work
- Data persistence (JSON/SQLite) — see guide not committed.
- Email sending for reminders — currently preferences only.

## Key Files (Quick Links)
- Model: [AppointmentManager.java](src/main/java/com/mycompany/model/AppointmentManager.java), [FeatureManager.java](src/main/java/com/mycompany/model/FeatureManager.java), [PatientManager.java](src/main/java/com/mycompany/model/PatientManager.java)
- Observers: [AppointmentObserver.java](src/main/java/com/mycompany/model/AppointmentObserver.java), [FeatureObserver.java](src/main/java/com/mycompany/model/FeatureObserver.java), [PatientObserver.java](src/main/java/com/mycompany/model/PatientObserver.java)
- View/Controller: [MainFrame.java](src/main/java/com/mycompany/ui/MainFrame.java)
- Table Adapter: [AppointmentTableModel.java](src/main/java/com/mycompany/ui/model/AppointmentTableModel.java)
- UI Components: [TimePickerPanel.java](src/main/java/com/mycompany/ui/components/TimePickerPanel.java), [AppointmentFilterPanel.java](src/main/java/com/mycompany/ui/components/AppointmentFilterPanel.java)
