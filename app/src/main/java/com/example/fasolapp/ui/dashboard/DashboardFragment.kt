package com.example.fasolapp.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fasolapp.R
import com.example.fasolapp.data.database.AppDatabase
import com.example.fasolapp.data.dao.CompletedTaskDao
import com.example.fasolapp.data.dao.TaskDao
import com.example.fasolapp.data.entity.CompletedTask
import com.example.fasolapp.data.entity.Shift
import com.example.fasolapp.data.entity.Task
import com.example.fasolapp.databinding.FragmentDashboardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var isShiftActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "Пользователь")
        val userId = sharedPreferences.getInt("userId", 0)
        val userRole = sharedPreferences.getString("userRole", "Кассир")
        binding.editheaderText.text = "Здравствуйте, $userName!"

        // Показать кнопку "Администрирование" только для руководителей
        if (userRole == "Руководитель") {
            binding.buttonAdmin.visibility = View.VISIBLE
        }

        binding.buttonTasks.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_tasksFragment)
        }

        binding.buttonStatistics.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_statisticsFragment)
        }

        binding.buttonAdmin.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_adminFragment)
        }

        binding.editstatsText.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_statisticsFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val shiftDao = db.shiftDao()
                val taskDao = db.taskDao()
                val completedTaskDao = db.completedTaskDao()

                val allTasks = taskDao.getAllTasks()
                if (allTasks.isEmpty()) {
                    insertDailyTasks(taskDao)
                }

                // Фильтруем задачи, доступные в данный момент
                val tasks = allTasks.filter { isTaskTimeValid(it.startTime, it.endTime) }

                val activeShift = shiftDao.getActiveShift(userId)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.timeInMillis

                val completedCount = completedTaskDao.countCompletedTasks(userId, startOfDay, endOfDay)

                withContext(Dispatchers.Main) {
                    isShiftActive = activeShift != null
                    binding.buttonShift.text = if (isShiftActive) "Завершить смену" else "Начать смену"
                    binding.editstatsText.text = if (allTasks.isEmpty()) {
                        "Нет задач"
                    } else {
                        "Задач выполнено: $completedCount / ${allTasks.size}"
                    }

                    // Проверяем, можно ли начать смену (8:00–19:00)
                    binding.buttonShift.isEnabled = isShiftStartTimeValid() && !isShiftActive

                    binding.tasksGrid.rowCount = (tasks.size + 1) / 2 // Округляем вверх
                    binding.tasksGrid.removeAllViews()
                    tasks.forEachIndexed { index, task ->
                        val taskView = createTaskView(task, userId, completedTaskDao, startOfDay, endOfDay)
                        val params = GridLayout.LayoutParams().apply {
                            width = 0 // Use weight to distribute evenly
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            rowSpec = GridLayout.spec(index / 2)
                            columnSpec = GridLayout.spec(index % 2, 1f) // Equal weight for columns
                            setMargins(8, 8, 8, 8)
                        }
                        taskView.layoutParams = params
                        binding.tasksGrid.addView(taskView)
                    }

                    binding.buttonShift.setOnClickListener {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            if (isShiftActive && activeShift != null) {
                                activeShift.endTime = System.currentTimeMillis()
                                shiftDao.updateShift(activeShift)
                                withContext(Dispatchers.Main) {
                                    isShiftActive = false
                                    binding.buttonShift.text = "Начать смену"
                                    binding.buttonShift.isEnabled = isShiftStartTimeValid()
                                }
                            } else if (isShiftStartTimeValid()) {
                                shiftDao.insertShift(Shift(employeeId = userId, startTime = System.currentTimeMillis()))
                                withContext(Dispatchers.Main) {
                                    isShiftActive = true
                                    binding.buttonShift.text = "Завершить смену"
                                    binding.buttonShift.isEnabled = true
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error loading data: ${e.message}", e)
            }
        }
    }

    private fun createTaskView(task: Task, employeeId: Int, completedTaskDao: CompletedTaskDao, startOfDay: Long, endOfDay: Long): View {
        val view = LayoutInflater.from(context).inflate(R.layout.task_item, null, false)
        val imageView: ImageView = view.findViewById(R.id.task_image)
        val nameText: TextView = view.findViewById(R.id.task_name)
        val timeText: TextView = view.findViewById(R.id.task_time)
        val statusText: TextView = view.findViewById(R.id.task_status)

        imageView.setImageResource(R.drawable.test)
        nameText.text = task.name
        timeText.text = "${task.startTime}–${task.endTime}"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val isCompleted = completedTaskDao.countCompletedTaskInDateRange(task.id, employeeId, startOfDay, endOfDay) > 0
            withContext(Dispatchers.Main) {
                statusText.text = if (isCompleted) "Выполнено" else "Не выполнено"
            }
        }

        return view
    }

    private suspend fun insertDailyTasks(taskDao: TaskDao) {
        taskDao.deleteAllTasks()
        val tasks = listOf(
            Task(name = "Открытие кассы", startTime = "8:00", endTime = "8:30"),
            Task(name = "Выкладка товара", startTime = "8:00", endTime = "16:00"),
            Task(name = "Фасовка товара", startTime = "10:00", endTime = "18:00"),
            Task(name = "Расстановка ценников", startTime = "8:00", endTime = "18:00"),
            Task(name = "Уборка", startTime = "20:30", endTime = "22:00"),
            Task(name = "Закрытие кассы", startTime = "21:30", endTime = "22:05"),
            Task(name = "Выкладка сигарет", startTime = "11:00", endTime = "16:00"),
            Task(name = "Очистка кофемашины", startTime = "8:00", endTime = "22:00")
        )
        taskDao.insertTasks(tasks)
    }

    private fun isTaskTimeValid(startTime: String, endTime: String): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            val currentTimeMillis = calendar.timeInMillis

            // Устанавливаем дату на текущий день
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Парсим время начала и окончания задачи (HH:mm)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTaskTime = timeFormat.parse(startTime)
            val endTaskTime = timeFormat.parse(endTime)

            val taskStartCalendar = Calendar.getInstance().apply {
                time = startTaskTime
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
            }
            val taskEndCalendar = Calendar.getInstance().apply {
                time = endTaskTime
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
            }

            // Проверяем, если endTime меньше startTime, добавляем 1 день к endTime
            if (taskEndCalendar.timeInMillis < taskStartCalendar.timeInMillis) {
                taskEndCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Проверяем, находится ли текущее время в пределах [startTime, endTime]
            currentTimeMillis >= taskStartCalendar.timeInMillis && currentTimeMillis <= taskEndCalendar.timeInMillis
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error parsing task time: ${e.message}", e)
            false // Если парсинг не удался, считаем задачу недоступной
        }
    }

    private fun isShiftStartTimeValid(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeInMinutes = currentHour * 60 + currentMinute
        val startTimeInMinutes = 8 * 60 // 8:00
        val endTimeInMinutes = 19 * 60 // 19:00
        return currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}