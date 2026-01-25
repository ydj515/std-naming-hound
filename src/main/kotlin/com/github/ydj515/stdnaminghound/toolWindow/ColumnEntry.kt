package com.github.ydj515.stdnaminghound.toolWindow

internal data class ColumnEntry(
    val name: String,
    val definition: String,
    val commentSql: String?,
    val domainName: String?,
    val description: String?,
)
