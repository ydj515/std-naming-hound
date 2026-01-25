package com.github.ydj515.stdnaminghound.settings

import com.github.ydj515.stdnaminghound.search.SearchIndexRepository
import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class StdNamingHoundConfigurable : Configurable {
    private val settings = service<StdNamingHoundSettings>()
    private val datasetRepository = service<DatasetRepository>()
    private val searchIndexRepository = service<SearchIndexRepository>()
    private val validator = CustomDatasetValidator()

    private var root: JPanel? = null
    private val useCustomOnlyCheck = JBCheckBox("커스텀 데이터만 사용")
    private val enableFuzzyCheck = JBCheckBox("퍼지 검색 활성화")
    private val dbDialectCombo = JComboBox(arrayOf("Postgres", "Oracle", "MySQL"))
    private val customJsonArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
    }
    private val importButton = javax.swing.JButton("Import")
    private val resetButton = javax.swing.JButton("Reset to Default")

    override fun createComponent(): JComponent {
        if (root == null) {
            root = JPanel(BorderLayout())
            val content = panel {
                row {
                    cell(useCustomOnlyCheck)
                }
                row {
                    cell(enableFuzzyCheck)
                }
                row("DB Dialect") {
                    cell(dbDialectCombo)
                }
                row("Custom Dataset JSON") {
                    val scroll = JBScrollPane(customJsonArea)
                    scroll.preferredSize = java.awt.Dimension(600, 240)
                    cell(scroll)
                }
                row {
                    cell(importButton)
                    cell(resetButton)
                }
            }
            root?.add(content, BorderLayout.CENTER)
        }

        importButton.addActionListener {
            val json = customJsonArea.text
            val result = validator.validate(json)
            if (!result.isValid) {
                Messages.showErrorDialog(
                    result.errors.joinToString("\n"),
                    "커스텀 JSON 검증 실패",
                )
                return@addActionListener
            }
            applySettings()
            Messages.showInfoMessage("커스텀 JSON이 적용되었습니다.", "Import 완료")
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
        state.customDatasetJson = customJsonArea.text.trim().ifBlank { null }
        datasetRepository.reload()
        searchIndexRepository.reload()
    }

    override fun reset() {
        val state = settings.state
        useCustomOnlyCheck.isSelected = state.useCustomOnly
        enableFuzzyCheck.isSelected = state.enableFuzzy
        dbDialectCombo.selectedItem = state.dbDialect
        customJsonArea.text = state.customDatasetJson.orEmpty()
    }

    override fun getDisplayName(): String = "Std Naming Hound"

    override fun disposeUIResources() {
        root = null
    }
}
