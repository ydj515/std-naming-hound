package com.github.ydj515.stdnaminghound.search

import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
class SearchIndexRepository {
    private val datasetRepository = service<DatasetRepository>()
    private val builder = SearchIndexBuilder()

    @Volatile
    private var cached: SearchIndex = builder.build(datasetRepository.getDataset())

    fun getIndex(): SearchIndex = cached

    fun reload(): SearchIndex {
        val next = builder.build(datasetRepository.reload())
        cached = next
        return next
    }
}
