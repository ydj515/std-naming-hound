package com.github.ydj515.stdnaminghound.model

/** 용어(Term) 정보를 표현한다. */
data class Term(
    val koName: String,
    val abbr: String?,
    val description: String?,
    val domainName: String?,
    val synonyms: List<String>,
)
