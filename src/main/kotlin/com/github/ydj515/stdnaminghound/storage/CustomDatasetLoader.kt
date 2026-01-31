package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.CustomDataset
import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.model.DatasetMeta
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger

/** 사용자 JSON을 Dataset으로 변환해 로드한다. */
class CustomDatasetLoader {
    private val logger = Logger.getInstance(CustomDatasetLoader::class.java)
    private val parser = DatasetJsonParser()

    /** JSON 문자열을 파싱해 Dataset으로 변환한다. */
    fun load(json: String): Dataset? {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            val version = obj.get("version")?.asString?.trim()
            if (version.isNullOrBlank()) {
                logger.warn("Custom dataset missing required field: version")
                return null
            }
            val meta = obj.getAsJsonObject("meta")?.let { parser.parseMeta(it) }
            val terms = obj.getAsJsonArray("terms")?.let { parser.parseTerms(it) }.orEmpty()
            val words = obj.getAsJsonArray("words")?.let { parser.parseWords(it) }.orEmpty()
            val domains = obj.getAsJsonArray("domains")?.let { parser.parseDomains(it) }.orEmpty()
            val custom = CustomDataset(
                version = version,
                meta = meta,
                terms = terms,
                words = words,
                domains = domains,
            )
            Dataset(
                meta = custom.meta ?: DatasetMeta(
                    datasetVersion = custom.version,
                    source = "custom",
                    generatedAt = null,
                    counts = null,
                ),
                terms = custom.terms,
                words = custom.words,
                domains = custom.domains,
            )
        } catch (e: Exception) {
            logger.warn("Custom dataset parsing error: ${e.message}")
            null
        }
    }
}
