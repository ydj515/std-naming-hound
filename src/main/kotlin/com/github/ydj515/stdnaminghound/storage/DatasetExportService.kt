package com.github.ydj515.stdnaminghound.storage

import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 데이터셋을 ZIP으로 내보내는 책임을 담당한다. */
@Service(Service.Level.APP)
class DatasetExportService {
    private val datasetRepository = service<DatasetRepository>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun writeBaseDatasetZip(stream: OutputStream) {
        ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
            val resources = listOf(
                "data/domains.json",
                "data/terms.json",
                "data/words.json",
            )
            for (path in resources) {
                val bytes = readResourceBytes(path)
                val entryName = path.substringAfterLast('/')
                writeZipEntry(zip, entryName, bytes)
            }
        }
    }

    fun writeMergedDatasetZip(stream: OutputStream) {
        ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
            val dataset = datasetRepository.getDataset()
            writeZipEntry(zip, "terms.json", gson.toJson(dataset.terms).toByteArray(StandardCharsets.UTF_8))
            writeZipEntry(zip, "words.json", gson.toJson(dataset.words).toByteArray(StandardCharsets.UTF_8))
            writeZipEntry(zip, "domains.json", gson.toJson(dataset.domains).toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun readResourceBytes(path: String): ByteArray {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("리소스를 찾을 수 없습니다: $path")
        return stream.use { it.readBytes() }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }
}
