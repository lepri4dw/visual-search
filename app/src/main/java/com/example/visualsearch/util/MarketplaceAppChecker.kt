package com.example.visualsearch.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.example.visualsearch.model.MarketplaceType
import com.example.visualsearch.model.FilterOptions

/**
 * Утилитарный класс для проверки наличия установленных приложений маркетплейсов
 * и построения соответствующих Intent-ов для запуска
 */
class MarketplaceAppChecker(private val context: Context) {

    // Пакеты приложений для популярных маркетплейсов
    companion object {
        private const val WILDBERRIES_PACKAGE = "com.wildberries.ru"
        private const val OZON_PACKAGE = "ru.ozon.app.android"
        private const val LALAFO_PACKAGE = "kg.lalafo.android"
        private const val ALI_EXPRESS_PACKAGE = "com.alibaba.aliexpresshd"
        // O Market и Bazar могут не иметь официальных приложений
    }

    /**
     * Проверяет, установлено ли приложение маркетплейса
     */
    fun isMarketplaceAppInstalled(marketplaceType: MarketplaceType): Boolean {
        val packageName = getPackageNameByType(marketplaceType) ?: return false
        
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Открывает маркетплейс с учетом фильтров
     */
    fun openMarketplaceWithFilters(
        marketplaceType: MarketplaceType,
        query: String,
        filterOptions: FilterOptions
    ) {
        try {
            val intent = getMarketplaceIntent(marketplaceType, query, filterOptions)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Не удалось открыть маркетплейс: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Возвращает Intent для открытия поиска в маркетплейсе
     * Если приложение установлено, открывает его
     * Если не установлено, открывает веб-страницу
     */
    fun getMarketplaceIntent(
        marketplaceType: MarketplaceType,
        query: String,
        filterOptions: FilterOptions
    ): Intent {
        // Проверяем, установлено ли приложение
        val isAppInstalled = isMarketplaceAppInstalled(marketplaceType)
        
        // Даже если приложение установлено, открываем веб-версию
        // чтобы корректно передать параметры фильтров
        val url = MarketplaceUrlBuilder.buildSearchUrl(marketplaceType, query, filterOptions)
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    /**
     * Возвращает Intent для открытия поиска в приложении маркетплейса
     */
    private fun getAppIntent(marketplaceType: MarketplaceType, query: String): Intent? {
        val packageName = getPackageNameByType(marketplaceType) ?: return null
        
        return when (marketplaceType) {
            MarketplaceType.WILDBERRIES -> {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.apply {
                    action = "ru.wildberries.action.SEARCH"
                    putExtra("query", query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            MarketplaceType.OZON -> {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.apply {
                    action = "ru.ozon.action.SEARCH"
                    putExtra("search_query", query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            MarketplaceType.LALAFO -> {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.apply {
                    action = "kg.lalafo.action.SEARCH"
                    putExtra("query", query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            MarketplaceType.ALI_EXPRESS -> {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.apply {
                    action = "com.alibaba.aliexpresshd.action.SEARCH"
                    putExtra("searchQuery", query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            // Для остальных маркетплейсов просто запускаем приложение
            else -> context.packageManager.getLaunchIntentForPackage(packageName)
        }
    }

    /**
     * Возвращает имя пакета по типу маркетплейса
     */
    private fun getPackageNameByType(marketplaceType: MarketplaceType): String? {
        return when (marketplaceType) {
            MarketplaceType.WILDBERRIES -> WILDBERRIES_PACKAGE
            MarketplaceType.OZON -> OZON_PACKAGE
            MarketplaceType.LALAFO -> LALAFO_PACKAGE
            MarketplaceType.ALI_EXPRESS -> ALI_EXPRESS_PACKAGE
            // Для маркетплейсов без приложений или с неизвестными пакетами возвращаем null
            else -> null
        }
    }
}