package com.github.ydj515.stdnaminghound.sql

/** SQL 출력 형식을 정의한다. */
enum class SqlFormat(val displayName: String) {
    COLUMN_DEFINITION("컬럼 정의"),
    CREATE_TABLE("CREATE TABLE"),
    ;

    /** UI 표시용 이름을 반환한다. */
    override fun toString(): String = displayName
}
