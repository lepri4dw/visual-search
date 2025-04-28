package com.example.visualsearch.ui.history

import android.os.Bundle
import androidx.navigation.NavDirections
import com.example.visualsearch.R

class ScanDetailFragmentDirections private constructor() {
    companion object {
        fun actionScanDetailToNavigationHome(
            query: String,
            productType: String,
            brand: String,
            model: String,
            color: String,
            imagePath: String
        ): NavDirections {
            val args = Bundle().apply {
                putString("query", query)
                putString("productType", productType)
                putString("brand", brand)
                putString("model", model)
                putString("color", color)
                putString("imagePath", imagePath)
            }

            return object : NavDirections {
                override val actionId: Int = R.id.action_scan_detail_to_navigation_home
                override val arguments: Bundle = args
            }
        }
    }
}