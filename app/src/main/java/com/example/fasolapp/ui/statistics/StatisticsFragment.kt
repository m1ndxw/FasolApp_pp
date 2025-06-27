package com.example.fasolapp.ui.statistics

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fasolapp.R
import com.example.fasolapp.data.database.AppDatabase
import com.example.fasolapp.data.entity.CompletedTask
import com.example.fasolapp.databinding.FragmentStatisticsBinding
import com.example.fasolapp.databinding.ItemStatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка кнопки "Назад"
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", 0)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val taskDao = db.taskDao()
                val shiftDao = db.shiftDao()
                val employeeDao = db.employeeDao()
                val completedTaskDao = db.completedTaskDao()

                // Пункт 1: Задачи за день
                val tasksToday = calculateTasksToday(userId, taskDao, completedTaskDao)

                // Пункт 2: Длительность смены
                val shiftDuration = calculateShiftDuration(userId, shiftDao)

                // Пункт 3: Средний КПД за неделю
                val weeklyKpi = calculateWeeklyKpi(userId, taskDao, completedTaskDao)

                // Пункт 4: Рейтинг сотрудника
                val employeeRanking = calculateEmployeeRanking(userId, employeeDao, taskDao, completedTaskDao)

                // Пункт 5: Часто выполняемые задачи
                val frequentTasks = calculateFrequentTasks(userId, taskDao, completedTaskDao)

                withContext(Dispatchers.Main) {
                    binding.statsLayout.removeAllViews()

                    // Добавление статистики в UI
                    addStatView("Сегодня выполнено: ${tasksToday.completed} / ${tasksToday.total} задач (${tasksToday.percentage}%)",
                        if (tasksToday.percentage >= 80) Color.GREEN else if (tasksToday.percentage >= 50) Color.parseColor("#FFA500") else Color.RED)
                    addStatView(shiftDuration.text, shiftDuration.color)
                    addStatView("Средний КПД за неделю: ${weeklyKpi}%",
                        if (weeklyKpi >= 60) Color.parseColor("#800080") else if (weeklyKpi >= 30) Color.parseColor("#FFA500") else Color.RED)
                    addStatView("Ваш рейтинг: ${employeeRanking.position} место из ${employeeRanking.total} (${employeeRanking.percentage}%)",
                        if (employeeRanking.position <= 3) Color.GREEN else Color.YELLOW)
                    addStatView("Часто выполняемые задачи: ${frequentTasks.joinToString { "${it.first} (${it.second} раз)" }}",
                        Color.CYAN)
                }
            } catch (e: Exception) {
                Log.e("StatisticsFragment", "Error loading stats: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val errorView = TextView(context).apply {
                        text = "Ошибка загрузки статистики"
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    binding.statsLayout.addView(errorView)
                }
            }
        }
    }

    private suspend fun calculateTasksToday(
        userId: Int,
        taskDao: com.example.fasolapp.data.dao.TaskDao,
        completedTaskDao: com.example.fasolapp.data.dao.CompletedTaskDao
    ): TasksToday {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        val totalTasks = taskDao.getAllTasks().size
        val completedTasks = completedTaskDao.countCompletedTasks(userId, startOfDay, endOfDay)
        val percentage = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

        return TasksToday(completedTasks, totalTasks, percentage)
    }

    private suspend fun calculateShiftDuration(
        userId: Int,
        shiftDao: com.example.fasolapp.data.dao.ShiftDao
    ): ShiftDuration {
        val activeShift = shiftDao.getActiveShift(userId)
        return if (activeShift != null) {
            val durationMs = System.currentTimeMillis() - activeShift.startTime
            val hours = durationMs / (1000 * 60 * 60)
            val minutes = (durationMs / (1000 * 60)) % 60
            ShiftDuration(
                text = "Текущая смена: $hours часов $minutes минут",
                color = if (hours >= 8) Color.parseColor("#FFA500") else Color.BLUE
            )
        } else {
            ShiftDuration(text = "Смена не начата", color = Color.BLUE)
        }
    }

    private suspend fun calculateWeeklyKpi(
        userId: Int,
        taskDao: com.example.fasolapp.data.dao.TaskDao,
        completedTaskDao: com.example.fasolapp.data.dao.CompletedTaskDao
    ): Int {
        val calendar = Calendar.getInstance()
        var totalPercentage = 0
        val days = 7
        val totalTasks = taskDao.getAllTasks().size

        if (totalTasks == 0) return 0

        repeat(days) { day ->
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_MONTH, -day)
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            val completedTasks = completedTaskDao.countCompletedTasks(userId, startOfDay, endOfDay)
            val dailyPercentage = (completedTasks * 100 / totalTasks)
            totalPercentage += dailyPercentage
        }

        return totalPercentage / days
    }

    private suspend fun calculateEmployeeRanking(
        userId: Int,
        employeeDao: com.example.fasolapp.data.dao.EmployeeDao,
        taskDao: com.example.fasolapp.data.dao.TaskDao,
        completedTaskDao: com.example.fasolapp.data.dao.CompletedTaskDao
    ): EmployeeRanking {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        val totalTasks = taskDao.getAllTasks().size
        if (totalTasks == 0) return EmployeeRanking(1, 1, 0)

        val employees = employeeDao.getAllEmployees()
        val rankings = employees.map { employee ->
            val completedTasks = completedTaskDao.countCompletedTasks(employee.id, startOfDay, endOfDay)
            val percentage = (completedTasks * 100 / totalTasks)
            employee.id to percentage
        }.sortedByDescending { it.second }

        val userPosition = rankings.indexOfFirst { it.first == userId } + 1
        val userPercentage = rankings.find { it.first == userId }?.second ?: 0

        return EmployeeRanking(userPosition, employees.size, userPercentage)
    }

    private suspend fun calculateFrequentTasks(
        userId: Int,
        taskDao: com.example.fasolapp.data.dao.TaskDao,
        completedTaskDao: com.example.fasolapp.data.dao.CompletedTaskDao
    ): List<Pair<String, Int>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val startOfWeek = calendar.timeInMillis

        val taskCounts = completedTaskDao.getCompletedTasksByUser(userId, startOfWeek)
            .groupBy { it.taskId }
            .map { (taskId, tasks) ->
                val taskName = taskDao.getTaskById(taskId)?.name ?: "Неизвестная задача"
                taskName to tasks.size
            }
            .sortedByDescending { it.second }
            .take(2)

        return taskCounts
    }

    private fun addStatView(text: String, backgroundColor: Int) {
        val statBinding = ItemStatBinding.inflate(LayoutInflater.from(context), binding.statsLayout, false)
        statBinding.statText.text = text
        statBinding.root.background.setTint(backgroundColor)
        binding.statsLayout.addView(statBinding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class TasksToday(val completed: Int, val total: Int, val percentage: Int)
    data class ShiftDuration(val text: String, val color: Int)
    data class EmployeeRanking(val position: Int, val total: Int, val percentage: Int)
}