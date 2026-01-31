package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.DatasetCounts
import com.github.ydj515.stdnaminghound.model.DatasetMeta
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.Word
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** 커스텀 JSON을 Dataset 구성 요소로 파싱한다. */
class DatasetJsonParser {
    /** 메타 정보를 파싱한다. */
    fun parseMeta(obj: JsonObject): DatasetMeta {
        val countsObj = obj.getAsJsonObject("counts")
        val counts = if (countsObj == null) {
            null
        } else {
            DatasetCounts(
                terms = countsObj.intOrZero("terms"),
                words = countsObj.intOrZero("words"),
                domains = countsObj.intOrZero("domains"),
            )
        }
        return DatasetMeta(
            datasetVersion = obj.stringOrNull("dataset_version"),
            source = obj.stringOrNull("source"),
            generatedAt = obj.stringOrNull("generated_at"),
            counts = counts,
        )
    }

    /** Term 배열을 파싱한다. */
    fun parseTerms(array: JsonArray): List<Term> {
        return array.mapNotNull { element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
            val koName = obj.stringOrEmpty("koName")
            if (koName.isBlank()) return@mapNotNull null
            Term(
                koName = koName,
                abbr = obj.stringOrNull("abbr"),
                description = obj.stringOrNull("description"),
                domainName = obj.stringOrNull("domainName"),
                synonyms = obj.stringList("synonyms"),
            )
        }
    }

    /** Word 배열을 파싱한다. */
    fun parseWords(array: JsonArray): List<Word> {
        return array.mapNotNull { element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
            val koName = obj.stringOrEmpty("koName")
            if (koName.isBlank()) return@mapNotNull null
            Word(
                koName = koName,
                enName = obj.stringOrNull("enName"),
                abbr = obj.stringOrNull("abbr"),
                description = obj.stringOrNull("description"),
                synonyms = obj.stringList("synonyms"),
                isFormWord = obj.booleanOrFalse("isFormWord"),
            )
        }
    }

    /** Domain 배열을 파싱한다. */
    fun parseDomains(array: JsonArray): List<Domain> {
        return array.mapNotNull { element ->
            val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
            val name = obj.stringOrEmpty("name")
            if (name.isBlank()) return@mapNotNull null
            Domain(
                name = name,
                dataType = obj.stringOrNull("dataType"),
                length = obj.intOrNull("length"),
                scale = obj.intOrNull("scale"),
                storageFormat = obj.stringOrNull("storageFormat"),
                displayFormat = obj.stringOrNull("displayFormat"),
                allowedValues = obj.stringOrNull("allowedValues"),
            )
        }
    }

    /** JsonObject에서 문자열을 안전하게 읽는다. */
    private fun JsonObject.stringOrNull(name: String): String? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        if (element.isJsonPrimitive) return element.asString
        return element.toString()
    }

    /** JsonObject에서 문자열을 읽되 없으면 빈 문자열을 반환한다. */
    private fun JsonObject.stringOrEmpty(name: String): String {
        val value = stringOrNull(name)
        return value ?: ""
    }

    /** JsonObject에서 문자열 배열을 읽는다. */
    private fun JsonObject.stringList(name: String): List<String> {
        val element = get(name) ?: return emptyList()
        if (!element.isJsonArray) return emptyList()
        return element.asJsonArray.mapNotNull { item ->
            if (item.isJsonPrimitive) item.asString else null
        }
    }

    /** JsonObject에서 불리언을 읽되 없으면 false를 반환한다. */
    private fun JsonObject.booleanOrFalse(name: String): Boolean {
        val element = get(name) ?: return false
        if (!element.isJsonPrimitive) return false
        return element.asBoolean
    }

    /** JsonObject에서 정수를 읽되 없으면 null을 반환한다. */
    private fun JsonObject.intOrNull(name: String): Int? {
        val element = get(name) ?: return null
        return element.toIntOrNull()
    }

    /** JsonObject에서 정수를 읽되 없으면 0을 반환한다. */
    private fun JsonObject.intOrZero(name: String): Int {
        return intOrNull(name) ?: 0
    }

    /** JsonElement를 정수로 변환한다. */
    private fun JsonElement.toIntOrNull(): Int? {
        if (isJsonNull) return null
        if (!isJsonPrimitive) return null
        val primitive = asJsonPrimitive
        return if (primitive.isNumber) {
            primitive.asNumber.toInt()
        } else {
            primitive.asString.toIntOrNull()
        }
    }

    /** JsonElement가 객체이면 JsonObject로 반환한다. */
    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        if (!isJsonObject) return null
        return asJsonObject
    }

}
