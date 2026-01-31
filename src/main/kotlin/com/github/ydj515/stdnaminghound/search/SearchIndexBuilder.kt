package com.github.ydj515.stdnaminghound.search

import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.DomainRef
import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.model.SearchItemType
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.TermRef
import com.github.ydj515.stdnaminghound.model.Word
import com.github.ydj515.stdnaminghound.model.WordRef

/** Dataset을 검색 인덱스로 변환한다. */
class SearchIndexBuilder {
    /** Dataset의 모든 항목을 SearchIndex로 변환한다. */
    fun build(dataset: Dataset): SearchIndex {
        val entries = mutableListOf<SearchEntry>()
        val items = mutableListOf<SearchItem>()

        dataset.terms.forEachIndexed { index, term ->
            val item = term.toSearchItem(index)
            items.add(item)
            entries.add(
                SearchEntry(
                    searchTextKo = Normalizer.normalizeKo(term.toKoSearchText()),
                    searchTextEn = Normalizer.normalizeEn(term.toEnSearchText()),
                    item = item,
                )
            )
        }

        dataset.words.forEachIndexed { index, word ->
            val item = word.toSearchItem(index)
            items.add(item)
            entries.add(
                SearchEntry(
                    searchTextKo = Normalizer.normalizeKo(word.toKoSearchText()),
                    searchTextEn = Normalizer.normalizeEn(word.toEnSearchText()),
                    item = item,
                )
            )
        }

        dataset.domains.forEachIndexed { index, domain ->
            val item = domain.toSearchItem(index)
            items.add(item)
            entries.add(
                SearchEntry(
                    searchTextKo = Normalizer.normalizeKo(domain.toKoSearchText()),
                    searchTextEn = Normalizer.normalizeEn(domain.toEnSearchText()),
                    item = item,
                )
            )
        }

        return SearchIndex(items = items, entries = entries)
    }

    /** Term을 검색용 아이템으로 변환한다. */
    private fun Term.toSearchItem(index: Int): SearchItem {
        val subText = listOfNotNull(
            domainName?.takeIf { it.isNotBlank() }?.let { "Domain: $it" },
            description?.takeIf { it.isNotBlank() },
        ).joinToString(" | ").ifBlank { null }
        return SearchItem(
            type = SearchItemType.TERM,
            titleKo = koName,
            primaryEn = abbr,
            abbr = abbr,
            subText = subText,
            score = 0.0,
            payloadRef = TermRef(index),
        )
    }

    /** Word를 검색용 아이템으로 변환한다. */
    private fun Word.toSearchItem(index: Int): SearchItem {
        return SearchItem(
            type = SearchItemType.WORD,
            titleKo = koName,
            primaryEn = enName,
            abbr = abbr,
            subText = description,
            score = 0.0,
            payloadRef = WordRef(index),
        )
    }

    /** Domain을 검색용 아이템으로 변환한다. */
    private fun Domain.toSearchItem(index: Int): SearchItem {
        val size = buildString {
            val len = length?.toString()
            val sc = scale?.toString()
            if (!len.isNullOrBlank()) {
                append(len)
                if (!sc.isNullOrBlank()) {
                    append(",")
                    append(sc)
                }
            }
        }
        val subText = listOfNotNull(
            dataType?.takeIf { it.isNotBlank() },
            size.takeIf { it.isNotBlank() },
        ).joinToString(" ")
            .ifBlank { null }
        return SearchItem(
            type = SearchItemType.DOMAIN,
            titleKo = name,
            primaryEn = dataType,
            abbr = null,
            subText = subText,
            score = 0.0,
            payloadRef = DomainRef(index),
        )
    }

    /** Term의 한글 검색 텍스트를 구성한다. */
    private fun Term.toKoSearchText(): String {
        return buildString {
            append(koName)
            if (!description.isNullOrBlank()) append(" ").append(description)
            if (!domainName.isNullOrBlank()) append(" ").append(domainName)
            synonyms.forEach { append(" ").append(it) }
        }
    }

    /** Term의 영문/약어 검색 텍스트를 구성한다. */
    private fun Term.toEnSearchText(): String {
        return listOfNotNull(abbr?.takeIf { it.isNotBlank() }).joinToString(" ")
    }

    /** Word의 한글 검색 텍스트를 구성한다. */
    private fun Word.toKoSearchText(): String {
        return buildString {
            append(koName)
            if (!description.isNullOrBlank()) append(" ").append(description)
            synonyms.forEach { append(" ").append(it) }
        }
    }

    /** Word의 영문/약어 검색 텍스트를 구성한다. */
    private fun Word.toEnSearchText(): String {
        return listOfNotNull(
            enName?.takeIf { it.isNotBlank() },
            abbr?.takeIf { it.isNotBlank() },
        ).joinToString(" ")
    }

    /** Domain의 한글 검색 텍스트를 구성한다. */
    private fun Domain.toKoSearchText(): String = name

    /** Domain의 영문 검색 텍스트를 구성한다. */
    private fun Domain.toEnSearchText(): String {
        return listOfNotNull(
            dataType?.takeIf { it.isNotBlank() },
        ).joinToString(" ")
    }
}
