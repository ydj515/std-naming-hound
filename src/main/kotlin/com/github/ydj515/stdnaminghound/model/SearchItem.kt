package com.github.ydj515.stdnaminghound.model

/** 검색 결과의 분류 타입을 표현한다. */
enum class SearchItemType {
    TERM,
    WORD,
    DOMAIN,
}

/** 원본 데이터 인덱스를 가리키는 공통 참조 타입이다. */
sealed interface PayloadRef {
    val index: Int
}

/** Term 인덱스 참조용 값 객체다. */
data class TermRef(override val index: Int) : PayloadRef

/** Word 인덱스 참조용 값 객체다. */
data class WordRef(override val index: Int) : PayloadRef

/** Domain 인덱스 참조용 값 객체다. */
data class DomainRef(override val index: Int) : PayloadRef

/** 검색 결과 리스트에 표시되는 항목을 표현한다. */
data class SearchItem(
    val type: SearchItemType,
    val titleKo: String,
    val primaryEn: String?,
    val abbr: String?,
    val subText: String?,
    val score: Double,
    val payloadRef: PayloadRef,
)
