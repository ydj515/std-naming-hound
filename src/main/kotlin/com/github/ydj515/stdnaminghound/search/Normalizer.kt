package com.github.ydj515.stdnaminghound.search

object Normalizer {
    private val koNoise = Regex("[\\s\\p{Punct}]+")
    private val enNoise = Regex("[_\\-\\s]+")

    fun normalizeKo(text: String): String {
        return text.trim().replace(koNoise, "")
    }

    fun normalizeEn(text: String): String {
        return text.trim().lowercase().replace(enNoise, " ")
    }
}
