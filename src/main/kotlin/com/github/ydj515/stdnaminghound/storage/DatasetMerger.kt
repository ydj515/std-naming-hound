package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.Word

class DatasetMerger {
    fun merge(base: Dataset, custom: Dataset, policy: MergePolicy): Dataset {
        return Dataset(
            meta = base.meta,
            terms = mergeTerms(base.terms, custom.terms, policy),
            words = mergeWords(base.words, custom.words, policy),
            domains = mergeDomains(base.domains, custom.domains, policy),
        )
    }

    private fun mergeTerms(base: List<Term>, custom: List<Term>, policy: MergePolicy): List<Term> {
        return when (policy) {
            MergePolicy.CUSTOM_FIRST -> mergeWithOrder(base, custom) { item, index, source ->
                termKey(item, index, source)
            }
            MergePolicy.BASE_FIRST -> mergeWithOrder(custom, base) { item, index, source ->
                termKey(item, index, source)
            }
            MergePolicy.DEDUPE_BASE -> mergeWithDedupeBase(base, custom) { item, index, source ->
                termKey(item, index, source)
            }
        }
    }

    private fun mergeWords(base: List<Word>, custom: List<Word>, policy: MergePolicy): List<Word> {
        return when (policy) {
            MergePolicy.CUSTOM_FIRST -> mergeWithOrder(base, custom) { item, index, source ->
                wordKey(item, index, source)
            }
            MergePolicy.BASE_FIRST -> mergeWithOrder(custom, base) { item, index, source ->
                wordKey(item, index, source)
            }
            MergePolicy.DEDUPE_BASE -> mergeWithDedupeBase(base, custom) { item, index, source ->
                wordKey(item, index, source)
            }
        }
    }

    private fun mergeDomains(base: List<Domain>, custom: List<Domain>, policy: MergePolicy): List<Domain> {
        return when (policy) {
            MergePolicy.CUSTOM_FIRST -> mergeWithOrder(base, custom) { item, index, source ->
                domainKey(item, index, source)
            }
            MergePolicy.BASE_FIRST -> mergeWithOrder(custom, base) { item, index, source ->
                domainKey(item, index, source)
            }
            MergePolicy.DEDUPE_BASE -> mergeWithDedupeBase(base, custom) { item, index, source ->
                domainKey(item, index, source)
            }
        }
    }

    private fun termKey(term: Term, index: Int, source: String): String {
        val koName = term.koName.trim()
        val abbr = term.abbr?.trim().orEmpty()
        return if (koName.isNotBlank() && abbr.isNotBlank()) {
            "$koName::$abbr"
        } else {
            "$source:term:$index"
        }
    }

    private fun wordKey(word: Word, index: Int, source: String): String {
        val koName = word.koName.trim()
        val abbr = word.abbr?.trim().orEmpty()
        return if (koName.isNotBlank() && abbr.isNotBlank()) {
            "$koName::$abbr"
        } else {
            "$source:word:$index"
        }
    }

    private fun domainKey(domain: Domain, index: Int, source: String): String {
        val name = domain.name.trim()
        return if (name.isNotBlank()) name else "$source:domain:$index"
    }

    private fun <T> mergeWithOrder(
        first: List<T>,
        second: List<T>,
        keyOf: (T, Int, String) -> String,
    ): List<T> {
        val merged = LinkedHashMap<String, T>(first.size + second.size)
        first.forEachIndexed { index, item -> merged[keyOf(item, index, "first")] = item }
        second.forEachIndexed { index, item -> merged[keyOf(item, index, "second")] = item }
        return merged.values.toList()
    }

    private fun <T> mergeWithDedupeBase(
        base: List<T>,
        custom: List<T>,
        keyOf: (T, Int, String) -> String,
    ): List<T> {
        val merged = LinkedHashMap<String, T>(base.size + custom.size)
        base.forEachIndexed { index, item -> merged[keyOf(item, index, "base")] = item }
        custom.forEachIndexed { index, item ->
            val key = keyOf(item, index, "custom")
            if (!merged.containsKey(key)) {
                merged[key] = item
            }
        }
        return merged.values.toList()
    }
}
