package com.example.visualsearch.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilterOptions(
    var priceFrom: Int? = null,
    var priceTo: Int? = null,
    var brand: String? = null,
    var sortType: SortType = SortType.POPULARITY,
    var deliveryToday: Boolean = false,
    var discount: Boolean = false,
    var categories: List<String> = emptyList(),
    var color: String? = null
) : Parcelable {
    companion object {
        // Минимальные и максимальные значения цен для слайдера
        const val MIN_PRICE = 0
        const val MAX_PRICE = 500000 // Всегда используем 500 000 как максимальную цену
        
        // Шаг цены
        const val PRICE_STEP = 1000
    }
}

enum class SortType {
    POPULARITY,
    PRICE_ASC,
    PRICE_DESC,
    RATING
}

enum class MarketplaceType {
    WILDBERRIES,
    OZON,
    LALAFO,
    ALI_EXPRESS,
    BAZAR
}