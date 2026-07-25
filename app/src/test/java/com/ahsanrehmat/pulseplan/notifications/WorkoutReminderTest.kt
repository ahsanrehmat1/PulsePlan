package com.ahsanrehmat.pulseplan.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkoutReminderTest {
    private val zone = ZoneId.of("Asia/Karachi")

    @Test
    fun `future reminder today uses the remaining time`() {
        val now = ZonedDateTime.of(2026, 7, 25, 6, 30, 0, 0, zone)

        val delay = WorkoutReminder.delayUntilNextReminder(
            now = now,
            hour = 7,
            minute = 15,
        )

        assertEquals(Duration.ofMinutes(45), delay)
    }

    @Test
    fun `past reminder rolls forward to tomorrow`() {
        val now = ZonedDateTime.of(2026, 7, 25, 8, 10, 0, 0, zone)

        val delay = WorkoutReminder.delayUntilNextReminder(
            now = now,
            hour = 7,
            minute = 0,
        )

        assertEquals(Duration.ofHours(22).plusMinutes(50), delay)
    }

    @Test
    fun `matching reminder time rolls forward to tomorrow`() {
        val now = ZonedDateTime.of(2026, 7, 25, 7, 0, 0, 0, zone)

        val delay = WorkoutReminder.delayUntilNextReminder(
            now = now,
            hour = 7,
            minute = 0,
        )

        assertEquals(Duration.ofDays(1), delay)
    }

    @Test
    fun `invalid reminder time is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkoutReminder.delayUntilNextReminder(
                now = ZonedDateTime.of(2026, 7, 25, 7, 0, 0, 0, zone),
                hour = 24,
                minute = 0,
            )
        }
    }
}
