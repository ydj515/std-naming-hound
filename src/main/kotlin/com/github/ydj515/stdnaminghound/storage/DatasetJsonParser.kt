package com.github.ydj515.stdnaminghound.storage

import com.github.ydj515.stdnaminghound.model.DatasetCounts
import com.github.ydj515.stdnaminghound.model.DatasetMeta
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.Word
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class DatasetJsonParser {
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

    private fun JsonObject.stringOrNull(name: String): String? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        if (element.isJsonPrimitive) return element.asString
        return element.toString()
    }

    private fun JsonObject.stringOrEmpty(name: String): String {
        val value = stringOrNull(name)
        return value ?: ""
    }

    private fun JsonObject.stringList(name: String): List<String> {
        val element = get(name) ?: return emptyList()
        if (!element.isJsonArray) return emptyList()
        return element.asJsonArray.mapNotNull { item ->
            if (item.isJsonPrimitive) item.asString else null
        }
    }

    private fun JsonObject.booleanOrFalse(name: String): Boolean {
        val element = get(name) ?: return false
        if (!element.isJsonPrimitive) return false
        return element.asBoolean
    }

    private fun JsonObject.intOrNull(name: String): Int? {
        val element = get(name) ?: return null
        return element.toIntOrNull()
    }

    private fun JsonObject.intOrZero(name: String): Int {
        return intOrNull(name) ?: 0
    }

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

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        if (!isJsonObject) return null
        return asJsonObject
    }

}
