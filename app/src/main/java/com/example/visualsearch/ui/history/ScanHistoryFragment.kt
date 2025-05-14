package com.example.visualsearch.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visualsearch.MainActivity
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentScanHistoryBinding
import com.example.visualsearch.util.CustomToast
import com.example.visualsearch.util.ToastType
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ScanHistoryFragment : Fragment() {

    private val TAG = "ScanHistoryFragment"
    private var _binding: FragmentScanHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScanHistoryViewModel
    private lateinit var adapter: ScanHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView вызван")
        _binding = FragmentScanHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated вызван")

        // Используем обновленную фабрику, которая сама создает FirestoreScanRepository
        val viewModelFactory = ScanHistoryViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, viewModelFactory).get(ScanHistoryViewModel::class.java)
        Log.d(TAG, "ViewModel создан")

        setupRecyclerView()
        setupButtons()
        observeViewModel()

        // Анимация появления заголовка
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        fadeIn.duration = 1000
        binding.textViewHistoryTitle.startAnimation(fadeIn)
        binding.textViewHistorySubtitle.startAnimation(fadeIn)
        binding.divider.startAnimation(fadeIn)
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Инициализация RecyclerView")
        
        // Проверяем состояние авторизации
        val isLoggedIn = viewModel.isUserLoggedIn()
        Log.d(TAG, "Пользователь авторизован: $isLoggedIn")
        
        adapter = ScanHistoryAdapter(requireContext()) { scanHistory ->
            // Navigate to scan detail fragment
            Log.d(TAG, "Нажатие на элемент истории: ${scanHistory.id}")
            val action = ScanDetailFragmentArgs(scanHistory.id).toNavDirections()
            findNavController().navigate(action)
        }

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ScanHistoryFragment.adapter
            Log.d(TAG, "Адаптер установлен для RecyclerView")

            // Добавляем анимацию при прокрутке
            postponeEnterTransition()
            viewTreeObserver.addOnPreDrawListener {
                startPostponedEnterTransition()
                true
            }
        }
    }

    private fun setupButtons() {
        // Настраиваем кнопку очистки истории
        binding.buttonClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }

        // Настраиваем кнопку "Начать поиск" для пустого состояния
        binding.buttonStartScan?.setOnClickListener {
            // Переходим на главный экран
            findNavController().navigate(R.id.navigation_home)
        }
    }

    private fun observeViewModel() {
        Log.d(TAG, "Настройка наблюдателя для userScans")
        
        // Проверяем статус авторизации сразу
        val loggedIn = viewModel.isUserLoggedIn()
        val userId = if (loggedIn) viewModel.getCurrentUserId() else "не авторизован"
        Log.d(TAG, "Статус авторизации при настройке наблюдателя: $loggedIn, userId: $userId")
        
        // Настраиваем отображение при запуске на основе статуса авторизации
        if (!loggedIn) {
            Log.d(TAG, "Пользователь не авторизован при первоначальной загрузке, показываем сообщение о входе")
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recyclerViewHistory.visibility = View.GONE
            binding.textViewEmptyHistory?.text = getString(R.string.login_to_save_history)
            binding.buttonStartScan?.text = getString(R.string.login)
            binding.buttonStartScan?.setOnClickListener {
                findNavController().navigate(R.id.action_history_to_login)
            }
        }
        
        viewModel.userScans.observe(viewLifecycleOwner) { scans ->
            Log.d(TAG, "Получены данные: ${scans.size} элементов")
            scans.forEach { scan ->
                Log.d(TAG, "Элемент: id=${scan.id}, запрос=${scan.query}, userId=${scan.userId}")
            }
            
            adapter.submitList(scans)
            Log.d(TAG, "Данные отправлены в адаптер")

            // Показываем пустое состояние или список
            if (scans.isEmpty()) {
                Log.d(TAG, "Список пуст, показываем пустое состояние")
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
                
                // Проверяем, авторизован ли пользователь
                if (!viewModel.isUserLoggedIn()) {
                    Log.d(TAG, "Пользователь не авторизован, показываем сообщение о входе")
                    // Если пользователь не авторизован, показываем сообщение о необходимости входа
                    binding.textViewEmptyHistory?.text = getString(R.string.login_to_save_history)
                    binding.buttonStartScan?.text = getString(R.string.login)
                    binding.buttonStartScan?.setOnClickListener {
                        findNavController().navigate(R.id.action_history_to_login)
                    }
                } else {
                    Log.d(TAG, "Пользователь авторизован, но история пуста")
                    // Если пользователь авторизован, но истории нет
                    binding.textViewEmptyHistory?.text = getString(R.string.empty_history)
                    binding.buttonStartScan?.text = getString(R.string.start_scanning)
                    binding.buttonStartScan?.setOnClickListener {
                        findNavController().navigate(R.id.navigation_home)
                    }
                }
                
            } else {
                Log.d(TAG, "Есть данные, показываем список")
                binding.emptyStateLayout.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.VISIBLE

                // Анимируем список при обновлении
                binding.recyclerViewHistory.scheduleLayoutAnimation()
            }
        }
    }

    private fun showClearHistoryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Очистить историю")
            .setMessage("Вы уверены, что хотите удалить всю историю поиска?")
            .setPositiveButton("Очистить") { _, _ ->
                viewModel.deleteAllUserScans()

                // Анимируем переход к пустому состоянию
                val fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
                fadeOut.duration = 300
                binding.recyclerViewHistory.startAnimation(fadeOut)

                CustomToast.showToast(requireContext(), "История очищена", type = ToastType.SUCCESS)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume вызван")
        
        // Обновляем данные при возобновлении фрагмента
        if (::viewModel.isInitialized) {
            viewModel.refreshUserScans()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView вызван")
        _binding = null
    }
}