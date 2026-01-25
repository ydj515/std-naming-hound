package com.github.ydj515.stdnaminghound.model

enum class SearchItemType {
    TERM,
    WORD,
    DOMAIN,
}

sealed interface PayloadRef {
    val index: Int
}

data class TermRef(override val index: Int) : PayloadRef

data class WordRef(override val index: Int) : PayloadRef

data class DomainRef(override val index: Int) : PayloadRef

data class SearchItem(
    val type: SearchItemType,
    val titleKo: String,
    val primaryEn: String?,
    val abbr: String?,
    val subText: String?,
    val score: Double,
    val payloadRef: PayloadRef,
)
