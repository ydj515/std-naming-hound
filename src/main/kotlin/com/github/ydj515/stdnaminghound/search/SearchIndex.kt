package com.github.ydj515.stdnaminghound.search

import com.github.ydj515.stdnaminghound.model.SearchItem

data class SearchEntry(
    val searchTextKo: String,
    val searchTextEn: String,
    val item: SearchItem,
)

data class SearchIndex(
    val items: List<SearchItem>,
    val entries: List<SearchEntry>,
)
