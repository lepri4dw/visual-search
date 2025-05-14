package com.example.visualsearch.ui.history

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.example.visualsearch.R

class ScanHistoryFragmentDirections private constructor() {
    companion object {
        fun actionHistoryToScanDetail(scanId: String): NavDirections {
            val bundle = ScanDetailFragmentArgs(scanId).toBundle()
            return object : NavDirections {
                override val actionId: Int = R.id.action_navigation_history_to_scan_detail
                override val arguments: Bundle = bundle
            }
        }
    }
}
