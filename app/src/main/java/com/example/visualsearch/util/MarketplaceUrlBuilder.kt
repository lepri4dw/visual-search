package com.example.visualsearch.util

import android.net.Uri
import com.example.visualsearch.model.FilterOptions
import com.example.visualsearch.model.MarketplaceType
import com.example.visualsearch.model.SortType

/**
 * Утилитарный класс для формирования URL с параметрами фильтрации для различных маркетплейсов
 */
object MarketplaceUrlBuilder {

    /**
     * Формирует URL для поиска на маркетплейсе с учетом фильтров
     */
    fun buildSearchUrl(
        marketplaceType: MarketplaceType,
        query: String,
        filterOptions: FilterOptions
    ): String {
        return when (marketplaceType) {
            MarketplaceType.WILDBERRIES -> buildWildberriesUrl(query, filterOptions)
            MarketplaceType.OZON -> buildOzonUrl(query, filterOptions)
            MarketplaceType.LALAFO -> buildLalafoUrl(query, filterOptions)
            MarketplaceType.ALI_EXPRESS -> buildAliExpressUrl(query, filterOptions)
            MarketplaceType.BAZAR -> buildBazarUrl(query, filterOptions)
        }
    }

    /**
     * Формирует URL для поиска на Wildberries с учетом фильтров
     */
    private fun buildWildberriesUrl(
        query: String,
        filterOptions: FilterOptions
    ): String {
        val baseUrl = "https://www.wildberries.ru/catalog/0/search.aspx"
        val urlBuilder = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("search", query)

        // Добавляем параметры фильтрации если они установлены
        
        // Диапазон цен в правильном формате priceU=min;max
        if (filterOptions.priceFrom != null || filterOptions.priceTo != null) {
            val minPrice = filterOptions.priceFrom ?: 0
            val maxPrice = filterOptions.priceTo ?: FilterOptions.MAX_PRICE
            // Цены на Wildberries указываются в копейках (умножаем на 100)
            val priceParam = "${minPrice}00;${maxPrice}00"
            urlBuilder.appendQueryParameter("priceU", priceParam)
        }
        
        // Бренд
        if (!filterOptions.brand.isNullOrEmpty()) {
            urlBuilder.appendQueryParameter("fbrand", filterOptions.brand)
        }
        
        // Сортировка
        val sort = when (filterOptions.sortType) {
            SortType.POPULARITY -> "popular"
            SortType.PRICE_ASC -> "priceup"
            SortType.PRICE_DESC -> "pricedown"
            SortType.RATING -> "rate"
        }
        urlBuilder.appendQueryParameter("sort", sort)
        
        // Доставка сегодня
        if (filterOptions.deliveryToday) {
            urlBuilder.appendQueryParameter("fdlvr", "1") // Параметр быстрой доставки
        }
        
        // Скидки
        if (filterOptions.discount) {
            urlBuilder.appendQueryParameter("action", "202422")
        }
        
        return urlBuilder.build().toString()
    }

    /**
     * Формирует URL для поиска на Ozon с учетом фильтров
     */
    private fun buildOzonUrl(
        query: String,
        filterOptions: FilterOptions
    ): String {
        val baseUrl = "https://www.ozon.ru/search/"
        val urlBuilder = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("text", query)
            .appendQueryParameter("from_global", "true") // Обязательный параметр для корректной работы поиска
        
        // Цена (используем формат currency_price=min;max)
        if (filterOptions.priceFrom != null && filterOptions.priceTo != null) {
            val priceParam = "${filterOptions.priceFrom}.000;${filterOptions.priceTo}.000"
            urlBuilder.appendQueryParameter("currency_price", priceParam)
        } else if (filterOptions.priceFrom != null) {
            val priceParam = "${filterOptions.priceFrom}.000;"
            urlBuilder.appendQueryParameter("currency_price", priceParam)
        } else if (filterOptions.priceTo != null) {
            val priceParam = ";${filterOptions.priceTo}.000"
            urlBuilder.appendQueryParameter("currency_price", priceParam)
        }
        
        // Бренд
        if (!filterOptions.brand.isNullOrEmpty()) {
            // Ozon использует query parameter "brand" для фильтрации по бренду
            urlBuilder.appendQueryParameter("brand", filterOptions.brand)
        }
        
        // Сортировка
        val sort = when (filterOptions.sortType) {
            SortType.POPULARITY -> "score"
            SortType.PRICE_ASC -> "price"
            SortType.PRICE_DESC -> "price_desc"
            SortType.RATING -> "rating"
        }
        urlBuilder.appendQueryParameter("sorting", sort)
        
        // Доставка сегодня
        if (filterOptions.deliveryToday) {
            urlBuilder.appendQueryParameter("express", "1")
        }
        
        // Скидки
        if (filterOptions.discount) {
            urlBuilder.appendQueryParameter("action", "202422")
        }
        
        return urlBuilder.build().toString()
    }

    /**
     * Формирует URL для поиска на Lalafo с учетом фильтров
     */
    private fun buildLalafoUrl(
        query: String,
        filterOptions: FilterOptions
    ): String {
        // Используем правильный формат URL для Lalafo
        val baseUrl = "https://lalafo.kg/kyrgyzstan/q-$query"
        val urlBuilder = Uri.parse(baseUrl).buildUpon()
        
        // Добавляем параметры фильтрации если они установлены
        
        // Диапазон цен
        if (filterOptions.priceFrom != null) {
            urlBuilder.appendQueryParameter("price[from]", filterOptions.priceFrom.toString())
        }
        
        if (filterOptions.priceTo != null) {
            urlBuilder.appendQueryParameter("price[to]", filterOptions.priceTo.toString())
        }
        
        // Сортировка
        val sort = when (filterOptions.sortType) {
            SortType.POPULARITY -> "newest"  // На Lalafo нет точного соответствия "популярности", используем "newest"
            SortType.PRICE_ASC -> "cheap"
            SortType.PRICE_DESC -> "expensive"
            SortType.RATING -> "nearest"     // Lalafo не имеет сортировки по рейтингу
        }
        urlBuilder.appendQueryParameter("order", sort)
        
        return urlBuilder.build().toString()
    }
    
    /**
     * Формирует URL для поиска на AliExpress с учетом фильтров
     */
    private fun buildAliExpressUrl(
        query: String,
        filterOptions: FilterOptions
    ): String {
        val baseUrl = "https://aliexpress.ru/wholesale"
        val urlBuilder = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("SearchText", query)
        
        // Диапазон цен
        if (filterOptions.priceFrom != null) {
            urlBuilder.appendQueryParameter("minPrice", filterOptions.priceFrom.toString())
        }
        
        if (filterOptions.priceTo != null) {
            urlBuilder.appendQueryParameter("maxPrice", filterOptions.priceTo.toString())
        }
        
        // Сортировка
        val sort = when (filterOptions.sortType) {
            SortType.POPULARITY -> "orders"
            SortType.PRICE_ASC -> "price_asc"
            SortType.PRICE_DESC -> "price_desc"
            SortType.RATING -> "feedback"
        }
        urlBuilder.appendQueryParameter("SortType", sort)
        
        // Скидки
        if (filterOptions.discount) {
            urlBuilder.appendQueryParameter("isFreeShip", "y")
        }
        
        return urlBuilder.build().toString()
    }
    
    /**
     * Формирует URL для поиска на Bazar с учетом фильтров
     */
    private fun buildBazarUrl(
        query: String,
        filterOptions: FilterOptions
    ): String {
        val baseUrl = "https://www.bazar.kg/kyrgyzstan"
        val urlBuilder = Uri.parse(baseUrl).buildUpon()
            .appendQueryParameter("search", query)
        
        // Добавляем параметры фильтрации если они установлены
        
        // Диапазон цен
        if (filterOptions.priceFrom != null) {
            urlBuilder.appendQueryParameter("price_from", filterOptions.priceFrom.toString())
        }
        
        if (filterOptions.priceTo != null) {
            urlBuilder.appendQueryParameter("price_to", filterOptions.priceTo.toString())
        }
        
        // Сортировка
        val sort = when (filterOptions.sortType) {
            SortType.POPULARITY -> "date"
            SortType.PRICE_ASC -> "price_asc"
            SortType.PRICE_DESC -> "price_desc"
            SortType.RATING -> "date"
        }
        urlBuilder.appendQueryParameter("order", sort)
        
        return urlBuilder.build().toString()
    }
}