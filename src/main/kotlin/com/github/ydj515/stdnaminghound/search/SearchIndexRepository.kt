package com.github.ydj515.stdnaminghound.search

import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

/** 데이터셋으로부터 검색 인덱스를 생성/캐시한다. */
@Service(Service.Level.APP)
class SearchIndexRepository {
    private val datasetRepository = service<DatasetRepository>()
    private val builder = SearchIndexBuilder()

    @Volatile
    private var cached: SearchIndex = builder.build(datasetRepository.getDataset())

    /** 현재 캐시된 검색 인덱스를 반환한다. */
    fun getIndex(): SearchIndex = cached

    /** 데이터셋을 다시 로드하고 인덱스를 재생성한다. */
    fun reload(): SearchIndex {
        val next = builder.build(datasetRepository.reload())
        cached = next
        return next
    }
}
