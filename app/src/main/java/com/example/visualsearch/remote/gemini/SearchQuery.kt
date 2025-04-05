package com.example.visualsearch.remote.gemini

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchQuery(
    val query: String,
    val productType: String,
    val modelName: String,
    val brand: String,
    val color: String = ""
) : Parcelable