package com.example.visualsearch.ui.history

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.example.visualsearch.R

class ScanHistoryFragmentDirections private constructor() {
    companion object {
        fun actionHistoryToScanDetail(scanId: Long): NavDirections {
            return ScanDetailFragmentArgs(scanId).toNavDirections()
        }
    }
}
