package com.github.ydj515.stdnaminghound.toolWindow.logic

import com.github.ydj515.stdnaminghound.builder.WordBuilder
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.DomainRef
import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.model.SearchItemType
import com.github.ydj515.stdnaminghound.model.TermRef
import com.github.ydj515.stdnaminghound.model.Word
import com.github.ydj515.stdnaminghound.model.WordRef
import com.github.ydj515.stdnaminghound.sql.DbDialect
import com.github.ydj515.stdnaminghound.sql.SqlFormat
import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import com.github.ydj515.stdnaminghound.toolWindow.context.ToolWindowContext
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.StringSelection
import javax.swing.DefaultComboBoxModel
import javax.swing.JPopupMenu
import javax.swing.JMenuItem

class ToolWindowLogic(private val context: ToolWindowContext) {
    private val ui = context.ui
    private val project = context.toolWindow.project

    fun refreshDomainCombo(domains: List<Domain>) {
        val names = mutableListOf("선택 안 함")
        names.addAll(domains.map { it.name })
        ui.domainCombo.model = DefaultComboBoxModel(names.toTypedArray())
    }

    fun selectDomainByName(name: String?) {
        val target = name?.trim().orEmpty()
        if (target.isBlank()) return
        ui.domainCombo.selectedItem = target
    }

    fun refreshMeta() {
        val dataset = context.datasetRepository.getDataset()
        val meta = dataset.meta
        ui.metaLabel.text = buildString {
            append("Dataset")
            if (meta?.datasetVersion != null) append(" v").append(meta.datasetVersion)
            append(": ")
            append("terms=").append(dataset.terms.size)
            append(", words=").append(dataset.words.size)
            append(", domains=").append(dataset.domains.size)
            append(" | Index=").append(context.searchIndexRepository.getIndex().entries.size)
        }
        refreshDomainCombo(dataset.domains)
    }

    fun updateResults(query: String) {
        val allowedTypes = mutableSetOf<SearchItemType>().apply {
            if (ui.termFilterCheck.isSelected) add(SearchItemType.TERM)
            if (ui.wordFilterCheck.isSelected) add(SearchItemType.WORD)
            if (!ui.builderModeCheck.isSelected) {
                add(SearchItemType.DOMAIN)
            }
        }
        if (allowedTypes.isEmpty()) {
            ui.listModel.clear()
            return
        }
        val results = context.searchEngine.search(query, 50, allowedTypes)
        ui.listModel.clear()
        results.forEach { ui.listModel.addElement(it) }
        if (results.isNotEmpty()) {
            ui.resultList.selectedIndex = 0
        }
    }

    fun updateBuilderPreview() {
        val name = context.builder.buildName()
        val text = if (name.isBlank()) "" else name
        ui.builderPreview.text = text
        ui.builderPreview.toolTipText = if (text.isBlank()) null else text
        ui.tokensPanel.removeAll()
        context.builder.getTokens().forEachIndexed { index, word ->
            if (index > 0) {
                ui.tokensPanel.add(javax.swing.Box.createHorizontalStrut(8))
            }
            val chip = javax.swing.JButton(word.abbr ?: word.koName).apply {
                addActionListener {
                    context.builder.removeAt(index)
                    updateBuilderPreview()
                }
            }
            ui.tokensPanel.add(chip)
        }
        ui.tokensPanel.revalidate()
        ui.tokensPanel.repaint()
        ui.builderClearButton.isEnabled = context.builder.getTokens().isNotEmpty()
        ui.addColumnButton.isEnabled = name.isNotBlank()
        ui.builderPreview.revalidate()
        ui.builderPreview.repaint()
    }

    fun updateActionButtons(addBuilderMenuItem: JMenuItem?) {
        val item = ui.resultList.selectedValue
        val canAdd = ui.builderModeCheck.isSelected &&
            (item?.type == SearchItemType.WORD || item?.type == SearchItemType.TERM)
        addBuilderMenuItem?.isEnabled = canAdd
    }

    fun copyText(text: String) {
        if (text.isBlank()) return
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    fun insertText(text: String) {
        if (text.isBlank()) return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            Messages.showInfoMessage(project, "현재 편집기를 찾지 못했습니다.", "Insert 실패")
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            val offset = editor.caretModel.offset
            editor.document.insertString(offset, text)
            editor.caretModel.moveToOffset(offset + text.length)
        }
    }

    fun resolveSelectedText(item: SearchItem): String {
        return when (item.type) {
            SearchItemType.TERM -> item.abbr ?: item.primaryEn ?: item.titleKo
            SearchItemType.WORD -> item.abbr ?: item.primaryEn ?: item.titleKo
            SearchItemType.DOMAIN -> item.titleKo
        }
    }

    fun addSelectedItemToBuilder(item: SearchItem) {
        when (item.type) {
            SearchItemType.WORD -> {
                val word = context.datasetRepository.getDataset()
                    .words
                    .getOrNull((item.payloadRef as WordRef).index)
                if (word != null) {
                    context.builder.add(word)
                    updateBuilderPreview()
                }
            }
            SearchItemType.TERM -> {
                val term = context.datasetRepository.getDataset()
                    .terms
                    .getOrNull((item.payloadRef as TermRef).index)
                if (term != null) {
                    selectDomainByName(term.domainName)
                    context.builder.add(
                        Word(
                            koName = term.koName,
                            enName = null,
                            abbr = term.abbr,
                            description = term.description,
                            synonyms = term.synonyms,
                            isFormWord = false,
                        )
                    )
                    updateBuilderPreview()
                }
            }
            else -> Unit
        }
    }

    fun findDomainByName(name: String?): Domain? {
        val key = name?.trim().orEmpty()
        if (key.isBlank()) return null
        return context.datasetRepository.getDataset().domains.firstOrNull { it.name == key }
    }

    fun selectedDomainFromCombo(): Domain? {
        val selected = ui.domainCombo.selectedItem as? String ?: return null
        if (selected == "선택 안 함") return null
        return findDomainByName(selected)
    }

    fun findDomainForBuilder(): Domain {
        val selected = selectedDomainFromCombo()
        if (selected != null) return selected
        val item = ui.resultList.selectedValue
        return when (item?.type) {
            SearchItemType.DOMAIN -> context.datasetRepository.getDataset()
                .domains
                .getOrNull((item.payloadRef as DomainRef).index)
                ?: defaultDomain()
            SearchItemType.TERM -> {
                val term = context.datasetRepository.getDataset()
                    .terms
                    .getOrNull((item.payloadRef as TermRef).index)
                findDomainByName(term?.domainName) ?: defaultDomain()
            }
            else -> defaultDomain()
        }
    }

    fun buildColumnEntryForBuilder(): ColumnEntry? {
        val name = context.builder.buildName()
        if (name.isBlank()) {
            Messages.showInfoMessage(project, "Builder 결과가 없습니다.", "컬럼 추가")
            return null
        }
        val domain = findDomainForBuilder()
        val dialect = DbDialect.fromName(context.settings.state.dbDialect)
        val comment = context.builder.getTokens().joinToString(" ") { it.koName }
        val result = context.sqlGenerator.generateColumnSql(
            columnName = name,
            domain = domain,
            description = comment,
            dialect = dialect,
            format = SqlFormat.COLUMN_DEFINITION,
        )
        val parts = splitColumnSql(result.sql)
        return ColumnEntry(
            name = name,
            definition = parts.definition,
            commentSql = parts.commentSql,
            domainName = domain.name,
            description = comment,
        )
    }

    fun buildColumnOutputFromColumns(): String {
        if (ui.columnsModel.size == 0) return ""
        return ui.columnsModel.elements().asSequence()
            .flatMap { entry ->
                sequenceOf(entry.definition, entry.commentSql)
                    .filterNot { it.isNullOrBlank() }
                    .map { it!!.trim() }
            }
            .joinToString("\n")
    }

    fun buildCreateTableFromColumns(): String {
        if (ui.columnsModel.size == 0) return ""
        val dialect = DbDialect.fromName(context.settings.state.dbDialect)
        val definitions = ui.columnsModel.elements().asSequence()
            .map { it.definition }
            .toList()
        val create = "CREATE TABLE TABLE_NAME (\n  ${definitions.joinToString(",\n  ")}\n);"
        if (dialect == DbDialect.MYSQL) {
            return create
        }
        val comments = ui.columnsModel.elements().asSequence()
            .map { it.commentSql }
            .filterNot { it.isNullOrBlank() }
            .toList()
        return if (comments.isEmpty()) create else "$create\n${comments.joinToString("\n")}" 
    }

    fun buildOutputSql(): String = buildCreateTableFromColumns()

    fun updateOutputLabel() {
        ui.outputLabel.text = "Output (SQL - ${context.settings.state.dbDialect})"
    }

    fun updateOutputPreview() {
        updateOutputLabel()
        ui.outputArea.text = buildOutputSql()
    }

    fun scheduleOutputRefresh() {
        context.outputAlarm.cancelAllRequests()
        context.outputAlarm.addRequest({ updateOutputPreview() }, 300)
    }

    fun rebuildColumnEntriesForDialect() {
        if (ui.columnsModel.size == 0) return
        val dialect = DbDialect.fromName(context.settings.state.dbDialect)
        val updated = ui.columnsModel.elements().asSequence()
            .map { entry ->
                val domain = findDomainByName(entry.domainName) ?: defaultDomain()
                val result = context.sqlGenerator.generateColumnSql(
                    columnName = entry.name,
                    domain = domain,
                    description = entry.description,
                    dialect = dialect,
                    format = SqlFormat.COLUMN_DEFINITION,
                )
                val parts = splitColumnSql(result.sql)
                entry.copy(
                    definition = parts.definition,
                    commentSql = parts.commentSql,
                    domainName = domain.name,
                )
            }
            .toList()
        ui.columnsModel.removeAllElements()
        updated.forEach { ui.columnsModel.addElement(it) }
    }

    fun showCopiedToast(text: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("StdNamingHound")
            .createNotification("${text} 복사됨", NotificationType.INFORMATION)
            .notify(project)
    }

    fun applyPopupTheme(popup: JPopupMenu) {
        val bg = javax.swing.UIManager.getColor("PopupMenu.background")
            ?: javax.swing.UIManager.getColor("Menu.background")
        val fg = javax.swing.UIManager.getColor("PopupMenu.foreground")
            ?: javax.swing.UIManager.getColor("Menu.foreground")
        if (bg != null) {
            popup.background = bg
            popup.isOpaque = true
        }
        if (fg != null) {
            popup.foreground = fg
        }
        for (i in 0 until popup.componentCount) {
            val comp = popup.getComponent(i)
            if (comp is JMenuItem) {
                if (bg != null) {
                    comp.background = bg
                    comp.isOpaque = true
                }
                if (fg != null) {
                    comp.foreground = fg
                }
            }
        }
    }

    fun defaultDomain(): Domain = DEFAULT_DOMAIN

    data class ColumnParts(
        val definition: String,
        val commentSql: String?,
    )

    fun splitColumnSql(sql: String): ColumnParts {
        val lines = sql.split("\n")
        val definition = lines.firstOrNull()?.trim()?.removeSuffix(";").orEmpty()
        val comment = lines.drop(1).joinToString("\n").trim().ifBlank { null }
        return ColumnParts(definition, comment)
    }

    companion object {
        private val DEFAULT_DOMAIN = Domain(
            name = "기본V255",
            dataType = "VARCHAR",
            length = 255,
            scale = null,
            storageFormat = null,
            displayFormat = null,
            allowedValues = null,
        )
    }
}
