package com.example.visualsearch.remote.gemini

import com.google.gson.annotations.SerializedName

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

data class Content(
    val parts: List<Part>?
)

data class Part(
    val text: String?
)

data class SearchQuery(
    val query: String,
    val productType: String,
    val modelName: String,
    val brand: String,
    val color: String = ""
)