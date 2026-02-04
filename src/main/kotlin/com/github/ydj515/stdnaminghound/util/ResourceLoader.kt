package com.github.ydj515.stdnaminghound.util

private object ResourceLoader

/** 클래스패스 리소스를 읽는 공통 유틸리티다. */
fun readResourceBytes(path: String): ByteArray {
    val stream = ResourceLoader::class.java.classLoader.getResourceAsStream(path)
        ?: throw IllegalStateException("리소스를 찾을 수 없습니다: $path")
    return stream.use { it.readBytes() }
}

/** 클래스패스 리소스를 UTF-8 텍스트로 읽는다. */
fun readResourceText(path: String): String {
    return readResourceBytes(path).toString(Charsets.UTF_8)
}
