package com.example.fasolapp.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var isShiftActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация View Binding
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получение данных пользователя из SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", "Пользователь")
        val userId = sharedPreferences.getInt("userId", 0)
        binding.editheaderText.text = "Здравствуйте, $userName!"

        // Навигация к экрану задач
        binding.buttonTasks.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_tasksFragment)
        }

        // Навигация к экрану статистики
        binding.editstatsText.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_statisticsFragment)
        }

        // Загрузка статуса смены и задач
        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val shiftDao = db.shiftDao()
            val taskDao = db.taskDao()
            val completedTaskDao = db.completedTaskDao()
            val activeShift = shiftDao.getActiveShift(userId)
            val tasks = taskDao.getAllTasks()

            // Вычисление начала и конца текущего дня
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
                binding.editstatsText.text = "Задач выполнено: $completedCount / ${tasks.size}"

                binding.tasksLayout.removeAllViews()
                tasks.forEach { task ->
                    val taskView = createTaskView(task, userId, completedTaskDao)
                    binding.tasksLayout.addView(taskView)
                }

                // Обработка кнопки начала/окончания смены
                binding.buttonShift.setOnClickListener {
                    GlobalScope.launch(Dispatchers.IO) {
                        if (isShiftActive && activeShift != null) {
                            activeShift.endTime = System.currentTimeMillis()
                            shiftDao.updateShift(activeShift)
                        } else {
                            shiftDao.insertShift(Shift(employeeId = userId, startTime = System.currentTimeMillis()))
                            insertDailyTasks(taskDao)
                        }
                        withContext(Dispatchers.Main) {
                            isShiftActive = !isShiftActive
                            binding.buttonShift.text = if (isShiftActive) "Завершить смену" else "Начать смену"
                        }
                    }
                }
            }
        }
    }

    // Создание представления для задачи
    private fun createTaskView(task: Task, employeeId: Int, completedTaskDao: CompletedTaskDao): View {
        val view = LayoutInflater.from(context).inflate(R.layout.task_item, null, false)
        val imageView: ImageView = view.findViewById(R.id.task_image)
        val nameText: TextView = view.findViewById(R.id.task_name)
        val timeText: TextView = view.findViewById(R.id.task_time)
        val statusText: TextView = view.findViewById(R.id.task_status)

        imageView.setImageResource(R.drawable.test) // Заглушка для изображения
        nameText.text = task.name
        timeText.text = "${task.startTime}–${task.endTime}"

        GlobalScope.launch(Dispatchers.IO) {
            val isCompleted = completedTaskDao.countCompletedTask(task.id, employeeId) > 0
            withContext(Dispatchers.Main) {
                statusText.text = if (isCompleted) "Выполнено" else "Не выполнено"
            }
        }

        return view
    }

    // Вставка ежедневных задач в БД
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очистка binding
    }
}