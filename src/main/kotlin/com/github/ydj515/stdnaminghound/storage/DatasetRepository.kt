package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.github.ydj515.stdnaminghound.storage.MergePolicy
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.components.Service

/** 기본/커스텀 데이터셋을 로드하고 병합하는 저장소다. */
@Service(Service.Level.APP)
class DatasetRepository {
    private val loader = ResourceDatasetLoader()
    private val customLoader = CustomDatasetLoader()
    private val merger = DatasetMerger()
    private val settings = service<StdNamingHoundSettings>()
    private val logger = Logger.getInstance(DatasetRepository::class.java)

    @Volatile
    private var cached: Dataset = loadMerged()

    /** 현재 캐시된 데이터셋을 반환한다. */
    fun getDataset(): Dataset = cached

    /** 설정을 반영해 데이터셋을 다시 로드한다. */
    fun reload(): Dataset {
        val next = loadMerged()
        cached = next
        return next
    }

    /** 기본/커스텀 데이터를 읽어 병합한 최종 결과를 만든다. */
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
            val policy = MergePolicy.fromName(settings.state.mergePolicy)
            merger.merge(base, custom, policy)
        }
    }
}
