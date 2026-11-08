package com.github.ydj515.stdnaminghound.builder

import com.github.ydj515.stdnaminghound.model.Word

/** 선택된 단어들을 조합해 이름을 만들어주는 빌더다. */
class WordBuilder {
    /** 조합 결과의 케이스 스타일을 정의한다. */
    enum class CaseStyle {
        SNAKE_UPPER,
        SNAKE_LOWER,
        CAMEL,
        PASCAL,
    }

    private val tokens = mutableListOf<Word>()
    var caseStyle: CaseStyle = CaseStyle.SNAKE_UPPER
        private set

    /** 케이스 스타일을 변경한다. */
    fun setCaseStyle(style: CaseStyle) {
        caseStyle = style
    }

    /** 단어를 빌더에 추가한다. */
    fun add(word: Word) {
        tokens.add(word)
    }

    /** 지정 인덱스의 단어를 제거한다. */
    fun removeAt(index: Int) {
        if (index in tokens.indices) {
            tokens.removeAt(index)
        }
    }

    /** 단어의 순서를 변경한다. */
    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in tokens.indices || toIndex !in tokens.indices) return
        if (fromIndex == toIndex) return
        val item = tokens.removeAt(fromIndex)
        tokens.add(toIndex, item)
    }

    /** 누적된 단어를 모두 비운다. */
    fun clear() {
        tokens.clear()
    }

    /** 현재 누적된 단어 목록을 반환한다. */
    fun getTokens(): List<Word> = tokens.toList()

    /** 현재 단어 목록을 케이스 규칙에 맞게 조합한다. */
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

    /** 스네이크 표기를 카멜/파스칼 표기로 변환한다. */
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
