package com.example.visualsearch.ui.history

import android.os.Bundle
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

    private var _binding: FragmentScanHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScanHistoryViewModel
    private lateinit var adapter: ScanHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(ScanHistoryViewModel::class.java)

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
        adapter = ScanHistoryAdapter(requireContext()) { scanHistory ->
            // Navigate to scan detail fragment
            val action = ScanDetailFragmentArgs(scanHistory.id).toNavDirections()
            findNavController().navigate(action)
        }

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ScanHistoryFragment.adapter
            
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
        viewModel.allScans.observe(viewLifecycleOwner) { scans ->
            adapter.submitList(scans)

            // Показываем пустое состояние или список
            if (scans.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
            } else {
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
                viewModel.clearHistory()
                
                // Анимируем переход к пустому состоянию
                val fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
                fadeOut.duration = 300
                binding.recyclerViewHistory.startAnimation(fadeOut)

                CustomToast.showToast(requireContext(), "История очищена", type = ToastType.SUCCESS)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ScanHistoryFragment"
    }
}