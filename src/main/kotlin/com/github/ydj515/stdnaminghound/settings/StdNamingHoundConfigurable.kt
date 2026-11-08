package com.github.ydj515.stdnaminghound.settings

import com.github.ydj515.stdnaminghound.search.SearchIndexRepository
import com.github.ydj515.stdnaminghound.storage.DatasetExportService
import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.github.ydj515.stdnaminghound.storage.MergePolicy
import com.github.ydj515.stdnaminghound.builder.WordBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.github.ydj515.stdnaminghound.util.readResourceText
import com.intellij.icons.AllIcons.General.ContextHelp
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.FileOutputStream
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

/** 설정 UI를 구성하고 변경 사항을 적용한다. */
class StdNamingHoundConfigurable : Configurable {
    private companion object {
        const val MERGED_CHOICE = 0
        const val ORIGINAL_CHOICE = 1
        const val CANCEL_CHOICE = 2
        private const val VERSION_PREFIX = "Your version is v"
        private const val UNCONFIRMED_VERSION_TEXT = "Your version is not confirmed"
        private const val SAMPLE_JSON_FILENAME = "std-naming-hound.sample.json"
        private const val BASE_DATA_ZIP_FILENAME = "std-naming-hound-base-data.zip"
        private const val SAMPLE_JSON_RESOURCE_PATH = "data/sample.json"
        private const val DOWNLOAD_SAMPLE_TITLE = "Download Sample JSON"
        private const val DOWNLOAD_SAMPLE_DESCRIPTION = "Saves a sample custom JSON file."
        private const val EXPORT_DATASET_TITLE = "Export Dataset"
        private const val EXPORT_DATASET_DESCRIPTION = "Save the selected dataset as a ZIP file."
        private const val EXPORT_MESSAGE_MERGED = "The merged dataset has been saved as a ZIP file."
        private const val EXPORT_MESSAGE_BASE = "The base dataset has been saved as a ZIP file."
        private const val EXPORT_FAILED_MESSAGE_PREFIX = "Failed to save ZIP: "
        private const val DATASET_LICENSE = "이용허락범위 제한 없음"
        private const val TERMS_URL = "https://www.data.go.kr/data/15156379/fileData.do"
        private const val WORDS_URL = "https://www.data.go.kr/data/15156439/fileData.do"
        private const val DOMAINS_URL = "https://www.data.go.kr/data/15156442/fileData.do"
    }

    private val settings: StdNamingHoundSettings = ApplicationManager.getApplication().service()
    private val datasetRepository: DatasetRepository = ApplicationManager.getApplication().service()
    private val datasetExportService: DatasetExportService = ApplicationManager.getApplication().service()
    private val searchIndexRepository: SearchIndexRepository = ApplicationManager.getApplication().service()
    private val validator = CustomDatasetValidator()

    private var root: JPanel? = null
    private val useCustomOnlyCheck = JBCheckBox("Use only custom data")
    private val datasetInfoLabel = JBLabel()
    private val datasetInfoHelp = JBLabel(com.intellij.icons.AllIcons.General.ContextHelp)
    private val datasetAboutLabel = JBLabel().apply {
        foreground = UIUtil.getContextHelpForeground()
    }
    private val dbDialectCombo = ComboBox(arrayOf("Postgres", "Oracle", "MySQL")).apply {
        minimumSize = JBUI.size(220, minimumSize.height)
    }
    private val caseStyleCombo = ComboBox(WordBuilder.CaseStyle.entries.toTypedArray()).apply {
        minimumSize = JBUI.size(220, minimumSize.height)
    }
    private val mergePolicyCombo = ComboBox(MergePolicy.entries.toTypedArray()).apply {
        minimumSize = JBUI.size(220, minimumSize.height)
        preferredSize = JBUI.size(220, preferredSize.height)
    }
    private val customJsonArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
    }
    private val loadFileButton = JButton("Import JSON")
    private val resetButton = JButton("Reset")
    private val exportBaseDataButton = JButton("Export Base Data")
    private val downloadSampleLink = HyperlinkLabel("Sample download here").apply {
        border = JBUI.Borders.empty(0, 0, JBUI.scale(10), 0)
        addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                downloadSample()
            }
        }
    }

    /** 설정 UI 컴포넌트를 생성한다. */
    override fun createComponent(): JComponent {
        if (root == null) {
            root = com.intellij.ui.components.JBPanel<com.intellij.ui.components.JBPanel<*>>(BorderLayout())
            val buttonsPanel = com.intellij.ui.components.JBPanel<com.intellij.ui.components.JBPanel<*>>(
                FlowLayout(FlowLayout.LEFT, 0, 0)
            ).apply {
                border = JBUI.Borders.empty(JBUI.scale(6), 0, JBUI.scale(6), 0)
                add(loadFileButton)
                add(Box.createHorizontalStrut(JBUI.scale(8)))
                add(exportBaseDataButton)
                add(Box.createHorizontalStrut(JBUI.scale(8)))
                add(resetButton)
            }

            val content = panel {
                group("Environment") {
                    row("DB Dialect") {
                        cell(dbDialectCombo)
                        val help =
                            JBLabel(ContextHelp).apply {
                                toolTipText = "Choose DB dialect to use in SQL Generator"
                            }
                        cell(help)
                    }
                    row("Case Style") {
                        cell(caseStyleCombo)
                    }
                    row("Merge Policy") {
                        cell(mergePolicyCombo)
                    }
                }
                group("Merge data") {
                    row {
                        cell(useCustomOnlyCheck)
                    }
                    row {
                        cell(datasetInfoLabel)
                        cell(datasetInfoHelp)
                    }
                    row {
                        val info = JBLabel(
                            "<html>version is required, but terms/words/domains can be left blank.<br/>" +
                                    "Check the 'Merge Policy' and 'Use only custom data' options together.</html>"
                        ).apply {
                            foreground = UIUtil.getContextHelpForeground()
                            border = JBUI.Borders.empty(0, 0, JBUI.scale(10), 0)
                        }
                        cell(info)
                    }
                    row {
                        cell(downloadSampleLink)
                    }.layout(RowLayout.PARENT_GRID)
                    row {
                        cell(buttonsPanel).align(AlignX.LEFT)
                    }.layout(RowLayout.PARENT_GRID)
                    row("Preview") {
                        val scroll = JBScrollPane(customJsonArea)
                        scroll.preferredSize = JBUI.size(475, 240)
                        cell(scroll)
                    }
                }
                group("About") {
                    row {
                        cell(datasetAboutLabel).align(AlignX.FILL)
                    }.layout(RowLayout.PARENT_GRID)
                }
            }
            root?.add(content, BorderLayout.CENTER)
            updateDatasetInfoLabel()
        }

        loadFileButton.addActionListener {
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("커스텀 JSON 파일 선택")
                .withDescription("version 필드를 포함한 JSON 파일을 선택하세요.")
            val file = FileChooser.chooseFile(descriptor, null, null) ?: return@addActionListener
            val text = VfsUtilCore.loadText(file)
            val result = validator.validate(text)
            if (!result.isValid) {
                Messages.showErrorDialog(
                    result.errors.joinToString("\n"),
                    "커스텀 JSON 검증 실패",
                )
                return@addActionListener
            }
            customJsonArea.text = text
            applySettings()
            Messages.showInfoMessage("커스텀 JSON이 적용되었습니다.", "Import 완료")
        }

        resetButton.addActionListener {
            customJsonArea.text = ""
        }

        exportBaseDataButton.addActionListener {
            exportDatasetZipWithChoice()
        }

        return root!!
    }

    /** UI 값이 저장된 설정과 다른지 판단한다. */
    override fun isModified(): Boolean {
        val state = settings.state
        if (useCustomOnlyCheck.isSelected != state.useCustomOnly) return true
        if (dbDialectCombo.selectedItem as String != state.dbDialect) return true
        val selectedPolicy = (mergePolicyCombo.selectedItem as MergePolicy).name
        if (selectedPolicy != state.mergePolicy) return true
        val selectedCaseStyle = (caseStyleCombo.selectedItem as WordBuilder.CaseStyle).name
        if (selectedCaseStyle != state.defaultCaseStyle) return true
        val currentJson = customJsonArea.text.trim()
        val savedJson = state.customDatasetJson?.trim().orEmpty()
        return currentJson != savedJson
    }

    /** UI 값을 검증한 뒤 설정에 반영한다. */
    override fun apply() {
        val json = customJsonArea.text
        val result = validator.validate(json)
        if (!result.isValid) {
            throw ConfigurationException(result.errors.joinToString("\n"))
        }
        applySettings()
    }

    /** UI 값을 설정에 저장하고 리포지토리를 갱신한다. */
    private fun applySettings() {
        val state = settings.state
        state.useCustomOnly = useCustomOnlyCheck.isSelected
        state.dbDialect = dbDialectCombo.selectedItem as String
        state.mergePolicy = (mergePolicyCombo.selectedItem as MergePolicy).name
        state.defaultCaseStyle = (caseStyleCombo.selectedItem as WordBuilder.CaseStyle).name
        state.customDatasetJson = customJsonArea.text.trim().ifBlank { null }
        datasetRepository.reload()
        searchIndexRepository.reload()
        updateDatasetInfoLabel()
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(StdNamingHoundSettings.TOPIC)
            .settingsChanged(state)
    }

    /** 저장된 설정을 UI에 다시 반영한다. */
    override fun reset() {
        val state = settings.state
        useCustomOnlyCheck.isSelected = state.useCustomOnly
        dbDialectCombo.selectedItem = state.dbDialect
        mergePolicyCombo.selectedItem = MergePolicy.fromName(state.mergePolicy)
        caseStyleCombo.selectedItem = runCatching {
            WordBuilder.CaseStyle.valueOf(state.defaultCaseStyle)
        }.getOrDefault(WordBuilder.CaseStyle.SNAKE_UPPER)
        customJsonArea.text = state.customDatasetJson.orEmpty()
        updateDatasetInfoLabel()
    }

    /** 설정 화면에 표시될 이름을 반환한다. */
    override fun getDisplayName(): String = "Std Naming Hound"

    /** UI 리소스를 해제한다. */
    override fun disposeUIResources() {
        root = null
    }

    /** 데이터셋 버전 라벨과 도움말 툴팁을 갱신한다. */
    private fun updateDatasetInfoLabel() {
        val dataset = datasetRepository.getDataset()
        val meta = dataset.meta
        val versionValue = meta?.datasetVersion?.trim().orEmpty()
        datasetInfoLabel.text = if (versionValue.isNotBlank()) {
            "$VERSION_PREFIX$versionValue"
        } else {
            UNCONFIRMED_VERSION_TEXT
        }
        val indexSize = searchIndexRepository.getIndex().entries.size
        datasetInfoHelp.toolTipText =
            "terms: ${dataset.terms.size}, words: ${dataset.words.size}, domain: ${dataset.domains.size}, index: $indexSize"
        datasetAboutLabel.text = buildDatasetAboutText(meta)
    }

    /** 내장 데이터셋의 출처와 이용허락 정보를 HTML 라벨로 구성한다. */
    private fun buildDatasetAboutText(meta: com.github.ydj515.stdnaminghound.model.DatasetMeta?): String {
        val source = escapeHtml(meta?.source?.takeIf { it.isNotBlank() } ?: "행정안전부 공공데이터 공통표준")
        val generatedAt = escapeHtml(meta?.generatedAt?.takeIf { it.isNotBlank() } ?: "unknown")
        return "<html>" +
                "Built-in dataset source: $source<br/>" +
                "Provider: 행정안전부 / 공공데이터포털<br/>" +
                "License: $DATASET_LICENSE<br/>" +
                "Official data: <a href=\"$TERMS_URL\">terms</a>, " +
                "<a href=\"$WORDS_URL\">words</a>, " +
                "<a href=\"$DOMAINS_URL\">domains</a><br/>" +
                "Generated at: $generatedAt" +
                "</html>"
    }

    /** HTML 라벨에 넣을 텍스트를 이스케이프한다. */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    /** 샘플 JSON을 저장할 위치를 선택받아 저장한다. */
    private fun downloadSample() {
        val descriptor = FileSaverDescriptor(DOWNLOAD_SAMPLE_TITLE, DOWNLOAD_SAMPLE_DESCRIPTION, "json")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
        val wrapper = dialog.save(null as java.nio.file.Path?, SAMPLE_JSON_FILENAME) ?: return
        val target = wrapper.file.toPath().toFile()
        val virtualFile = wrapper.virtualFile
        val content = readResourceText(SAMPLE_JSON_RESOURCE_PATH)
        when {
            virtualFile != null -> VfsUtil.saveText(virtualFile, content)
            target != null -> target.writeText(content)
            else -> return
        }
        Messages.showInfoMessage("The sample JSON has been saved.", "Save Complete")
    }

    /** 내보낼 데이터셋 종류를 선택받아 ZIP으로 저장한다. */
    private fun exportDatasetZipWithChoice() {
        val choice = Messages.showDialog(
            null,
            "Choose the data to export.\nOriginal (base resources) / Merged (final dataset).",
            EXPORT_DATASET_TITLE,
            arrayOf("Merged", "Original", "Cancel"),
            0,
            null
        )
        if (choice == CANCEL_CHOICE || choice == -1) return

        val descriptor = FileSaverDescriptor(EXPORT_DATASET_TITLE, EXPORT_DATASET_DESCRIPTION, "zip")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
        val wrapper = dialog.save(null as java.nio.file.Path?, BASE_DATA_ZIP_FILENAME) ?: return
        try {
            val virtualFile = wrapper.virtualFile
            val targetFile = wrapper.file.toPath().toFile()
            when {
                virtualFile != null -> {
                    ApplicationManager.getApplication().runWriteAction {
                        virtualFile.getOutputStream(this).use { stream ->
                            writeZip(stream, choice)
                        }
                    }
                }

                targetFile != null -> {
                    FileOutputStream(targetFile).use { stream ->
                        writeZip(stream, choice)
                    }
                }

                else -> return
            }
            val message = if (choice == MERGED_CHOICE) {
                EXPORT_MESSAGE_MERGED
            } else {
                EXPORT_MESSAGE_BASE
            }
            Messages.showInfoMessage(message, "Export Complete")
        } catch (e: Exception) {
            Messages.showErrorDialog("${EXPORT_FAILED_MESSAGE_PREFIX}${e.message}", "Export Failed")
        }
    }

    /** 선택된 데이터셋 종류에 맞는 ZIP을 출력한다. */
    private fun writeZip(stream: java.io.OutputStream, choice: Int) {
        if (choice == MERGED_CHOICE) {
            datasetExportService.writeMergedDatasetZip(stream)
        } else {
            datasetExportService.writeBaseDatasetZip(stream)
        }
    }

}
