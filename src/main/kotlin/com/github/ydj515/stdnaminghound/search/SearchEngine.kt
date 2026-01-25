package com.github.ydj515.stdnaminghound.search

import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.intellij.openapi.components.service

class SearchEngine(
    private val indexRepository: SearchIndexRepository = service(),
    private val settings: StdNamingHoundSettings = service(),
) {
    fun search(
        rawQuery: String,
        limit: Int = 50,
        allowedTypes: Set<com.github.ydj515.stdnaminghound.model.SearchItemType>? = null,
    ): List<SearchItem> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return emptyList()
        val index = indexRepository.getIndex()
        val isKoreanQuery = containsHangul(query)
        val normalizedQuery = if (isKoreanQuery) {
            Normalizer.normalizeKo(query)
        } else {
            Normalizer.normalizeEn(query)
        }
        if (normalizedQuery.isBlank()) return emptyList()

        val results = mutableListOf<ScoredItem>()
        val useFuzzy = settings.state.enableFuzzy

        for (entry in index.entries) {
            if (allowedTypes != null && entry.item.type !in allowedTypes) continue
            val target = if (isKoreanQuery) entry.searchTextKo else entry.searchTextEn
            if (target.isBlank()) continue
            var score = scoreMatch(normalizedQuery, target)
            if (useFuzzy && score < FUZZY_SCORE_THRESHOLD) {
                val fuzzy = fuzzyScore(normalizedQuery, target)
                if (fuzzy > score) {
                    score = fuzzy * FUZZY_SCORE_WEIGHT
                }
            }
            if (score > 0.0) {
                val boosted = score + typeBoost(entry.item)
                results.add(ScoredItem(entry.item, boosted))
            }
        }

        return results
            .sortedWith(
                compareByDescending<ScoredItem> { it.score }
                    .thenBy { typePriority(it.item) }
                    .thenBy { it.item.titleKo }
            )
            .take(limit)
            .map { it.item.copy(score = it.score) }
    }

    private fun scoreMatch(query: String, target: String): Double {
        if (query == target) return 1.0
        if (target.startsWith(query)) return 0.9
        if (target.contains(query)) return 0.6
        val tokens = target.split(' ').filter { it.isNotBlank() }
        if (tokens.any { it == query }) return 0.7
        if (tokens.any { it.startsWith(query) }) return 0.55
        return 0.0
    }

    private fun fuzzyScore(query: String, target: String): Double {
        val grams = ngrams(query, 2)
        if (grams.isEmpty()) return 0.0
        val targetGrams = ngrams(target, 2)
        if (targetGrams.isEmpty()) return 0.0
        val intersection = grams.count { it in targetGrams }
        val union = grams.size + targetGrams.size - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private fun ngrams(text: String, n: Int): Set<String> {
        if (text.length < n) return emptySet()
        return text.windowed(size = n, step = 1).toSet()
    }

    private fun containsHangul(text: String): Boolean {
        return text.any { it in '\uAC00'..'\uD7A3' }
    }

    private fun typeBoost(item: SearchItem): Double {
        return when (item.type) {
            com.github.ydj515.stdnaminghound.model.SearchItemType.TERM -> 0.08
            com.github.ydj515.stdnaminghound.model.SearchItemType.WORD -> 0.04
            com.github.ydj515.stdnaminghound.model.SearchItemType.DOMAIN -> 0.0
        }
    }

    private fun typePriority(item: SearchItem): Int {
        return when (item.type) {
            com.github.ydj515.stdnaminghound.model.SearchItemType.TERM -> 0
            com.github.ydj515.stdnaminghound.model.SearchItemType.WORD -> 1
            com.github.ydj515.stdnaminghound.model.SearchItemType.DOMAIN -> 2
        }
    }

    private data class ScoredItem(
        val item: SearchItem,
        val score: Double,
    )

    companion object {
        private const val FUZZY_SCORE_THRESHOLD = 0.3
        private const val FUZZY_SCORE_WEIGHT = 0.8
    }
}
