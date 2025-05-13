package com.example.visualsearch.ui.profile

import android.os.Bundle
import android.util.Log // Добавь импорт для логирования, если нужно
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentProfileBinding
import com.example.visualsearch.ui.auth.AuthViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ProfileFragment", "onCreateView") // Лог для отладки
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ProfileFragment", "onViewCreated") // Лог для отладки

        // Инициализация ViewModel
        // Используем requireActivity() чтобы ViewModel была связана с Activity и переживала смену фрагментов
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        // Наблюдение за данными пользователя
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            Log.d("ProfileFragment", "currentUser observed: ${user?.email}") // Лог для отладки
            if (user != null) {
                // Пользователь авторизован, отображаем данные
                binding.userNameTextView.text = user.displayName ?: "Имя не указано"
                binding.userEmailTextView.text = user.email
                binding.logoutButton.visibility = View.VISIBLE // Показываем кнопку выхода
                // Можно добавить логику скрытия/показа других элементов для авторизованного/неавторизованного состояния

                // ... код загрузки аватара ...

            } else {
                // Пользователь НЕ авторизован
                binding.logoutButton.visibility = View.GONE // Скрываем кнопку выхода
                // Очищаем поля или показываем placeholder'ы
                binding.userNameTextView.text = "Гость"
                binding.userEmailTextView.text = "Войдите в аккаунт"


                // Перенаправляем на экран входа ТОЛЬКО ЕСЛИ мы все еще на экране профиля
                // Используем НОВОЕ действие, чтобы не очищать весь стек
                if (findNavController().currentDestination?.id == R.id.navigation_profile) {
                    Log.d("ProfileFragment", "User null, redirecting via action_profile_redirect_unauthenticated_to_login") // Лог
                    findNavController().navigate(R.id.action_profile_redirect_unauthenticated_to_login) // <-- ИСПОЛЬЗУЕМ НОВОЕ ДЕЙСТВИЕ
                } else {
                    Log.d("ProfileFragment", "User null, but current destination is not Profile (${findNavController().currentDestination?.label}), no redirect.") // Лог
                }
            }
        }

        // Обработка нажатия кнопки выхода
        binding.logoutButton.setOnClickListener {
            Log.d("ProfileFragment", "Logout button clicked") // Лог
            // Используем СТАРОЕ действие для полного выхода и очистки стека
            authViewModel.logout()
            Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show()
            // findNavController().navigate(R.id.action_navigation_profile_to_loginFragment) // <-- НЕ НАДО, Observer сделает это сам, используя НУЖНОЕ действие
            // Важно: после logout() значение currentUser изменится на null, и код в Observer выше сработает сам,
            // выполнив навигацию через action_profile_redirect_unauthenticated_to_login.
            // Если же нужен именно ПОЛНЫЙ сброс стека при нажатии кнопки, то тогда раскомментируй строку ниже
            // и закомментируй навигацию в блоке else у Observer'a. Но текущий вариант с Observer'ом кажется логичнее.
            // findNavController().navigate(R.id.action_navigation_profile_to_loginFragment) // Если нужен полный сброс стека *именно* по кнопке
        }

        // Обработка нажатия кнопки редактирования профиля
        binding.editProfileButton.setOnClickListener {
            Toast.makeText(requireContext(), "Редактирование профиля пока недоступно", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ProfileFragment", "onDestroyView") // Лог для отладки
        _binding = null
    }
}