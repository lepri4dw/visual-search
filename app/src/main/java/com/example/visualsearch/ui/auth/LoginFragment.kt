package com.example.visualsearch.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LoginFragment", "onCreateView")
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("LoginFragment", "onViewCreated")
        
        // Инициализация ViewModel
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]
        
        // Наблюдение за состоянием авторизации
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> showLoading(true)
                is AuthViewModel.AuthState.Success -> {
                    showLoading(false)
                    // Переход на главный экран после успешного входа
                    findNavController().navigate(R.id.action_loginFragment_to_navigation_home)
                }
                is AuthViewModel.AuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> showLoading(false)
            }
        }
        
        // Обработка нажатия на кнопку входа
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            
            if (validateInput(email, password)) {
                authViewModel.login(email, password)
            }
        }
        
        // Переход на экран регистрации
        binding.registerLinkTextView.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        
        // Заглушка для Google авторизации (пока не реализовано)
        binding.googleSignInButton.setOnClickListener {
            Toast.makeText(requireContext(), "Авторизация через Google пока не реализована", 
                Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d("LoginFragment", "onResume")
        
        // Сброс состояния авторизации при каждом открытии экрана
        authViewModel.resetAuthState()
    }
    
    override fun onPause() {
        super.onPause()
        Log.d("LoginFragment", "onPause")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LoginFragment", "onDestroyView")
        _binding = null
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true
        
        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Введите email"
            isValid = false
        } else {
            binding.emailInputLayout.error = null
        }
        
        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Введите пароль"
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }
        
        return isValid
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !isLoading
        binding.googleSignInButton.isEnabled = !isLoading
        binding.emailInputLayout.isEnabled = !isLoading
        binding.passwordInputLayout.isEnabled = !isLoading
    }
}