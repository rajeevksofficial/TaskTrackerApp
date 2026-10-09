package com.tasktracker.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tasktracker.app.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: TaskDatabaseHelper
    private lateinit var adapter: TaskAdapter

    private var currentStatusFilter: String = "ALL"
    private var currentCategoryFilter: String = "ALL"
    private var currentSortOrder: String = "DUE_DATE_ASC"
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = TaskDatabaseHelper(this)

        setupHeader()
        setupRecyclerView()
        setupSearch()
        setupStatusFilters()
        setupCategoryFilters()
        setupActionButtons()

        refreshData()
    }

    private fun setupHeader() {
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        binding.tvCurrentDate.text = dateFormat.format(Date())
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            tasks = emptyList(),
            onToggleCompleted = { task ->
                val newStatus = if (task.isCompleted) TaskItem.STATUS_TODO else TaskItem.STATUS_COMPLETED
                dbHelper.updateTaskStatus(task.id, newStatus)
                val msg = if (newStatus == TaskItem.STATUS_COMPLETED) "Task completed! 🎉" else "Task marked as To Do"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                refreshData()
            },
            onEdit = { task ->
                showAddOrEditTaskDialog(task)
            },
            onDelete = { task ->
                showDeleteConfirmationDialog(task)
            }
        )
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchTasks.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                refreshTaskList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchTasks.text?.clear()
        }
    }

    private fun setupStatusFilters() {
        val statusViews = listOf(
            binding.filterStatusAll to "ALL",
            binding.filterStatusTodo to TaskItem.STATUS_TODO,
            binding.filterStatusInProgress to TaskItem.STATUS_IN_PROGRESS,
            binding.filterStatusCompleted to TaskItem.STATUS_COMPLETED,
            binding.filterStatusOverdue to "OVERDUE"
        )

        for ((view, status) in statusViews) {
            view.setOnClickListener {
                currentStatusFilter = status
                highlightStatusFilter(view)
                refreshTaskList()
            }
        }
    }

    private fun highlightStatusFilter(activeView: TextView) {
        val allViews = listOf(
            binding.filterStatusAll,
            binding.filterStatusTodo,
            binding.filterStatusInProgress,
            binding.filterStatusCompleted,
            binding.filterStatusOverdue
        )

        for (v in allViews) {
            if (v == activeView) {
                v.setBackgroundResource(R.drawable.bg_chip_active)
                v.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                v.setBackgroundResource(R.drawable.bg_chip)
                if (v == binding.filterStatusOverdue) {
                    v.setTextColor(ContextCompat.getColor(this, R.color.overdue_red))
                } else {
                    v.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
        }
    }

    private fun setupCategoryFilters() {
        val catViews = listOf(
            binding.filterCatAll to "ALL",
            binding.filterCatWork to TaskItem.CATEGORY_WORK,
            binding.filterCatPersonal to TaskItem.CATEGORY_PERSONAL,
            binding.filterCatStudy to TaskItem.CATEGORY_STUDY,
            binding.filterCatHealth to TaskItem.CATEGORY_HEALTH,
            binding.filterCatFinance to TaskItem.CATEGORY_FINANCE,
            binding.filterCatShopping to TaskItem.CATEGORY_SHOPPING
        )

        for ((view, cat) in catViews) {
            view.setOnClickListener {
                currentCategoryFilter = cat
                highlightCategoryFilter(view)
                refreshTaskList()
            }
        }
    }

    private fun highlightCategoryFilter(activeView: TextView) {
        val allViews = listOf(
            binding.filterCatAll,
            binding.filterCatWork,
            binding.filterCatPersonal,
            binding.filterCatStudy,
            binding.filterCatHealth,
            binding.filterCatFinance,
            binding.filterCatShopping
        )

        for (v in allViews) {
            if (v == activeView) {
                v.setBackgroundResource(R.drawable.bg_chip_active)
                v.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                v.setBackgroundResource(R.drawable.bg_chip)
                v.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    private fun setupActionButtons() {
        // FAB Add Task
        binding.fabAddTask.setOnClickListener {
            showAddOrEditTaskDialog(null)
        }

        // Share Task List
        binding.btnShareTasks.setOnClickListener {
            shareTaskListSummary()
        }

        // Sort Options Dialog
        binding.btnSortOptions.setOnClickListener {
            showSortDialog()
        }
    }

    private fun refreshData() {
        refreshDashboardStats()
        refreshTaskList()
    }

    private fun refreshDashboardStats() {
        val stats = dbHelper.getTaskStats()
        binding.tvStatTotal.text = stats.total.toString()
        binding.tvStatTodo.text = stats.todo.toString()
        binding.tvStatInProgress.text = stats.inProgress.toString()
        binding.tvStatCompleted.text = stats.completed.toString()
        binding.tvProgressPercentage.text = "${stats.completionPercentage}% Done"
        binding.progressBarTasks.progress = stats.completionPercentage
    }

    private fun refreshTaskList() {
        val tasks = dbHelper.getFilteredTasks(
            query = currentSearchQuery,
            statusFilter = currentStatusFilter,
            categoryFilter = currentCategoryFilter,
            sortOrder = currentSortOrder
        )

        adapter.updateTasks(tasks)

        if (tasks.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvTasks.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvTasks.visibility = View.VISIBLE
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "Due Date (Earliest first)",
            "Due Date (Latest first)",
            "Priority (High to Low)",
            "Title (A to Z)"
        )
        val sortKeys = arrayOf("DUE_DATE_ASC", "DUE_DATE_DESC", "PRIORITY", "TITLE")
        val currentIdx = sortKeys.indexOf(currentSortOrder).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Sort Tasks")
            .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                currentSortOrder = sortKeys[which]
                refreshTaskList()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(task: TaskItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteTask(task.id)
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                refreshData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddOrEditTaskDialog(existingTask: TaskItem?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val tvTitle: TextView = dialogView.findViewById(R.id.tvDialogTitle)
        val etTitle: EditText = dialogView.findViewById(R.id.etDialogTitle)
        val etDesc: EditText = dialogView.findViewById(R.id.etDialogDesc)
        val btnDate: TextView = dialogView.findViewById(R.id.btnPickDueDate)
        val btnTime: TextView = dialogView.findViewById(R.id.btnPickDueTime)
        val btnSave: TextView = dialogView.findViewById(R.id.btnSaveTask)
        val btnCancel: TextView = dialogView.findViewById(R.id.btnCancelDialog)

        // Category chips
        val chipWork: TextView = dialogView.findViewById(R.id.chipCatWork)
        val chipPersonal: TextView = dialogView.findViewById(R.id.chipCatPersonal)
        val chipStudy: TextView = dialogView.findViewById(R.id.chipCatStudy)
        val chipHealth: TextView = dialogView.findViewById(R.id.chipCatHealth)
        val chipFinance: TextView = dialogView.findViewById(R.id.chipCatFinance)
        val chipShopping: TextView = dialogView.findViewById(R.id.chipCatShopping)
        val categoryChips = listOf(
            chipWork to TaskItem.CATEGORY_WORK,
            chipPersonal to TaskItem.CATEGORY_PERSONAL,
            chipStudy to TaskItem.CATEGORY_STUDY,
            chipHealth to TaskItem.CATEGORY_HEALTH,
            chipFinance to TaskItem.CATEGORY_FINANCE,
            chipShopping to TaskItem.CATEGORY_SHOPPING
        )

        // Priority chips
        val chipPriHigh: TextView = dialogView.findViewById(R.id.chipPriorityHigh)
        val chipPriMed: TextView = dialogView.findViewById(R.id.chipPriorityMed)
        val chipPriLow: TextView = dialogView.findViewById(R.id.chipPriorityLow)
        val priorityChips = listOf(
            chipPriHigh to TaskItem.PRIORITY_HIGH,
            chipPriMed to TaskItem.PRIORITY_MEDIUM,
            chipPriLow to TaskItem.PRIORITY_LOW
        )

        // Status chips
        val chipStatTodo: TextView = dialogView.findViewById(R.id.chipStatusTodo)
        val chipStatProg: TextView = dialogView.findViewById(R.id.chipStatusInProgress)
        val chipStatDone: TextView = dialogView.findViewById(R.id.chipStatusDone)
        val statusChips = listOf(
            chipStatTodo to TaskItem.STATUS_TODO,
            chipStatProg to TaskItem.STATUS_IN_PROGRESS,
            chipStatDone to TaskItem.STATUS_COMPLETED
        )

        var selectedCategory = existingTask?.category ?: TaskItem.CATEGORY_WORK
        var selectedPriority = existingTask?.priority ?: TaskItem.PRIORITY_MEDIUM
        var selectedStatus = existingTask?.status ?: TaskItem.STATUS_TODO

        val calendar = Calendar.getInstance()
        if (existingTask != null) {
            calendar.timeInMillis = existingTask.dueDate
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // default tomorrow
        }

        fun updateDateDisplays() {
            val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
            btnDate.text = dateFmt.format(calendar.time)
            btnTime.text = timeFmt.format(calendar.time)
        }
        updateDateDisplays()

        btnDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateDisplays()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    updateDateDisplays()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        fun refreshCategorySelections() {
            for ((chip, cat) in categoryChips) {
                if (cat == selectedCategory) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
        }

        for ((chip, cat) in categoryChips) {
            chip.setOnClickListener {
                selectedCategory = cat
                refreshCategorySelections()
            }
        }

        fun refreshPrioritySelections() {
            chipPriHigh.setBackgroundResource(if (selectedPriority == TaskItem.PRIORITY_HIGH) R.drawable.bg_priority_high else R.drawable.bg_chip)
            chipPriHigh.setTextColor(ContextCompat.getColor(this, if (selectedPriority == TaskItem.PRIORITY_HIGH) R.color.priority_high else R.color.text_secondary))

            chipPriMed.setBackgroundResource(if (selectedPriority == TaskItem.PRIORITY_MEDIUM) R.drawable.bg_priority_med else R.drawable.bg_chip)
            chipPriMed.setTextColor(ContextCompat.getColor(this, if (selectedPriority == TaskItem.PRIORITY_MEDIUM) R.color.priority_medium else R.color.text_secondary))

            chipPriLow.setBackgroundResource(if (selectedPriority == TaskItem.PRIORITY_LOW) R.drawable.bg_priority_low else R.drawable.bg_chip)
            chipPriLow.setTextColor(ContextCompat.getColor(this, if (selectedPriority == TaskItem.PRIORITY_LOW) R.color.priority_low else R.color.text_secondary))
        }

        for ((chip, pri) in priorityChips) {
            chip.setOnClickListener {
                selectedPriority = pri
                refreshPrioritySelections()
            }
        }

        fun refreshStatusSelections() {
            for ((chip, stat) in statusChips) {
                if (stat == selectedStatus) {
                    chip.setBackgroundResource(R.drawable.bg_chip_active)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
            }
        }

        for ((chip, stat) in statusChips) {
            chip.setOnClickListener {
                selectedStatus = stat
                refreshStatusSelections()
            }
        }

        // Prefill for edit mode
        if (existingTask != null) {
            tvTitle.text = "Edit Task"
            etTitle.setText(existingTask.title)
            etDesc.setText(existingTask.description)
            btnSave.text = "Update Task"
        } else {
            tvTitle.text = "Create New Task"
            btnSave.text = "Save Task"
        }

        refreshCategorySelections()
        refreshPrioritySelections()
        refreshStatusSelections()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "Title cannot be empty"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            val desc = etDesc.text.toString().trim()
            val dueDate = calendar.timeInMillis

            if (existingTask != null) {
                val updated = existingTask.copy(
                    title = title,
                    description = desc,
                    dueDate = dueDate,
                    priority = selectedPriority,
                    category = selectedCategory,
                    status = selectedStatus
                )
                dbHelper.updateTask(updated)
                Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show()
            } else {
                val newTask = TaskItem(
                    title = title,
                    description = desc,
                    dueDate = dueDate,
                    priority = selectedPriority,
                    category = selectedCategory,
                    status = selectedStatus
                )
                dbHelper.insertTask(newTask)
                Toast.makeText(this, "Task created! 🚀", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            refreshData()
        }

        dialog.show()
    }

    private fun shareTaskListSummary() {
        val tasks = dbHelper.getFilteredTasks(query = null, statusFilter = null, categoryFilter = null, sortOrder = "PRIORITY")
        if (tasks.isEmpty()) {
            Toast.makeText(this, "No tasks to share", Toast.LENGTH_SHORT).show()
            return
        }

        val stats = dbHelper.getTaskStats()
        val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        val summary = buildString {
            append("📋 TaskMaster Pro - Summary Report\n")
            append("📊 Overall Progress: ${stats.completionPercentage}% (${stats.completed}/${stats.total} completed)\n")
            append("⏳ Pending: ${stats.todo} | 🚀 In Progress: ${stats.inProgress} | ⚠️ Overdue: ${stats.overdue}\n\n")

            append("📌 ACTIVE TASKS:\n")
            val pendingTasks = tasks.filter { !it.isCompleted }
            if (pendingTasks.isNotEmpty()) {
                pendingTasks.forEachIndexed { i, t ->
                    val priEmoji = when (t.priority) {
                        TaskItem.PRIORITY_HIGH -> "🔴"
                        TaskItem.PRIORITY_MEDIUM -> "🟡"
                        else -> "🟢"
                    }
                    val dueStr = dateFmt.format(Date(t.dueDate))
                    append("${i + 1}. $priEmoji [${t.category}] ${t.title}\n")
                    append("   Status: ${t.status} | Due: $dueStr\n")
                    if (t.description.isNotEmpty()) {
                        append("   Notes: ${t.description}\n")
                    }
                }
            } else {
                append("   None! All caught up 🎉\n")
            }

            append("\n✅ COMPLETED TASKS:\n")
            val completedTasks = tasks.filter { it.isCompleted }
            if (completedTasks.isNotEmpty()) {
                completedTasks.forEachIndexed { i, t ->
                    append("${i + 1}. ✓ [${t.category}] ${t.title}\n")
                }
            } else {
                append("   None completed yet.\n")
            }

            append("\nGenerated via TaskMaster Pro 🚀")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, summary)
            putExtra(Intent.EXTRA_SUBJECT, "Task List Report")
            type = "text/plain"
        }

        startActivity(Intent.createChooser(sendIntent, "Share Tasks via"))
    }
}
