# LINFO2252: Smart Medical Appointment Manager (Group 07)

## 1. Project Overview

This repository contains the source code for the dynamically adaptive **Smart Medical Appointment Manager** system, developed for the LINFO2252: Software Maintenance and Evolution course at UCL.

The application is built around a **Feature Model** (designed in Lab 1) which allows core functionalities and optional features (like Payment, Reminders, and Dark Mode) to be activated or deactivated at runtime, changing the application's behavior and UI instantly.

### Key Architectural Goals:
1.  **Maintainability:** Achieved through the **Model-View-Controller (MVC)** design pattern.
2.  **Dynamic Adaptability:** Managed by the **Controller** interface and the **Time Event System (TES)**.

---

## 2. Running the Application

### 2.1 Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher (e.g., OpenJDK 21).
* **IDE:** IntelliJ IDEA or Eclipse.

### 2.2 Execution
1.  Clone the repository: `git clone <repository_url>`
2.  Open the project in your IDE.
3.  Compile all `.java` files.
4.  Run the `Main.java` class.

### 2.3 Interactive Commands (Lab 3)

Once the application is running, enter commands in the terminal to interact with the system and test dynamic adaptation:

| Command | Action | Component Tested |
| :--- | :--- | :--- |
| `dark` / `light` | Toggle the `DARK_MODE` UI feature. | Controller, Model, View (Dynamic UI) |
| `add` / `remove` | Toggle the `DYNAMIC_BUTTON` feature. | Controller, Model, View (Dynamic UI) |
| `title <text>` | Change the window title. | View (Direct Manipulation) |
| `day` / `week` | Advance the simulated time. | Model (Time Event System Placeholder) |
| `event <name>` | Trigger a specific adaptive event (e.g., `event doctor_gone`). | Model (Adaptive Logic) |
| `stop` | Shut down the application. | System Control |

---

## 3. Repository Structure

| File/Folder | Purpose | Lab Focus |
| :--- | :--- | :--- |
| `ControllerInterface.java` | Defines the required interface for feature management and UI control. | Labs 0-3 |
| `SmartMedicalController.java` | **C (Controller):** Mediates commands, handles feature (de)activations, and notifies the View. | Labs 0-3 |
| `SmartMedicalModel.java` | **M (Model):** Stores the application data, manages the **active feature set**, and enforces constraints. | Labs 0-3 |
| `SmartMedicalView.java` | **V (View):** Handles all Java Swing UI components and adapts its display based on the Model's state. | Lab 3 |
| `Feature.java` | Enumeration of all features from the Lab 1 Feature Model. | Labs 1-3 |
| `Main.java` | Entry point; sets up the MVC structure and runs the command-line loop. | Labs 0-3 |
| `docs/` | Contains project documentation (e.g., `feature_model.xml`, `M1_Group_SME07.pdf`). | Lab 1 |

---

## 4. Collaboration Workflow (Group 07)

**Goal:** Ensure we never push directly to `main` and avoid merge conflicts.

1.  **Pull First:** Always run `git pull origin main` before starting a new task.
2.  **Branch for Work:** For any new feature or bug fix, create a new branch:
    `git checkout -b feature/implement-tes-logic`
3.  **Commit Regularly:** Commit your changes with descriptive messages.
4.  **Push and PR:** When the feature is complete, push your branch and open a **Pull Request (PR)** on GitHub targeting `main`.
5.  **Review:** **The teammate not working on the PR must review and approve it** before merging to `main`.
