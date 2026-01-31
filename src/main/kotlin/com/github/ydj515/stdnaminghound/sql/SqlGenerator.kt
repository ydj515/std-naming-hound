package com.github.ydj515.stdnaminghound.sql

import com.github.ydj515.stdnaminghound.model.Domain

/** 도메인과 설명을 기반으로 SQL 문자열을 생성한다. */
class SqlGenerator {
    /** SQL 결과와 경고 메시지를 함께 전달한다. */
    data class SqlResult(
        val sql: String,
        val warning: String? = null,
    )

    /** 컬럼 정의 혹은 CREATE TABLE 형식의 SQL을 생성한다. */
    fun generateColumnSql(
        columnName: String,
        domain: Domain?,
        description: String?,
        dialect: DbDialect,
        format: SqlFormat,
        tableName: String = "TABLE_NAME",
    ): SqlResult {
        if (domain == null) {
            return SqlResult(
                sql = "",
                warning = "도메인이 없어서 SQL 타입을 생성할 수 없습니다.",
            )
        }
        val type = toColumnType(domain, dialect)
        val comment = description?.trim().orEmpty()
        val definition = buildColumnDefinition(columnName, type, comment, dialect)
        return when (format) {
            SqlFormat.COLUMN_DEFINITION -> {
                val sql = buildColumnOnlySql(definition, comment, dialect, tableName, columnName)
                SqlResult(sql)
            }
            SqlFormat.CREATE_TABLE -> {
                val sql = buildCreateTableSql(definition, comment, dialect, tableName, columnName)
                SqlResult(sql)
            }
        }
    }

    /** 단일 컬럼 정의 SQL을 생성한다. */
    private fun buildColumnOnlySql(
        definition: String,
        comment: String,
        dialect: DbDialect,
        tableName: String,
        columnName: String,
    ): String {
        if (dialect == DbDialect.MYSQL) {
            return definition
        }
        if (comment.isBlank()) {
            return definition
        }
        val commentSql = "COMMENT ON COLUMN $tableName.$columnName IS '${escapeSql(comment)}';"
        return "$definition;\n$commentSql"
    }

    /** CREATE TABLE SQL을 생성한다. */
    private fun buildCreateTableSql(
        definition: String,
        comment: String,
        dialect: DbDialect,
        tableName: String,
        columnName: String,
    ): String {
        val create = "CREATE TABLE $tableName (\n  $definition\n);"
        if (dialect == DbDialect.MYSQL || comment.isBlank()) {
            return create
        }
        val commentSql = "COMMENT ON COLUMN $tableName.$columnName IS '${escapeSql(comment)}';"
        return "$create\n$commentSql"
    }

    /** 컬럼 정의 문자열을 생성한다. */
    private fun buildColumnDefinition(
        columnName: String,
        type: String,
        comment: String,
        dialect: DbDialect,
    ): String {
        return if (dialect == DbDialect.MYSQL && comment.isNotBlank()) {
            "$columnName $type COMMENT '${escapeSql(comment)}'"
        } else {
            "$columnName $type"
        }
    }

    /** 도메인 정보를 방언에 맞는 타입 문자열로 변환한다. */
    private fun toColumnType(domain: Domain, dialect: DbDialect): String {
        val dataType = domain.dataType?.uppercase()?.trim().orEmpty()
        val length = domain.length
        val scale = domain.scale
        val suffix = when {
            length != null && scale != null -> "($length,$scale)"
            length != null -> "($length)"
            dataType.isBlank() -> "(255)"
            else -> ""
        }
        val effectiveType = if (dataType.isBlank()) "VARCHAR" else dataType
        return when (effectiveType) {
            "NUMERIC" -> when (dialect) {
                DbDialect.ORACLE -> "NUMBER$suffix"
                DbDialect.MYSQL -> "DECIMAL$suffix"
                else -> "NUMERIC$suffix"
            }
            "CHAR" -> "CHAR$suffix"
            "VARCHAR", "VARCHAR2" -> when (dialect) {
                DbDialect.ORACLE -> "VARCHAR2$suffix"
                else -> "VARCHAR$suffix"
            }
            else -> if (suffix.isNotBlank()) "$effectiveType$suffix" else effectiveType
        }
    }

    /** SQL 문자열에서 따옴표를 이스케이프한다. */
    private fun escapeSql(text: String): String = text.replace("'", "''")
}
