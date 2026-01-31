package com.github.ydj515.stdnaminghound.model

/** 단어(Word) 정보를 표현한다. */
data class Word(
    val koName: String,
    val enName: String?,
    val abbr: String?,
    val description: String?,
    val synonyms: List<String>,
    val isFormWord: Boolean,
)
