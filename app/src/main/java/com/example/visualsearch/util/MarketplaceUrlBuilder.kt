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
        
        // Диапазон цен
        if (filterOptions.priceFrom != null) {
            urlBuilder.appendQueryParameter("priceU", "${filterOptions.priceFrom}00;") // Умножаем на 100 для копеек
        }
        
        if (filterOptions.priceTo != null) {
            val priceFromParam = urlBuilder.build().getQueryParameter("priceU") ?: "0;"
            val newPriceParam = if (priceFromParam.endsWith(";")) {
                priceFromParam + "${filterOptions.priceTo}00"
            } else {
                "${filterOptions.priceFrom ?: 0}00;${filterOptions.priceTo}00"
            }
            
            urlBuilder.appendQueryParameter("priceU", newPriceParam)
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
            urlBuilder.appendQueryParameter("discount", "1")
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
        
        // Добавляем параметры фильтрации если они установлены
        
        // Диапазон цен
        if (filterOptions.priceFrom != null) {
            urlBuilder.appendQueryParameter("from_price", filterOptions.priceFrom.toString())
        }
        
        if (filterOptions.priceTo != null) {
            urlBuilder.appendQueryParameter("to_price", filterOptions.priceTo.toString())
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
            urlBuilder.appendQueryParameter("discount", "1")
        }
        
        return urlBuilder.build().toString()
    }
}