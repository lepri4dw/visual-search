package com.example.visualsearch.ui.history

import android.os.Bundle
import androidx.navigation.NavArgs
import androidx.navigation.NavDirections
import com.example.visualsearch.R
import java.io.Serializable

class ScanDetailFragmentArgs(val scanId: String) : NavArgs {
    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("scanId", scanId)
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
        fun fromBundle(bundle: Bundle): ScanDetailFragmentArgs {
            bundle.classLoader = ScanDetailFragmentArgs::class.java.classLoader
            return ScanDetailFragmentArgs(bundle.getString("scanId", ""))
        }
    }
}