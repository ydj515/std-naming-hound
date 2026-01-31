package com.github.ydj515.stdnaminghound.sql

/** SQL 생성 시 사용할 DB를 정의한다. */
enum class DbDialect {
    POSTGRES,
    ORACLE,
    MYSQL,
    ;

    companion object {
        /** 설정 문자열을 enum으로 변환한다. */
        fun fromName(name: String?): DbDialect {
            return when (name?.lowercase()) {
                "oracle" -> ORACLE
                "mysql" -> MYSQL
                else -> POSTGRES
            }
        }
    }
}
