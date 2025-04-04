package com.example.visualsearch.model

data class FilterOptions(
    var priceFrom: Int? = null,
    var priceTo: Int? = null,
    var brand: String? = null,
    var sortType: SortType = SortType.POPULARITY,
    var deliveryToday: Boolean = false,
    var discount: Boolean = false
)

enum class SortType {
    POPULARITY,
    PRICE_ASC,
    PRICE_DESC,
    RATING
}

enum class MarketplaceType {
    WILDBERRIES,
    OZON,
    LALAFO
}