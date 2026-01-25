package com.github.ydj515.stdnaminghound.model

data class Dataset(
    val meta: DatasetMeta?,
    val terms: List<Term>,
    val words: List<Word>,
    val domains: List<Domain>,
)

data class DatasetMeta(
    val datasetVersion: String?,
    val source: String?,
    val generatedAt: String?,
    val counts: DatasetCounts?,
)

data class DatasetCounts(
    val terms: Int,
    val words: Int,
    val domains: Int,
)
