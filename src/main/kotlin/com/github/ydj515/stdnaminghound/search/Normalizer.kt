package com.github.ydj515.stdnaminghound.search

/** 검색을 위해 텍스트를 정규화한다. */
object Normalizer {
    private val koNoise = Regex("[\\s\\p{Punct}]+")
    private val enNoise = Regex("[_\\-\\s]+")

    /** 한글 텍스트에서 공백/기호를 제거한다. */
    fun normalizeKo(text: String): String {
        return text.trim().replace(koNoise, "")
    }

    /** 영문 텍스트를 소문자화하고 구분자를 공백으로 통일한다. */
    fun normalizeEn(text: String): String {
        return text.trim().lowercase().replace(enNoise, " ")
    }
}
