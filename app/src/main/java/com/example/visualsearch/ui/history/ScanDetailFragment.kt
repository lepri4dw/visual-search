package com.example.visualsearch.ui.history

import android.content.Intent
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
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentScanDetailBinding
import com.example.visualsearch.remote.gemini.SearchQuery
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.visualsearch.data.entity.ScanHistoryEntity
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanDetailFragment : Fragment() {

    private var _binding: FragmentScanDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScanHistoryViewModel
    private val args: ScanDetailFragmentArgs by navArgs()
    // LiveData для выбранного сканирования
    private val _selectedScan = MutableLiveData<ScanHistoryEntity?>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Используем обновленную фабрику, которая сама создает FirestoreScanRepository
        val factory = ScanHistoryViewModelFactory(requireActivity().application)
        
        // Получаем ViewModel с использованием фабрики
        viewModel = ViewModelProvider(this, factory).get(ScanHistoryViewModel::class.java)
        
        // Загружаем данные о сканировании
        CoroutineScope(Dispatchers.Main).launch {
            val scan = withContext(Dispatchers.IO) {
                viewModel.getScanById(args.scanId)
            }
            _selectedScan.value = scan
        }

        setupButtons()
        setupAnimations()

        // Наблюдаем за изменениями в данных сканирования
        _selectedScan.observe(viewLifecycleOwner) { scan ->
            if (scan != null) {
                // Загружаем изображение в зависимости от источника (URL или локальный файл)
                if (scan.imagePath.startsWith("https://")) {
                    // Загружаем из Firebase Storage
                    Glide.with(this)
                        .load(scan.imagePath)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(binding.imageViewDetail)
                } else {
                    // Загружаем локальный файл
                    val imageFile = File(scan.imagePath)
                    if (imageFile.exists()) {
                        Glide.with(this)
                            .load(imageFile)
                            .centerCrop()
                            .into(binding.imageViewDetail)
                    } else {
                        // Если файл не существует, показываем заглушку
                        Glide.with(this)
                            .load(R.drawable.ic_image_placeholder)
                            .centerCrop()
                            .into(binding.imageViewDetail)
                    }
                }

                binding.textViewDetailProductType.text = scan.productType
                binding.textViewDetailQuery.text = scan.query
                binding.textViewDetailBrand.text = scan.brand.ifEmpty { "Не определен" }
                binding.textViewDetailModel.text = scan.modelName.ifEmpty { "Не определена" }
                binding.textViewDetailColor.text = scan.color.ifEmpty { "Не определен" }

                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(scan.getDateAsJavaDate())
                binding.textViewDetailDate.text = formattedDate
            }
        }
    }

    private fun setupButtons() {
        // Кнопка просмотра в маркетплейсах
        binding.buttonRescan.setOnClickListener {
            navigateToHomeWithData()
        }

        // Кнопка поделиться
        binding.buttonShare.setOnClickListener {
            shareItemInfo()
        }

        // Кнопка удаления
        binding.buttonDeleteScan.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // Кнопка возврата назад
        binding.fabBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAnimations() {
        // Анимации для карточек при появлении
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_from_bottom)
        slideUp.startOffset = 200
        binding.cardViewDetails.startAnimation(slideUp)
        
        val slideUpLater = AnimationUtils.loadAnimation(requireContext(), R.anim.item_animation_from_bottom)
        slideUpLater.startOffset = 400
        binding.actionsCard.startAnimation(slideUpLater)
    }

    private fun navigateToHomeWithData() {
        val scan = _selectedScan.value ?: return

        findNavController().navigate(
            ScanDetailFragmentDirections.actionScanDetailToNavigationHome(
                query = scan.query,
                productType = scan.productType,
                brand = scan.brand,
                model = scan.modelName,
                color = scan.color,
                imagePath = scan.imagePath
            )
        )
    }

    private fun shareItemInfo() {
        val scan = _selectedScan.value ?: return
        
        try {
            val shareText = "Найден товар: ${scan.productType}\n" +
                    "Поисковый запрос: ${scan.query}\n" +
                    "Бренд: ${scan.brand}\n" +
                    "Модель: ${scan.modelName}\n" +
                    "Цвет: ${scan.color}\n\n" +
                    "Найдено с помощью приложения Visual Search"

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }

            startActivity(Intent.createChooser(shareIntent, "Поделиться информацией о товаре"))
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Не удалось поделиться: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить из истории")
            .setMessage("Вы уверены, что хотите удалить этот товар из истории поиска?")
            .setPositiveButton("Удалить") { _, _ ->
                _selectedScan.value?.let { scan ->
                    viewModel.deleteScan(scan)
                    findNavController().navigateUp()
                    
                    // Показываем уведомление об успешном удалении
                    Snackbar.make(requireActivity().findViewById(android.R.id.content), 
                        "Товар удален из истории", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .setBackground(resources.getDrawable(R.drawable.dialog_background_rounded, null))
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ScanDetailFragment"
    }
}