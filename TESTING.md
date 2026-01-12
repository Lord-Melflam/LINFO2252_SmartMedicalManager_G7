# Testing Guide

## Testing Philosophy

The SmartMedicalManager project follows a comprehensive testing approach that emphasizes:

### Core Principles

1. **Observer Pattern Validation**: Tests verify the correct implementation of the Observer pattern across multiple managers (Notification, Time Events, Appointments, Features).

2. **Singleton Pattern Testing**: Several manager classes use the Singleton pattern, and tests include setup utilities ([SingletonReset.java](src/test/java/com/mycompany/testsupport/SingletonReset.java)) to ensure proper isolation between test cases.

3. **Time-based Event Testing**: The TimeEventManager allows simulating time progression, enabling deterministic testing of scheduled events and reminders without relying on actual system time.

4. **Business Logic Focus**: Tests concentrate on core business logic rather than UI components, ensuring the model layer is robust and reliable.

## Test Structure

### Test Organization

```
src/test/java/com/mycompany/
├── data/
│   └── AppointmentTest.java          # Data model tests
├── model/
│   ├── AppointmentManagerTest.java
│   ├── AppointmentNotificationManagerTest.java
│   ├── FeatureManagerTest.java
│   ├── MedicationManagerTest.java
│   ├── NotificationManagerTest.java
│   └── TimeEventManagerTest.java
└── testsupport/
    └── SingletonReset.java            # Test utility for singleton reset
```

### Test Coverage

| Test Class | Focus Area | Number of Tests |
|------------|-----------|-----------------|
| `AppointmentTest` | Appointment data model | 3 |
| `NotificationManagerTest` | Notification dispatch | 1 |
| `MedicationManagerTest` | Medication reminders | 1 |
| `AppointmentManagerTest` | Appointment management | 2 |
| `TimeEventManagerTest` | Time event scheduling | 3 |
| `AppointmentNotificationManagerTest` | Appointment reminders | 2 |
| `FeatureManagerTest` | Feature toggle management | 2 |
| **Total** | | **14** |

## Running Tests

### Using Maven

#### Run All Tests
```bash
mvn test
```

#### Run a Specific Test Class
```bash
mvn test -Dtest=AppointmentManagerTest
```

#### Run Tests with Detailed Output
```bash
mvn test -X
```

#### Clean Build and Test
```bash
mvn clean test
```

### Using IDE

#### IntelliJ IDEA
- Right-click on the `test` folder → **Run 'All Tests'**
- Right-click on a specific test class → **Run 'TestClassName'**
- Use keyboard shortcut: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)

#### NetBeans
- Right-click on the project → **Test**
- Right-click on a test class → **Test File** (`Alt+F6`)
- Right-click on a test method → **Run Focused Test Method**

## Test Framework

### Dependencies

The project uses **JUnit 5 (Jupiter)** for testing:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.4</version>
    <scope>test</scope>
</dependency>
```

### Maven Surefire Plugin

Tests are executed using the Maven Surefire plugin (version 3.5.2):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
</plugin>
```

## Testing Patterns

### 1. Observer Pattern Testing

Tests verify that observers are properly notified when events occur:

```java
// Example: NotificationManager test
@Test
void testNotificationDispatch() {
    NotificationManager manager = NotificationManager.getInstance();
    manager.addListener(notification -> {
        // Verify notification received
    });
    manager.notifyObservers(notification);
}
```

### 2. Time Simulation

The `TimeEventManager` allows setting arbitrary dates for testing time-dependent behavior:

```java
// Set a specific date for testing
timeEventManager.setCurrentDate(LocalDateTime.of(2026, 1, 11, 10, 0));

// Schedule an event
timeEventManager.scheduleEvent(eventId, targetDateTime, description);

// Advance time and verify event fires
timeEventManager.setCurrentDate(targetDateTime);
```

### 3. Singleton Reset

Use `SingletonReset` utility to ensure test isolation:

```java
@BeforeEach
void setUp() {
    SingletonReset.resetAllSingletons();
}
```

## Expected Test Output

A successful test run should produce:

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.mycompany.data.AppointmentTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.mycompany.model.NotificationManagerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] Results:
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Logging in Tests

Tests produce structured logging output that helps verify system behavior:

```
[TimeEventManager] Current date set to 11-01-2026 10:00
[TimeEventManager] Scheduled event 'reminder' at 11-01-2026 10:01
[NotificationManager] Dispatching notification: Medication Reminder
```

These logs demonstrate:
- Event scheduling and firing
- Notification dispatch
- Observer registration and callbacks
- Time progression simulation

## Best Practices

1. **Test Isolation**: Each test should be independent and not rely on the state from other tests.

2. **Descriptive Names**: Use clear, descriptive test method names that explain what is being tested.

3. **Arrange-Act-Assert**: Follow the AAA pattern:
   - **Arrange**: Set up test data and conditions
   - **Act**: Execute the code being tested
   - **Assert**: Verify the results

4. **Mock Time**: Use `TimeEventManager.setCurrentDate()` instead of relying on system time for predictable tests.

5. **Clean Up**: Reset singletons between tests to avoid side effects.

## Continuous Integration

The test suite is designed to run in CI/CD pipelines:

```bash
# Simple CI command
mvn clean test

# With coverage reporting (if configured)
mvn clean test jacoco:report
```

## Future Enhancements

Potential areas for expanding test coverage:

- [ ] UI component testing (currently minimal)
- [ ] Integration tests for complete user workflows
- [ ] Performance/stress testing for appointment scheduling
- [ ] Edge case testing for date/time boundaries
- [ ] Patient data management tests
- [ ] Database persistence tests (if implemented)

## Troubleshooting

### Common Issues

**Problem**: Tests fail with singleton-related errors  
**Solution**: Ensure `SingletonReset.resetAllSingletons()` is called in `@BeforeEach`

**Problem**: Time-dependent tests are flaky  
**Solution**: Use `TimeEventManager.setCurrentDate()` instead of `LocalDateTime.now()`

**Problem**: Maven can't find test dependencies  
**Solution**: Run `mvn clean install` to download dependencies

## Additional Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Maven Surefire Plugin Documentation](https://maven.apache.org/surefire/maven-surefire-plugin/)
- Project source code: [src/test/java/](src/test/java/)
