package com.github.ydj515.stdnaminghound.sql

enum class DbDialect {
    POSTGRES,
    ORACLE,
    MYSQL,
    ;

    companion object {
        fun fromName(name: String?): DbDialect {
            return when (name?.lowercase()) {
                "oracle" -> ORACLE
                "mysql" -> MYSQL
                else -> POSTGRES
            }
        }
    }
}
