package com.example.fasolapp.ui.tasks

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fasolapp.R
import com.example.fasolapp.data.database.AppDatabase
import com.example.fasolapp.data.dao.CompletedTaskDao
import com.example.fasolapp.data.entity.CompletedTask
import com.example.fasolapp.data.entity.Task
import com.example.fasolapp.databinding.FragmentTasksBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TasksFragment : Fragment() {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация View Binding
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получение ID пользователя
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", 0)

        // Загрузка задач
        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val taskDao = db.taskDao()
            val completedTaskDao = db.completedTaskDao()
            val tasks = taskDao.getAllTasks()

            withContext(Dispatchers.Main) {
                binding.tasksLayout.removeAllViews()
                tasks.forEach { task ->
                    val taskView = createTaskView(task, userId, completedTaskDao)
                    binding.tasksLayout.addView(taskView)
                }
            }
        }
    }

    // Создание представления для задачи с кнопкой
    private fun createTaskView(task: Task, employeeId: Int, completedTaskDao: CompletedTaskDao): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_task_action, null, false)
        val imageView: ImageView = view.findViewById(R.id.task_image)
        val nameText: TextView = view.findViewById(R.id.task_name)
        val timeText: TextView = view.findViewById(R.id.task_time)
        val actionButton: Button = view.findViewById(R.id.action_button)

        imageView.setImageResource(R.drawable.test) // Заглушка для изображения
        nameText.text = task.name
        timeText.text = "${task.startTime}–${task.endTime}"

        GlobalScope.launch(Dispatchers.IO) {
            val isCompleted = completedTaskDao.countCompletedTask(task.id, employeeId) > 0
            withContext(Dispatchers.Main) {
                actionButton.isEnabled = !isCompleted
                actionButton.text = if (isCompleted) "Выполнено" else "Отметить"
                if (!isCompleted) {
                    actionButton.setOnClickListener {
                        GlobalScope.launch(Dispatchers.IO) {
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
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очистка binding
    }
}