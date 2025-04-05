package com.example.visualsearch.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RadioButton
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.example.visualsearch.R
import com.example.visualsearch.databinding.DialogMarketplaceFiltersBinding
import com.example.visualsearch.model.FilterOptions
import com.example.visualsearch.model.MarketplaceType
import com.example.visualsearch.model.SortType
import com.google.android.material.slider.RangeSlider
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class FilterDialogFragment : DialogFragment() {
    
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
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        return dialog
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
        
        // Инициализация слайдера цен
        setupPriceRangeSlider()
        
        // Предзаполняем поле бренда, если он был определен при анализе
        val brand = arguments?.getString("brand")
        if (!brand.isNullOrEmpty()) {
            binding.etBrand.setText(brand)
            filterOptions.brand = brand
        }
        
        // Предзаполняем поле цвета, если он был определен при анализе
        val color = arguments?.getString("color")
        if (!color.isNullOrEmpty()) {
            binding.etColor.setText(color)
            filterOptions.color = color
        }
        
        setupListeners()
    }
    
    private fun setupPriceRangeSlider() {
        // Настройка слайдера диапазона цен
        with(binding.priceRangeSlider) {
            valueFrom = FilterOptions.MIN_PRICE.toFloat()
            valueTo = FilterOptions.MAX_PRICE.toFloat()
            stepSize = FilterOptions.PRICE_STEP.toFloat()
            values = listOf(FilterOptions.MIN_PRICE.toFloat(), FilterOptions.MAX_PRICE.toFloat())
            
            // Обработчик изменения значений
            addOnChangeListener { slider, _, _ ->
                val values = slider.values
                updatePriceLabels(values[0].toInt(), values[1].toInt())
                
                // Обновляем фильтры
                filterOptions.priceFrom = values[0].toInt()
                filterOptions.priceTo = values[1].toInt()
            }
        }
        
        // Инициализация текстовых меток с начальными значениями
        updatePriceLabels(FilterOptions.MIN_PRICE, FilterOptions.MAX_PRICE)
    }
    
    private fun updatePriceLabels(minPrice: Int, maxPrice: Int) {
        binding.tvMinPrice.text = currencyFormatter.format(minPrice)
        binding.tvMaxPrice.text = currencyFormatter.format(maxPrice)
    }
    
    private fun setupMarketplaceSpecificUI() {
        when (marketplaceType) {
            MarketplaceType.WILDBERRIES -> {
                // Настройки для Wildberries
                binding.etBrand.hint = "Фильтр по бренду"
                binding.rbPopularity.text = "По популярности"
                binding.rbPriceAsc.text = "По возрастанию цены"
                binding.rbPriceDesc.text = "По убыванию цены"
                binding.rbRating.text = "По рейтингу"
                binding.cbDeliveryToday.text = "Быстрая доставка"
                binding.cbDiscount.text = "Товары со скидкой"
            }
            MarketplaceType.OZON -> {
                // Настройки для Ozon
                binding.etBrand.hint = "Фильтр по бренду"
                binding.rbPopularity.text = "По популярности"
                binding.rbPriceAsc.text = "По возрастанию цены"
                binding.rbPriceDesc.text = "По убыванию цены"
                binding.rbRating.text = "По рейтингу"
                binding.cbDeliveryToday.text = "Экспресс-доставка"
                binding.cbDiscount.text = "Товары со скидкой"
            }
            MarketplaceType.LALAFO -> {
                // Настройки для Lalafo
                binding.etBrand.visibility = View.GONE
                binding.rbPopularity.text = "По новизне"
                binding.rbPriceAsc.text = "Сначала дешевле"
                binding.rbPriceDesc.text = "Сначала дороже"
                binding.rbRating.text = "По близости"
                binding.cbDeliveryToday.visibility = View.GONE
                binding.cbDiscount.visibility = View.GONE
            }
            MarketplaceType.ALI_EXPRESS -> {
                // Настройки для AliExpress
                binding.etBrand.hint = "Фильтр по бренду"
                binding.rbPopularity.text = "По заказам"
                binding.rbPriceAsc.text = "По возрастанию цены"
                binding.rbPriceDesc.text = "По убыванию цены"
                binding.rbRating.text = "По отзывам"
                binding.cbDeliveryToday.text = "Бесплатная доставка"
                binding.cbDiscount.text = "Распродажа"
            }
            MarketplaceType.BAZAR -> {
                // Настройки для Bazar
                binding.etBrand.visibility = View.GONE
                binding.rbPopularity.text = "По дате"
                binding.rbPriceAsc.text = "Сначала дешевле"
                binding.rbPriceDesc.text = "Сначала дороже"
                binding.rbRating.text = "По дате"
                binding.cbDeliveryToday.visibility = View.GONE
                binding.cbDiscount.visibility = View.GONE
            }
        }
    }
    
    private fun setupListeners() {
        // Обработчики текстовых полей
        binding.etBrand.doAfterTextChanged { text ->
            filterOptions.brand = text.toString().takeIf { it.isNotEmpty() }
        }
        
        binding.etColor.doAfterTextChanged { text ->
            filterOptions.color = text.toString().takeIf { it.isNotEmpty() }
        }
        
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
        
        // Обработчики чекбоксов
        binding.cbDeliveryToday.setOnCheckedChangeListener { _, isChecked ->
            filterOptions.deliveryToday = isChecked
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
            if (applyToAll) {
                dismiss()
            }
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