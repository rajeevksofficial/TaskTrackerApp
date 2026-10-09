package com.tasktracker.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskItemTest {

    @Test
    fun testTaskCompletionState() {
        val pendingTask = TaskItem(
            title = "Test Pending",
            status = TaskItem.STATUS_TODO
        )
        assertFalse(pendingTask.isCompleted)

        val inProgressTask = TaskItem(
            title = "Test In Progress",
            status = TaskItem.STATUS_IN_PROGRESS
        )
        assertFalse(inProgressTask.isCompleted)

        val completedTask = TaskItem(
            title = "Test Completed",
            status = TaskItem.STATUS_COMPLETED
        )
        assertTrue(completedTask.isCompleted)
    }

    @Test
    fun testTaskOverdueCalculation() {
        val pastTime = System.currentTimeMillis() - 100000L
        val futureTime = System.currentTimeMillis() + 100000L

        val overdueTask = TaskItem(
            title = "Overdue Task",
            dueDate = pastTime,
            status = TaskItem.STATUS_TODO
        )
        assertTrue(overdueTask.isOverdue)

        val futureTask = TaskItem(
            title = "Future Task",
            dueDate = futureTime,
            status = TaskItem.STATUS_TODO
        )
        assertFalse(futureTask.isOverdue)

        // Completed tasks should never be marked overdue
        val completedPastTask = TaskItem(
            title = "Completed Past Task",
            dueDate = pastTime,
            status = TaskItem.STATUS_COMPLETED
        )
        assertFalse(completedPastTask.isOverdue)
    }

    @Test
    fun testTaskStats() {
        val stats = TaskStats(
            total = 10,
            todo = 4,
            inProgress = 2,
            completed = 4,
            overdue = 1,
            completionPercentage = 40
        )
        assertEquals(10, stats.total)
        assertEquals(40, stats.completionPercentage)
        assertEquals(4, stats.completed)
    }
}
