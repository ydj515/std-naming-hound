package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.Dataset
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.Word

class DatasetMerger {
    fun merge(base: Dataset, custom: Dataset): Dataset {
        return Dataset(
            meta = base.meta,
            terms = mergeTerms(base.terms, custom.terms),
            words = mergeWords(base.words, custom.words),
            domains = mergeDomains(base.domains, custom.domains),
        )
    }

    private fun mergeTerms(base: List<Term>, custom: List<Term>): List<Term> {
        val merged = LinkedHashMap<String, Term>(base.size + custom.size)
        base.forEach { merged[termKey(it)] = it }
        custom.forEach { merged[termKey(it)] = it }
        return merged.values.toList()
    }

    private fun mergeWords(base: List<Word>, custom: List<Word>): List<Word> {
        val merged = LinkedHashMap<String, Word>(base.size + custom.size)
        base.forEach { merged[wordKey(it)] = it }
        custom.forEach { merged[wordKey(it)] = it }
        return merged.values.toList()
    }

    private fun mergeDomains(base: List<Domain>, custom: List<Domain>): List<Domain> {
        val merged = LinkedHashMap<String, Domain>(base.size + custom.size)
        base.forEach { merged[domainKey(it)] = it }
        custom.forEach { merged[domainKey(it)] = it }
        return merged.values.toList()
    }

    private fun termKey(term: Term): String {
        val abbr = term.abbr?.trim().orEmpty()
        return if (abbr.isNotBlank()) abbr else term.koName.trim()
    }

    private fun wordKey(word: Word): String {
        val abbr = word.abbr?.trim().orEmpty()
        return if (abbr.isNotBlank()) abbr else word.koName.trim()
    }

    private fun domainKey(domain: Domain): String = domain.name.trim()
}
