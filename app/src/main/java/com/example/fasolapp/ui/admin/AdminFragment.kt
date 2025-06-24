package com.example.fasolapp.ui.admin

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fasolapp.R
import com.example.fasolapp.data.database.AppDatabase
import com.example.fasolapp.data.entity.CompletedTask
import com.example.fasolapp.data.entity.Employee
import com.example.fasolapp.data.entity.Shift
import com.example.fasolapp.data.entity.Task
import com.example.fasolapp.databinding.FragmentAdminBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        val db = AppDatabase.getDatabase(requireContext())
        val employeeDao = db.employeeDao()
        val taskDao = db.taskDao()
        val completedTaskDao = db.completedTaskDao()
        val shiftDao = db.shiftDao()

        setupEmployeesList(employeeDao)
        setupTasksList(taskDao)
        setupCompletedTasksList(completedTaskDao, employeeDao, taskDao)
        setupShiftsList(shiftDao, employeeDao)
        setupStatistics(completedTaskDao, employeeDao, shiftDao)

        binding.addEmployeeButton.setOnClickListener {
            showAddEmployeeDialog(employeeDao)
        }

        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog(taskDao)
        }
    }

    private fun setupEmployeesList(employeeDao: com.example.fasolapp.data.dao.EmployeeDao) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val employees = employeeDao.getAllEmployees()
            withContext(Dispatchers.Main) {
                binding.employeesList.layoutManager = LinearLayoutManager(context)
                binding.employeesList.adapter = EmployeeAdapter(employees) { employee ->
                    showEditEmployeeDialog(employee, employeeDao)
                }
            }
        }
    }

    private fun setupTasksList(taskDao: com.example.fasolapp.data.dao.TaskDao) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val tasks = taskDao.getAllTasks()
            withContext(Dispatchers.Main) {
                binding.tasksList.layoutManager = LinearLayoutManager(context)
                binding.tasksList.adapter = TaskAdapter(tasks) { task ->
                    showEditTaskDialog(task, taskDao)
                }
            }
        }
    }

    private fun setupCompletedTasksList(
        completedTaskDao: com.example.fasolapp.data.dao.CompletedTaskDao,
        employeeDao: com.example.fasolapp.data.dao.EmployeeDao,
        taskDao: com.example.fasolapp.data.dao.TaskDao
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val completedTasks = completedTaskDao.getAllCompletedTasks()
            val employeeNames = employeeDao.getAllEmployees().associate { it.id to it.fullName }
            val taskNames = taskDao.getAllTasks().associate { it.id to it.name }
            withContext(Dispatchers.Main) {
                binding.completedTasksList.layoutManager = LinearLayoutManager(context)
                binding.completedTasksList.adapter = CompletedTaskAdapter(completedTasks, employeeNames, taskNames) { completedTask ->
                    showEditCompletedTaskDialog(completedTask, completedTaskDao)
                }
            }
        }
    }

    private fun setupShiftsList(shiftDao: com.example.fasolapp.data.dao.ShiftDao, employeeDao: com.example.fasolapp.data.dao.EmployeeDao) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val shifts = shiftDao.getAllShifts()
            val employeeNames = employeeDao.getAllEmployees().associate { it.id to it.fullName }
            withContext(Dispatchers.Main) {
                binding.shiftsList.layoutManager = LinearLayoutManager(context)
                binding.shiftsList.adapter = ShiftAdapter(shifts, employeeNames) { shift ->
                    showEditShiftDialog(shift, shiftDao)
                }
            }
        }
    }

    private fun setupStatistics(
        completedTaskDao: com.example.fasolapp.data.dao.CompletedTaskDao,
        employeeDao: com.example.fasolapp.data.dao.EmployeeDao,
        shiftDao: com.example.fasolapp.data.dao.ShiftDao
    ) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val endOfDay = calendar.timeInMillis
            calendar.add(Calendar.MONTH, -1)
            val startOfMonth = calendar.timeInMillis

            val employees = employeeDao.getAllEmployees()
            val topEmployeesMonth = employees.map { employee ->
                val taskCount = completedTaskDao.countCompletedTasks(employee.id, startOfMonth, endOfDay)
                employee.fullName to taskCount
            }.sortedByDescending { it.second }.take(3)

            val activeShifts = shiftDao.getAllShifts().filter { it.endTime == null }
            val topEmployeesShift = employees.map { employee ->
                val taskCount = activeShifts.filter { it.employeeId == employee.id }
                    .sumOf { shift -> completedTaskDao.countCompletedTasks(employee.id, shift.startTime, System.currentTimeMillis()) }
                employee.fullName to taskCount
            }.sortedByDescending { it.second }.take(3)

            withContext(Dispatchers.Main) {
                binding.topEmployeesMonth.text = "Топ сотрудников за месяц: " +
                        topEmployeesMonth.joinToString { "${it.first} (${it.second} задач)" }
                binding.topEmployeesMonth.background.setTint(Color.CYAN)
                binding.topEmployeesShift.text = "Топ сотрудников за смену: " +
                        topEmployeesShift.joinToString { "${it.first} (${it.second} задач)" }
                binding.topEmployeesShift.background.setTint(Color.CYAN)
            }
        }
    }

    private fun showAddEmployeeDialog(employeeDao: com.example.fasolapp.data.dao.EmployeeDao) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_employee, null)
        val loginInput = dialogView.findViewById<EditText>(R.id.login_input)
        val passwordInput = dialogView.findViewById<EditText>(R.id.password_input)
        val fullNameInput = dialogView.findViewById<EditText>(R.id.full_name_input)
        val roleInput = dialogView.findViewById<EditText>(R.id.role_input)

        AlertDialog.Builder(requireContext())
            .setTitle("Добавить сотрудника")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val employee = Employee(
                    login = loginInput.text.toString(),
                    password = passwordInput.text.toString(),
                    fullName = fullNameInput.text.toString(),
                    role = roleInput.text.toString()
                )
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    employeeDao.insertEmployee(employee)
                    val employees = employeeDao.getAllEmployees()
                    withContext(Dispatchers.Main) {
                        (binding.employeesList.adapter as EmployeeAdapter).updateData(employees)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditEmployeeDialog(employee: Employee, employeeDao: com.example.fasolapp.data.dao.EmployeeDao) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_employee, null)
        val loginInput = dialogView.findViewById<EditText>(R.id.login_input)
        val passwordInput = dialogView.findViewById<EditText>(R.id.password_input)
        val fullNameInput = dialogView.findViewById<EditText>(R.id.full_name_input)
        val roleInput = dialogView.findViewById<EditText>(R.id.role_input)

        loginInput.setText(employee.login)
        passwordInput.setText(employee.password)
        fullNameInput.setText(employee.fullName)
        roleInput.setText(employee.role)

        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать сотрудника")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedEmployee = employee.copy(
                    login = loginInput.text.toString(),
                    password = passwordInput.text.toString(),
                    fullName = fullNameInput.text.toString(),
                    role = roleInput.text.toString()
                )
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    employeeDao.updateEmployee(updatedEmployee)
                    val employees = employeeDao.getAllEmployees()
                    withContext(Dispatchers.Main) {
                        (binding.employeesList.adapter as EmployeeAdapter).updateData(employees)
                    }
                }
            }
            .setNegativeButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    employeeDao.deleteEmployee(employee)
                    val employees = employeeDao.getAllEmployees()
                    withContext(Dispatchers.Main) {
                        (binding.employeesList.adapter as EmployeeAdapter).updateData(employees)
                    }
                }
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun showAddTaskDialog(taskDao: com.example.fasolapp.data.dao.TaskDao) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.task_name_input)
        val startTimeInput = dialogView.findViewById<EditText>(R.id.start_time_input)
        val endTimeInput = dialogView.findViewById<EditText>(R.id.end_time_input)

        AlertDialog.Builder(requireContext())
            .setTitle("Добавить задачу")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.insertTask(
                        nameInput.text.toString(),
                        startTimeInput.text.toString(),
                        endTimeInput.text.toString()
                    )
                    val tasks = taskDao.getAllTasks()
                    withContext(Dispatchers.Main) {
                        (binding.tasksList.adapter as TaskAdapter).updateData(tasks)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditTaskDialog(task: Task, taskDao: com.example.fasolapp.data.dao.TaskDao) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.task_name_input)
        val startTimeInput = dialogView.findViewById<EditText>(R.id.start_time_input)
        val endTimeInput = dialogView.findViewById<EditText>(R.id.end_time_input)

        nameInput.setText(task.name)
        startTimeInput.setText(task.startTime)
        endTimeInput.setText(task.endTime)

        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать задачу")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedTask = task.copy(
                    name = nameInput.text.toString(),
                    startTime = startTimeInput.text.toString(),
                    endTime = endTimeInput.text.toString()
                )
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.updateTask(updatedTask)
                    val tasks = taskDao.getAllTasks()
                    withContext(Dispatchers.Main) {
                        (binding.tasksList.adapter as TaskAdapter).updateData(tasks)
                    }
                }
            }
            .setNegativeButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.deleteTask(task)
                    val tasks = taskDao.getAllTasks()
                    withContext(Dispatchers.Main) {
                        (binding.tasksList.adapter as TaskAdapter).updateData(tasks)
                    }
                }
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun showEditCompletedTaskDialog(completedTask: CompletedTask, completedTaskDao: com.example.fasolapp.data.dao.CompletedTaskDao) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить выполненную задачу")
            .setMessage("Удалить запись о выполнении задачи?")
            .setPositiveButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    completedTaskDao.deleteCompletedTask(completedTask)
                    val completedTasks = completedTaskDao.getAllCompletedTasks()
                    withContext(Dispatchers.Main) {
                        (binding.completedTasksList.adapter as CompletedTaskAdapter).updateData(completedTasks)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditShiftDialog(shift: Shift, shiftDao: com.example.fasolapp.data.dao.ShiftDao) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить смену")
            .setMessage("Удалить запись о смене?")
            .setPositiveButton("Удалить") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    shiftDao.deleteShift(shift)
                    val shifts = shiftDao.getAllShifts()
                    withContext(Dispatchers.Main) {
                        (binding.shiftsList.adapter as ShiftAdapter).updateData(shifts)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class EmployeeAdapter(
        private var employees: List<Employee>,
        private val onClick: (Employee) -> Unit
    ) : RecyclerView.Adapter<EmployeeAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val employee = employees[position]
            holder.textView.text = "${employee.fullName} (${employee.login}, ${employee.role})"
            holder.itemView.setOnClickListener { onClick(employee) }
        }

        override fun getItemCount(): Int = employees.size

        fun updateData(newEmployees: List<Employee>) {
            employees = newEmployees
            notifyDataSetChanged()
        }
    }

    class TaskAdapter(
        private var tasks: List<Task>,
        private val onClick: (Task) -> Unit
    ) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.textView.text = "${task.name} (${task.startTime}–${task.endTime})"
            holder.itemView.setOnClickListener { onClick(task) }
        }

        override fun getItemCount(): Int = tasks.size

        fun updateData(newTasks: List<Task>) {
            tasks = newTasks
            notifyDataSetChanged()
        }
    }

    class CompletedTaskAdapter(
        private var completedTasks: List<CompletedTask>,
        private val employeeNames: Map<Int, String>,
        private val taskNames: Map<Int, String>,
        private val onClick: (CompletedTask) -> Unit
    ) : RecyclerView.Adapter<CompletedTaskAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val completedTask = completedTasks[position]
            val employeeName = employeeNames[completedTask.employeeId] ?: "Неизвестный"
            val taskName = taskNames[completedTask.taskId] ?: "Неизвестная задача"
            holder.textView.text = "$employeeName: $taskName (${completedTask.completionDate})"
            holder.itemView.setOnClickListener { onClick(completedTask) }
        }

        override fun getItemCount(): Int = completedTasks.size

        fun updateData(newCompletedTasks: List<CompletedTask>) {
            completedTasks = newCompletedTasks
            notifyDataSetChanged()
        }
    }

    class ShiftAdapter(
        private var shifts: List<Shift>,
        private val employeeNames: Map<Int, String>,
        private val onClick: (Shift) -> Unit
    ) : RecyclerView.Adapter<ShiftAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val shift = shifts[position]
            val employeeName = employeeNames[shift.employeeId] ?: "Неизвестный"
            val endTime = shift.endTime?.toString() ?: "Активна"
            holder.textView.text = "$employeeName: ${shift.startTime}–$endTime"
            holder.itemView.setOnClickListener { onClick(shift) }
        }

        override fun getItemCount(): Int = shifts.size

        fun updateData(newShifts: List<Shift>) {
            shifts = newShifts
            notifyDataSetChanged()
        }
    }
}