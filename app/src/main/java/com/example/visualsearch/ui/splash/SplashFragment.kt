package com.example.visualsearch.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentSplashBinding
import com.example.visualsearch.ui.auth.AuthViewModel

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация ViewModel
        authViewModel = ViewModelProvider(requireActivity())[AuthViewModel::class.java]

        // Имитация загрузки в течение 2 секунд
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 2000)
    }

    private fun navigateToNextScreen() {
        // Проверяем, авторизован ли пользователь
        if (authViewModel.isLoggedIn()) {
            // Если авторизован, переходим на главный экран
            findNavController().navigate(R.id.action_splashFragment_to_navigation_home)
        } else {
            // Если не авторизован, переходим на экран входа
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}