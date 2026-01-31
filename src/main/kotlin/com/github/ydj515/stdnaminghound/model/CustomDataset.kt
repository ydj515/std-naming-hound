package com.github.ydj515.stdnaminghound.model

/** 사용자 커스텀 데이터셋(JSON) 구조를 표현한다. */
data class CustomDataset(
    val version: String,
    val meta: DatasetMeta? = null,
    val terms: List<Term> = emptyList(),
    val words: List<Word> = emptyList(),
    val domains: List<Domain> = emptyList(),
)
