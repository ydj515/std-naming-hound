package com.github.ydj515.stdnaminghound.settings

import com.github.ydj515.stdnaminghound.search.SearchIndexRepository
import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.github.ydj515.stdnaminghound.storage.MergePolicy
import com.intellij.openapi.application.ApplicationManager
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
import java.awt.BorderLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class StdNamingHoundConfigurable : Configurable {
    private val settings =
        ApplicationManager.getApplication().getService(StdNamingHoundSettings::class.java)
    private val datasetRepository =
        ApplicationManager.getApplication().getService(DatasetRepository::class.java)
    private val searchIndexRepository =
        ApplicationManager.getApplication().getService(SearchIndexRepository::class.java)
    private val validator = CustomDatasetValidator()

    private var root: JPanel? = null
    private val useCustomOnlyCheck = JBCheckBox("Use only custom data")
    private val enableFuzzyCheck = JBCheckBox("Enable fuzzy search")
    private val dbDialectCombo = JComboBox(arrayOf("Postgres", "Oracle", "MySQL"))
    private val mergePolicyCombo = JComboBox(MergePolicy.entries.toTypedArray()).apply {
        preferredSize = java.awt.Dimension(220, preferredSize.height)
    }
    private val customJsonArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
    }
    private val loadFileButton = javax.swing.JButton("JSON 파일 임포트")
    private val downloadSampleButton = javax.swing.JButton("샘플 JSON 다운로드")
    private val resetButton = javax.swing.JButton("Reset to Default")

    override fun createComponent(): JComponent {
        if (root == null) {
            root = JPanel(BorderLayout())
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
                row("Merge Policy") {
                    cell(mergePolicyCombo)
                }
                row {
                    val info = com.intellij.ui.components.JBLabel(
                        "<html>필수 필드: version. terms/words/domains는 비워둘 수 있습니다.<br/>" +
                                "병합 정책과 '커스텀 데이터만 사용' 옵션을 함께 확인하세요.</html>"
                    )
                    cell(info)
                }
                row("Preview") {
                    val scroll = JBScrollPane(customJsonArea)
                    scroll.preferredSize = java.awt.Dimension(600, 240)
                    cell(scroll)
                }
                row {
                    cell(loadFileButton)
                    cell(downloadSampleButton)
                    cell(resetButton)
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
            val content = sampleJson()
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

        return root!!
    }

    override fun isModified(): Boolean {
        val state = settings.state
        if (useCustomOnlyCheck.isSelected != state.useCustomOnly) return true
        if (enableFuzzyCheck.isSelected != state.enableFuzzy) return true
        if (dbDialectCombo.selectedItem as String != state.dbDialect) return true
        val selectedPolicy = (mergePolicyCombo.selectedItem as MergePolicy).name
        if (selectedPolicy != state.mergePolicy) return true
        val currentJson = customJsonArea.text.trim()
        val savedJson = state.customDatasetJson?.trim().orEmpty()
        return currentJson != savedJson
    }

    override fun apply() {
        val json = customJsonArea.text
        val result = validator.validate(json)
        if (!result.isValid) {
            throw ConfigurationException(result.errors.joinToString("\n"))
        }
        applySettings()
    }

    private fun applySettings() {
        val state = settings.state
        state.useCustomOnly = useCustomOnlyCheck.isSelected
        state.enableFuzzy = enableFuzzyCheck.isSelected
        state.dbDialect = dbDialectCombo.selectedItem as String
        state.mergePolicy = (mergePolicyCombo.selectedItem as MergePolicy).name
        state.customDatasetJson = customJsonArea.text.trim().ifBlank { null }
        datasetRepository.reload()
        searchIndexRepository.reload()
    }

    override fun reset() {
        val state = settings.state
        useCustomOnlyCheck.isSelected = state.useCustomOnly
        enableFuzzyCheck.isSelected = state.enableFuzzy
        dbDialectCombo.selectedItem = state.dbDialect
        mergePolicyCombo.selectedItem = MergePolicy.fromName(state.mergePolicy)
        customJsonArea.text = state.customDatasetJson.orEmpty()
    }

    override fun getDisplayName(): String = "Std Naming Hound"

    override fun disposeUIResources() {
        root = null
    }

    private fun sampleJson(): String {
        return """
            {
              "version": "2026.01",
              "meta": {
                "dataset_version": "2026.01",
                "source": "custom",
                "generated_at": "2026-01-24T12:00:00Z",
                "counts": { "terms": 1, "words": 1, "domains": 1 }
              },
              "terms": [
                {
                  "koName": "등록가능여부",
                  "abbr": "REG_PSBLTY_YN",
                  "description": "등록 가능한지 여부",
                  "domainName": "여부C1",
                  "synonyms": ["등록가능"]
                }
              ],
              "words": [
                {
                  "koName": "등록",
                  "enName": "Registration",
                  "abbr": "REG",
                  "description": "등록 행위",
                  "synonyms": [],
                  "isFormWord": false
                }
              ],
              "domains": [
                {
                  "name": "여부C1",
                  "dataType": "CHAR",
                  "length": 1,
                  "scale": null,
                  "storageFormat": "Y/N",
                  "displayFormat": "Y/N",
                  "allowedValues": "Y or N"
                }
              ]
            }
        """.trimIndent()
    }
}
