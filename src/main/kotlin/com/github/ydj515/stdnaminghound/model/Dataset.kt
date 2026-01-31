package com.github.ydj515.stdnaminghound.model

/** 기본/커스텀 데이터를 합친 최종 데이터셋을 표현한다. */
data class Dataset(
    val meta: DatasetMeta?,
    val terms: List<Term>,
    val words: List<Word>,
    val domains: List<Domain>,
)

/** 데이터셋 메타 정보를 표현한다. */
data class DatasetMeta(
    val datasetVersion: String?,
    val source: String?,
    val generatedAt: String?,
    val counts: DatasetCounts?,
)

/** 데이터셋 항목 수 집계를 표현한다. */
data class DatasetCounts(
    val terms: Int,
    val words: Int,
    val domains: Int,
)
