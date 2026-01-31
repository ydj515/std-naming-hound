package com.github.ydj515.stdnaminghound.search

import com.github.ydj515.stdnaminghound.model.SearchItem

/** 검색 인덱싱을 위한 단일 엔트리를 표현한다. */
data class SearchEntry(
    val searchTextKo: String,
    val searchTextEn: String,
    val item: SearchItem,
)

/** 검색 가능한 항목과 인덱스 엔트리를 보관한다. */
data class SearchIndex(
    val items: List<SearchItem>,
    val entries: List<SearchEntry>,
)
