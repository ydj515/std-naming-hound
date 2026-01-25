package com.github.ydj515.stdnaminghound.model

data class CustomDataset(
    val version: String,
    val meta: DatasetMeta? = null,
    val terms: List<Term> = emptyList(),
    val words: List<Word> = emptyList(),
    val domains: List<Domain> = emptyList(),
)
