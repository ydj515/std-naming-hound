package com.github.ydj515.stdnaminghound.toolWindow

import com.github.ydj515.stdnaminghound.builder.WordBuilder
import com.github.ydj515.stdnaminghound.model.Domain
import com.github.ydj515.stdnaminghound.model.DomainRef
import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.model.SearchItemType
import com.github.ydj515.stdnaminghound.model.Term
import com.github.ydj515.stdnaminghound.model.TermRef
import com.github.ydj515.stdnaminghound.model.Word
import com.github.ydj515.stdnaminghound.model.WordRef
import com.github.ydj515.stdnaminghound.search.SearchIndexRepository
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettingsListener
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundConfigurable
import com.github.ydj515.stdnaminghound.sql.DbDialect
import com.github.ydj515.stdnaminghound.sql.SqlFormat
import com.github.ydj515.stdnaminghound.sql.SqlGenerator
import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.EditorTextField
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.JBColor
import com.intellij.ui.content.ContentFactory
import com.intellij.util.Alarm
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.options.ShowSettingsUtil
import com.github.ydj515.stdnaminghound.search.SearchEngine
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.UIManager
import javax.swing.Box
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JList
import javax.swing.TransferHandler
import javax.swing.DropMode
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable


class StdNamingHoundToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = StdNamingHoundToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class StdNamingHoundToolWindow(private val toolWindow: ToolWindow) {

        private val datasetRepository = service<DatasetRepository>()
        private val searchIndexRepository = service<SearchIndexRepository>()
        private val searchEngine = SearchEngine(searchIndexRepository)
        private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, toolWindow.disposable)
        private val outputAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, toolWindow.disposable)
        private val builder = WordBuilder()
        private val settings = service<StdNamingHoundSettings>()
        private val sqlGenerator = SqlGenerator()
        private val project = toolWindow.project

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()

            val searchField = SearchTextField()
            val listModel = DefaultListModel<SearchItem>()
            val resultList = JBList(listModel)
            val termFilterCheck = JCheckBox("용어", true)
            val wordFilterCheck = JCheckBox("단어", true)
            val builderModeCheck = JCheckBox("Builder")
            val settingsButton = JButton(AllIcons.General.GearPlain).apply {
                toolTipText = "설정 열기"
                isFocusable = false
                isContentAreaFilled = false
                border = JBUI.Borders.empty(2)
                preferredSize = JBUI.size(20, 20)
            }
            val columnsModel = DefaultListModel<ColumnEntry>()
            val columnsList = JBList(columnsModel).apply {
                cellRenderer = ColumnEntryRenderer()
                dragEnabled = true
                dropMode = DropMode.INSERT
            }
            val metaLabel = JBLabel()
            val builderPreview = LinkLabel.create("", null).apply {
                toolTipText = ""
            }
            val tokensPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            val domainCombo = JComboBox<String>()
            val caseCombo = JComboBox(WordBuilder.CaseStyle.entries.toTypedArray())
                .apply {
                    selectedItem = runCatching {
                        WordBuilder.CaseStyle.valueOf(settings.state.defaultCaseStyle)
                    }.getOrDefault(WordBuilder.CaseStyle.SNAKE_UPPER)
                }
            domainCombo.preferredSize = JBUI.size(JBUI.scale(165), domainCombo.preferredSize.height)
            domainCombo.maximumSize = JBUI.size(JBUI.scale(165), domainCombo.preferredSize.height)
            val previewHeight = JBUI.scale(24)
            builderPreview.preferredSize = JBUI.size(JBUI.scale(220), previewHeight)
            builderPreview.minimumSize = JBUI.size(JBUI.scale(120), previewHeight)
            builderPreview.maximumSize = java.awt.Dimension(Int.MAX_VALUE, previewHeight)
            val builderClearButton = JButton(AllIcons.Actions.GC).apply {
                toolTipText = "Clear Builder"
            }
            val addColumnButton = JButton(AllIcons.General.Add).apply {
                toolTipText = "Add Stage"
            }
            val outputArea = EditorTextField("", project, PlainTextFileType.INSTANCE).apply {
                isViewer = true
                setOneLineMode(false)
                preferredSize = Dimension(600, 160)
            }
            val outputCopyButton = JButton(AllIcons.Actions.Copy).apply {
                toolTipText = "SQL 복사"
            }
            var addBuilderMenuItem: JMenuItem? = null

            val renderer = SearchItemRenderer()
            resultList.cellRenderer = renderer

            val buttonRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                add(termFilterCheck)
                add(wordFilterCheck)
                add(builderModeCheck)
                add(settingsButton)
            }
            val buttonBar = JPanel(java.awt.GridBagLayout()).apply {
                add(
                    buttonRow,
                    java.awt.GridBagConstraints().apply {
                        anchor = java.awt.GridBagConstraints.CENTER
                    }
                )
            }
            val topBar = JPanel(BorderLayout()).apply {
                add(searchField, BorderLayout.CENTER)
                add(buttonBar, BorderLayout.EAST)
            }
            val center = JBScrollPane(resultList)
            val leftInset = JBUI.scale(12)
            val iconColumnWidth = JBUI.scale(20)
            val iconContentGap = JBUI.scale(8)
            tokensPanel.border = JBUI.Borders.empty()
            val builderControls = JPanel().apply {
                layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
                val row2 = JPanel(BorderLayout()).apply {
                    val iconPanel = JPanel(BorderLayout()).apply {
                        add(JBLabel(AllIcons.Actions.Preview), BorderLayout.CENTER)
                    }.apply {
                        preferredSize = JBUI.size(iconColumnWidth, preferredSize.height)
                        minimumSize = JBUI.size(iconColumnWidth, minimumSize.height)
                        maximumSize = JBUI.size(iconColumnWidth, maximumSize.height)
                    }
                    val contentPanel = JPanel(BorderLayout()).apply {
                        border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                        add(builderPreview, BorderLayout.CENTER)
                    }
                    border = JBUI.Borders.empty(0, leftInset, 0, 0)
                    add(iconPanel, BorderLayout.WEST)
                    add(contentPanel, BorderLayout.CENTER)
                }
                val row3 = JPanel(BorderLayout()).apply {
                    val iconPanel = JPanel(BorderLayout()).apply {
                        add(JBLabel(AllIcons.Nodes.DataTables), BorderLayout.CENTER)
                    }.apply {
                        preferredSize = JBUI.size(iconColumnWidth, preferredSize.height)
                        minimumSize = JBUI.size(iconColumnWidth, minimumSize.height)
                        maximumSize = JBUI.size(iconColumnWidth, maximumSize.height)
                    }
                    val contentPanel = JPanel(BorderLayout()).apply {
                        border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                        add(domainCombo, BorderLayout.WEST)
                    }
                    border = JBUI.Borders.empty(0, leftInset, 0, 0)
                    add(iconPanel, BorderLayout.WEST)
                    add(contentPanel, BorderLayout.CENTER)
                }
                val row4 = JPanel(BorderLayout()).apply {
                    val spacer = Box.createHorizontalStrut(iconColumnWidth)
                    val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                        add(builderClearButton)
                        add(Box.createHorizontalStrut(JBUI.scale(8)))
                        add(addColumnButton)
                    }
                    border = JBUI.Borders.empty(0, leftInset, 0, 0)
                    val contentWithGap = JPanel(BorderLayout()).apply {
                        border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                        add(contentPanel, BorderLayout.CENTER)
                    }
                    add(spacer, BorderLayout.WEST)
                    add(contentWithGap, BorderLayout.CENTER)
                }
                add(row2)
                add(row3)
                add(row4)
            }
            val builderPanel = JPanel(BorderLayout()).apply {
                val tokensRow = JPanel(BorderLayout()).apply {
                    val iconPanel = JPanel(BorderLayout()).apply {
                        val iconLabel = JBLabel(AllIcons.General.FitContent)
                        add(iconLabel, BorderLayout.CENTER)
                    }
                    iconPanel.preferredSize = JBUI.size(iconColumnWidth, iconPanel.preferredSize.height)
                    iconPanel.minimumSize = JBUI.size(iconColumnWidth, iconPanel.minimumSize.height)
                    iconPanel.maximumSize = JBUI.size(iconColumnWidth, iconPanel.maximumSize.height)
                    val contentPanel = JPanel(BorderLayout()).apply {
                        border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                        add(tokensPanel, BorderLayout.CENTER)
                    }
                    border = JBUI.Borders.empty(0, leftInset, 0, 0)
                    add(iconPanel, BorderLayout.WEST)
                    add(contentPanel, BorderLayout.CENTER)
                }
                add(tokensRow, BorderLayout.NORTH)
                add(builderControls, BorderLayout.CENTER)
            }
            val columnsPanel = JPanel(BorderLayout()).apply {
                val label = JBLabel("Stage")
                val header = JPanel(BorderLayout()).apply {
                    add(label, BorderLayout.WEST)
                }
                val scroll = JBScrollPane(columnsList).apply {
                    preferredSize = Dimension(600, 140)
                }
                add(header, BorderLayout.NORTH)
                add(scroll, BorderLayout.CENTER)
            }
            val stagingPanel = JPanel(BorderLayout()).apply {
                add(builderPanel, BorderLayout.NORTH)
                add(columnsPanel, BorderLayout.CENTER)
                add(metaLabel, BorderLayout.SOUTH)
            }
            val outputLabel = JBLabel()
            val outputPanel = JPanel(BorderLayout()).apply {
                val header = JPanel(BorderLayout()).apply {
                    add(outputLabel, BorderLayout.WEST)
                    add(outputCopyButton, BorderLayout.EAST)
                }
                add(header, BorderLayout.NORTH)
                add(outputArea, BorderLayout.CENTER)
            }
            val clearColumnsButton = JButton(AllIcons.Actions.GC).apply {
                toolTipText = "Clear Stage"
            }
            val topPanel = JPanel(BorderLayout()).apply {
                add(topBar, BorderLayout.NORTH)
                add(center, BorderLayout.CENTER)
            }
            val lowerSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, stagingPanel, outputPanel).apply {
                resizeWeight = 0.6
                isContinuousLayout = true
                dividerSize = JBUI.scale(6)
                setDividerLocation(0.6)
            }
            val mainSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, lowerSplit).apply {
                resizeWeight = 0.5
                isContinuousLayout = true
                dividerSize = JBUI.scale(6)
                setDividerLocation(0.5)
            }

            add(mainSplit, BorderLayout.CENTER)


            val columnsHeader = JPanel(BorderLayout()).apply {
                add(JBLabel("Stage"), BorderLayout.WEST)
                add(clearColumnsButton, BorderLayout.EAST)
            }
            columnsPanel.removeAll()
            columnsPanel.add(columnsHeader, BorderLayout.NORTH)
            columnsPanel.add(JBScrollPane(columnsList).apply {
                preferredSize = Dimension(600, 140)
            }, BorderLayout.CENTER)

            val gap = JBUI.scale(6)
            topPanel.border = JBUI.Borders.empty(0, 0, gap, 0)
            stagingPanel.border = JBUI.Borders.empty(gap, 0, gap, 0)
            outputPanel.border = JBUI.Borders.empty(gap, 0, 0, 0)

            fun refreshDomainCombo(domains: List<Domain>) {
                val names = mutableListOf("선택 안 함")
                names.addAll(domains.map { it.name })
                domainCombo.model = DefaultComboBoxModel(names.toTypedArray())
            }

            fun selectDomainByName(name: String?) {
                val target = name?.trim().orEmpty()
                if (target.isBlank()) return
                val model = domainCombo.model
                for (i in 0 until model.size) {
                    if (model.getElementAt(i) == target) {
                        domainCombo.selectedItem = target
                        return
                    }
                }
            }

            fun refreshMeta() {
                val dataset = datasetRepository.getDataset()
                val meta = dataset.meta
                metaLabel.text = buildString {
                    append("Dataset")
                    if (meta?.datasetVersion != null) append(" v").append(meta.datasetVersion)
                    append(": ")
                    append("terms=").append(dataset.terms.size)
                    append(", words=").append(dataset.words.size)
                    append(", domains=").append(dataset.domains.size)
                    append(" | Index=").append(searchIndexRepository.getIndex().entries.size)
                }
                refreshDomainCombo(dataset.domains)
            }

            fun updateResults(query: String) {
                val allowedTypes = mutableSetOf<SearchItemType>().apply {
                    if (termFilterCheck.isSelected) add(SearchItemType.TERM)
                    if (wordFilterCheck.isSelected) add(SearchItemType.WORD)
                    if (!builderModeCheck.isSelected) {
                        add(SearchItemType.DOMAIN)
                    }
                }
                if (allowedTypes.isEmpty()) {
                    listModel.clear()
                    return
                }
                val results = searchEngine.search(query, 50, allowedTypes)
                listModel.clear()
                results.forEach { listModel.addElement(it) }
                if (results.isNotEmpty()) {
                    resultList.selectedIndex = 0
                }
            }

            fun updateBuilderPreview() {
                val name = builder.buildName()
                val text = if (name.isBlank()) "" else name
                builderPreview.text = text
                builderPreview.toolTipText = if (text.isBlank()) null else text
                tokensPanel.removeAll()
                builder.getTokens().forEachIndexed { index, word ->
                    if (index > 0) {
                        tokensPanel.add(Box.createHorizontalStrut(8))
                    }
                    val chip = JButton(word.abbr ?: word.koName).apply {
                        addActionListener {
                            builder.removeAt(index)
                            updateBuilderPreview()
                        }
                    }
                    tokensPanel.add(chip)
                }
                tokensPanel.revalidate()
                tokensPanel.repaint()
                builderClearButton.isEnabled = builder.getTokens().isNotEmpty()
                addColumnButton.isEnabled = name.isNotBlank()
                builderPreview.revalidate()
                builderPreview.repaint()
            }

            fun updateActionButtons() {
                val item = resultList.selectedValue
                val canAdd = builderModeCheck.isSelected &&
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
                        val word = datasetRepository.getDataset()
                            .words
                            .getOrNull((item.payloadRef as WordRef).index)
                        if (word != null) {
                            builder.add(word)
                            updateBuilderPreview()
                        }
                    }
                    SearchItemType.TERM -> {
                        val term = datasetRepository.getDataset()
                            .terms
                            .getOrNull((item.payloadRef as TermRef).index)
                        if (term != null) {
                            selectDomainByName(term.domainName)
                            builder.add(
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
                return datasetRepository.getDataset().domains.firstOrNull { it.name == key }
            }

            fun selectedDomainFromCombo(): Domain? {
                val selected = domainCombo.selectedItem as? String ?: return null
                if (selected == "선택 안 함") return null
                return findDomainByName(selected)
            }

            fun findDomainForBuilder(): Domain {
                val selected = selectedDomainFromCombo()
                if (selected != null) return selected
                val item = resultList.selectedValue
                return when (item?.type) {
                    SearchItemType.DOMAIN -> datasetRepository.getDataset()
                        .domains
                        .getOrNull((item.payloadRef as DomainRef).index)
                        ?: defaultDomain()
                    SearchItemType.TERM -> {
                        val term = datasetRepository.getDataset()
                            .terms
                            .getOrNull((item.payloadRef as TermRef).index)
                        findDomainByName(term?.domainName) ?: defaultDomain()
                    }
                    else -> defaultDomain()
                }
            }

            fun buildColumnEntryForBuilder(): ColumnEntry? {
                val name = builder.buildName()
                if (name.isBlank()) {
                    Messages.showInfoMessage(project, "Builder 결과가 없습니다.", "컬럼 추가")
                    return null
                }
                val domain = findDomainForBuilder()
                val dialect = DbDialect.fromName(settings.state.dbDialect)
                val comment = builder.getTokens().joinToString(" ") { it.koName }
                val result = sqlGenerator.generateColumnSql(
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
                if (columnsModel.size == 0) return ""
                val lines = mutableListOf<String>()
                for (i in 0 until columnsModel.size) {
                    val entry = columnsModel.getElementAt(i)
                    lines.add(entry.definition)
                    if (!entry.commentSql.isNullOrBlank()) {
                        lines.add(entry.commentSql)
                    }
                }
                return lines.joinToString("\n")
            }

            fun buildCreateTableFromColumns(): String {
                if (columnsModel.size == 0) return ""
                val dialect = DbDialect.fromName(settings.state.dbDialect)
                val definitions = mutableListOf<String>()
                for (i in 0 until columnsModel.size) {
                    definitions.add(columnsModel.getElementAt(i).definition)
                }
                val create = "CREATE TABLE TABLE_NAME (\n  ${definitions.joinToString(",\n  ")}\n);"
                if (dialect == DbDialect.MYSQL) {
                    return create
                }
                val comments = mutableListOf<String>()
                for (i in 0 until columnsModel.size) {
                    val comment = columnsModel.getElementAt(i).commentSql
                    if (!comment.isNullOrBlank()) {
                        comments.add(comment)
                    }
                }
                return if (comments.isEmpty()) create else "$create\n${comments.joinToString("\n")}"
            }

            fun buildOutputSql(): String {
                return buildCreateTableFromColumns()
            }

            fun updateOutputLabel() {
                outputLabel.text = "Output (SQL - ${settings.state.dbDialect})"
            }

            fun updateOutputPreview() {
                updateOutputLabel()
                outputArea.text = buildOutputSql()
            }

            fun scheduleOutputRefresh() {
                outputAlarm.cancelAllRequests()
                outputAlarm.addRequest({
                    updateOutputPreview()
                }, 300)
            }

            fun rebuildColumnEntriesForDialect() {
                if (columnsModel.size == 0) return
                val dialect = DbDialect.fromName(settings.state.dbDialect)
                val updated = mutableListOf<ColumnEntry>()
                for (i in 0 until columnsModel.size) {
                    val entry = columnsModel.getElementAt(i)
                    val domain = findDomainByName(entry.domainName) ?: defaultDomain()
                    val result = sqlGenerator.generateColumnSql(
                        columnName = entry.name,
                        domain = domain,
                        description = entry.description,
                        dialect = dialect,
                        format = SqlFormat.COLUMN_DEFINITION,
                    )
                    val parts = splitColumnSql(result.sql)
                    updated.add(
                        entry.copy(
                            definition = parts.definition,
                            commentSql = parts.commentSql,
                            domainName = domain.name,
                        )
                    )
                }
                columnsModel.removeAllElements()
                updated.forEach { columnsModel.addElement(it) }
            }

            columnsList.transferHandler = ColumnReorderTransferHandler(columnsList) {
                scheduleOutputRefresh()
            }

            val columnsPopup = JPopupMenu().apply {
                val removeItem = JMenuItem("컬럼 삭제", AllIcons.Actions.DeleteTag)
                add(removeItem)
                removeItem.addActionListener {
                    val index = columnsList.selectedIndex
                    if (index < 0) return@addActionListener
                    val entry = columnsModel.getElementAt(index)
                    val result = Messages.showYesNoDialog(
                        project,
                        "${entry.name} 컬럼을 삭제하시겠습니까?",
                        "컬럼 삭제",
                        null
                    )
                    if (result == Messages.YES) {
                        columnsModel.remove(index)
                        scheduleOutputRefresh()
                    }
                }
            }
            columnsList.addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mousePressed(e: java.awt.event.MouseEvent) {
                    if (e.isPopupTrigger) showColumnsPopup(e)
                }

                override fun mouseReleased(e: java.awt.event.MouseEvent) {
                    if (e.isPopupTrigger) showColumnsPopup(e)
                }

                private fun showColumnsPopup(e: java.awt.event.MouseEvent) {
                    val index = columnsList.locationToIndex(e.point)
                    if (index >= 0) {
                        columnsList.selectedIndex = index
                        applyPopupTheme(columnsPopup)
                        columnsPopup.updateUI()
                        columnsPopup.show(columnsList, e.x, e.y)
                    }
                }
            })

            searchField.textEditor.document.addDocumentListener(object : javax.swing.event.DocumentListener {
                override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = schedule()
                override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = schedule()
                override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = schedule()

                private fun schedule() {
                    alarm.cancelAllRequests()
                    alarm.addRequest({
                        val query = searchField.text
                        ApplicationManager.getApplication().invokeLater {
                            updateResults(query)
                        }
                    }, 200)
                }
            })

            columnsModel.addListDataListener(object : javax.swing.event.ListDataListener {
                override fun intervalAdded(e: javax.swing.event.ListDataEvent?) = scheduleOutputRefresh()
                override fun intervalRemoved(e: javax.swing.event.ListDataEvent?) = scheduleOutputRefresh()
                override fun contentsChanged(e: javax.swing.event.ListDataEvent?) = scheduleOutputRefresh()
            })

            resultList.addListSelectionListener {
                resultList.selectedValue ?: return@addListSelectionListener
                updateActionButtons()
            }

            builderModeCheck.addActionListener {
                updateResults(searchField.text)
                updateActionButtons()
            }
            termFilterCheck.addActionListener {
                updateResults(searchField.text)
            }
            wordFilterCheck.addActionListener {
                updateResults(searchField.text)
            }

            val popupMenu = JPopupMenu().apply {
                addBuilderMenuItem = JMenuItem("Add to Builder", AllIcons.General.Add)
                val insertItem = JMenuItem("Insert Name", AllIcons.Actions.Edit)
                val copyItem = JMenuItem("Copy Name", AllIcons.Actions.Copy)
                add(addBuilderMenuItem)
                add(insertItem)
                add(copyItem)
                copyItem.addActionListener {
                    val item = resultList.selectedValue ?: return@addActionListener
                    copyText(resolveSelectedText(item))
                }
                insertItem.addActionListener {
                    val item = resultList.selectedValue ?: return@addActionListener
                    insertText(resolveSelectedText(item))
                }
                addBuilderMenuItem?.addActionListener {
                    val item = resultList.selectedValue ?: return@addActionListener
                    addSelectedItemToBuilder(item)
                }
            }

            resultList.addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mousePressed(e: java.awt.event.MouseEvent) {
                    if (e.isPopupTrigger) showPopup(e)
                }

                override fun mouseReleased(e: java.awt.event.MouseEvent) {
                    if (e.isPopupTrigger) showPopup(e)
                }

                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    if (e.clickCount == 2 && builderModeCheck.isSelected) {
                        val index = resultList.locationToIndex(e.point)
                        if (index >= 0) {
                            resultList.selectedIndex = index
                            val item = resultList.selectedValue ?: return
                            addSelectedItemToBuilder(item)
                        }
                    }
                }

                private fun showPopup(e: java.awt.event.MouseEvent) {
                    val index = resultList.locationToIndex(e.point)
                    if (index >= 0) {
                        resultList.selectedIndex = index
                        updateActionButtons()
                        applyPopupTheme(popupMenu)
                        popupMenu.updateUI()
                        popupMenu.show(resultList, e.x, e.y)
                    }
                }
            })

            resultList.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
                override fun mouseMoved(e: java.awt.event.MouseEvent) {
                    val index = resultList.locationToIndex(e.point)
                    if (index >= 0 && index != resultList.selectedIndex) {
                        resultList.selectedIndex = index
                        updateActionButtons()
                    }
                    if (renderer.hoverIndex != index) {
                        renderer.hoverIndex = index
                        resultList.repaint()
                    }
                }
            })

            resultList.addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseExited(e: java.awt.event.MouseEvent) {
                    if (renderer.hoverIndex != -1) {
                        renderer.hoverIndex = -1
                        resultList.repaint()
                    }
                }
            })

            builderClearButton.addActionListener {
                builder.clear()
                domainCombo.selectedItem = "선택 안 함"
                updateBuilderPreview()
            }
            addColumnButton.addActionListener {
                val entry = buildColumnEntryForBuilder()
                if (entry != null) {
                    if (columnsModel.containsName(entry.name)) {
                        Messages.showInfoMessage(project, "이미 추가된 컬럼입니다: ${entry.name}", "컬럼 추가")
                        return@addActionListener
                    }
                    columnsModel.addElement(entry)
                    builder.clear()
                    domainCombo.selectedItem = "선택 안 함"
                    updateBuilderPreview()
                }
            }
            outputCopyButton.addActionListener {
                val text = buildOutputSql()
                copyText(text)
                if (text.isNotBlank()) {
                    showCopiedToast(text)
                }
            }
            settingsButton.addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, StdNamingHoundConfigurable::class.java)
            }
            clearColumnsButton.addActionListener {
                if (columnsModel.size == 0) return@addActionListener
                val result = Messages.showYesNoDialog(
                    project,
                    "모든 컬럼을 삭제하시겠습니까?",
                    "컬럼 삭제",
                    null
                )
                if (result == Messages.YES) {
                    columnsModel.removeAllElements()
                    scheduleOutputRefresh()
                }
            }
            builderPreview.setListener({ _, _ ->
                val text = builder.buildName()
                copyText(text)
                showCopiedToast(text)
            }, null)

            val menuMask = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
            val inputMap = resultList.getInputMap(JComponent.WHEN_FOCUSED)
            val actionMap = resultList.actionMap
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "insertSelected")
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuMask), "copySelected")
            inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK), "addBuilder")
            actionMap.put("insertSelected", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    val item = resultList.selectedValue ?: return
                    insertText(resolveSelectedText(item))
                }
            })
            actionMap.put("copySelected", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    val item = resultList.selectedValue ?: return
                    copyText(resolveSelectedText(item))
                }
            })
            actionMap.put("addBuilder", object : javax.swing.AbstractAction() {
                override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                    val item = resultList.selectedValue ?: return
                    addSelectedItemToBuilder(item)
                }
            })

            refreshMeta()
            builder.setCaseStyle(caseCombo.selectedItem as WordBuilder.CaseStyle)
            updateBuilderPreview()
            updateActionButtons()
            updateOutputPreview()

            val connection = ApplicationManager.getApplication().messageBus.connect(toolWindow.disposable)
            connection.subscribe(StdNamingHoundSettings.TOPIC, object : StdNamingHoundSettingsListener {
                override fun settingsChanged(state: StdNamingHoundSettings.State) {
                    val style = runCatching {
                        WordBuilder.CaseStyle.valueOf(state.defaultCaseStyle)
                    }.getOrDefault(WordBuilder.CaseStyle.SNAKE_UPPER)
                    caseCombo.selectedItem = style
                    builder.setCaseStyle(style)
                    updateBuilderPreview()
                    updateOutputLabel()
                    rebuildColumnEntriesForDialect()
                    scheduleOutputRefresh()
                }
            })
        }

        private fun defaultDomain(): Domain = DEFAULT_DOMAIN

        private fun applyPopupTheme(popup: JPopupMenu) {
            val bg = UIManager.getColor("PopupMenu.background") ?: UIManager.getColor("Menu.background")
            val fg = UIManager.getColor("PopupMenu.foreground") ?: UIManager.getColor("Menu.foreground")
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

        private fun showCopiedToast(text: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("StdNamingHound")
                .createNotification("${text} 복사됨", NotificationType.INFORMATION)
                .notify(project)
        }
    }
}

private class SqlPreviewDialog(
    project: Project,
    private val sql: String,
    private val onCopy: (String) -> Unit,
    private val onInsert: (String) -> Unit,
) : DialogWrapper(project) {
    init {
        title = "SQL Preview"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val area = JTextArea(sql).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        val scroll = JScrollPane(area)
        scroll.preferredSize = Dimension(600, 240)
        return scroll
    }

    override fun createActions(): Array<javax.swing.Action> {
        val copyAction = object : DialogWrapperAction("Copy") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                onCopy(sql)
            }
        }
        val insertAction = object : DialogWrapperAction("Insert") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                onInsert(sql)
            }
        }
        return arrayOf(copyAction, insertAction, okAction)
    }
}

private data class ColumnParts(
    val definition: String,
    val commentSql: String?,
)

private fun splitColumnSql(sql: String): ColumnParts {
    val lines = sql.split("\n")
    val definition = lines.firstOrNull()?.trim()?.removeSuffix(";").orEmpty()
    val comment = lines.drop(1).joinToString("\n").trim().ifBlank { null }
    return ColumnParts(definition, comment)
}

private fun DefaultListModel<ColumnEntry>.containsName(name: String): Boolean {
    for (i in 0 until size) {
        if (getElementAt(i).name == name) return true
    }
    return false
}

private class ColumnEntryTransferable(
    private val entry: ColumnEntry,
    private val flavor: DataFlavor,
) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(flavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == this.flavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) {
            throw java.awt.datatransfer.UnsupportedFlavorException(flavor)
        }
        return entry
    }
}

private class ColumnReorderTransferHandler(
    private val list: JList<ColumnEntry>,
    private val onChanged: () -> Unit,
) : TransferHandler() {
    private val flavor = DataFlavor(ColumnEntry::class.java, "ColumnEntry")
    private var fromIndex = -1

    override fun getSourceActions(c: JComponent): Int = MOVE

    override fun createTransferable(c: JComponent): Transferable {
        fromIndex = list.selectedIndex
        val value = list.selectedValue ?: return ColumnEntryTransferable(
            ColumnEntry("", "", null, null, null),
            flavor,
        )
        return ColumnEntryTransferable(value, flavor)
    }

    override fun canImport(support: TransferSupport): Boolean {
        return support.isDrop && support.isDataFlavorSupported(flavor)
    }

    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) return false
        val dropLocation = support.dropLocation as JList.DropLocation
        val toIndex = dropLocation.index
        val model = list.model as DefaultListModel<ColumnEntry>
        val data = support.transferable.getTransferData(flavor) as ColumnEntry
        if (fromIndex < 0 || toIndex < 0) return false
        model.remove(fromIndex)
        val insertIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
        model.add(insertIndex, data)
        list.selectedIndex = insertIndex
        fromIndex = -1
        onChanged()
        return true
    }
}

private class ColumnEntryRenderer : javax.swing.ListCellRenderer<ColumnEntry> {
    private val label = com.intellij.ui.components.JBLabel()

    override fun getListCellRendererComponent(
        list: javax.swing.JList<out ColumnEntry>,
        value: ColumnEntry,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): java.awt.Component {
        label.text = value.definition
        label.foreground = if (isSelected) list.selectionForeground else list.foreground
        label.background = if (isSelected) list.selectionBackground else list.background
        label.isOpaque = true
        label.border = javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
        return label
    }
}

private class SearchItemRenderer : javax.swing.ListCellRenderer<SearchItem> {
    private val panel = JBPanel<JBPanel<*>>(BorderLayout())
    private val title = SimpleColoredComponent()
    private val sub = SimpleColoredComponent()
    var hoverIndex: Int = -1

    override fun getListCellRendererComponent(
        list: javax.swing.JList<out SearchItem>,
        value: SearchItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): java.awt.Component {
        val badge = when (value.type) {
            SearchItemType.TERM -> "[용어]"
            SearchItemType.WORD -> "[단어]"
            SearchItemType.DOMAIN -> "[도메인]"
        }
        val koName = value.titleKo
        val abbr = value.abbr
        val en = value.primaryEn
        val desc = value.subText.orEmpty()

        title.clear()
        title.append(badge, com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
        title.append(" ", com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES)
        title.append(koName, com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        if (!abbr.isNullOrBlank() && abbr != koName) {
            title.append("  ", com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES)
            title.append("[$abbr]", com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }

        sub.clear()
        val subText = buildString {
            if (!en.isNullOrBlank()) append(en)
            if (desc.isNotBlank()) {
                if (isNotEmpty()) append("  |  ")
                append(ellipsize(desc, 80))
            }
        }
        sub.append(subText, com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
        sub.toolTipText = desc.ifBlank { null }
        val hover = index == hoverIndex && !isSelected
        val fg = when {
            isSelected || hover -> list.selectionForeground
            else -> list.foreground
        }
        title.foreground = fg
        sub.foreground = if (isSelected || hover) fg else JBColor.GRAY
        panel.background = when {
            isSelected || hover -> list.selectionBackground
            else -> list.background
        }
        panel.border = javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)
        panel.removeAll()
        panel.add(title, BorderLayout.NORTH)
        panel.add(sub, BorderLayout.SOUTH)
        return panel
    }

    private fun ellipsize(text: String, max: Int): String {
        val trimmed = text.trim()
        if (trimmed.length <= max) return trimmed
        return trimmed.substring(0, max - 3) + "..."
    }
}
