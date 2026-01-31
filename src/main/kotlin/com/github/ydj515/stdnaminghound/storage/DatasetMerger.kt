package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.Word

/** 기본/커스텀 데이터셋을 병합 정책에 따라 합친다. */
class DatasetMerger {
    /** 병합 정책에 따라 새 Dataset을 생성한다. */
    fun merge(base: Dataset, custom: Dataset, policy: MergePolicy): Dataset {
        return Dataset(
            meta = base.meta,
            terms = mergeTerms(base.terms, custom.terms, policy),
            words = mergeWords(base.words, custom.words, policy),
            domains = mergeDomains(base.domains, custom.domains, policy),
        )
    }

    /** Term 목록을 병합한다. */
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

    /** Word 목록을 병합한다. */
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

    /** Domain 목록을 병합한다. */
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

    /** Term의 중복 판정 키를 생성한다. */
    private fun termKey(term: Term, index: Int, source: String): String {
        val koName = term.koName.trim()
        val abbr = term.abbr?.trim().orEmpty()
        return if (koName.isNotBlank() && abbr.isNotBlank()) {
            "$koName::$abbr"
        } else {
            "$source:term:$index"
        }
    }

    /** Word의 중복 판정 키를 생성한다. */
    private fun wordKey(word: Word, index: Int, source: String): String {
        val koName = word.koName.trim()
        val abbr = word.abbr?.trim().orEmpty()
        return if (koName.isNotBlank() && abbr.isNotBlank()) {
            "$koName::$abbr"
        } else {
            "$source:word:$index"
        }
    }

    /** Domain의 중복 판정 키를 생성한다. */
    private fun domainKey(domain: Domain, index: Int, source: String): String {
        val name = domain.name.trim()
        return if (name.isNotBlank()) name else "$source:domain:$index"
    }

    /** 우선순위 목록을 먼저 넣고, 나머지를 덮어쓰는 방식으로 병합한다. */
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

    /** 기본 목록을 기준으로 커스텀 중복을 제거한다. */
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
