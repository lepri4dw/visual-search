package com.example.visualsearch.ui.dialog

import android.app.Dialog
import android.os.Bundle
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

class FilterDialogFragment : DialogFragment() {
    
    private var _binding: DialogMarketplaceFiltersBinding? = null
    private val binding get() = _binding!!
    
    private var filterOptions = FilterOptions()
    private var marketplaceType = MarketplaceType.WILDBERRIES
    private var listener: FilterDialogListener? = null
    
    interface FilterDialogListener {
        fun onFilterOptionsSelected(marketplaceType: MarketplaceType, filterOptions: FilterOptions)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
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
                else -> MarketplaceType.WILDBERRIES
            }
            
            // Установка подсказки в заголовке в зависимости от маркетплейса
            val marketplaceName = when (marketplaceType) {
                MarketplaceType.WILDBERRIES -> "Wildberries"
                MarketplaceType.OZON -> "Ozon"
                MarketplaceType.LALAFO -> "Lalafo"
            }
            binding.tvDialogTitle.text = "Параметры поиска в $marketplaceName"
            
            // Настраиваем интерфейс в зависимости от маркетплейса
            setupMarketplaceSpecificUI()
        }
        
        // Предзаполняем поле бренда, если он был определен при анализе
        val brand = arguments?.getString("brand")
        if (!brand.isNullOrEmpty()) {
            binding.etBrand.setText(brand)
            filterOptions.brand = brand
        }
        
        setupListeners()
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
        }
    }
    
    private fun setupListeners() {
        // Обработчики текстовых полей
        binding.etPriceFrom.doAfterTextChanged { text ->
            filterOptions.priceFrom = text.toString().toIntOrNull()
        }
        
        binding.etPriceTo.doAfterTextChanged { text ->
            filterOptions.priceTo = text.toString().toIntOrNull()
        }
        
        binding.etBrand.doAfterTextChanged { text ->
            filterOptions.brand = text.toString().takeIf { it.isNotEmpty() }
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
        
        // Кнопки
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnApply.setOnClickListener {
            listener?.onFilterOptionsSelected(marketplaceType, filterOptions)
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