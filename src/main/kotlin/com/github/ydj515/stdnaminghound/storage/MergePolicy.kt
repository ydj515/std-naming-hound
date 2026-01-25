package com.github.ydj515.stdnaminghound.storage

enum class MergePolicy(val displayName: String) {
    CUSTOM_FIRST("Custom First"),
    BASE_FIRST("Default First"),
    DEDUPE_BASE("Remove duplicates (keep default)"),
    ;

    override fun toString(): String = displayName

    companion object {
        fun fromName(name: String?): MergePolicy {
            return entries.firstOrNull { it.name == name } ?: CUSTOM_FIRST
        }
    }
}
