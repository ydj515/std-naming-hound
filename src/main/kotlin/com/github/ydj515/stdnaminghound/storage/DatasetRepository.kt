package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class DatasetRepository {
    private val loader = ResourceDatasetLoader()
    private val customLoader = CustomDatasetLoader()
    private val merger = DatasetMerger()
    private val settings = service<StdNamingHoundSettings>()
    private val logger = Logger.getInstance(DatasetRepository::class.java)

    @Volatile
    private var cached: Dataset = loadMerged()

    fun getDataset(): Dataset = cached

    fun reload(): Dataset {
        val next = loadMerged()
        cached = next
        return next
    }

    private fun loadMerged(): Dataset {
        val base = loader.load()
        val customJson = settings.state.customDatasetJson
        if (customJson.isNullOrBlank()) return base
        val custom = customLoader.load(customJson)
        if (custom == null) {
            logger.warn("Custom dataset JSON parsing failed. Falling back to base dataset.")
            return base
        }
        return if (settings.state.useCustomOnly) {
            custom
        } else {
            merger.merge(base, custom)
        }
    }
}
