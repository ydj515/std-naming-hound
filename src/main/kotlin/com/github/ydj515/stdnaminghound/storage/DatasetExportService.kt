package com.github.ydj515.stdnaminghound.storage

import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.github.ydj515.stdnaminghound.util.readResourceBytes
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 데이터셋을 ZIP으로 내보내는 책임을 담당한다. */
@Service(Service.Level.APP)
class DatasetExportService {
    private companion object {
        const val DOMAINS_RESOURCE = "data/domains.json"
        const val TERMS_RESOURCE = "data/terms.json"
        const val WORDS_RESOURCE = "data/words.json"
        const val DOMAINS_FILE = "domains.json"
        const val TERMS_FILE = "terms.json"
        const val WORDS_FILE = "words.json"
    }
    private val datasetRepository = service<DatasetRepository>()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun writeBaseDatasetZip(stream: OutputStream) {
        ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
            val resources = listOf(
                DOMAINS_RESOURCE,
                TERMS_RESOURCE,
                WORDS_RESOURCE,
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
            writeZipEntry(zip, TERMS_FILE, gson.toJson(dataset.terms).toByteArray(StandardCharsets.UTF_8))
            writeZipEntry(zip, WORDS_FILE, gson.toJson(dataset.words).toByteArray(StandardCharsets.UTF_8))
            writeZipEntry(zip, DOMAINS_FILE, gson.toJson(dataset.domains).toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }
}
