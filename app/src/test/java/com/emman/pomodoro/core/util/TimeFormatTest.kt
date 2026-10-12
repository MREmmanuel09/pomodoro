package com.emman.pomodoro.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun mmss_zero() {
        assertEquals("00:00", TimeFormat.mmss(0))
    }

    @Test
    fun mmss_underOneMinute() {
        assertEquals("00:59", TimeFormat.mmss(59))
    }

    @Test
    fun mmss_oneMinute() {
        assertEquals("01:00", TimeFormat.mmss(60))
    }

    @Test
    fun mmss_typicalPomodoro() {
        assertEquals("25:00", TimeFormat.mmss(1500))
    }

    @Test
    fun mmss_overOneHour() {
        assertEquals("61:01", TimeFormat.mmss(3661))
    }

    @Test
    fun mmss_negativeClampsToZero() {
        assertEquals("00:00", TimeFormat.mmss(-5))
    }

    @Test
    fun hms_underOneHour() {
        assertEquals("59:59", TimeFormat.hms(3599))
    }

    @Test
    fun hms_exactlyOneHour() {
        assertEquals("1:00:00", TimeFormat.hms(3600))
    }

    @Test
    fun hms_overOneHour() {
        assertEquals("2:03:04", TimeFormat.hms(7384))
    }

    @Test
    fun hms_negativeClampsToZero() {
        assertEquals("00:00", TimeFormat.hms(-1))
    }

    @Test
    fun minutesLabel_roundsToNearestMinute() {
        assertEquals("25 min", TimeFormat.minutesLabel(1500))
        assertEquals("1 min", TimeFormat.minutesLabel(30))
        assertEquals("0 min", TimeFormat.minutesLabel(29))
    }
}
