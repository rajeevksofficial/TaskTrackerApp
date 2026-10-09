package com.tasktracker.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TaskDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "task_master.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_TASKS = "tasks"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_DESC = "description"
        private const val COL_STATUS = "status"
        private const val COL_DUE_DATE = "due_date"
        private const val COL_PRIORITY = "priority"
        private const val COL_CATEGORY = "category"
        private const val COL_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createQuery = """
            CREATE TABLE $TABLE_TASKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_DESC TEXT,
                $COL_STATUS TEXT NOT NULL,
                $COL_DUE_DATE INTEGER NOT NULL,
                $COL_PRIORITY TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createQuery)

        // Seed initial sample tasks
        seedSampleTasks(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }

    private fun seedSampleTasks(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        val oneHour = 3600000L
        val oneDay = 86400000L

        val samples = listOf(
            TaskItem(
                title = "Design High-Fidelity UI Mockups",
                description = "Build clean interactive components for upcoming product release",
                status = TaskItem.STATUS_IN_PROGRESS,
                dueDate = now + oneDay * 2,
                priority = TaskItem.PRIORITY_HIGH,
                category = TaskItem.CATEGORY_WORK,
                createdAt = now - oneDay
            ),
            TaskItem(
                title = "Quarterly Budget Planning & Review",
                description = "Analyze monthly expenses and finalize Q4 budget allocation",
                status = TaskItem.STATUS_TODO,
                dueDate = now + oneHour * 5, // Due today
                priority = TaskItem.PRIORITY_HIGH,
                category = TaskItem.CATEGORY_FINANCE,
                createdAt = now - oneDay * 2
            ),
            TaskItem(
                title = "Complete Chapter 4 Machine Learning",
                description = "Study neural network backpropagation and practice PyTorch tensor operations",
                status = TaskItem.STATUS_IN_PROGRESS,
                dueDate = now + oneDay * 3,
                priority = TaskItem.PRIORITY_MEDIUM,
                category = TaskItem.CATEGORY_STUDY,
                createdAt = now - oneDay
            ),
            TaskItem(
                title = "Weekly Grocery Shopping",
                description = "Buy fresh fruits, vegetables, almond milk, and pantry essentials",
                status = TaskItem.STATUS_COMPLETED,
                dueDate = now - oneDay, // yesterday
                priority = TaskItem.PRIORITY_LOW,
                category = TaskItem.CATEGORY_SHOPPING,
                createdAt = now - oneDay * 3
            ),
            TaskItem(
                title = "Gym Workout - Cardio & Core",
                description = "30 mins HIIT running + 20 mins core stability training",
                status = TaskItem.STATUS_TODO,
                dueDate = now + oneHour * 3, // Today evening
                priority = TaskItem.PRIORITY_MEDIUM,
                category = TaskItem.CATEGORY_HEALTH,
                createdAt = now
            ),
            TaskItem(
                title = "Renew Vehicle Insurance Policy",
                description = "Compare insurance quotes and renew policy before month ends",
                status = TaskItem.STATUS_TODO,
                dueDate = now - oneHour * 10, // Overdue
                priority = TaskItem.PRIORITY_HIGH,
                category = TaskItem.CATEGORY_PERSONAL,
                createdAt = now - oneDay * 5
            )
        )

        for (task in samples) {
            val values = ContentValues().apply {
                put(COL_TITLE, task.title)
                put(COL_DESC, task.description)
                put(COL_STATUS, task.status)
                put(COL_DUE_DATE, task.dueDate)
                put(COL_PRIORITY, task.priority)
                put(COL_CATEGORY, task.category)
                put(COL_CREATED_AT, task.createdAt)
            }
            db.insert(TABLE_TASKS, null, values)
        }
    }

    fun insertTask(task: TaskItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, task.title)
            put(COL_DESC, task.description)
            put(COL_STATUS, task.status)
            put(COL_DUE_DATE, task.dueDate)
            put(COL_PRIORITY, task.priority)
            put(COL_CATEGORY, task.category)
            put(COL_CREATED_AT, task.createdAt)
        }
        return db.insert(TABLE_TASKS, null, values)
    }

    fun updateTask(task: TaskItem): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, task.title)
            put(COL_DESC, task.description)
            put(COL_STATUS, task.status)
            put(COL_DUE_DATE, task.dueDate)
            put(COL_PRIORITY, task.priority)
            put(COL_CATEGORY, task.category)
        }
        return db.update(TABLE_TASKS, values, "$COL_ID = ?", arrayOf(task.id.toString()))
    }

    fun updateTaskStatus(id: Long, newStatus: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, newStatus)
        }
        return db.update(TABLE_TASKS, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteTask(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_TASKS, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun getFilteredTasks(
        query: String? = null,
        statusFilter: String? = null,
        categoryFilter: String? = null,
        sortOrder: String? = null
    ): List<TaskItem> {
        val db = readableDatabase
        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (!query.isNullOrBlank()) {
            selectionClauses.add("($COL_TITLE LIKE ? OR $COL_DESC LIKE ?)")
            val arg = "%${query.trim()}%"
            selectionArgs.add(arg)
            selectionArgs.add(arg)
        }

        if (!statusFilter.isNullOrBlank() && statusFilter != "ALL") {
            if (statusFilter == "OVERDUE") {
                val now = System.currentTimeMillis()
                selectionClauses.add("$COL_STATUS != ? AND $COL_DUE_DATE < ?")
                selectionArgs.add(TaskItem.STATUS_COMPLETED)
                selectionArgs.add(now.toString())
            } else {
                selectionClauses.add("$COL_STATUS = ?")
                selectionArgs.add(statusFilter)
            }
        }

        if (!categoryFilter.isNullOrBlank() && categoryFilter != "ALL") {
            selectionClauses.add("$COL_CATEGORY = ?")
            selectionArgs.add(categoryFilter)
        }

        val selection = if (selectionClauses.isNotEmpty()) {
            selectionClauses.joinToString(" AND ")
        } else null

        val orderBy = when (sortOrder) {
            "DUE_DATE_DESC" -> "$COL_DUE_DATE DESC"
            "PRIORITY" -> "CASE $COL_PRIORITY WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END ASC, $COL_DUE_DATE ASC"
            "TITLE" -> "$COL_TITLE ASC"
            else -> "$COL_DUE_DATE ASC" // Default DUE_DATE_ASC
        }

        val tasks = mutableListOf<TaskItem>()
        val cursor = db.query(
            TABLE_TASKS,
            null,
            selection,
            selectionArgs.toTypedArray(),
            null,
            null,
            orderBy
        )

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(COL_ID)
            val titleCol = it.getColumnIndexOrThrow(COL_TITLE)
            val descCol = it.getColumnIndexOrThrow(COL_DESC)
            val statusCol = it.getColumnIndexOrThrow(COL_STATUS)
            val dueCol = it.getColumnIndexOrThrow(COL_DUE_DATE)
            val priorityCol = it.getColumnIndexOrThrow(COL_PRIORITY)
            val catCol = it.getColumnIndexOrThrow(COL_CATEGORY)
            val createdCol = it.getColumnIndexOrThrow(COL_CREATED_AT)

            while (it.moveToNext()) {
                tasks.add(
                    TaskItem(
                        id = it.getLong(idCol),
                        title = it.getString(titleCol),
                        description = it.getString(descCol) ?: "",
                        status = it.getString(statusCol),
                        dueDate = it.getLong(dueCol),
                        priority = it.getString(priorityCol),
                        category = it.getString(catCol),
                        createdAt = it.getLong(createdCol)
                    )
                )
            }
        }

        return tasks
    }

    fun getTaskStats(): TaskStats {
        val allTasks = getFilteredTasks()
        val total = allTasks.size
        var todo = 0
        var inProgress = 0
        var completed = 0
        var overdue = 0

        for (t in allTasks) {
            when (t.status) {
                TaskItem.STATUS_TODO -> todo++
                TaskItem.STATUS_IN_PROGRESS -> inProgress++
                TaskItem.STATUS_COMPLETED -> completed++
            }
            if (t.isOverdue) {
                overdue++
            }
        }

        val percentage = if (total > 0) ((completed.toDouble() / total.toDouble()) * 100).toInt() else 0
        return TaskStats(total, todo, inProgress, completed, overdue, percentage)
    }
}
