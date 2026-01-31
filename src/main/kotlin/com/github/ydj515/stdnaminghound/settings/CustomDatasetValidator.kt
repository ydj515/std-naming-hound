package com.github.ydj515.stdnaminghound.settings

import com.google.gson.JsonElement
import com.google.gson.JsonParser

/** 커스텀 JSON의 구조를 간단히 검증한다. */
class CustomDatasetValidator {
    /** 검증 결과와 오류 메시지를 전달한다. */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>,
    )

    /** JSON 문자열을 검사하고 결과를 반환한다. */
    fun validate(json: String): ValidationResult {
        if (json.isBlank()) {
            return ValidationResult(isValid = true, errors = emptyList())
        }
        return try {
            val root = JsonParser.parseString(json)
            if (!root.isJsonObject) {
                return ValidationResult(false, listOf("루트는 JSON 객체여야 합니다."))
            }
            val obj = root.asJsonObject
            val version = obj.get("version")?.asString?.trim()
            if (version.isNullOrBlank()) {
                return ValidationResult(false, listOf("필수 필드 version이 없습니다."))
            }
            val errors = mutableListOf<String>()
            validateArray(obj.get("terms"), "terms", "koName", errors)
            validateArray(obj.get("words"), "words", "koName", errors)
            validateArray(obj.get("domains"), "domains", "name", errors)
            ValidationResult(errors.isEmpty(), errors)
        } catch (e: Exception) {
            ValidationResult(false, listOf("JSON 파싱 오류: ${e.message}"))
        }
    }

    /** 배열 요소가 객체이며 필수 키를 갖는지 검사한다. */
    private fun validateArray(element: JsonElement?, field: String, requiredKey: String, errors: MutableList<String>) {
        if (element == null || element.isJsonNull) return
        if (!element.isJsonArray) {
            errors.add("$field 필드는 배열이어야 합니다.")
            return
        }
        element.asJsonArray.forEachIndexed { index, item ->
            if (!item.isJsonObject) {
                errors.add("$field[$index]는 객체여야 합니다.")
                return@forEachIndexed
            }
            val obj = item.asJsonObject
            val value = obj.get(requiredKey)?.asString?.trim()
            if (value.isNullOrBlank()) {
                errors.add("$field[$index]에 필수 필드 $requiredKey 가 없습니다.")
            }
        }
    }
}
