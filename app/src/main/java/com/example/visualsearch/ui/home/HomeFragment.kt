package com.example.visualsearch.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.google.android.material.chip.Chip
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import com.example.visualsearch.ui.history.ScanHistoryViewModel
import androidx.navigation.fragment.navArgs
import androidx.room.util.query

class HomeFragment : Fragment() {
    private lateinit var historyViewModel: ScanHistoryViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val args: HomeFragmentArgs by navArgs()

    private lateinit var geminiApiClient: GeminiApiClient
    private var isProcessing = false
    private var currentSearchQuery: SearchQuery? = null
    private var alternativeSearchQuery: SearchQuery? = null

    // Список недавних поисковых запросов
    private val recentSearches = mutableListOf<String>()
    private val maxRecentSearches = 5

    // Адаптер для маркетплейсов
    private lateinit var marketplaceAdapter: MarketplaceAdapter

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
// Initialize ScanHistoryViewModel
        historyViewModel = ViewModelProvider(this).get(ScanHistoryViewModel::class.java)
        // Инициализируем клиент Gemini API
        geminiApiClient = GeminiApiClient(getString(R.string.gemini_api_key))

        // Загружаем сохраненные поисковые запросы
        loadRecentSearches()

        // Настраиваем поисковую строку
        setupSearchBar()

        // Настраиваем RecyclerView для маркетплейсов
        setupRecyclerView()

        // Настраиваем кнопки
        setupButtons()
        setupButtonAnimations()

        // Добавляем обработчик для кнопки закрытия результатов
        binding.btnCloseResults.setOnClickListener {
            binding.resultsContainer.visibility = View.GONE
            binding.tvPlaceholder.visibility = View.VISIBLE
        }

        // Показываем приветственное сообщение
        showWelcomeMessage()

        // Process arguments if they exist (when coming from scan detail)
        handleNavigationArguments()
    }

    private fun handleNavigationArguments() {
        // Check if we have navigation arguments (from ScanDetailFragment)
        if (args.query != null && args.imagePath != null) {
            try {
                // Create SearchQuery object from arguments
                val searchQuery = SearchQuery(
                    query = args.query ?: "",
                    productType = args.productType ?: "",
                    brand = args.brand ?: "",
                    modelName = args.model ?: "",
                    color = args.color ?: ""
                )

                // Load and display the image
                val bitmap = BitmapFactory.decodeFile(args.imagePath)
                displayImage(bitmap)

                // Display the search results directly
                displayResult(searchQuery)

                // Update currentSearchQuery for marketplace searches
                currentSearchQuery = searchQuery

                Log.d(TAG, "Successfully processed navigation arguments")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing navigation arguments", e)
                Toast.makeText(requireContext(), "Error loading previous scan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showWelcomeMessage() {
        // Проверяем, первый ли это запуск приложения
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("first_run", true)

        if (isFirstRun) {
            // Показываем приветственное сообщение
            Toast.makeText(
                requireContext(),
                "Добро пожаловать! Введите текст для поиска или выберите изображение товара",
                Toast.LENGTH_LONG
            ).show()

            // Отмечаем, что приложение уже запускалось
            sharedPreferences.edit().putBoolean("first_run", false).apply()
        }
    }

    private fun setupSearchBar() {
        // Настраиваем действие поиска по нажатию кнопки Enter на клавиатуре
        binding.etSearch.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = textView.text.toString().trim()
                if (query.isNotEmpty()) {
                    resetImageView()
                    performSearch(query)
                    hideKeyboard()
                    return@setOnEditorActionListener true
                }
            }
            false
        }

        // Настраиваем кнопку поиска
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                resetImageView()
                performSearch(query)
                hideKeyboard()
            } else {
                Toast.makeText(requireContext(), "Введите текст для поиска", Toast.LENGTH_SHORT).show()
            }
        }

        // Настраиваем чипы с недавними поисками
        updateRecentSearchesChips()

        // Добавляем обработчик изменения текста
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Показываем или скрываем кнопку очистки поля
                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE

                // Показываем чипы с недавними запросами, если поле пустое
                binding.chipGroupRecentSearches.visibility = if (s.isNullOrEmpty() && recentSearches.isNotEmpty())
                    View.VISIBLE else View.GONE
            }
        })

        // Добавляем обработчик для кнопки очистки поля
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            binding.btnClearSearch.visibility = View.GONE
            // Проверяем, пустое ли поле поиска ПОСЛЕ очистки
            binding.chipGroupRecentSearches.visibility = if (binding.etSearch.text.isEmpty() && recentSearches.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun resetImageView() {
        binding.tvPlaceholder.visibility = View.VISIBLE
        binding.imageView.setImageDrawable(null) // Очищаем изображение
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun performSearch(query: String) {
        // Создаем объект SearchQuery с текстовым запросом
        val productType = extractProductType(query)

        val searchQuery = SearchQuery(
            query = query,
            productType = productType,
            modelName = "",
            brand = "",
            color = ""
        )

        // Сохраняем поисковый запрос и обновляем чипы
        addRecentSearch(query)

        // Отображаем результаты поиска
        currentSearchQuery = searchQuery

        // Также устанавливаем альтернативный поисковый запрос (для кнопки "Найти похожие")
        // В данном случае это может быть общая категория товара
        val category = getGeneralCategory(productType)
        alternativeSearchQuery = SearchQuery(
            query = category,
            productType = category,
            modelName = "",
            brand = "",
            color = ""
        )

        // Показываем результаты поиска
        displayTextSearchResult(searchQuery)
    }

    private fun extractProductType(query: String): String {
        // Простая логика извлечения типа товара из запроса
        // В реальном приложении здесь могла бы быть более сложная логика или вызов API
        val words = query.split(" ")
        return if (words.size > 1) words[0] else query
    }

    private fun getGeneralCategory(productType: String): String {
        // Простая логика определения общей категории на основе типа товара
        // В реальном приложении здесь могла бы быть база данных категорий или вызов API
        val categories = mapOf(
            "телефон" to "смартфоны",
            "смартфон" to "смартфоны",
            "iphone" to "смартфоны apple",
            "наушники" to "аудиотехника",
            "колонка" to "аудиотехника",
            "часы" to "умные часы и браслеты",
            "кроссовки" to "спортивная обувь",
            "кубик" to "головоломки"
        )

        // Ищем соответствие по ключевым словам
        for ((key, value) in categories) {
            if (productType.lowercase().contains(key.lowercase())) {
                return value
            }
        }

        // Если соответствие не найдено, возвращаем исходный тип
        return productType
    }

    private fun displayTextSearchResult(searchQuery: SearchQuery) {
        // Показываем весь контейнер результатов
        binding.resultsContainer.visibility = View.VISIBLE
        binding.tvPlaceholder.visibility = View.GONE

        // Показываем поисковый запрос в результатах
        binding.tvGarbageType.text = "Поисковый запрос:"
        binding.tvInstructions.text = searchQuery.query

        // Показываем дополнительную информацию
        binding.tvEstimatedCost.visibility = View.VISIBLE
        binding.tvEstimatedCost.text = "Найдено по запросу: \"${searchQuery.query}\""

        // Показываем кнопки действий и маркетплейсы
        showActionButtonsAndMarketplaces(searchQuery)

        // Анимация появления результатов
        val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.resultsContainer.startAnimation(slideUpAnimation)
    }

    private fun addRecentSearch(query: String) {
        // Удаляем запрос, если он уже есть в списке (чтобы добавить его в начало)
        recentSearches.remove(query)

        // Добавляем запрос в начало списка
        recentSearches.add(0, query)

        // Оставляем только последние maxRecentSearches запросов
        if (recentSearches.size > maxRecentSearches) {
            recentSearches.removeAt(recentSearches.size - 1)
        }

        // Сохраняем список запросов
        saveRecentSearches()

        // Обновляем чипы с недавними поисками
        updateRecentSearchesChips()
    }

    private fun saveRecentSearches() {
        val sharedPreferences = requireContext().getSharedPreferences("SearchPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Сохраняем список запросов как строку, разделенную запятыми
        editor.putString("recent_searches", recentSearches.joinToString(","))
        editor.apply()
    }

    private fun loadRecentSearches() {
        val sharedPreferences = requireContext().getSharedPreferences("SearchPreferences", Context.MODE_PRIVATE)
        val savedSearches = sharedPreferences.getString("recent_searches", "")

        // Если есть сохраненные запросы, добавляем их в список
        if (!savedSearches.isNullOrEmpty()) {
            recentSearches.clear()
            recentSearches.addAll(savedSearches.split(","))
        }
    }

    private fun updateRecentSearchesChips() {
        binding.chipGroupRecentSearches.removeAllViews()

        // Добавляем чипы для каждого недавнего поиска
        for (search in recentSearches) {
            val chip = Chip(requireContext())
            chip.text = search
            chip.isClickable = true
            chip.isCheckable = false

            // Устанавливаем стиль чипа
            chip.chipBackgroundColor = resources.getColorStateList(R.color.chip_background, null)
            chip.setTextColor(resources.getColorStateList(R.color.text_primary, null))

            // Добавляем обработчик нажатия
            chip.setOnClickListener {
                binding.etSearch.setText(search)
                performSearch(search)
            }

            binding.chipGroupRecentSearches.addView(chip)
        }

        // Показываем или скрываем контейнер с чипами в зависимости от наличия недавних запросов
        binding.chipGroupRecentSearches.visibility = if (recentSearches.isEmpty() ||
            binding.etSearch.text.isNotEmpty()) View.GONE else View.VISIBLE
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

        binding.btnSearch.setOnTouchListener { v, event ->
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
            override fun onSuccess(searchQueryResult: GeminiApiClient.SearchQueryResult) {
                if (activity == null) return

                activity?.runOnUiThread {
                    isProcessing = false
                    showLoading(false)

                    // Use searchQueryResult.mainQuery instead of just searchQuery
                    displayResult(searchQueryResult.mainQuery)

                    // Save to history using mainQuery
                    historyViewModel.saveScanWithBitmap(searchQueryResult.mainQuery, bitmap)

                    // Save both query variants
                    currentSearchQuery = searchQueryResult.mainQuery
                    alternativeSearchQuery = searchQueryResult.alternativeQuery

                    // Show main result
                    displayResult(searchQueryResult.mainQuery)

                    // Fill search field with main query
                    binding.etSearch.setText(searchQueryResult.mainQuery.query)

                    // Save query to history
                    addRecentSearch(searchQueryResult.mainQuery.query)
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
        binding.btnSearch.isEnabled = !isLoading
        binding.etSearch.isEnabled = !isLoading
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
        binding.tvPlaceholder.visibility = View.GONE

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

    private fun findSimilarProducts() {
        // Проверяем, есть ли альтернативный запрос
        val altQuery = alternativeSearchQuery
        if (altQuery != null) {
            // Обновляем текущий запрос на альтернативный
            currentSearchQuery = altQuery
            binding.etSearch.setText(altQuery.query)

            // Показываем результаты поиска для альтернативного запроса
            displayTextSearchResult(altQuery)

            // Уведомляем пользователя
            Toast.makeText(
                requireContext(),
                "Поиск похожих товаров: ${altQuery.query}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Если альтернативный запрос не найден, генерируем его
            val currentQuery = currentSearchQuery
            if (currentQuery != null) {
                val generalCategory = getGeneralCategory(currentQuery.productType)
                val altSearchQuery = SearchQuery(
                    query = generalCategory,
                    productType = generalCategory,
                    modelName = "",
                    brand = "",
                    color = ""
                )

                // Обновляем текущий запрос
                currentSearchQuery = altSearchQuery
                alternativeSearchQuery = altSearchQuery

                // Обновляем текст в поле поиска
                binding.etSearch.setText(generalCategory)

                // Показываем результаты поиска для нового запроса
                displayTextSearchResult(altSearchQuery)

                // Добавляем запрос в историю
                addRecentSearch(generalCategory)

                // Уведомляем пользователя
                Toast.makeText(
                    requireContext(),
                    "Поиск похожих товаров в категории: $generalCategory",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Если текущий запрос отсутствует, сообщаем пользователю
                Toast.makeText(
                    requireContext(),
                    "Сначала выполните поиск товара",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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