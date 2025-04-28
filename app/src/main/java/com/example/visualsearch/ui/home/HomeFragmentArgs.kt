package com.example.visualsearch.ui.home

import android.os.Bundle
import androidx.navigation.NavArgs
import androidx.navigation.NavDirections
import com.example.visualsearch.R
import java.io.Serializable

class HomeFragmentArgs(
    val query: String? = null,
    val imagePath: String? = null,
    val productType: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null
) : NavArgs {
    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("query", query)
        bundle.putString("imagePath", imagePath)
        bundle.putString("productType", productType)
        bundle.putString("brand", brand)
        bundle.putString("model", model)
        bundle.putString("color", color)
        return bundle
    }

    fun toNavDirections(): NavDirections {
        return object : NavDirections {
            override val actionId: Int = R.id.action_navigation_history_to_scan_detail
            override val arguments: Bundle = toBundle()
        }
    }

    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): HomeFragmentArgs {
            bundle.classLoader = HomeFragmentArgs::class.java.classLoader
            return HomeFragmentArgs(
                query = bundle.getString("query"),
                imagePath = bundle.getString("imagePath"),
                productType = bundle.getString("productType"),
                brand = bundle.getString("brand"),
                model = bundle.getString("model"),
                color = bundle.getString("color")
            )
        }
    }
}