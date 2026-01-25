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
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.content.ContentFactory
import com.intellij.util.Alarm
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
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


class StdNamingHoundToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = StdNamingHoundToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class StdNamingHoundToolWindow(toolWindow: ToolWindow) {

        private val datasetRepository = service<DatasetRepository>()
        private val searchIndexRepository = service<SearchIndexRepository>()
        private val searchEngine = SearchEngine(searchIndexRepository)
        private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, toolWindow.disposable)
        private val builder = WordBuilder()
        private val settings = service<StdNamingHoundSettings>()
        private val sqlGenerator = SqlGenerator()
        private val project = toolWindow.project

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()

            val searchField = SearchTextField()
            val listModel = DefaultListModel<SearchItem>()
            val resultList = JBList(listModel)
            val builderModeCheck = JCheckBox("Builder 모드")
            val sqlFormatCombo = JComboBox(SqlFormat.entries.toTypedArray()).apply {
                preferredSize = Dimension(140, preferredSize.height)
            }
            val sqlButton = JButton(AllIcons.Actions.Execute).apply {
                toolTipText = "SQL 생성(선택 항목)"
            }
            val detailArea = JBTextArea().apply {
                isEditable = false
                lineWrap = true
                wrapStyleWord = true
            }
            val metaLabel = JBLabel()
            val builderPreview = LinkLabel.create("", null).apply {
                toolTipText = ""
            }
            val tokensPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            val domainCombo = JComboBox<String>()
            val caseCombo = JComboBox(WordBuilder.CaseStyle.entries.toTypedArray())
                .apply {
                    preferredSize = JBUI.size(JBUI.scale(165), preferredSize.height)
                    maximumSize = JBUI.size(JBUI.scale(165), preferredSize.height)
                }
            domainCombo.preferredSize = JBUI.size(JBUI.scale(165), domainCombo.preferredSize.height)
            domainCombo.maximumSize = JBUI.size(JBUI.scale(165), domainCombo.preferredSize.height)
            val previewHeight = caseCombo.preferredSize.height
            builderPreview.preferredSize = JBUI.size(JBUI.scale(220), previewHeight)
            builderPreview.minimumSize = JBUI.size(JBUI.scale(120), previewHeight)
            builderPreview.maximumSize = java.awt.Dimension(Int.MAX_VALUE, previewHeight)
            val builderClearButton = JButton(AllIcons.Actions.GC).apply {
                toolTipText = "Clear Builder"
            }
            val builderSqlButton = JButton(AllIcons.Actions.Execute).apply {
                toolTipText = "SQL 생성(Builder)"
            }
            var addBuilderMenuItem: JMenuItem? = null

            val renderer = SearchItemRenderer()
            resultList.cellRenderer = renderer

            val buttonBar = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(builderModeCheck)
                add(JBLabel("SQL 형식"))
                add(sqlFormatCombo)
                add(sqlButton)
            }
            val topBar = JPanel(BorderLayout()).apply {
                add(searchField, BorderLayout.CENTER)
                add(buttonBar, BorderLayout.SOUTH)
            }
            val center = JBScrollPane(resultList)
            val detail = JBScrollPane(detailArea)
            val leftInset = JBUI.scale(12)
            val iconColumnWidth = JBUI.scale(20)
            val iconContentGap = JBUI.scale(8)
            tokensPanel.border = JBUI.Borders.empty()
            val builderControls = JPanel().apply {
                layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
                val row1 = JPanel(BorderLayout()).apply {
                    val iconPanel = JPanel(BorderLayout()).apply {
                        add(JBLabel(AllIcons.General.GearPlain), BorderLayout.CENTER)
                    }.apply {
                        preferredSize = JBUI.size(iconColumnWidth, preferredSize.height)
                        minimumSize = JBUI.size(iconColumnWidth, minimumSize.height)
                        maximumSize = JBUI.size(iconColumnWidth, maximumSize.height)
                    }
                    val comboPanel = JPanel(BorderLayout()).apply {
                        add(caseCombo, BorderLayout.CENTER)
                    }.apply {
                        preferredSize = caseCombo.preferredSize
                        minimumSize = caseCombo.minimumSize
                        maximumSize = caseCombo.maximumSize
                    }
                    val contentPanel = JPanel(BorderLayout()).apply {
                        border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                        add(comboPanel, BorderLayout.WEST)
                    }
                    border = JBUI.Borders.empty(0, leftInset, 0, 0)
                    add(iconPanel, BorderLayout.WEST)
                    add(contentPanel, BorderLayout.CENTER)
                }
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
                        add(builderSqlButton)
                    }
                    border = JBUI.Borders.empty(0, leftInset, 0, 0)
                    val contentWithGap = JPanel(BorderLayout()).apply {
                        border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                        add(contentPanel, BorderLayout.CENTER)
                    }
                    add(spacer, BorderLayout.WEST)
                    add(contentWithGap, BorderLayout.CENTER)
                }
                add(row1)
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
            val bottomPanel = JPanel(BorderLayout()).apply {
                add(builderPanel, BorderLayout.NORTH)
                add(detail, BorderLayout.CENTER)
                add(metaLabel, BorderLayout.SOUTH)
            }
            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, center, bottomPanel).apply {
                resizeWeight = 0.7
                isContinuousLayout = true
                dividerSize = JBUI.scale(6)
                setDividerLocation(0.65)
            }

            add(topBar, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)

            fun refreshDomainCombo(domains: List<Domain>) {
                val names = mutableListOf("선택 안 함")
                names.addAll(domains.map { it.name })
                domainCombo.model = DefaultComboBoxModel(names.toTypedArray())
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
                val allowedTypes = if (builderModeCheck.isSelected) {
                    setOf(SearchItemType.WORD)
                } else {
                    null
                }
                val results = searchEngine.search(query, 50, allowedTypes)
                listModel.clear()
                results.forEach { listModel.addElement(it) }
                if (results.isNotEmpty()) {
                    resultList.selectedIndex = 0
                } else {
                    detailArea.text = ""
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
                builderSqlButton.isEnabled = name.isNotBlank()
                builderPreview.revalidate()
                builderPreview.repaint()
            }

            fun updateActionButtons() {
                val item = resultList.selectedValue
                val canAdd = builderModeCheck.isSelected && item?.type == SearchItemType.WORD
                addBuilderMenuItem?.isEnabled = canAdd
                sqlButton.isEnabled = item?.type == SearchItemType.TERM
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

            fun addSelectedWordToBuilder(item: SearchItem) {
                if (item.type != SearchItemType.WORD) return
                val word = datasetRepository.getDataset()
                    .words
                    .getOrNull((item.payloadRef as WordRef).index)
                if (word != null) {
                    builder.add(word)
                    updateBuilderPreview()
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

            fun buildSqlForSelectedTerm(): String? {
                val item = resultList.selectedValue ?: return null
                if (item.type != SearchItemType.TERM) {
                    Messages.showInfoMessage(project, "TERM을 선택하세요.", "SQL 생성")
                    return null
                }
                val term = datasetRepository.getDataset().terms.getOrNull((item.payloadRef as TermRef).index)
                if (term == null) {
                    Messages.showInfoMessage(project, "선택한 TERM을 찾지 못했습니다.", "SQL 생성")
                    return null
                }
                val domain = findDomainByName(term.domainName) ?: defaultDomain()
                val dialect = DbDialect.fromName(settings.state.dbDialect)
                val format = sqlFormatCombo.selectedItem as SqlFormat
                val columnName = term.abbr?.takeIf { it.isNotBlank() } ?: term.koName
                val result = sqlGenerator.generateColumnSql(
                    columnName = columnName,
                    domain = domain,
                    description = term.koName,
                    dialect = dialect,
                    format = format,
                )
                return result.sql
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

            fun buildSqlForBuilder(): String? {
                val name = builder.buildName()
                if (name.isBlank()) {
                    Messages.showInfoMessage(project, "Builder 결과가 없습니다.", "SQL 생성")
                    return null
                }
                val domain = findDomainForBuilder()
                val dialect = DbDialect.fromName(settings.state.dbDialect)
                val format = sqlFormatCombo.selectedItem as SqlFormat
                val comment = builder.getTokens().joinToString(" ") { it.koName }
                val result = sqlGenerator.generateColumnSql(
                    columnName = name,
                    domain = domain,
                    description = comment,
                    dialect = dialect,
                    format = format,
                )
                return result.sql
            }

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

            resultList.addListSelectionListener {
                val item = resultList.selectedValue ?: return@addListSelectionListener
                detailArea.text = buildDetailText(item)
                updateActionButtons()
            }

            builderModeCheck.addActionListener {
                updateResults(searchField.text)
                updateActionButtons()
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
                    addSelectedWordToBuilder(item)
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
                            if (item.type == SearchItemType.WORD) {
                                addSelectedWordToBuilder(item)
                            }
                        }
                    }
                }

                private fun showPopup(e: java.awt.event.MouseEvent) {
                    val index = resultList.locationToIndex(e.point)
                    if (index >= 0) {
                        resultList.selectedIndex = index
                        updateActionButtons()
                        applyMenuHoverColors(resultList)
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

            caseCombo.addActionListener {
                val style = caseCombo.selectedItem as? WordBuilder.CaseStyle ?: return@addActionListener
                builder.setCaseStyle(style)
                updateBuilderPreview()
            }
            builderClearButton.addActionListener {
                builder.clear()
                updateBuilderPreview()
            }
            builderSqlButton.addActionListener {
                val sql = buildSqlForBuilder()
                if (sql != null) {
                    SqlPreviewDialog(project, sql, ::copyText, ::insertText).show()
                }
            }
            builderPreview.setListener({ _, _ ->
                val text = builder.buildName()
                copyText(text)
                showCopiedToast(text)
            }, null)

            sqlButton.addActionListener {
                val sql = buildSqlForSelectedTerm()
                if (sql != null) {
                    SqlPreviewDialog(project, sql, ::copyText, ::insertText).show()
                }
            }

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
                    addSelectedWordToBuilder(item)
                }
            })

            refreshMeta()
            updateBuilderPreview()
            updateActionButtons()
        }

        private fun applyMenuHoverColors(list: javax.swing.JList<*>) {
            val bg = list.selectionBackground
            val fg = list.selectionForeground
            UIManager.put("MenuItem.selectionBackground", bg)
            UIManager.put("MenuItem.selectionForeground", fg)
            UIManager.put("Menu.selectionBackground", bg)
            UIManager.put("Menu.selectionForeground", fg)
            UIManager.put("MenuItem.acceleratorSelectionForeground", fg)
        }

        private fun defaultDomain(): Domain = DEFAULT_DOMAIN

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

        private fun buildDetailText(item: SearchItem): String {
            val dataset = datasetRepository.getDataset()
            return when (item.type) {
                SearchItemType.TERM -> {
                    val term = dataset.terms.getOrNull((item.payloadRef as TermRef).index)
                    term?.toDetailText().orEmpty()
                }
                SearchItemType.WORD -> {
                    val word = dataset.words.getOrNull((item.payloadRef as WordRef).index)
                    word?.toDetailText().orEmpty()
                }
                SearchItemType.DOMAIN -> {
                    val domain = dataset.domains.getOrNull((item.payloadRef as DomainRef).index)
                    domain?.toDetailText().orEmpty()
                }
            }
        }

        private fun showCopiedToast(text: String) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("StdNamingHound")
                .createNotification("${text} 복사됨", NotificationType.INFORMATION)
                .notify(project)
        }

        private fun Term.toDetailText(): String {
            return buildString {
                append("용어: ").append(koName).append("\n")
                if (!abbr.isNullOrBlank()) append("약어: ").append(abbr).append("\n")
                if (!domainName.isNullOrBlank()) append("도메인: ").append(domainName).append("\n")
                if (!description.isNullOrBlank()) append("설명: ").append(description).append("\n")
                if (synonyms.isNotEmpty()) append("동의어: ").append(synonyms.joinToString(", ")).append("\n")
            }
        }

        private fun Word.toDetailText(): String {
            return buildString {
                append("단어: ").append(koName).append("\n")
                if (!enName.isNullOrBlank()) append("영문명: ").append(enName).append("\n")
                if (!abbr.isNullOrBlank()) append("약어: ").append(abbr).append("\n")
                append("형식단어: ").append(if (isFormWord) "Y" else "N").append("\n")
                if (!description.isNullOrBlank()) append("설명: ").append(description).append("\n")
                if (synonyms.isNotEmpty()) append("동의어: ").append(synonyms.joinToString(", ")).append("\n")
            }
        }

        private fun Domain.toDetailText(): String {
            val size = buildString {
                val len = length?.toString()
                val sc = scale?.toString()
                if (!len.isNullOrBlank()) {
                    append(len)
                    if (!sc.isNullOrBlank()) {
                        append(",")
                        append(sc)
                    }
                }
            }
            return buildString {
                append("도메인: ").append(name).append("\n")
                if (!dataType.isNullOrBlank()) append("타입: ").append(dataType).append("\n")
                if (size.isNotBlank()) append("길이/스케일: ").append(size).append("\n")
                if (!storageFormat.isNullOrBlank()) append("저장형식: ").append(storageFormat).append("\n")
                if (!displayFormat.isNullOrBlank()) append("표시형식: ").append(displayFormat).append("\n")
                if (!allowedValues.isNullOrBlank()) append("허용값: ").append(allowedValues).append("\n")
            }
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

private class SearchItemRenderer : javax.swing.ListCellRenderer<SearchItem> {
    private val panel = JBPanel<JBPanel<*>>(BorderLayout())
    private val title = SimpleColoredComponent()
    private val sub = com.intellij.ui.components.JBLabel()
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
        title.clear()
        title.append(badge, com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
        title.append(" ", com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES)
        title.append(value.titleKo, com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        val en = value.primaryEn
        if (!en.isNullOrBlank()) {
            title.append("  |  ", com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
            title.append(en, com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }
        val abbr = value.abbr
        if (!abbr.isNullOrBlank() && abbr != en) {
            title.append("  |  ", com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
            title.append(abbr, com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }
        sub.text = value.subText.orEmpty()
        val hover = index == hoverIndex && !isSelected
        val fg = when {
            isSelected || hover -> list.selectionForeground
            else -> list.foreground
        }
        title.foreground = fg
        sub.foreground = fg
        panel.background = when {
            isSelected || hover -> list.selectionBackground
            else -> list.background
        }
        panel.border = javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8)
        panel.removeAll()
        panel.add(title, BorderLayout.NORTH)
        panel.add(sub, BorderLayout.SOUTH)
        return panel
    }
}
