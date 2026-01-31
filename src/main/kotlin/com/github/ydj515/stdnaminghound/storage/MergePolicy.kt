package com.github.ydj515.stdnaminghound.storage

/** 커스텀/기본 데이터 병합 정책을 정의한다. */
enum class MergePolicy(val displayName: String) {
    CUSTOM_FIRST("Custom First"),
    BASE_FIRST("Default First"),
    DEDUPE_BASE("Remove duplicates (keep default)"),
    ;

    /** UI 표시용 이름을 반환한다. */
    override fun toString(): String = displayName

    companion object {
        /** 설정 문자열을 정책 enum으로 변환한다. */
        fun fromName(name: String?): MergePolicy {
            return entries.firstOrNull { it.name == name } ?: CUSTOM_FIRST
        }
    }
}
