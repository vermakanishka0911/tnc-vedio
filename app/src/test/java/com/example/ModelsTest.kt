package com.example

import com.example.data.model.Course
import com.example.data.model.Session
import com.example.data.model.StudyLeaderboardRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {

    @Test
    fun testSessionNumericSerialSorting() {
        val s1 = Session(rowId = "1", title = "Lecture 1", serialNo = "8")
        val s2 = Session(rowId = "2", title = "Lecture 2", serialNo = "9")
        val s3 = Session(rowId = "3", title = "Lecture 3", serialNo = "10")
        val s4 = Session(rowId = "4", title = "Lecture 4", serialNo = "29")

        val list = listOf(s3, s1, s4, s2)
        val sorted = list.sortedBy { it.serialNumberInt }

        assertEquals("8", sorted[0].serialNo)
        assertEquals("9", sorted[1].serialNo)
        assertEquals("10", sorted[2].serialNo)
        assertEquals("29", sorted[3].serialNo)
    }

    @Test
    fun testStudyLeaderboardRowTimeFormat() {
        val row1 = StudyLeaderboardRow(firstName = "Nurse Aarti", seconds = 7200, sessions = 4)
        assertEquals("2h 0m", row1.formattedTime)
        assertEquals("Nurse Aarti", row1.displayName)

        val row2 = StudyLeaderboardRow(visitorName = "Learner Vikas", seconds = 1800, sessions = 1)
        assertEquals("30m", row2.formattedTime)
        assertEquals("Learner Vikas", row2.displayName)
    }

    @Test
    fun testCourseModel() {
        val course = Course(rowId = "c1", name = "NORCET 2026 Foundation")
        assertEquals("c1", course.rowId)
        assertEquals("NORCET 2026 Foundation", course.name)
    }

    @Test
    fun testWeeklyResetCalculation() {
        val oneWeekMillis = 7L * 24 * 60 * 60 * 1000L
        val weekStart = 1000000L
        val nowWithinWeek = weekStart + (3L * 24 * 60 * 60 * 1000L)
        val diff = maxOf(0L, (weekStart + oneWeekMillis) - nowWithinWeek)
        val remainingDays = diff / (24 * 60 * 60 * 1000L)

        assertEquals(4L, remainingDays)
        assertTrue(diff > 0L)
    }
}
