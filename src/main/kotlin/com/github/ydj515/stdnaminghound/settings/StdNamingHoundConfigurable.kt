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
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.io.FileOutputStream
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

/** 설정 UI를 구성하고 변경 사항을 적용한다. */
class StdNamingHoundConfigurable : Configurable {
    private val settings: StdNamingHoundSettings = ApplicationManager.getApplication().service()
    private val datasetRepository: DatasetRepository = ApplicationManager.getApplication().service()
    private val datasetExportService: DatasetExportService = ApplicationManager.getApplication().service()
    private val searchIndexRepository: SearchIndexRepository = ApplicationManager.getApplication().service()
    private val validator = CustomDatasetValidator()

    private var root: JPanel? = null
    private val useCustomOnlyCheck = JBCheckBox("Use only custom data")
    private val enableFuzzyCheck = JBCheckBox("Enable fuzzy search")
    private val dbDialectCombo = JComboBox(arrayOf("Postgres", "Oracle", "MySQL")).apply {
        minimumSize = java.awt.Dimension(220, minimumSize.height)
    }
    private val caseStyleCombo = JComboBox(WordBuilder.CaseStyle.entries.toTypedArray()).apply {
        minimumSize = java.awt.Dimension(220, minimumSize.height)
    }
    private val mergePolicyCombo = JComboBox(MergePolicy.entries.toTypedArray()).apply {
        minimumSize = java.awt.Dimension(220, minimumSize.height)
        preferredSize = java.awt.Dimension(220, preferredSize.height)
    }
    private val customJsonArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
    }
    private val loadFileButton = javax.swing.JButton("Import JSON")
    private val downloadSampleButton = javax.swing.JButton("Download Sample")
    private val resetButton = javax.swing.JButton("Reset to Default")
    private val exportBaseDataButton = javax.swing.JButton("Export Base Data")

    /** 설정 UI 컴포넌트를 생성한다. */
    override fun createComponent(): JComponent {
        if (root == null) {
            root = JPanel(BorderLayout())
            val buttonsPanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
                add(loadFileButton)
                add(downloadSampleButton)
                add(resetButton)
                add(exportBaseDataButton)
            }
            val content = panel {
                row {
                    cell(useCustomOnlyCheck)
                }
                row("DB Dialect") {
                    cell(dbDialectCombo)
                    val help =
                        com.intellij.ui.components.JBLabel(com.intellij.icons.AllIcons.General.ContextHelp).apply {
                            toolTipText = "SQL Generator에서 사용할 DB 유형 선택\n(Choose DB dialect to use in SQL Generator)"
                        }
                    cell(help)
                }
                row("Case Style") {
                    cell(caseStyleCombo)
                }
                row("Merge Policy") {
                    cell(mergePolicyCombo)
                }
                row {
                    val info = com.intellij.ui.components.JBLabel(
                        "<html>version is required, but terms/words/domains can be left blank.<br/>" +
                                "Check the 'Merge Policy' and 'Use only custom data' options together.</html>"
                    )
                    cell(info)
                }
                row("Preview") {
                    val scroll = JBScrollPane(customJsonArea)
                    scroll.preferredSize = java.awt.Dimension(475, 240)
                    cell(scroll)
                }
                row {
                    cell(buttonsPanel)
                }
            }
            root?.add(content, BorderLayout.CENTER)
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

        downloadSampleButton.addActionListener {
            val descriptor = FileSaverDescriptor("Download Sample JSON ", "샘플 커스텀 JSON 파일을 저장합니다.", "json")
            val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
            val wrapper =
                dialog.save(null as java.nio.file.Path?, "std-naming-hound.sample.json") ?: return@addActionListener
            val target = wrapper.file?.toPath()?.toFile()
            val virtualFile = wrapper.virtualFile
            val content = readResourceText("data/sample.json")
            when {
                virtualFile != null -> VfsUtil.saveText(virtualFile, content)
                target != null -> target.writeText(content)
                else -> return@addActionListener
            }
            Messages.showInfoMessage("샘플 JSON이 저장되었습니다.", "저장 완료")
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
        if (enableFuzzyCheck.isSelected != state.enableFuzzy) return true
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
        state.enableFuzzy = enableFuzzyCheck.isSelected
        state.dbDialect = dbDialectCombo.selectedItem as String
        state.mergePolicy = (mergePolicyCombo.selectedItem as MergePolicy).name
        state.defaultCaseStyle = (caseStyleCombo.selectedItem as WordBuilder.CaseStyle).name
        state.customDatasetJson = customJsonArea.text.trim().ifBlank { null }
        datasetRepository.reload()
        searchIndexRepository.reload()
        ApplicationManager.getApplication()
            .messageBus
            .syncPublisher(StdNamingHoundSettings.TOPIC)
            .settingsChanged(state)
    }

    /** 저장된 설정을 UI에 다시 반영한다. */
    override fun reset() {
        val state = settings.state
        useCustomOnlyCheck.isSelected = state.useCustomOnly
        enableFuzzyCheck.isSelected = state.enableFuzzy
        dbDialectCombo.selectedItem = state.dbDialect
        mergePolicyCombo.selectedItem = MergePolicy.fromName(state.mergePolicy)
        caseStyleCombo.selectedItem = runCatching {
            WordBuilder.CaseStyle.valueOf(state.defaultCaseStyle)
        }.getOrDefault(WordBuilder.CaseStyle.SNAKE_UPPER)
        customJsonArea.text = state.customDatasetJson.orEmpty()
    }

    /** 설정 화면에 표시될 이름을 반환한다. */
    override fun getDisplayName(): String = "Std Naming Hound"

    /** UI 리소스를 해제한다. */
    override fun disposeUIResources() {
        root = null
    }

    private fun exportDatasetZipWithChoice() {
        val choice = Messages.showDialog(
            null,
            "Choose the data to export.\nOriginal (base resources) / Merged (final dataset).",
            "Export Dataset",
            arrayOf("Merged", "Original", "Cancel"),
            0,
            null
        )
        if (choice == 2 || choice == -1) return

        val descriptor = FileSaverDescriptor("Export Dataset", "Save the selected dataset as a ZIP file.", "zip")
        val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, null)
        val wrapper = dialog.save(null as java.nio.file.Path?, "std-naming-hound-base-data.zip") ?: return
        try {
            val virtualFile = wrapper.virtualFile
            val targetFile = wrapper.file?.toPath()?.toFile()
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
            val message = if (choice == 0) {
                "The merged dataset has been saved as a ZIP file."
            } else {
                "The base dataset has been saved as a ZIP file."
            }
            Messages.showInfoMessage(message, "Export Complete")
        } catch (e: Exception) {
            Messages.showErrorDialog("Failed to save ZIP: ${e.message}", "Export Failed")
        }
    }

    private fun writeZip(stream: java.io.OutputStream, choice: Int) {
        if (choice == 0) {
            datasetExportService.writeMergedDatasetZip(stream)
        } else {
            datasetExportService.writeBaseDatasetZip(stream)
        }
    }

    private fun readResourceText(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: throw IllegalStateException("리소스를 찾을 수 없습니다: $path")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
