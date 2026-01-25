package com.github.ydj515.stdnaminghound.model

data class Word(
    val koName: String,
    val enName: String?,
    val abbr: String?,
    val description: String?,
    val synonyms: List<String>,
    val isFormWord: Boolean,
)
