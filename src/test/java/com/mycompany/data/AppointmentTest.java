package com.mycompany.data;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentTest {

    @Test
    void getDateAsDate_parsesDateAndTime() {
        Appointment a = new Appointment("11-01-2026", "09:15", "Dr", "Loc", "Reason", "Scheduled");
        Date d = a.getDateAsDate();
        assertNotNull(d);

        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        assertEquals("11-01-2026 09:15", fmt.format(d));
    }

    @Test
    void getDateAsDate_parsesDateWhenTimeMissing() {
        Appointment a = new Appointment("11-01-2026", "", "Dr", "Loc", "Reason", "Scheduled");
        Date d = a.getDateAsDate();
        assertNotNull(d);

        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy");
        assertEquals("11-01-2026", fmt.format(d));
    }

    @Test
    void setDateFromDate_setsBothDateAndTime() {
        Appointment a = new Appointment("01-01-2026", "09:00", "Dr", "Loc", "Reason", "Scheduled");
        Date input = new Date(0L); // 01-01-1970 00:00 UTC-ish; formatting depends on local TZ but date+time should be non-null.

        a.setDateFromDate(input);
        assertNotNull(a.getDate());
        assertNotNull(a.getTime());
        assertFalse(a.getDate().isBlank());
        assertFalse(a.getTime().isBlank());
    }
}
