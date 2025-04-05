package com.example.visualsearch.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.visualsearch.R
import com.example.visualsearch.model.FilterOptions
import com.example.visualsearch.model.MarketplaceType
import com.example.visualsearch.remote.gemini.SearchQuery
import com.example.visualsearch.util.MarketplaceAppChecker

/**
 * Адаптер для отображения списка маркетплейсов
 */
class MarketplaceAdapter(
    private val context: Context,
    private val searchQuery: SearchQuery,
    private val onMarketplaceSelected: (MarketplaceType) -> Unit
) : RecyclerView.Adapter<MarketplaceAdapter.MarketplaceViewHolder>() {

    private val marketplaceAppChecker = MarketplaceAppChecker(context)
    
    // Фильтры, которые будут применяться ко всем маркетплейсам
    private var filterOptions: FilterOptions? = null
    
    // Список маркетплейсов, которые будут отображаться
    private val marketplaces = listOf(
        MarketplaceItem(
            MarketplaceType.WILDBERRIES,
            "Wildberries",
            "Скидки до 90%",
            R.color.wildberries_color,
            R.drawable.ic_wildberries
        ),
        MarketplaceItem(
            MarketplaceType.OZON,
            "Ozon",
            "Быстрая доставка",
            R.color.ozon_color,
            R.drawable.ic_ozon
        ),
        MarketplaceItem(
            MarketplaceType.LALAFO,
            "Lalafo",
            "Объявления в КР",
            R.color.lalafo_color,
            R.drawable.ic_lalafo
        ),
        MarketplaceItem(
            MarketplaceType.ALI_EXPRESS,
            "AliExpress",
            "Международная доставка",
            R.color.aliexpress_color,
            R.drawable.ic_aliexpress
        ),
        MarketplaceItem(
            MarketplaceType.BAZAR,
            "Bazar",
            "Объявления в КР",
            R.color.bazar_color,
            R.drawable.ic_bazar
        )
    )

    /**
     * Обновляет фильтры для всех маркетплейсов
     */
    fun updateFilters(filterOptions: FilterOptions) {
        this.filterOptions = filterOptions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketplaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marketplace_card, parent, false)
        return MarketplaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarketplaceViewHolder, position: Int) {
        val marketplace = marketplaces[position]
        holder.bind(marketplace)
    }

    override fun getItemCount(): Int = marketplaces.size

    inner class MarketplaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardMarketplace: CardView = itemView.findViewById(R.id.cardMarketplace)
        private val colorStrip: View = itemView.findViewById(R.id.viewColorStrip)
        private val logoImageView: ImageView = itemView.findViewById(R.id.ivMarketplaceLogo)
        private val nameTextView: TextView = itemView.findViewById(R.id.tvMarketplaceName)
        private val infoTextView: TextView = itemView.findViewById(R.id.tvMarketplaceInfo)
        private val installedIndicator: ImageView = itemView.findViewById(R.id.ivInstalled)

        fun bind(marketplace: MarketplaceItem) {
            // Установка цвета полосы
            colorStrip.setBackgroundColor(ContextCompat.getColor(context, marketplace.colorResource))
            
            // Установка логотипа
            logoImageView.setImageResource(marketplace.logoResource)
            
            // Установка названия и информации
            nameTextView.text = marketplace.name
            
            // Добавляем информацию о примененных фильтрах, если они есть
            val infoText = if (filterOptions != null) {
                val priceInfo = if (filterOptions?.priceFrom != null || filterOptions?.priceTo != null) {
                    "Цена: ${filterOptions?.priceFrom ?: 0} - ${filterOptions?.priceTo ?: "макс."} ₽"
                } else {
                    ""
                }
                
                val brandInfo = if (!filterOptions?.brand.isNullOrEmpty()) {
                    "Бренд: ${filterOptions?.brand}"
                } else {
                    ""
                }
                
                if (priceInfo.isNotEmpty() || brandInfo.isNotEmpty()) {
                    "$priceInfo ${if(priceInfo.isNotEmpty() && brandInfo.isNotEmpty()) "• " else ""}$brandInfo".trim()
                } else {
                    marketplace.info
                }
            } else {
                marketplace.info
            }
            
            infoTextView.text = infoText
            
            // Проверка, установлено ли приложение
            val isAppInstalled = marketplaceAppChecker.isMarketplaceAppInstalled(marketplace.type)
            installedIndicator.visibility = if (isAppInstalled) View.VISIBLE else View.GONE
            
            // Обработка нажатия
            cardMarketplace.setOnClickListener {
                // Если есть установленные фильтры, применяем их
                if (filterOptions != null) {
                    marketplaceAppChecker.openMarketplaceWithFilters(
                        marketplace.type,
                        searchQuery.query,
                        filterOptions!!
                    )
                } else {
                    onMarketplaceSelected(marketplace.type)
                }
            }
        }
    }

    /**
     * Модель элемента маркетплейса для отображения в списке
     */
    data class MarketplaceItem(
        val type: MarketplaceType,
        val name: String,
        val info: String,
        val colorResource: Int,
        val logoResource: Int
    )
}