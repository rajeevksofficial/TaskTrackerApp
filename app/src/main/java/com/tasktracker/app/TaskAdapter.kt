package com.tasktracker.app

import android.graphics.Paint
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private var tasks: List<TaskItem> = emptyList(),
    private val onToggleCompleted: (TaskItem) -> Unit,
    private val onEdit: (TaskItem) -> Unit,
    private val onDelete: (TaskItem) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvTaskCategory)
        val tvPriority: TextView = view.findViewById(R.id.tvTaskPriority)
        val tvStatus: TextView = view.findViewById(R.id.tvTaskStatus)
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvTaskDesc)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDateBadge)
        val tvCreated: TextView = view.findViewById(R.id.tvCreatedDate)
        val ivCheck: ImageView = view.findViewById(R.id.ivCheckToggle)
        val ivCalendar: ImageView = view.findViewById(R.id.ivCalendarIcon)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditTask)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val context = holder.itemView.context

        // Title & Description
        holder.tvTitle.text = task.title
        if (task.description.isNotBlank()) {
            holder.tvDesc.text = task.description
            holder.tvDesc.visibility = View.VISIBLE
        } else {
            holder.tvDesc.visibility = View.GONE
        }

        // Strikethrough & Completed appearance
        if (task.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            holder.ivCheck.setImageResource(R.drawable.ic_check_circle)
            holder.itemView.alpha = 0.75f
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.ivCheck.setImageResource(R.drawable.ic_check_circle_outline)
            holder.itemView.alpha = 1.0f
        }

        // Category Badge
        val (catName, catColor) = when (task.category) {
            TaskItem.CATEGORY_WORK -> "Work 💼" to R.color.cat_work
            TaskItem.CATEGORY_PERSONAL -> "Personal 🏠" to R.color.cat_personal
            TaskItem.CATEGORY_STUDY -> "Study 📚" to R.color.cat_study
            TaskItem.CATEGORY_HEALTH -> "Health 🏃" to R.color.cat_health
            TaskItem.CATEGORY_FINANCE -> "Finance 💰" to R.color.cat_finance
            TaskItem.CATEGORY_SHOPPING -> "Shopping 🛒" to R.color.cat_shopping
            else -> task.category to R.color.cat_work
        }
        holder.tvCategory.text = catName
        holder.tvCategory.setTextColor(ContextCompat.getColor(context, catColor))

        // Priority Badge
        when (task.priority) {
            TaskItem.PRIORITY_HIGH -> {
                holder.tvPriority.text = "🔴 High"
                holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_high)
                holder.tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_high))
            }
            TaskItem.PRIORITY_MEDIUM -> {
                holder.tvPriority.text = "🟡 Med"
                holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_med)
                holder.tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_medium))
            }
            else -> {
                holder.tvPriority.text = "🟢 Low"
                holder.tvPriority.setBackgroundResource(R.drawable.bg_priority_low)
                holder.tvPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_low))
            }
        }

        // Status Badge
        when (task.status) {
            TaskItem.STATUS_COMPLETED -> {
                holder.tvStatus.text = "Done"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_completed))
            }
            TaskItem.STATUS_IN_PROGRESS -> {
                holder.tvStatus.text = "In Progress"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_in_progress))
            }
            else -> {
                holder.tvStatus.text = "To Do"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_todo))
            }
        }

        // Due Date formatting
        formatDueDate(holder, task)

        // Created relative time
        val relativeCreated = DateUtils.getRelativeTimeSpanString(
            task.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        holder.tvCreated.text = relativeCreated

        // Click listeners
        holder.ivCheck.setOnClickListener {
            onToggleCompleted(task)
        }
        holder.btnEdit.setOnClickListener {
            onEdit(task)
        }
        holder.btnDelete.setOnClickListener {
            onDelete(task)
        }
    }

    private fun formatDueDate(holder: TaskViewHolder, task: TaskItem) {
        val context = holder.itemView.context
        val now = System.currentTimeMillis()
        val dueCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isToday = dueCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                dueCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = dueCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                dueCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(task.dueDate))
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(task.dueDate))

        when {
            task.isCompleted -> {
                holder.tvDueDate.text = "Completed"
                holder.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.status_completed))
            }
            task.isOverdue -> {
                holder.tvDueDate.text = "⚠️ Overdue ($dateFormat)"
                holder.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.overdue_red))
            }
            isToday -> {
                holder.tvDueDate.text = "📅 Due Today ($timeFormat)"
                holder.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.due_today_amber))
            }
            isTomorrow -> {
                holder.tvDueDate.text = "📅 Due Tomorrow ($timeFormat)"
                holder.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.secondary))
            }
            else -> {
                holder.tvDueDate.text = "📅 Due $dateFormat ($timeFormat)"
                holder.tvDueDate.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<TaskItem>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
