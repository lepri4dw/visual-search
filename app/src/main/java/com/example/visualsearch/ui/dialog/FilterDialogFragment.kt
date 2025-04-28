package com.example.visualsearch.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RadioButton
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.visualsearch.R
import com.example.visualsearch.databinding.DialogMarketplaceFiltersBinding
import com.example.visualsearch.model.FilterOptions
import com.example.visualsearch.model.MarketplaceType
import com.example.visualsearch.model.SortType
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class FilterDialogFragment : BottomSheetDialogFragment() {
    
    private var _binding: DialogMarketplaceFiltersBinding? = null
    private val binding get() = _binding!!
    
    private var filterOptions = FilterOptions()
    private var marketplaceType = MarketplaceType.WILDBERRIES
    private var listener: FilterDialogListener? = null
    private var applyToAll: Boolean = false
    
    // Форматтер для отображения цены
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("ru", "RU")).apply {
        currency = Currency.getInstance("RUB")
        maximumFractionDigits = 0
    }
    
    interface FilterDialogListener {
        fun onFilterOptionsSelected(marketplaceType: MarketplaceType, filterOptions: FilterOptions, applyToAll: Boolean)
    }
    
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMarketplaceFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Установка полноэкранного режима
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        
        // Получаем аргументы
        arguments?.let {
            marketplaceType = when (it.getInt("marketplace_type")) {
                0 -> MarketplaceType.WILDBERRIES
                1 -> MarketplaceType.OZON
                2 -> MarketplaceType.LALAFO
                3 -> MarketplaceType.ALI_EXPRESS
                4 -> MarketplaceType.BAZAR
                else -> MarketplaceType.WILDBERRIES
            }
            
            // Установка подсказки в заголовке в зависимости от маркетплейса
            val marketplaceName = when (marketplaceType) {
                MarketplaceType.WILDBERRIES -> "Wildberries"
                MarketplaceType.OZON -> "Ozon"
                MarketplaceType.LALAFO -> "Lalafo"
                MarketplaceType.ALI_EXPRESS -> "AliExpress"
                MarketplaceType.BAZAR -> "Bazar"
            }
            binding.tvDialogTitle.text = "Параметры поиска в $marketplaceName"
            
            // Настраиваем интерфейс в зависимости от маркетплейса
            setupMarketplaceSpecificUI()
        }
        
        // Инициализация полей цен
        setupPriceRangeSlider()
        
        // Бренд и цвет определяются автоматически без возможности ручного ввода
        val brand = arguments?.getString("brand")
        if (!brand.isNullOrEmpty()) {
            filterOptions.brand = brand
        }
        
        val color = arguments?.getString("color")
        if (!color.isNullOrEmpty()) {
            filterOptions.color = color
        }
        
        setupListeners()
    }
    
    private fun setupPriceRangeSlider() {
        // Настройка полей ввода для диапазона цен
        binding.etMinPrice.setText(FilterOptions.MIN_PRICE.toString())
        binding.etMaxPrice.setText(FilterOptions.MAX_PRICE.toString())
        
        // Сразу устанавливаем значения по умолчанию в фильтр
        filterOptions.priceFrom = FilterOptions.MIN_PRICE
        filterOptions.priceTo = FilterOptions.MAX_PRICE
        
        // Обработчик изменения значений минимальной цены
        binding.etMinPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    val minPrice = if (s.isNullOrEmpty()) FilterOptions.MIN_PRICE else s.toString().toInt()
                    filterOptions.priceFrom = minPrice
                } catch (e: NumberFormatException) {
                    // Если введено некорректное значение, установим значение по умолчанию
                    binding.etMinPrice.setText(FilterOptions.MIN_PRICE.toString())
                    filterOptions.priceFrom = FilterOptions.MIN_PRICE
                }
            }
        })
        
        // Обработчик изменения значений максимальной цены
        binding.etMaxPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    // Если поле пустое, всегда устанавливаем 500 000
                    val maxPrice = if (s.isNullOrEmpty()) FilterOptions.MAX_PRICE else s.toString().toInt()
                    filterOptions.priceTo = maxPrice
                } catch (e: NumberFormatException) {
                    // Если введено некорректное значение, установим значение по умолчанию
                    binding.etMaxPrice.setText(FilterOptions.MAX_PRICE.toString())
                    filterOptions.priceTo = FilterOptions.MAX_PRICE
                }
            }
        })
    }
    
    private fun setupMarketplaceSpecificUI() {
        when (marketplaceType) {
            MarketplaceType.WILDBERRIES -> {
                // Настройки для Wildberries
                binding.rbPopularity.text = "По популярности"
                binding.rbPriceAsc.text = "По возрастанию цены"
                binding.rbPriceDesc.text = "По убыванию цены"
                binding.rbRating.text = "По рейтингу"
                binding.cbDiscount.text = "Товары со скидкой"
            }
            MarketplaceType.OZON -> {
                // Настройки для Ozon
                binding.rbPopularity.text = "По популярности"
                binding.rbPriceAsc.text = "По возрастанию цены"
                binding.rbPriceDesc.text = "По убыванию цены"
                binding.rbRating.text = "По рейтингу"
                binding.cbDiscount.text = "Товары со скидкой"
            }
            MarketplaceType.LALAFO -> {
                // Настройки для Lalafo
                binding.rbPopularity.text = "По новизне"
                binding.rbPriceAsc.text = "Сначала дешевле"
                binding.rbPriceDesc.text = "Сначала дороже"
                binding.rbRating.text = "По близости"
                binding.cbDiscount.visibility = View.GONE
            }
            MarketplaceType.ALI_EXPRESS -> {
                // Настройки для AliExpress
                binding.rbPopularity.text = "По заказам"
                binding.rbPriceAsc.text = "По возрастанию цены"
                binding.rbPriceDesc.text = "По убыванию цены"
                binding.rbRating.text = "По отзывам"
                binding.cbDiscount.text = "Распродажа"
            }
            MarketplaceType.BAZAR -> {
                // Настройки для Bazar
                binding.rbPopularity.text = "По дате"
                binding.rbPriceAsc.text = "Сначала дешевле"
                binding.rbPriceDesc.text = "Сначала дороже"
                binding.rbRating.text = "По дате"
                binding.cbDiscount.visibility = View.GONE
            }
        }
    }
    
    private fun setupListeners() {
        // Обработчик переключателей сортировки
        binding.rgSort.setOnCheckedChangeListener { _, checkedId ->
            filterOptions.sortType = when (checkedId) {
                R.id.rbPopularity -> SortType.POPULARITY
                R.id.rbPriceAsc -> SortType.PRICE_ASC
                R.id.rbPriceDesc -> SortType.PRICE_DESC
                R.id.rbRating -> SortType.RATING
                else -> SortType.POPULARITY
            }
        }
        
        binding.cbDiscount.setOnCheckedChangeListener { _, isChecked ->
            filterOptions.discount = isChecked
        }
        
        // Обработчик чекбокса "Применить ко всем"
        binding.cbApplyToAll.setOnCheckedChangeListener { _, isChecked ->
            applyToAll = isChecked
        }
        
        // Кнопка "Готово" (закрывает диалог)
        binding.btnDone.setOnClickListener {
            if (applyToAll) {
                listener?.onFilterOptionsSelected(marketplaceType, filterOptions, true)
            }
            dismiss()
        }
        
        // Кнопки действий
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnApply.setOnClickListener {
            listener?.onFilterOptionsSelected(marketplaceType, filterOptions, applyToAll)
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    fun setFilterDialogListener(listener: FilterDialogListener) {
        this.listener = listener
    }
    
    companion object {
        fun newInstance(marketplaceType: MarketplaceType, brand: String?): FilterDialogFragment {
            val fragment = FilterDialogFragment()
            val args = Bundle()
            args.putInt("marketplace_type", marketplaceType.ordinal)
            args.putString("brand", brand)
            fragment.arguments = args
            return fragment
        }
    }
}