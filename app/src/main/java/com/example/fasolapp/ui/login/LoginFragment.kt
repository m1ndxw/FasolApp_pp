package com.example.fasolapp.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fasolapp.R
import com.example.fasolapp.data.database.AppDatabase
import com.example.fasolapp.data.entity.Employee
import com.example.fasolapp.databinding.FragmentLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация View Binding
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация тестовых пользователей
        GlobalScope.launch(Dispatchers.IO) {
            val employeeDao = AppDatabase.getDatabase(requireContext()).employeeDao()
            // Проверяем, есть ли пользователи в базе
            val existingUsers = employeeDao.getEmployeeByLogin("ivan", "pass123")
            if (existingUsers == null) {
                // Добавляем тестовых пользователей
                employeeDao.insertEmployee(
                    Employee(
                        login = "ivan",
                        password = "pass123",
                        fullName = "Иван Иванов",
                        role = "Кассир"
                    )
                )
                employeeDao.insertEmployee(
                    Employee(
                        login = "anna",
                        password = "pass456",
                        fullName = "Анна Петрова",
                        role = "Руководитель"
                    )
                )
            }
        }

        // Обработка нажатия на кнопку входа
        binding.buttonLogin.setOnClickListener {
            val login = binding.editTextLogin.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            GlobalScope.launch(Dispatchers.IO) {
                val employeeDao = AppDatabase.getDatabase(requireContext()).employeeDao()
                val employee = employeeDao.getEmployeeByLogin(login, password)
                withContext(Dispatchers.Main) {
                    if (employee != null) {
                        // Сохранение данных пользователя в SharedPreferences
                        val sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putInt("userId", employee.id)
                            .putString("userName", employee.fullName)
                            .putString("userRole", employee.role)
                            .apply()

                        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                    } else {
                        Toast.makeText(context, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Очистка binding
    }
}