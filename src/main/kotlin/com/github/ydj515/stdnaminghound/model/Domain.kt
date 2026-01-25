package com.github.ydj515.stdnaminghound.model

data class Domain(
    val name: String,
    val dataType: String?,
    val length: Int?,
    val scale: Int?,
    val storageFormat: String?,
    val displayFormat: String?,
    val allowedValues: String?,
)
