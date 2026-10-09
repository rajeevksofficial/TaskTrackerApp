package com.tasktracker.app

data class TaskItem(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val status: String = STATUS_TODO,
    val dueDate: Long = System.currentTimeMillis() + 86400000L, // default 24h later
    val priority: String = PRIORITY_MEDIUM,
    val category: String = CATEGORY_WORK,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_TODO = "TODO"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"

        const val PRIORITY_HIGH = "HIGH"
        const val PRIORITY_MEDIUM = "MEDIUM"
        const val PRIORITY_LOW = "LOW"

        const val CATEGORY_WORK = "Work"
        const val CATEGORY_PERSONAL = "Personal"
        const val CATEGORY_STUDY = "Study"
        const val CATEGORY_HEALTH = "Health"
        const val CATEGORY_FINANCE = "Finance"
        const val CATEGORY_SHOPPING = "Shopping"
    }

    val isCompleted: Boolean
        get() = status == STATUS_COMPLETED

    val isOverdue: Boolean
        get() = !isCompleted && dueDate < System.currentTimeMillis()
}

data class TaskStats(
    val total: Int,
    val todo: Int,
    val inProgress: Int,
    val completed: Int,
    val overdue: Int,
    val completionPercentage: Int
)
