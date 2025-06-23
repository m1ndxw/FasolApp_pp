package com.example.fasolapp.ui.statistics

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fasolapp.R
import com.example.fasolapp.data.database.AppDatabase
import com.example.fasolapp.data.dao.CompletedTaskDao
import com.example.fasolapp.data.dao.TaskDao
import com.example.fasolapp.data.entity.Shift
import com.example.fasolapp.databinding.FragmentStatisticsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
        // Инициализация View Binding
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получение ID пользователя
        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("userId", 0)

        // Загрузка статистики
        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val shiftDao = db.shiftDao()
            val completedTaskDao = db.completedTaskDao()
            val taskDao = db.taskDao()

            // Статистика за неделю
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis
            val weekEnd = System.currentTimeMillis()
            val weekShifts = shiftDao.getShiftsInRange(userId, weekStart, weekEnd)
            val weekCompleted = completedTaskDao.countCompletedTasks(userId, weekStart, weekEnd)
            val totalTasksWeek = taskDao.getAllTasks().size * 7

            // Статистика за месяц
            calendar.add(Calendar.DAY_OF_MONTH, -23) // Отмотать назад до 30 дней
            val monthStart = calendar.timeInMillis
            val monthEnd = System.currentTimeMillis()
            val monthShifts = shiftDao.getShiftsInRange(userId, monthStart, monthEnd)
            val monthCompleted = completedTaskDao.countCompletedTasks(userId, monthStart, monthEnd)
            val totalTasksMonth = taskDao.getAllTasks().size * 30
            val totalAllCompleted = completedTaskDao.countAllCompletedTasks(monthStart, monthEnd)
            val avgCompleted = if (totalAllCompleted > 0) totalAllCompleted / 30.0 else 1.0

            withContext(Dispatchers.Main) {
                // Вычисление часов работы
                val weekDuration = weekShifts.filter { it.endTime != null }
                    .sumOf { (it.endTime!! - it.startTime) / 3600000.0 }
                val monthDuration = monthShifts.filter { it.endTime != null }
                    .sumOf { (it.endTime!! - it.startTime) / 3600000.0 }
                val successRate = if (avgCompleted > 0) (monthCompleted / avgCompleted * 100) else 0.0

                // Добавление элементов статистики
                binding.statsLayout.addView(createStatView("Часы за неделю: ${String.format("%.2f", weekDuration)} ч"))
                binding.statsLayout.addView(createStatView("Задачи за неделю: $weekCompleted/$totalTasksWeek"))
                binding.statsLayout.addView(createStatView("Часы за месяц: ${String.format("%.2f", monthDuration)} ч"))
                binding.statsLayout.addView(createStatView("Задачи за месяц: $monthCompleted/$totalTasksMonth"))
                binding.statsLayout.addView(createStatView("Успеваемость: ${String.format("%.1f", successRate)}%"))
            }
        }
    }

    // Создание представления для статистики
    private fun createStatView(text: String): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_stat, null, false)
        val textView: TextView = view.findViewById(R.id.stat_text)
        textView.text = text
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очистка binding
    }
}