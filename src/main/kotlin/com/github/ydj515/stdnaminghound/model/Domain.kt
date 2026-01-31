package com.github.ydj515.stdnaminghound.model

/** 도메인(Domain) 정보를 표현한다. */
data class Domain(
    val name: String,
    val dataType: String?,
    val length: Int?,
    val scale: Int?,
    val storageFormat: String?,
    val displayFormat: String?,
    val allowedValues: String?,
)
