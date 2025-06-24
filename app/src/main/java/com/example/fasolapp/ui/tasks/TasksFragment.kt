package com.example.fasolapp.ui.tasks

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fasolapp.R
import com.example.fasolapp.data.database.AppDatabase
import com.example.fasolapp.data.dao.CompletedTaskDao
import com.example.fasolapp.data.entity.CompletedTask
import com.example.fasolapp.data.entity.Task
import com.example.fasolapp.databinding.FragmentTasksBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TasksFragment : Fragment() {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
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
                val completedTaskDao = db.completedTaskDao()
                val shiftDao = db.shiftDao()
                val tasks = taskDao.getAllTasks()

                Log.d("TasksFragment", "Tasks loaded: ${tasks.size}")

                withContext(Dispatchers.Main) {
                    binding.tasksLayout.removeAllViews()
                    if (tasks.isEmpty()) {
                        val emptyView = TextView(context).apply {
                            text = "Нет задач"
                            textSize = 16f
                            setPadding(16, 16, 16, 16)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        }
                        binding.tasksLayout.addView(emptyView)
                    } else {
                        tasks.forEach { task ->
                            val taskView = createTaskView(task, userId, completedTaskDao, shiftDao)
                            binding.tasksLayout.addView(taskView)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TasksFragment", "Error loading tasks: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    val errorView = TextView(context).apply {
                        text = "Ошибка загрузки задач"
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    binding.tasksLayout.addView(errorView)
                }
            }
        }
    }

    private fun createTaskView(
        task: Task,
        employeeId: Int,
        completedTaskDao: CompletedTaskDao,
        shiftDao: com.example.fasolapp.data.dao.ShiftDao
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task_action, binding.tasksLayout, false)
        val imageView: ImageView = view.findViewById(R.id.task_image)
        val nameText: TextView = view.findViewById(R.id.task_name)
        val timeText: TextView = view.findViewById(R.id.task_time)
        val actionButton: Button = view.findViewById(R.id.action_button)

        imageView.setImageResource(R.drawable.test)
        nameText.text = task.name
        timeText.text = "${task.startTime}–${task.endTime}"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val isCompleted = completedTaskDao.countCompletedTask(task.id, employeeId) > 0
                val activeShift = shiftDao.getActiveShift(employeeId)
                val isShiftActive = activeShift != null
                val isTaskTimeValid = isTaskStartTimeValid(task.startTime)

                withContext(Dispatchers.Main) {
                    actionButton.isEnabled = !isCompleted && isShiftActive && isTaskTimeValid
                    actionButton.text = if (isCompleted) "Выполнено" else "Отметить"

                    if (!isShiftActive) {
                        actionButton.setOnClickListener {
                            Toast.makeText(context, "Для выполнения задачи начните смену", Toast.LENGTH_SHORT).show()
                        }
                    } else if (!isTaskTimeValid) {
                        actionButton.setOnClickListener {
                            Toast.makeText(context, "Время начала задачи ещё не наступило", Toast.LENGTH_SHORT).show()
                        }
                    } else if (!isCompleted) {
                        actionButton.setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                completedTaskDao.insertCompletedTask(
                                    CompletedTask(
                                        taskId = task.id,
                                        employeeId = employeeId,
                                        completionDate = System.currentTimeMillis()
                                    )
                                )
                                withContext(Dispatchers.Main) {
                                    actionButton.isEnabled = false
                                    actionButton.text = "Выполнено"
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TasksFragment", "Error checking task status: ${e.message}", e)
            }
        }
        return view
    }

    private fun isTaskStartTimeValid(startTime: String): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            val currentTimeMillis = calendar.timeInMillis

            // Устанавливаем дату на текущий день
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Парсим время начала задачи (HH:mm)
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val taskTime = timeFormat.parse(startTime)
            val taskCalendar = Calendar.getInstance().apply {
                time = taskTime
                set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
            }

            // Сравниваем с текущим временем
            currentTimeMillis >= taskCalendar.timeInMillis
        } catch (e: Exception) {
            Log.e("TasksFragment", "Error parsing task start time: ${e.message}", e)
            false // Если парсинг не удался, считаем задачу недоступной
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}