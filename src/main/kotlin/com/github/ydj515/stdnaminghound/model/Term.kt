package com.github.ydj515.stdnaminghound.model

data class Term(
    val koName: String,
    val abbr: String?,
    val description: String?,
    val domainName: String?,
    val synonyms: List<String>,
)
