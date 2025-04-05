package com.example.visualsearch.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.visualsearch.CameraActivity
import com.example.visualsearch.R
import com.example.visualsearch.databinding.FragmentHomeBinding
import com.example.visualsearch.model.FilterOptions
import com.example.visualsearch.model.MarketplaceType
import com.example.visualsearch.util.MarketplaceUrlBuilder
import com.example.visualsearch.remote.gemini.GeminiApiClient
import com.example.visualsearch.remote.gemini.SearchQuery
import com.example.visualsearch.ui.dialog.FilterDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visualsearch.ui.adapter.MarketplaceAdapter
import com.example.visualsearch.util.MarketplaceAppChecker
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var geminiApiClient: GeminiApiClient
    private var isProcessing = false
    private var currentSearchQuery: SearchQuery? = null

    // Лаунчер для выбора изображения из галереи
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                    displayImage(bitmap)
                    processImage(bitmap)
                } catch (e: IOException) {
                    Toast.makeText(requireContext(), "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Ошибка загрузки изображения из галереи", e)
                }
            }
        }
    }

    // Лаунчер для получения изображения с камеры
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val imagePath = result.data?.getStringExtra("image_path")
            if (imagePath != null) {
                try {
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    displayImage(bitmap)
                    processImage(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка загрузки изображения: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Ошибка обработки результата с камеры", e)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализируем клиент Gemini API
        geminiApiClient = GeminiApiClient(getString(R.string.gemini_api_key))

        // Настраиваем RecyclerView для маркетплейсов
        setupRecyclerView()

        // Настраиваем кнопки
        setupButtons()
        setupButtonAnimations()
        
        // Добавляем обработчик для кнопки закрытия результатов
        binding.btnCloseResults.setOnClickListener {
            binding.resultsContainer.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewMarketplaces.layoutManager = LinearLayoutManager(context)
    }

    private fun setupButtons() {
        // Кнопка выбора изображения из галереи
        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // Кнопка открытия камеры
        binding.btnScanWithCamera.setOnClickListener {
            openCameraScanner()
        }

        // Кнопка фильтров
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // Кнопка "Найти похожие"
        binding.btnFindSimilar.setOnClickListener {
            findSimilarProducts()
        }

        // Кнопка "Поделиться"
        binding.btnShare.setOnClickListener {
            shareResults()
        }
    }

    private fun setupButtonAnimations() {
        // Загружаем анимацию пульсации
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_scale)

        // Добавляем слушатели касания для анимации кнопок
        binding.btnSelectImage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation)
            }
            false
        }

        binding.btnScanWithCamera.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.startAnimation(pulseAnimation)
            }
            false
        }
    }

    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        Dexter.withContext(requireContext())
            .withPermission(permission)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    openImagePicker()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    Toast.makeText(requireContext(), "Необходимо разрешение для выбора изображения", Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun openCameraScanner() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun displayImage(bitmap: Bitmap) {
        binding.tvPlaceholder.visibility = View.GONE
        Glide.with(this).load(bitmap).into(binding.imageView)
    }

    private fun processImage(bitmap: Bitmap) {
        if (isProcessing) return

        isProcessing = true
        showLoading(true)
        hideResult()

        geminiApiClient.analyzeImage(bitmap, object : GeminiApiClient.GeminiApiListener {
            override fun onSuccess(searchQuery: SearchQuery) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    displayResult(searchQuery)
                }
            }

            override fun onError(e: Exception) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)
                    Toast.makeText(requireContext(), "Ошибка анализа изображения: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Ошибка анализа изображения", e)
                }
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        val fadeAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_out)

        if (isLoading) {
            binding.progressBar.startAnimation(fadeAnimation)
            binding.tvLoading.startAnimation(fadeAnimation)
            binding.tvPlaceholder.visibility = View.GONE
        }

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSelectImage.isEnabled = !isLoading
        binding.btnScanWithCamera.isEnabled = !isLoading
    }

    private fun hideResult() {
        binding.resultsContainer.visibility = View.GONE
    }

    private fun displayResult(searchQuery: SearchQuery) {
        val resultBuilder = StringBuilder()

        resultBuilder.append("Поисковый запрос: ").append(searchQuery.query).append("\n\n")

        if (searchQuery.productType.isNotEmpty()) {
            resultBuilder.append("Тип товара: ").append(searchQuery.productType).append("\n\n")
        }

        if (searchQuery.brand.isNotEmpty()) {
            resultBuilder.append("Бренд: ").append(searchQuery.brand).append("\n\n")
        }

        if (searchQuery.modelName.isNotEmpty()) {
            resultBuilder.append("Модель: ").append(searchQuery.modelName).append("\n\n")
        }

        if (searchQuery.color.isNotEmpty()) {
            resultBuilder.append("Цвет: ").append(searchQuery.color)
        }

        // Показываем весь контейнер результатов
        binding.resultsContainer.visibility = View.VISIBLE

        // Обновляем содержимое результата
        binding.tvGarbageType.text = "Анализ товара:"
        binding.tvInstructions.text = resultBuilder.toString().trim()

        // Показываем дополнительную информацию
        binding.tvEstimatedCost.visibility = View.VISIBLE
        binding.tvEstimatedCost.text = "Сформирован поисковый запрос: \"${searchQuery.query}\""

        // Сохраняем текущий поисковый запрос
        currentSearchQuery = searchQuery.copy()

        // Показываем кнопки действий и маркетплейсы
        showActionButtonsAndMarketplaces(searchQuery)

        // Анимация появления результатов
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.resultsContainer.startAnimation(slideUpAnimation)

        // Показываем информацию о результате в Toast
        val toastMessage = StringBuilder("Товар: ${searchQuery.productType}")
        if (searchQuery.brand.isNotEmpty()) {
            toastMessage.append(", Бренд: ${searchQuery.brand}")
        }
        Toast.makeText(requireContext(), toastMessage.toString(), Toast.LENGTH_LONG).show()
    }
    
    private fun setupMarketplaceButtons(query: String) {
        // Здесь ничего не делаем, так как теперь у нас RecyclerView вместо кнопок
        // Всё взаимодействие настраивается в setupMarketplacesList
    }
    
    private fun showFilterDialog(marketplaceType: MarketplaceType, query: String) {
        val dialogFragment = FilterDialogFragment.newInstance(marketplaceType, currentSearchQuery?.brand)
        dialogFragment.setFilterDialogListener(object : FilterDialogFragment.FilterDialogListener {
            override fun onFilterOptionsSelected(marketplaceType: MarketplaceType, filterOptions: FilterOptions, applyToAll: Boolean) {
                if (applyToAll) {
                    // Применяем фильтры ко всем маркетплейсам
                Toast.makeText(requireContext(), getString(R.string.filters_applied_all), Toast.LENGTH_SHORT).show()
                    // Обновляем адаптер с новыми фильтрами
                    val adapter = (binding.recyclerViewMarketplaces.adapter as? MarketplaceAdapter)
                    adapter?.updateFilters(filterOptions)
                } else {
                    // Строим URL с учетом фильтров и открываем его только для выбранного маркетплейса
                    val url = MarketplaceUrlBuilder.buildSearchUrl(marketplaceType, query, filterOptions)
                    openMarketplaceSearch(url)
                }
            }
        })
        dialogFragment.show(parentFragmentManager, "FilterDialog")
    }
    
    private fun openMarketplaceSearch(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Не удалось открыть браузер: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Ошибка при открытии URL", e)
        }
    }

    private fun showActionButtonsAndMarketplaces(searchQuery: SearchQuery) {
        // Все элементы уже видимы, так как находятся в общем контейнере
        // Нам нужно только настроить адаптер и содержимое
        setupMarketplacesList(searchQuery)
    }
    
    private fun setupMarketplacesList(searchQuery: SearchQuery) {
        val adapter = MarketplaceAdapter(requireContext(), searchQuery) { marketplaceType ->
            openMarketplace(marketplaceType, searchQuery)
        }
        binding.recyclerViewMarketplaces.adapter = adapter
    }
    
    private fun openMarketplace(marketplaceType: MarketplaceType, searchQuery: SearchQuery) {
        // Используем MarketplaceAppChecker для проверки приложений и создания Intent
        val marketplaceAppChecker = MarketplaceAppChecker(requireContext())
        val filterOptions = FilterOptions() // Используем настройки по умолчанию
        
        try {
            val intent = marketplaceAppChecker.getMarketplaceIntent(
                marketplaceType,
                searchQuery.query,
                filterOptions
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Не удалось открыть маркетплейс: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "Ошибка при открытии маркетплейса", e)
        }
    }
    
    private fun showFilterDialog() {
        // Показываем диалог фильтров для всех маркетплейсов
        val dialogFragment = FilterDialogFragment.newInstance(MarketplaceType.WILDBERRIES, currentSearchQuery?.brand)
        dialogFragment.setFilterDialogListener(object : FilterDialogFragment.FilterDialogListener {
            override fun onFilterOptionsSelected(marketplaceType: MarketplaceType, filterOptions: FilterOptions, applyToAll: Boolean) {
                // Применяем фильтры ко всем маркетплейсам
                val adapter = (binding.recyclerViewMarketplaces.adapter as? MarketplaceAdapter)
                adapter?.updateFilters(filterOptions)
                Toast.makeText(requireContext(), getString(R.string.filters_applied), Toast.LENGTH_SHORT).show()
            }
        })
        dialogFragment.show(parentFragmentManager, "FilterDialog")
    }
    
    private fun findSimilarProducts() {
        Toast.makeText(
            requireContext(),
            "Поиск похожих товаров",
            Toast.LENGTH_SHORT
        ).show()
        // Здесь будет логика поиска похожих товаров
    }
    
    private fun shareResults() {
        try {
            val searchQuery = currentSearchQuery ?: return
            val shareText = "Я нашел этот товар в приложении Visual Search: ${searchQuery.query}\n\n" +
                    "Тип товара: ${searchQuery.productType}\n" +
                    "Бренд: ${searchQuery.brand}\n" +
                    "Модель: ${searchQuery.modelName}"
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            
            startActivity(Intent.createChooser(shareIntent, "Поделиться результатами поиска"))
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Не удалось поделиться: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "Ошибка при отправке результатов", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}