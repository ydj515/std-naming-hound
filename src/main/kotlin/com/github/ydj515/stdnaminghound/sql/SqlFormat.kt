package com.github.ydj515.stdnaminghound.sql

enum class SqlFormat(val displayName: String) {
    COLUMN_DEFINITION("컬럼 정의"),
    CREATE_TABLE("CREATE TABLE"),
    ;

    override fun toString(): String = displayName
}
