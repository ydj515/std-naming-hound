package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.model.DatasetMeta
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.Word
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import java.nio.charset.StandardCharsets

class ResourceDatasetLoader {
    private val logger = Logger.getInstance(ResourceDatasetLoader::class.java)
    private val parser = DatasetJsonParser()

    fun load(): Dataset {
        val meta = loadMeta()
        val terms = loadTerms()
        val words = loadWords()
        val domains = loadDomains()
        return Dataset(meta = meta, terms = terms, words = words, domains = domains)
    }

    private fun loadMeta(): DatasetMeta? {
        val text = readResourceText("/data/meta.json") ?: return null
        val obj = JsonParser.parseString(text).asJsonObject
        return parser.parseMeta(obj)
    }

    private fun loadTerms(): List<Term> {
        val text = readResourceText("/data/terms.json") ?: return emptyList()
        val array = JsonParser.parseString(text).asJsonArray
        return parser.parseTerms(array)
    }

    private fun loadWords(): List<Word> {
        val text = readResourceText("/data/words.json") ?: return emptyList()
        val array = JsonParser.parseString(text).asJsonArray
        return parser.parseWords(array)
    }

    private fun loadDomains(): List<Domain> {
        val text = readResourceText("/data/domains.json") ?: return emptyList()
        val array = JsonParser.parseString(text).asJsonArray
        return parser.parseDomains(array)
    }

    private fun readResourceText(path: String): String? {
        val stream = javaClass.getResourceAsStream(path) ?: run {
            logger.warn("Dataset resource not found: $path")
            return null
        }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

}
