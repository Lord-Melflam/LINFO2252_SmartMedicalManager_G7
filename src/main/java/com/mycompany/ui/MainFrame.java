/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.ui;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.mycompany.data.Appointment;
import com.mycompany.data.AppointmentTableModel;
import com.mycompany.data.DataProvider;
import com.mycompany.data.Notification;
import com.mycompany.model.*;
import com.mycompany.ui.components.*;

import javax.swing.*;
import javax.swing.table.TableRowSorter;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * Main application frame for the Smart Medical Manager.
 * Follows MVC pattern with separated concerns.
 * Observes Model changes and updates View accordingly.
 *
 * @author Ji
 */
public class MainFrame extends javax.swing.JFrame implements FeatureObserver, PatientObserver, AppointmentObserver, NotificationObserver, TimeChangeObserver {
    
    private static final Logger logger = Logger.getInstance();
    private static final String TAG = "View";
    private static final DateTimeFormatter UI_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter UI_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private AppointmentTableModel appointmentModel;
    private AppointmentManager appointmentManager;
    private FeatureManager featureManager;
    private PatientManager patientManager;
    private DataProvider dataProvider;
    private TimePickerPanel timePicker;
    private TimePickerPanel adminTimePicker;
    private boolean showUpcomingOnHome = true; // Toggle for home page appointments
    private Appointment appointmentBeingModified = null; // Track which appointment is being edited
    private AppointmentNotificationManager appointmentNotificationManager;
    private boolean homeReminderListListenerInstalled = false;
    private NotificationManager notificationManager;
    private TimeEventManager timeEventManager;
    private DefaultListModel<String> homeFeedModel;
    private final java.util.List<HomeFeedItem> homeFeedItems = new java.util.ArrayList<>();
    private final java.util.List<Notification> homeNotifications = new java.util.ArrayList<>();
    private static final int HOME_MAX_NOTIFICATIONS = 30;
    private javax.swing.JScrollPane adminFeatureScroll;
    private javax.swing.JPanel adminFeaturePanel;
    private final Map<String, JPanel> adminFeatureRows = new HashMap<>();
    private TableRowSorter<AppointmentTableModel> appointmentSorter;

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();

        // Home feed list combines reminders + notifications
        this.homeFeedModel = new DefaultListModel<>();
        jList1.setModel(homeFeedModel);

        this.appointmentManager = AppointmentManager.getInstance();
        this.featureManager = FeatureManager.getInstance();
        this.patientManager = PatientManager.getInstance();
        this.dataProvider = DataProvider.getInstance();
        this.timeEventManager = TimeEventManager.getInstance();
        this.notificationManager = NotificationManager.getInstance();

        timeEventManager.registerTimeObserver(this);

        // Register as observer for model changes
        appointmentManager.registerObserver(this);
        featureManager.registerObserver(this);
        patientManager.registerObserver(this);
        notificationManager.registerObserver(this);
        
        appointmentNotificationManager = new AppointmentNotificationManager(notificationManager, timeEventManager);
        
        initializeUI();
        logger.log(TAG, "MainFrame constructed and UI initialized.");
    }

    @Override
    public void onTimeChanged(java.util.Date newNow) {
        SwingUtilities.invokeLater(() -> {
            updateHomePageAppointments();
            // Also repaint appointments table because sorting/filtering depends on simulated 'now'
            if (appointmentsTable != null) {
                appointmentsTable.repaint();
            }
        });
    }
    
    /**
     * Initialize UI with data from Model layer.
     * Separates data loading from UI generation.
     */
    private void initializeUI() {
        initializeAppointmentData();
        setupSearchFunctionality();
        populateDropdowns();
        initializeTimePicker();
        updateProfileDisplay();
        updateHomePageAppointments();
        buildAdminFeatureControls();
        refreshFeatureUI();
        bindTimeEventSystem();
    }

    private void bindTimeEventSystem() {
        try {
            jCalendar1.addPropertyChangeListener("calendar", evt -> onAdminCalendarChanged());
            jCalendar1.addPropertyChangeListener("date", evt -> onAdminCalendarChanged());

            if (adminTimePicker != null) {
                adminTimePicker.addChangeListener(e -> onAdminCalendarChanged());
            }

            // Apply current UI selection once at startup
            onAdminCalendarChanged();
        } catch (Exception e) {
            logger.logError(TAG, "Failed to bind time event calendar: " + e.getMessage());
        }
    }

    private void onAdminCalendarChanged() {
        try {
            java.util.Date d = jCalendar1.getDate();
            if (d == null) return;

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(d);

            if (adminTimePicker != null) {
                String t = adminTimePicker.getSelectedTime();
                String[] parts = (t == null) ? new String[0] : t.split(":");
                if (parts.length == 2) {
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    timeEventManager.setDateTime(d, hour, minute);
                    logger.log(TAG, "Admin date/time changed, updated TimeEventManager date.");
                    return;
                }
            }
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);

            timeEventManager.setDate(cal.getTime());
            logger.log(TAG, "Admin calendar changed, updated TimeEventManager date.");
        } catch (Exception e) {
            logger.logError(TAG, "Error applying time event calendar selection: " + e.getMessage());
        }
    }

    private void refreshFeatureUI() {
        SwingUtilities.invokeLater(() -> {
            updateThemeByFeatures();
            updateFilterByFeatures();
            updateSortingByFeatures();
            updateBookingDropdownsByFeatures();
            updateSearchByFeatures();
        });
    }

    private void updateSortingByFeatures() {
        boolean sortEnabled = featureManager.isFeatureActive("Sort");

        if (!sortEnabled) {
            appointmentSorter = null;
            appointmentsTable.setRowSorter(null);
            return;
        }

        if (appointmentSorter == null || appointmentsTable.getRowSorter() != appointmentSorter) {
            appointmentSorter = new TableRowSorter<>(appointmentModel);

            appointmentSorter.setComparator(0, (a, b) -> {
                try {
                    LocalDate da = LocalDate.parse(String.valueOf(a), UI_DATE_FMT);
                    LocalDate db = LocalDate.parse(String.valueOf(b), UI_DATE_FMT);
                    return da.compareTo(db);
                } catch (Exception ignored) {
                    return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
                }
            });

            appointmentSorter.setComparator(1, (a, b) -> {
                try {
                    LocalTime ta = LocalTime.parse(String.valueOf(a), UI_TIME_FMT);
                    LocalTime tb = LocalTime.parse(String.valueOf(b), UI_TIME_FMT);
                    return ta.compareTo(tb);
                } catch (Exception ignored) {
                    return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
                }
            });

            appointmentsTable.setRowSorter(appointmentSorter);
        }

        boolean sortByDate = featureManager.isFeatureActive("SortByDate");
        boolean sortByType = featureManager.isFeatureActive("SortByType");
        boolean sortByService = featureManager.isFeatureActive("SortByService");

        // Column mapping in AppointmentTableModel:
        // 0 Date, 1 Time, 2 Doctor, 3 Location, 4 Reason, 5 Status
        //
        // SortByDate -> Date + Time
        appointmentSorter.setSortable(0, sortByDate);
        appointmentSorter.setSortable(1, sortByDate);

        // SortByService -> Doctor + Location (service/staff-related)
        appointmentSorter.setSortable(2, sortByService);
        appointmentSorter.setSortable(3, sortByService);

        // SortByType -> Reason (consultation type/reason)
        appointmentSorter.setSortable(4, sortByType);
        appointmentSorter.setSortable(5, true);
    }

    private void updateFilterByFeatures() {
        boolean pastEnabled = featureManager.isFeatureActive("PastConsultations");
        String currentlySelected = timePeriodList.getSelectedValue();

        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("Today");
        items.add("This Week");
        items.add("All Upcoming");
        if (pastEnabled) {
            items.add("Past Appointments");
        }

        timePeriodList.setModel(new javax.swing.AbstractListModel<String>() {
            @Override
            public int getSize() { return items.size(); }
            @Override
            public String getElementAt(int i) { return items.get(i); }
        });

        if (currentlySelected != null && items.contains(currentlySelected)) {
            timePeriodList.setSelectedValue(currentlySelected, true);
        } else if (!items.isEmpty()) {
            timePeriodList.setSelectedIndex(0);
        }
    }

    private void updateThemeByFeatures() {
        boolean dark = featureManager.isFeatureActive("DarkMode");
        try {
            if (dark) {
                FlatDarculaLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            FlatLaf.updateUI();
            SwingUtilities.updateComponentTreeUI(this);
            this.repaint();
        } catch (Exception e) {
            logger.logError(TAG, "Failed to apply theme: " + e.getMessage());
        }
    }

    private void updateSearchByFeatures() {
        boolean searchEnabled = featureManager.isFeatureActive("BasicSearch") || featureManager.isFeatureActive("AdvancedSearch");
        searchBar.setEnabled(searchEnabled);
        // disable filter too
        applyFilterBtn.setEnabled(searchEnabled);

        if (!searchEnabled) {
            appointmentModel.clearFilter();
        }
    }

    private void updateBookingDropdownsByFeatures() {
        consultationType.setEnabled(featureManager.isFeatureActive("ConsultationType"));
        location.setEnabled(featureManager.isFeatureActive("ConsultationLocation"));
        roomType.setEnabled(featureManager.isFeatureActive("RoomType"));
        personnel.setEnabled(featureManager.isFeatureActive("Personel"));
    }

    private void buildAdminFeatureControls() {
        adminFeaturePanel.removeAll();
        adminFeatureRows.clear();

        for (String feature : FeatureManager.getAvailableFeatures()) {
            if (featureManager.isMandatory(feature)) {
                continue; 
            }

            logger.log(TAG, "Building admin control for feature: " + feature);

            javax.swing.JPanel row = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 2));
            javax.swing.JCheckBox toggle = new javax.swing.JCheckBox(feature);
            try {
                FeatureManager.ChoiceDefinition def = FeatureManager.getChoiceDefinition(feature);
                if (def != null && !def.getChoices().isEmpty()) {
                    toggle.setToolTipText("Choices: " + def.getChoices());
                }
            } catch (Exception ignored) {
                // Keep UI resilient
            }
            toggle.setSelected(featureManager.isFeatureActive(feature));
            toggle.addActionListener(e -> handleFeatureToggle(feature, toggle.isSelected(), toggle));

            javax.swing.JButton configBtn = new javax.swing.JButton("Configure");
            configBtn.addActionListener(e -> {
                FeatureManager.ChoiceDefinition def;
                try {
                    def = FeatureManager.getChoiceDefinition(feature);
                } catch (Exception ex) {
                    def = null;
                }

                if (def != null && !def.getChoices().isEmpty()) {
                    Object existing = featureManager.getFeatureAttribute(feature, "value");
                    String existingStr = (existing == null) ? null : String.valueOf(existing);

                    Object selected = javax.swing.JOptionPane.showInputDialog(
                        this,
                        "Select value for " + feature + ":",
                        "Configure " + feature,
                        javax.swing.JOptionPane.QUESTION_MESSAGE,
                        null,
                        def.getChoices().toArray(new String[0]),
                        existingStr
                    );

                    if (selected != null) {
                        featureManager.setFeatureAttribute(feature, "value", String.valueOf(selected));
                    }
                    return;
                }

                Object existing = featureManager.getFeatureAttribute(feature, "value");
                String val = javax.swing.JOptionPane.showInputDialog(this, "Set value for " + feature, existing);
                if (val != null) {
                    featureManager.setFeatureAttribute(feature, "value", val);
                }
            });

            row.add(toggle);
            row.add(configBtn);
            adminFeatureRows.put(feature, row);
            adminFeaturePanel.add(row);
        }

        adminFeaturePanel.revalidate();
        adminFeaturePanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        adminFeaturePanel = new javax.swing.JPanel();
        adminFeatureScroll = new javax.swing.JScrollPane();
        jPopupMenu1 = new javax.swing.JPopupMenu();
        mainTabs = new javax.swing.JTabbedPane();
        homeTab = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        upcomingAppointments = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        date1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        date2 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        date3 = new javax.swing.JLabel();
        jButton3 = new javax.swing.JButton();
        notificationsRemindersList = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jLabel6 = new javax.swing.JLabel();
        toggleAppointmentsBtn = new javax.swing.JButton();
        appointmentsTab = new javax.swing.JPanel();
        appointmentsView = new javax.swing.JPanel();
        appointmentsTableScroll = new javax.swing.JScrollPane();
        appointmentsTable = new javax.swing.JTable();
        newBtn = new javax.swing.JButton();
        filterLabel = new javax.swing.JLabel();
        appointmentOpsLabel = new javax.swing.JLabel();
        searchBar = new javax.swing.JTextField();
        modifyBtn = new javax.swing.JButton();
        cancelAppBtn = new javax.swing.JButton();
        applyFilterBtn = new javax.swing.JButton();
        timePeriodList = new javax.swing.JList<>();
        filterSeparator = new javax.swing.JSeparator();
        bookPanel = new javax.swing.JPanel();
        consultationType = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        location = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        personnel = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        roomType = new javax.swing.JComboBox<>();
        date = new com.toedter.calendar.JCalendar();
        bookBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        profileTab = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanel9 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();

        jCheckBox5 = new javax.swing.JCheckBox();
        jCheckBox4 = new javax.swing.JCheckBox();
        jCheckBox1 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jComboBox4 = new javax.swing.JComboBox<>();
        jComboBox5 = new javax.swing.JComboBox<>();
        jLabel17 = new javax.swing.JLabel();
        adminTab = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jCalendar1 = new com.toedter.calendar.JCalendar();
        adminTimePicker = new com.mycompany.ui.components.TimePickerPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel2.setText("Notifications & Reminders");

        jPanel2.setLayout(new java.awt.GridLayout(0, 2));

        date1.setText("");
        jPanel2.add(date1);

        jButton1.setText("View Details");
        jButton1.addActionListener(this::jButton1ActionPerformed);
        jPanel2.add(jButton1);

        date2.setText("");
        jPanel2.add(date2);

        jButton2.setText("View Details");
        jButton2.addActionListener(this::jButton2ActionPerformed);
        jPanel2.add(jButton2);

        date3.setText("");
        jPanel2.add(date3);

        jButton3.setText("View Details");
        jButton3.addActionListener(this::jButton3ActionPerformed);
        jPanel2.add(jButton3);

        upcomingAppointments.setViewportView(jPanel2);

        notificationsRemindersList.setViewportView(jList1);

        jLabel6.setText("Upcoming appointment(s)");
        jLabel6.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));

        toggleAppointmentsBtn.setText("Show Past Appointments");
        toggleAppointmentsBtn.addActionListener(e -> toggleHomePageAppointments());

        javax.swing.GroupLayout homeTabLayout = new javax.swing.GroupLayout(homeTab);
        homeTab.setLayout(homeTabLayout);
        homeTabLayout.setHorizontalGroup(
                homeTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(homeTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(homeTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(upcomingAppointments)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 783, Short.MAX_VALUE)
                                        .addComponent(notificationsRemindersList)
                                        .addGroup(homeTabLayout.createSequentialGroup()
                                                .addComponent(jLabel6)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(toggleAppointmentsBtn)))
                                .addContainerGap())
        );
        homeTabLayout.setVerticalGroup(
                homeTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(homeTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(homeTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(toggleAppointmentsBtn))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(upcomingAppointments, javax.swing.GroupLayout.PREFERRED_SIZE, 451, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(notificationsRemindersList, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        mainTabs.addTab("Home", homeTab);

        appointmentsTab.setLayout(new java.awt.CardLayout());

        // Table model will be set in initializeAppointmentData()
        appointmentsTable.setModel(new javax.swing.table.DefaultTableModel());
        appointmentsTable.setColumnSelectionAllowed(true);
        appointmentsTableScroll.setViewportView(appointmentsTable);
        appointmentsTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        if (appointmentsTable.getColumnModel().getColumnCount() > 0) {
            appointmentsTable.getColumnModel().getColumn(0).setResizable(false);
            appointmentsTable.getColumnModel().getColumn(1).setResizable(false);
            appointmentsTable.getColumnModel().getColumn(2).setResizable(false);
            appointmentsTable.getColumnModel().getColumn(3).setResizable(false);
            appointmentsTable.getColumnModel().getColumn(4).setResizable(false);
        }

        newBtn.setText("New Appointment");
        newBtn.addActionListener(this::newBtnActionPerformed);

        filterLabel.setText("Search & Filters");

        appointmentOpsLabel.setText("Appointment Operations");

        searchBar.setText("Search...");
        searchBar.setToolTipText("");
        searchBar.addActionListener(this::searchBarActionPerformed);

        modifyBtn.setText("Modify Appointment");
        modifyBtn.addActionListener(this::modifyBtnActionPerformed);

        cancelAppBtn.setText("Cancel Appointment");
        cancelAppBtn.addActionListener(this::cancelAppBtnActionPerformed);

        applyFilterBtn.setText("Apply Filter");
        applyFilterBtn.addActionListener(this::applyFilterBtnActionPerformed);

        timePeriodList.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Today", "This Week", "All Upcoming", "Past Appointments" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });

        filterSeparator.setOrientation(javax.swing.SwingConstants.HORIZONTAL);

        javax.swing.GroupLayout appointmentsViewLayout = new javax.swing.GroupLayout(appointmentsView);
        appointmentsView.setLayout(appointmentsViewLayout);
        appointmentsViewLayout.setHorizontalGroup(
                appointmentsViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(appointmentsViewLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(appointmentsViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(modifyBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(newBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(applyFilterBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(filterSeparator)
                                        .addComponent(searchBar)
                                        .addComponent(cancelAppBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(filterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(appointmentOpsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(appointmentsViewLayout.createSequentialGroup()
                                                .addGap(0, 2, Short.MAX_VALUE)
                                                .addComponent(timePeriodList, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(18, 18, 18)
                                .addComponent(appointmentsTableScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 593, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        appointmentsViewLayout.setVerticalGroup(
                appointmentsViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(appointmentsViewLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(appointmentsViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(appointmentsTableScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 726, Short.MAX_VALUE)
                                        .addGroup(appointmentsViewLayout.createSequentialGroup()
                                                .addComponent(filterLabel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(searchBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(timePeriodList, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(14, 14, 14)
                                                .addComponent(applyFilterBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(filterSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(appointmentOpsLabel)
                                                .addGap(18, 18, 18)
                                                .addComponent(newBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(modifyBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(cancelAppBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addContainerGap())))
        );

        appointmentsTab.add(appointmentsView, "card6");

        consultationType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Surgery", "Dentist", "Type1", "Type2", "Type3" }));
        consultationType.addActionListener(this::consultationTypeActionPerformed);

        jLabel7.setText("Consultation Type");

        jLabel8.setText("Location");

        location.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hospital 1", "Hospital 2", "Hospital 3" }));
        location.addActionListener(this::locationActionPerformed);

        jLabel9.setText("Personnel");

        personnel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Doctor A", "Doctor B", "Doctor C" }));
        personnel.addActionListener(this::personnelActionPerformed);

        jLabel10.setText("Room Type");

        roomType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Shared", "Private" }));
        roomType.addActionListener(this::roomTypeActionPerformed);

        bookBtn.setText("Book");
        bookBtn.addActionListener(this::bookBtnActionPerformed);

        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(this::cancelBtnActionPerformed);

        javax.swing.GroupLayout bookPanelLayout = new javax.swing.GroupLayout(bookPanel);
        bookPanel.setLayout(bookPanelLayout);
        bookPanelLayout.setHorizontalGroup(
                bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(bookPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(bookPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(consultationType, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(bookPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(location, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(bookPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(personnel, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(bookPanelLayout.createSequentialGroup()
                                                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(roomType, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(cancelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 378, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(date, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(bookBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 377, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );
        bookPanelLayout.setVerticalGroup(
                bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(bookPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(bookPanelLayout.createSequentialGroup()
                                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(consultationType)
                                                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(location)
                                                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(personnel)
                                                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(roomType)
                                                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addComponent(date, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 452, Short.MAX_VALUE)
                                .addGroup(bookPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(bookBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cancelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );

        appointmentsTab.add(bookPanel, "card3");

        mainTabs.addTab("Appointments", appointmentsTab);

        jLabel12.setText("First Name : Abcdefg");

        jLabel13.setText("Last Name : Hijklmnop");

        jLabel14.setText("Sex : M");

        jLabel15.setText("Age : 31");

        jLabel16.setText("Ect : ...");

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2)
                                        .addGroup(jPanel8Layout.createSequentialGroup()
                                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel12)
                                                        .addComponent(jLabel13)
                                                        .addComponent(jLabel14)
                                                        .addComponent(jLabel15)
                                                        .addComponent(jLabel16))
                                                .addGap(0, 511, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel13)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 68, Short.MAX_VALUE)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        jLabel11.setText("Profile Image");

        jButton6.setText("Request Attestation");
        jButton6.addActionListener(this::jButton6ActionPerformed);

        jButton7.setText("Modify Billing");
        jButton7.addActionListener(this::jButton7ActionPerformed);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButton7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup()
                                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel18.setText("Billing Information");

        jTextArea2.setColumns(20);
        jTextArea2.setRows(5);
        jScrollPane3.setViewportView(jTextArea2);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel18)
                                .addContainerGap(533, Short.MAX_VALUE))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 632, Short.MAX_VALUE)
                                        .addContainerGap()))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel18)
                                .addContainerGap(405, Short.MAX_VALUE))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(38, 38, 38)
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap(276, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout profileTabLayout = new javax.swing.GroupLayout(profileTab);
        profileTab.setLayout(profileTabLayout);
        profileTabLayout.setHorizontalGroup(
                profileTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(profileTabLayout.createSequentialGroup()
                                .addGroup(profileTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(profileTabLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        profileTabLayout.setVerticalGroup(
                profileTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(profileTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(15, 15, 15))
                        .addGroup(profileTabLayout.createSequentialGroup()
                                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );

        mainTabs.addTab("Profile", profileTab);

        jLabel19.setText("Time Event System");

        adminFeaturePanel.setLayout(new java.awt.GridLayout(0, 2, 8, 4));
        adminFeatureScroll.setViewportView(adminFeaturePanel);
        javax.swing.GroupLayout adminTabLayout = new javax.swing.GroupLayout(adminTab);
        adminTab.setLayout(adminTabLayout);
        adminTabLayout.setHorizontalGroup(
                adminTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(adminTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(adminTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(adminFeatureScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
                                        .addComponent(jLabel19)
                        .addGroup(adminTabLayout.createSequentialGroup()
                            .addComponent(jCalendar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(adminTimePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        adminTabLayout.setVerticalGroup(
                adminTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(adminTabLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(adminTabLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jCalendar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(adminTimePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(adminFeatureScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );



        mainTabs.addTab("Admin", adminTab);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainTabs)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainTabs, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        mainTabs.getAccessibleContext().setAccessibleName("mainTabs");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void searchBarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchBarActionPerformed
        performSearch();
    }//GEN-LAST:event_searchBarActionPerformed
    
    /**
     * Initializes appointment data from the Model layer.
     * Replaces hardcoded sample data with data from AppointmentManager.
     */
    private void initializeAppointmentData() {
        appointmentModel = new AppointmentTableModel();
        
        // Load all appointments from manager
        for (Appointment appointment : appointmentManager.getAllAppointments()) {
            appointmentModel.addAppointment(appointment);
        }
        
        // Set the model to the table
        appointmentsTable.setModel(appointmentModel);
    }
    
    /**
     * Setup search functionality with real-time filtering.
     * Delegates filtering logic to the model layer.
     */
    private void setupSearchFunctionality() {
        // Add document listener for real-time search as user types
        searchBar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
            
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
            
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
        });
        
        // Clear placeholder text on focus
        searchBar.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if ("Search...".equals(searchBar.getText())) {
                    searchBar.setText("");
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchBar.getText().isEmpty()) {
                    searchBar.setText("Search...");
                }
            }
        });
    }
    
    /**
     * Performs search by delegating to the model.
     * UI layer only handles getting the search text.
     */
    private void performSearch() {
        if (!(featureManager.isFeatureActive("BasicSearch")) && !(featureManager.isFeatureActive("AdvancedSearch"))) {
            appointmentModel.clearFilter();
            return; 
        }

        String searchText = searchBar.getText().trim();
        
        // Don't filter on the placeholder text
        if ("Search...".equals(searchText)) {
            appointmentModel.clearFilter();
        } else {
            appointmentModel.applyFilter(searchText);
        }
    }
    
    /**
     * Populates dropdown menus with data from DataProvider.
     * Replaces hardcoded options throughout the UI.
     */
    private void populateDropdowns() {
        consultationType.setModel(new javax.swing.DefaultComboBoxModel<>(
            dataProvider.getOptionsArray(DataProvider.CATEGORY_CONSULTATION_TYPES)));
        location.setModel(new javax.swing.DefaultComboBoxModel<>(
            dataProvider.getOptionsArray(DataProvider.CATEGORY_LOCATIONS)));
        personnel.setModel(new javax.swing.DefaultComboBoxModel<>(
            dataProvider.getOptionsArray(DataProvider.CATEGORY_PERSONNEL)));
        roomType.setModel(new javax.swing.DefaultComboBoxModel<>(
            dataProvider.getOptionsArray(DataProvider.CATEGORY_ROOM_TYPES)));
    }
    
    /**
     * Initialize time picker for appointment scheduling.
     */
    private void initializeTimePicker() {
        timePicker = new TimePickerPanel();
        
        // Fix calendar display issue - ensure font is set properly
        try {
            date.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
            // Force calendar to redraw with proper font
            java.awt.Component[] components = date.getComponents();
            for (java.awt.Component comp : components) {
                if (comp instanceof javax.swing.JPanel) {
                    comp.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
                }
            }
        } catch (Exception e) {
            logger.logError(TAG, "Could not fix calendar font: " + e.getMessage());
        }
        
        // Add time picker to booking panel below the calendar
        // Use BorderLayout to position it properly
        javax.swing.JPanel rightPanel = new javax.swing.JPanel();
        rightPanel.setLayout(new java.awt.BorderLayout(5, 5));
        rightPanel.add(date, java.awt.BorderLayout.NORTH);
        
        javax.swing.JPanel timePanel = new javax.swing.JPanel();
        timePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Time"));
        timePanel.add(timePicker);
        rightPanel.add(timePanel, java.awt.BorderLayout.CENTER);
        
        // Replace calendar in bookPanel with our new panel containing both calendar and time picker
        // This requires modifying the layout, so we rebuild the right side
        bookPanel.removeAll();
        rebuildBookingPanel(rightPanel);

        // Default booking date/time to the simulated "now" from the time-event system
        setBookingPickersToSimulatedNow();
    }

    private void setBookingPickersToSimulatedNow() {
        try {
            java.util.Date now = (timeEventManager == null) ? new java.util.Date() : timeEventManager.getDate();
            if (date != null) {
                date.setDate(now);
            }
            if (timePicker != null) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(now);
                String timeStr = String.format("%02d:%02d",
                        cal.get(java.util.Calendar.HOUR_OF_DAY),
                        cal.get(java.util.Calendar.MINUTE));
                timePicker.setSelectedTime(timeStr);
            }
        } catch (Exception ignored) {
            // keep defaults
        }
    }
    
    /**
     * Rebuilds booking panel with time picker integrated.
     */
    private void rebuildBookingPanel(javax.swing.JPanel rightPanel) {
        bookPanel.setLayout(new java.awt.BorderLayout(10, 10));
        
        // Left side with form fields
        javax.swing.JPanel leftPanel = new javax.swing.JPanel();
        leftPanel.setLayout(new java.awt.GridLayout(5, 2, 5, 5));
        leftPanel.add(jLabel7);
        leftPanel.add(consultationType);
        leftPanel.add(jLabel8);
        leftPanel.add(location);
        leftPanel.add(jLabel9);
        leftPanel.add(personnel);
        leftPanel.add(jLabel10);
        leftPanel.add(roomType);
        
        // Button panel at bottom
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
        buttonPanel.setLayout(new java.awt.GridLayout(1, 2, 10, 0));
        buttonPanel.add(cancelBtn);
        buttonPanel.add(bookBtn);
        
        // Add all to bookPanel
        bookPanel.add(leftPanel, java.awt.BorderLayout.WEST);
        bookPanel.add(rightPanel, java.awt.BorderLayout.CENTER);
        bookPanel.add(buttonPanel, java.awt.BorderLayout.SOUTH);
        
        bookPanel.revalidate();
        bookPanel.repaint();
    }
    
    /**
     * Updates profile display with current patient information.
     * Gets data from PatientManager instead of hardcoded placeholders.
     */
    private void updateProfileDisplay() {
        PatientManager.Patient patient = patientManager.getCurrentPatient();
        jLabel12.setText("First Name : " + patient.getFirstName());
        jLabel13.setText("Last Name : " + patient.getLastName());
        jLabel14.setText("Sex : " + patient.getSex());
        jLabel15.setText("Age : " + patient.getAge());
        jLabel16.setText("Insurance : " + patient.getInsuranceLevel());
        jTextArea1.setText("Current Medication: " + patient.getCurrentMedication() + "\n" +
                          "Vaccines: " + patient.getVaccines());
    }
    
    /**
     * Updates home page with upcoming or past appointments dynamically.
     */
    private void updateHomePageAppointments() {
        try {
            java.util.List<Appointment> appointments = showUpcomingOnHome ?
                appointmentManager.getUpcomingAppointments() :
                appointmentManager.getPastAppointments();

            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");

            // Update header and toggle button
            jLabel6.setText(showUpcomingOnHome ? "Upcoming Appointments" : "Past Appointments");
            if (toggleAppointmentsBtn != null) {
                toggleAppointmentsBtn.setText(showUpcomingOnHome ? "Show Past Appointments" : "Show Upcoming Appointments");
            }

            jPanel2.removeAll();

            if (!homeReminderListListenerInstalled) {
                homeReminderListListenerInstalled = true;
                jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                jList1.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int idx = jList1.locationToIndex(e.getPoint());
                            if (idx >= 0 && idx < homeFeedItems.size()) {
                                HomeFeedItem item = homeFeedItems.get(idx);
                                if (item == null) {
                                    return;
                                }
                                if (item.type == HomeFeedItemType.REMINDER && item.appointment != null) {
                                    navigateToAppointment(item.appointment);
                                } else if (item.type == HomeFeedItemType.NOTIFICATION && item.notification != null) {
                                    javax.swing.JOptionPane.showMessageDialog(
                                            MainFrame.this,
                                            item.notification.getMessage(),
                                            item.notification.getTitle(),
                                            javax.swing.JOptionPane.INFORMATION_MESSAGE
                                    );
                                }
                            }
                        }
                    }
                });
            }

            if (appointments.isEmpty()) {
                String emptyMessage = showUpcomingOnHome ?
                    "No upcoming appointments scheduled" :
                    "No past appointments found";
                JLabel emptyLabel = new JLabel(emptyMessage);
                jPanel2.add(emptyLabel);
                jLabel2.setVisible(showUpcomingOnHome);
                notificationsRemindersList.setVisible(showUpcomingOnHome);
            } else {
                // Build rows: [Label][View Details]
                for (Appointment apt : appointments) {
                    try {
                        LocalDate aptDate = LocalDate.parse(apt.getDate(), formatter);
                        DayOfWeek dayOfWeek = aptDate.getDayOfWeek();
                        String dayName = dayOfWeek.toString().substring(0, 1).toUpperCase() +
                                dayOfWeek.toString().substring(1).toLowerCase();
                        String display = dayName + " " + apt.getDate() +
                                " at " + apt.getTime() + " with " + apt.getDoctor();

                        javax.swing.JLabel lbl = new javax.swing.JLabel(display);
                        javax.swing.JButton viewBtn = new javax.swing.JButton("View Details");
                        viewBtn.addActionListener(ev -> {
                            navigateToAppointment(apt);
                        });
                        jPanel2.add(lbl);
                        jPanel2.add(viewBtn);
                    } catch (Exception e) {
                        // Fallback label if date parsing fails
                        javax.swing.JLabel lbl = new javax.swing.JLabel(apt.getDate() + " at " + apt.getTime() + " with " + apt.getDoctor());
                        javax.swing.JButton viewBtn = new javax.swing.JButton("View Details");
                        viewBtn.addActionListener(ev -> {
                            navigateToAppointment(apt);
                        });
                        jPanel2.add(lbl);
                        jPanel2.add(viewBtn);
                    }
                }

                jLabel2.setVisible(showUpcomingOnHome);
                notificationsRemindersList.setVisible(showUpcomingOnHome);
            }

            rebuildHomeFeedModel();

            // Refresh the panel
            jPanel2.revalidate();
            jPanel2.repaint();
        } catch (Exception e) {
            logger.logError(TAG, "Error updating home page: " + e.getMessage());
        }
    }

    private void rebuildHomeFeedModel() {
        if (homeFeedModel == null) {
            return;
        }

        homeFeedModel.clear();
        homeFeedItems.clear();

        if (!showUpcomingOnHome) {
            return;
        }

        addHomeFeedItem(new HomeFeedItem(HomeFeedItemType.HEADER,
                "Reminders (double-click to open appointment)", null, null));

        java.util.List<Appointment> reminders = java.util.Collections.emptyList();
        try {
            if (appointmentNotificationManager != null) {
                reminders = appointmentNotificationManager.getUpcomingReminderAppointments(10);
            }
        } catch (Exception ignored) {
            reminders = java.util.Collections.emptyList();
        }

        reminders.sort(java.util.Comparator.comparing(a -> {
            java.util.Date d = a.getDateAsDate();
            return (d == null) ? new java.util.Date(Long.MAX_VALUE) : d;
        }));

        if (reminders.isEmpty()) {
            addHomeFeedItem(new HomeFeedItem(HomeFeedItemType.PLAIN, "(no scheduled reminders)", null, null));
        } else {
            for (Appointment apt : reminders) {
                String display = "REMINDER: " + apt.getDoctor() + " — " + apt.getDate() + " " + apt.getTime();
                addHomeFeedItem(new HomeFeedItem(HomeFeedItemType.REMINDER, display, apt, null));
            }
        }

        addHomeFeedItem(new HomeFeedItem(HomeFeedItemType.PLAIN, " ", null, null));

        addHomeFeedItem(new HomeFeedItem(HomeFeedItemType.HEADER,
                "Notifications (double-click to view)", null, null));

        if (homeNotifications.isEmpty()) {
            addHomeFeedItem(new HomeFeedItem(HomeFeedItemType.PLAIN, "(no notifications yet)", null, null));
        } else {
            int count = 0;
            for (Notification n : homeNotifications) {
                if (n == null) continue;
                if (count >= HOME_MAX_NOTIFICATIONS) break;
                count++;
                String ts = (timeEventManager == null) ? "" : timeEventManager.formatMillis(n.getTimestamp());
                String display = "[" + ts + "] " + n.getTitle();
                addHomeFeedItem(new HomeFeedItem(HomeFeedItemType.NOTIFICATION, display, null, n));
            }
        }
    }

    private void addHomeFeedItem(HomeFeedItem item) {
        homeFeedItems.add(item);
        homeFeedModel.addElement(item.displayText);
    }

    private void navigateToAppointment(Appointment apt) {
        if (apt == null) {
            return;
        }
        mainTabs.setSelectedIndex(1); 
        logger.log(TAG, "Navigated to Appointments tab from home page");

        try {
            // Ensure the appointment is visible (clear any search filter)
            appointmentModel.clearFilter();
            int rows = appointmentModel.getRowCount();
            for (int i = 0; i < rows; i++) {
                Appointment rowApt = appointmentModel.getAppointmentAt(i);
                if (rowApt == apt) {
                    appointmentsTable.getSelectionModel().setSelectionInterval(i, i);
                    java.awt.Rectangle rect = appointmentsTable.getCellRect(i, 0, true);
                    appointmentsTable.scrollRectToVisible(rect);
                    return;
                }
                if (rowApt != null &&
                    Objects.equals(rowApt.getDate(), apt.getDate()) &&
                    Objects.equals(rowApt.getTime(), apt.getTime()) &&
                    Objects.equals(rowApt.getDoctor(), apt.getDoctor())) {
                    appointmentsTable.getSelectionModel().setSelectionInterval(i, i);
                    java.awt.Rectangle rect = appointmentsTable.getCellRect(i, 0, true);
                    appointmentsTable.scrollRectToVisible(rect);
                    return;
                }
            }
        } catch (Exception ignored) {
            // Best-effort navigation
        }
    }
    
    /**
     * Toggles between showing upcoming and past appointments on home page.
     */
    private void toggleHomePageAppointments() {
        showUpcomingOnHome = !showUpcomingOnHome;
        updateHomePageAppointments();
        logger.log(TAG, "Toggled to show " + (showUpcomingOnHome ? "upcoming" : "past") + " appointments");
    }
    
    /**
     * Handles feature activation/deactivation when checkbox is toggled.
     * Special handling for Reminders to show/hide notification preferences.
     */
    private void handleFeatureToggle(String feature, boolean activate, javax.swing.JCheckBox checkbox) {
        try {
            if (activate) {
                featureManager.activateFeatures(feature);
                logger.log(TAG, "Feature activated: " + feature);
                
                // Special handling for Reminders
                if ("Reminders".equals(feature)) {
                    showReminderOptions();
                }
            } else {
                featureManager.deactivateFeatures(feature);
                logger.log(TAG, "Feature deactivated: " + feature);

                if ("Reminders".equals(feature)) {
                    hideReminderNotifications();
                }
            }
        } catch (IllegalStateException e) {
            logger.log(TAG, "[WARN] Cannot deactivate mandatory feature: " + feature);
            checkbox.setSelected(true); 
            javax.swing.JOptionPane.showMessageDialog(this, 
                "Cannot deactivate mandatory feature: " + feature);
        }
    }
    
    /**
     * Shows reminder options when Reminders feature is enabled.
     */
    private void showReminderOptions() {
        String[] options = {"In App", "Email", "Cancel"};
        int choice = javax.swing.JOptionPane.showOptionDialog(this,
            "How would you like to receive appointment reminders?",
            "Reminder Preferences",
            javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            // In App reminders
            featureManager.setFeatureAttribute("Reminders", "type", "InApp");
            logger.log(TAG, "Reminders set to: In App");
        } else if (choice == 1) {
            // Email reminders - ask for email
            String email = javax.swing.JOptionPane.showInputDialog(this,
                "Enter your email address for appointment reminders:",
                "Email Reminders",
                javax.swing.JOptionPane.QUESTION_MESSAGE);
            
            if (email != null && !email.trim().isEmpty()) {
                if (isValidEmail(email)) {
                    featureManager.setFeatureAttribute("Reminders", "type", "Email");
                    featureManager.setFeatureAttribute("Reminders", "email", email);
                    logger.log(TAG, "Reminders set to: Email (" + email + ")");
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this,
                        "Invalid email format. Please enter a valid email.",
                        "Invalid Email",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    
    /**
     * Hides reminder notifications when Reminders feature is disabled.
     */
    private void hideReminderNotifications() {
        // Hide reminders from home page notifications list
        updateHomePageAppointments();
    }
    
    /**
     * Simple email validation.
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void consultationTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consultationTypeActionPerformed
        String selected = (String) consultationType.getSelectedItem();
        logger.log(TAG, "Consultation type selected: " + selected);
    }//GEN-LAST:event_consultationTypeActionPerformed


    private void locationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationActionPerformed
        String selected = (String) location.getSelectedItem();
        logger.log(TAG, "Location selected: " + selected);
    }//GEN-LAST:event_locationActionPerformed

    private void personnelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_personnelActionPerformed
        String selected = (String) personnel.getSelectedItem();
        logger.log(TAG, "Personnel selected: " + selected);
    }//GEN-LAST:event_personnelActionPerformed

    private void roomTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_roomTypeActionPerformed
        String selected = (String) roomType.getSelectedItem();
        logger.log(TAG, "Room type selected: " + selected);
    }//GEN-LAST:event_roomTypeActionPerformed

    private void bookBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bookBtnActionPerformed
        try {
            String consultType = (String) consultationType.getSelectedItem();
            String loc = (String) location.getSelectedItem();
            String doctor = (String) personnel.getSelectedItem();
            String roomTypeSelected = (String) roomType.getSelectedItem();
            
            // Get date from calendar in dd-MM-yyyy format
            Date selectedDate = date.getDate();
            if (selectedDate == null) {
                JOptionPane.showMessageDialog(this, "Please select a date.");
                return;
            }

            String dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy").format(selectedDate);
            
            // Get time from time picker if available, otherwise default to 09:00
            String timeStr = "09:00";
            if (timePicker != null) {
                timeStr = timePicker.getSelectedTime();
            }

            // Only allow booking/rescheduling to future date-times (relative to simulated time-event "now")
                if (timeEventManager != null && !timeEventManager.isFutureAppointment(selectedDate, timeStr)) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Please choose a future appointment date/time.\nCurrent simulated time: " + timeEventManager.nowString(),
                    "Invalid appointment time",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
                return;
                }
            
            if (appointmentBeingModified != null) {
                // UPDATE MODE: Modify existing appointment
                appointmentBeingModified.setDate(dateStr);
                appointmentBeingModified.setTime(timeStr);
                appointmentBeingModified.setLocation(loc);
                appointmentBeingModified.setDoctor(doctor);
                appointmentBeingModified.setReason(consultType);
                appointmentBeingModified.setAttribute("roomType", roomTypeSelected);
                
                appointmentManager.updateAppointment(appointmentBeingModified);
                
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Appointment updated successfully!\n" + dateStr + " at " + timeStr + " with " + doctor);
                
                logger.log(TAG, "Appointment updated: " + dateStr + " at " + timeStr + " with " + doctor);
                
                // Reset the modify flag
                appointmentBeingModified = null;
                bookBtn.setText("Book");
            } else {
                Appointment newAppointment = new Appointment(
                    dateStr, timeStr, doctor, loc, consultType, "Scheduled",
                    java.util.Map.of(
                        "consultationType", consultType,
                        "roomType", roomTypeSelected
                    )
                );

                // Schedule reminder for new appointment (24 hours before) BEFORE notifying observers
                // so the Home reminders list can reflect it immediately.
                appointmentNotificationManager.scheduleAppointmentReminder(newAppointment, 24);

                appointmentManager.addAppointment(newAppointment);
                
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Appointment booked successfully!\n" + dateStr + " at " + timeStr + " with " + doctor);
                
                logger.log(TAG, "Appointment booked: " + dateStr + " at " + timeStr + " with " + doctor);
            }
            
            // Auto-return to appointments view after successful booking/update
            // Use CardLayout to switch back to appointmentsView
            java.awt.CardLayout cl = (java.awt.CardLayout) appointmentsTab.getLayout();
            cl.show(appointmentsTab, "card6");  // card6 is the appointmentsView card
            
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error booking appointment: " + e.getMessage());
            logger.logError(TAG, "Error booking appointment: " + e.getMessage());
        }
    }//GEN-LAST:event_bookBtnActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        javax.swing.JOptionPane.showMessageDialog(this, "Attestation request submitted.");
        logger.log(TAG, "Attestation request submitted");
    }//GEN-LAST:event_jButton6ActionPerformed

    private void newBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newBtnActionPerformed
        // Reset the form for a new appointment
        appointmentBeingModified = null;
        bookBtn.setText("Book");
        
        // Clear form fields
        consultationType.setSelectedIndex(0);
        location.setSelectedIndex(0);
        personnel.setSelectedIndex(0);
        roomType.setSelectedIndex(0);
        setBookingPickersToSimulatedNow();
        
        // Show the book panel using CardLayout
        java.awt.CardLayout cl = (java.awt.CardLayout) appointmentsTab.getLayout();
        cl.show(appointmentsTab, "card3");  // card3 is the bookPanel card
        logger.log(TAG, "New appointment form opened");
    }//GEN-LAST:event_newBtnActionPerformed

    private void modifyBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modifyBtnActionPerformed
        int selectedRow = appointmentsTable.getSelectedRow();
        if (selectedRow >= 0) {
            Appointment selected = appointmentModel.getAppointmentAt(selectedRow);

            boolean reschedulingCancelled = false;

            // If appointment is cancelled, confirm rescheduling
            if ("Cancelled".equalsIgnoreCase(selected.getStatus())) {
                int choice = javax.swing.JOptionPane.showConfirmDialog(this,
                    "This appointment is cancelled. Do you want to reschedule it?",
                    "Reschedule cancelled appointment",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE);
                if (choice != javax.swing.JOptionPane.YES_OPTION) {
                    return;
                }
                selected.setStatus("Scheduled");
                appointmentManager.updateAppointment(selected);
                reschedulingCancelled = true;
            }

            appointmentBeingModified = selected; // Track which appointment is being modified
            
            // Pre-fill the booking form with current appointment details
            try {
                // Set all form fields to the appointment's current values
                consultationType.setSelectedItem(selected.getAttribute("consultationType") != null ? 
                    selected.getAttribute("consultationType") : "General Consultation");
                
                location.setSelectedItem(selected.getLocation());
                personnel.setSelectedItem(selected.getDoctor());
                
                // Scroll to date in calendar
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
                java.time.LocalDate aptDate = java.time.LocalDate.parse(selected.getDate(), formatter);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(aptDate.getYear(), aptDate.getMonthValue() - 1, aptDate.getDayOfMonth());
                date.setDate(cal.getTime());
                
                // Set time in time picker
                if (timePicker != null) {
                    timePicker.setSelectedTime(selected.getTime());
                }

                // For rescheduling flow, start from the simulated current date/time
                if (reschedulingCancelled) {
                    setBookingPickersToSimulatedNow();
                }
                
                // Change button text and navigate to booking form
                bookBtn.setText("Update Appointment");
                mainTabs.setSelectedIndex(1); // Navigate to Appointments tab
                java.awt.CardLayout cl = (java.awt.CardLayout) appointmentsTab.getLayout();
                cl.show(appointmentsTab, "card3");  // Show booking panel
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Modify the appointment details and click 'Update Appointment' to save changes.",
                    "Modify Appointment",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                logger.log(TAG, "Loaded appointment for modification: " + selected.getDate());
            } catch (Exception e) {
                logger.logError(TAG, "Error loading appointment for modification: " + e.getMessage());
                javax.swing.JOptionPane.showMessageDialog(this, 
                    "Error loading appointment: " + e.getMessage());
            }
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Please select an appointment to modify.");
        }
    }//GEN-LAST:event_modifyBtnActionPerformed

    private void cancelAppBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelAppBtnActionPerformed
        int selectedRow = appointmentsTable.getSelectedRow();
        if (selectedRow >= 0) {
            Appointment selected = appointmentModel.getAppointmentAt(selectedRow);
            
            // Ask for cancellation reason
            String[] options = {"Patient Request", "Doctor Unavailable", "Other"};
            int choice = javax.swing.JOptionPane.showOptionDialog(this,
                "Reason for cancellation:",
                "Cancel Appointment",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            
            if (choice >= 0) {
                String cancelledBy = choice == 1 ? "Doctor" : "Patient";
                String reason = options[choice];
                
                appointmentManager.cancelAppointment(selected);
                
                // Send cancellation notification
                appointmentNotificationManager.sendCancellationNotice(selected, cancelledBy, reason);
                
                javax.swing.JOptionPane.showMessageDialog(this, "Appointment cancelled successfully.");
                logger.log(TAG, "Appointment cancelled by " + cancelledBy);
            }
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Please select an appointment to cancel.");
        }
    }//GEN-LAST:event_cancelAppBtnActionPerformed

    private void applyFilterBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyFilterBtnActionPerformed
        int selectedIndex = timePeriodList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String selected = timePeriodList.getModel().getElementAt(selectedIndex);
            applyDateFilter(selected);
            logger.log(TAG, "Filter applied: " + selected);
        }
    }//GEN-LAST:event_applyFilterBtnActionPerformed
    
    /**
     * Apply date-based filtering to appointments table.
     */
    private void applyDateFilter(String filterType) {
        try {
            java.util.List<Appointment> allAppointments = appointmentManager.getAllAppointments();
            Date now = timeEventManager.getDate();
            
            // Rebuild model with all appointments first
            appointmentModel = new AppointmentTableModel();
            
            for (Appointment apt : allAppointments) {
                try {
                    Date aptDate = apt.getDateAsDate();
                    
                    boolean include = false;
                    
                    switch (filterType) {
                        case "Today":
                            include = aptDate.equals(now);
                            break;
                        case "This Week":
                            include = !aptDate.before(now) && !aptDate.after(new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000));
                            break;
                        case "All Upcoming":
                            include = !aptDate.before(now);
                            break;
                        case "Past Appointments":
                            include = aptDate.before(now);
                            break;
                    }
                    
                    if (include) {
                        appointmentModel.addAppointment(apt);
                    }
                } catch (Exception e) {
                    // Skip invalid dates
                }
            }
            
            appointmentsTable.setModel(appointmentModel);
        } catch (Exception e) {
            logger.logError(TAG, "Error applying filter: " + e.getMessage());
        }
    }

    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelBtnActionPerformed
        // Show the appointments view panel using CardLayout
        java.awt.CardLayout cl = (java.awt.CardLayout) appointmentsTab.getLayout();
        cl.show(appointmentsTab, "card6");  // card6 is the appointmentsView card
        logger.log(TAG, "New appointment form cancelled");
    }//GEN-LAST:event_cancelBtnActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        javax.swing.JOptionPane.showMessageDialog(this, "Opening billing modification panel...");
        logger.log(TAG, "Billing modification opened");
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // Navigate to Appointments tab
        mainTabs.setSelectedIndex(1); // 1 = Appointments tab
        logger.log(TAG, "Navigated to Appointments tab from home page");
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // Navigate to Appointments tab
        mainTabs.setSelectedIndex(1); // 1 = Appointments tab
        logger.log(TAG, "Navigated to Appointments tab from home page");
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // Navigate to Appointments tab
        mainTabs.setSelectedIndex(1); // 1 = Appointments tab
        logger.log(TAG, "Navigated to Appointments tab from home page");
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        String selected = (String) jComboBox1.getSelectedItem();
        logger.log(TAG, "Theme selected: " + selected);
    }//GEN-LAST:event_jComboBox1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        FlatLightLaf.setup();
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }
    
    // ============ OBSERVER PATTERN IMPLEMENTATIONS ============
    
    @Override
    public void onFeaturesActivated(List<String> features) {
        refreshFeatureUI();
    }
    
    @Override
    public void onFeaturesDeactivated(List<String> features) {
        refreshFeatureUI();
    }
    
    @Override
    public void onInsuranceLevelChanged(String level) {
        logger.log(TAG, "Insurance level changed to: " + level);
        if (level != null && !level.trim().isEmpty()) {
            patientManager.setInsuranceLevel(level.trim());
        }
        updateProfileDisplay();
        // TODO: Update available appointment options based on insurance
    }
    
    @Override
    public void onPatientChanged(PatientManager.Patient patient) {
        logger.log(TAG, "Patient changed");
        updateProfileDisplay();
    }
    
    @Override
    public void onPatientUpdated(PatientManager.Patient patient) {
        logger.log(TAG, "Patient information updated");
        updateProfileDisplay();
    }
    
    @Override
    public void onAppointmentAdded(Appointment appointment) {
        logger.log(TAG, "Appointment added: " + appointment.getDate());
        appointmentModel.addAppointment(appointment);
        updateHomePageAppointments();
    }
    
    @Override
    public void onAppointmentRemoved(Appointment appointment) {
        logger.log(TAG, "Appointment removed");
        appointmentModel.removeAppointment(appointmentModel.getFilteredAppointmentCount() - 1);
        updateHomePageAppointments();
    }
    
    @Override
    public void onAppointmentUpdated(Appointment appointment) {
        logger.log(TAG, "Appointment updated");
        appointmentModel.applyFilter(appointmentModel.getFilteredAppointmentCount() > 0 ? "" : "");
        updateHomePageAppointments();
    }

    /**
     * Implement NotificationListener to receive notifications in UI.
     */
    @Override
    public void onNotification(Notification notification) {
        if (notification == null) {
            return;
        }

        logger.log(TAG, "Notification received in UI: " + notification.getTitle());

        SwingUtilities.invokeLater(() -> {
            homeNotifications.add(0, notification);
            if (homeNotifications.size() > HOME_MAX_NOTIFICATIONS) {
                homeNotifications.subList(HOME_MAX_NOTIFICATIONS, homeNotifications.size()).clear();
            }

            rebuildHomeFeedModel();

            if (notification.getTitle().contains("Cancelled") || notification.getTitle().contains("Unavailable")) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        notification.getMessage(),
                        notification.getTitle(),
                        javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private enum HomeFeedItemType {
        HEADER,
        PLAIN,
        REMINDER,
        NOTIFICATION
    }

    private static final class HomeFeedItem {
        private final HomeFeedItemType type;
        private final String displayText;
        private final Appointment appointment;
        private final Notification notification;

        private HomeFeedItem(HomeFeedItemType type, String displayText, Appointment appointment, Notification notification) {
            this.type = type;
            this.displayText = displayText;
            this.appointment = appointment;
            this.notification = notification;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel adminTab;
    private javax.swing.JLabel appointmentOpsLabel;
    private javax.swing.JButton applyFilterBtn;
    private javax.swing.JPanel appointmentsTab;
    private javax.swing.JTable appointmentsTable;
    private javax.swing.JScrollPane appointmentsTableScroll;
    private javax.swing.JPanel appointmentsView;
    private javax.swing.JButton bookBtn;
    private javax.swing.JPanel bookPanel;
    private javax.swing.JButton cancelAppBtn;
    private javax.swing.JButton cancelBtn;
    private javax.swing.JComboBox<String> consultationType;
    private com.toedter.calendar.JCalendar date;
    private javax.swing.JLabel date1;
    private javax.swing.JLabel date2;
    private javax.swing.JLabel date3;
    private javax.swing.JPanel featurePanel;
    private javax.swing.JLabel filterLabel;
    private javax.swing.JSeparator filterSeparator;
    private javax.swing.JPanel homeTab;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private com.toedter.calendar.JCalendar jCalendar1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBox3;
    private javax.swing.JComboBox<String> jComboBox4;
    private javax.swing.JComboBox<String> jComboBox5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JList<String> jList1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JComboBox<String> location;
    private javax.swing.JTabbedPane mainTabs;
    private javax.swing.JButton modifyBtn;
    private javax.swing.JButton newBtn;
    private javax.swing.JScrollPane notificationsRemindersList;
    private javax.swing.JComboBox<String> personnel;
    private javax.swing.JPanel profileTab;
    private javax.swing.JComboBox<String> roomType;
    private javax.swing.JTextField searchBar;
    private javax.swing.JPanel settingsTab;
    private javax.swing.JList<String> timePeriodList;
    private javax.swing.JScrollPane upcomingAppointments;
    private javax.swing.JButton toggleAppointmentsBtn;
    // End of variables declaration//GEN-END:variables
}
