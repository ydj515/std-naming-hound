package com.github.ydj515.stdnaminghound.sql

import com.github.ydj515.stdnaminghound.model.Domain

class SqlGenerator {
    data class SqlResult(
        val sql: String,
        val warning: String? = null,
    )

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

    private fun toColumnType(domain: Domain, dialect: DbDialect): String {
        val dataType = domain.dataType?.uppercase()?.trim().orEmpty()
        val length = domain.length
        val scale = domain.scale
        val suffix = when {
            length != null && scale != null -> "($length,$scale)"
            length != null -> "($length)"
            else -> ""
        }
        return when (dataType) {
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
            else -> if (suffix.isNotBlank()) "$dataType$suffix" else dataType
        }
    }

    private fun escapeSql(text: String): String = text.replace("'", "''")
}
