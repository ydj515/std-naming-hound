package com.github.ydj515.stdnaminghound

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.StdNamingHoundBundle"

/** 번들 메시지 조회를 위한 헬퍼 객체다. */
object StdNamingHoundBundle : DynamicBundle(BUNDLE) {

    /** 키와 파라미터로 즉시 메시지를 반환한다. */
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    /** 지연 평가되는 메시지 포인터를 반환한다. */
    @Suppress("unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
