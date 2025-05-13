package com.example.visualsearch.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализация ViewModel
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        
        // Наблюдение за состоянием авторизации
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> showLoading(true)
                is AuthViewModel.AuthState.Success -> {
                    showLoading(false)
                    // Переход на главный экран после успешной регистрации
                    findNavController().navigate(R.id.action_registerFragment_to_navigation_home)
                }
                is AuthViewModel.AuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> showLoading(false)
            }
        }
        
        // Обработка нажатия на кнопку регистрации
        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
            
            if (validateInput(name, email, password, confirmPassword)) {
                authViewModel.register(email, password, name)
            }
        }
        
        // Переход на экран входа
        binding.loginLinkTextView.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        
        // Заглушка для Google авторизации (пока не реализовано)
        binding.googleSignInButton.setOnClickListener {
            Toast.makeText(requireContext(), "Регистрация через Google пока не реализована", 
                Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true
        
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Введите имя пользователя"
            isValid = false
        } else {
            binding.nameInputLayout.error = null
        }
        
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Введите email"
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }
        
        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Введите пароль"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error = "Пароль должен содержать минимум 6 символов"
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }
        
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = "Подтвердите пароль"
            isValid = false
        } else if (password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "Пароли не совпадают"
            isValid = false
        } else {
            binding.confirmPasswordInputLayout.error = null
        }
        
        return isValid
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !isLoading
        binding.googleSignInButton.isEnabled = !isLoading
        binding.nameInputLayout.isEnabled = !isLoading
        binding.emailInputLayout.isEnabled = !isLoading
        binding.passwordInputLayout.isEnabled = !isLoading
        binding.confirmPasswordInputLayout.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}