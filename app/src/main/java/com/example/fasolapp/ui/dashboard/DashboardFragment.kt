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
import java.util.Calendar

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
        binding.editheaderText.text = "Здравствуйте, $userName!"

        binding.buttonTasks.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_tasksFragment)
        }

        binding.buttonStatistics.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_statisticsFragment)
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

                val tasks = taskDao.getAllTasks()
                if (tasks.isEmpty()) {
                    insertDailyTasks(taskDao)
                }

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
                    binding.editstatsText.text = if (tasks.isEmpty()) {
                        "Нет задач"
                    } else {
                        "Задач выполнено: $completedCount / ${tasks.size}"
                    }

                    binding.tasksGrid.rowCount = tasks.size / 2 + tasks.size % 2
                    binding.tasksGrid.removeAllViews()
                    tasks.forEach { task ->
                        val taskView = createTaskView(task, userId, completedTaskDao)
                        val params = GridLayout.LayoutParams().apply {
                            width = 0
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
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
                            } else {
                                shiftDao.insertShift(Shift(employeeId = userId, startTime = System.currentTimeMillis()))
                            }
                            withContext(Dispatchers.Main) {
                                isShiftActive = !isShiftActive
                                binding.buttonShift.text = if (isShiftActive) "Завершить смену" else "Начать смену"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error loading data: ${e.message}", e)
            }
        }
    }

    private fun createTaskView(task: Task, employeeId: Int, completedTaskDao: CompletedTaskDao): View {
        val view = LayoutInflater.from(context).inflate(R.layout.task_item, null, false)
        val imageView: ImageView = view.findViewById(R.id.task_image)
        val nameText: TextView = view.findViewById(R.id.task_name)
        val timeText: TextView = view.findViewById(R.id.task_time)
        val statusText: TextView = view.findViewById(R.id.task_status)

        imageView.setImageResource(R.drawable.test)
        nameText.text = task.name
        timeText.text = "${task.startTime}–${task.endTime}"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val isCompleted = completedTaskDao.countCompletedTask(task.id, employeeId) > 0
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}