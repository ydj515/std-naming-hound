package com.github.ydj515.stdnaminghound.builder

import com.github.ydj515.stdnaminghound.model.Word

class WordBuilder {
    enum class CaseStyle {
        SNAKE_UPPER,
        SNAKE_LOWER,
        CAMEL,
        PASCAL,
    }

    private val tokens = mutableListOf<Word>()
    var caseStyle: CaseStyle = CaseStyle.SNAKE_UPPER
        private set

    fun setCaseStyle(style: CaseStyle) {
        caseStyle = style
    }

    fun add(word: Word) {
        tokens.add(word)
    }

    fun removeAt(index: Int) {
        if (index in tokens.indices) {
            tokens.removeAt(index)
        }
    }

    fun clear() {
        tokens.clear()
    }

    fun getTokens(): List<Word> = tokens.toList()

    fun buildName(): String {
        if (tokens.isEmpty()) return ""
        val parts = tokens.map { token ->
            val raw = token.abbr?.takeIf { it.isNotBlank() }
                ?: token.enName?.takeIf { it.isNotBlank() }
                ?: token.koName
            raw.trim()
        }
        val snake = parts
            .flatMap { it.split(Regex("[\\s_\\-]+")).filter(String::isNotBlank) }
            .joinToString("_")
        return when (caseStyle) {
            CaseStyle.SNAKE_UPPER -> snake.uppercase()
            CaseStyle.SNAKE_LOWER -> snake.lowercase()
            CaseStyle.CAMEL -> toCamel(snake, upperFirst = false)
            CaseStyle.PASCAL -> toCamel(snake, upperFirst = true)
        }
    }

    private fun toCamel(snake: String, upperFirst: Boolean): String {
        val parts = snake.split("_").filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        return buildString {
            parts.forEachIndexed { index, part ->
                val lower = part.lowercase()
                if (index == 0 && !upperFirst) {
                    append(lower)
                } else {
                    append(lower.replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}
