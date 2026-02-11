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
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.datatransfer.StringSelection
import kotlin.math.abs
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

/** ToolWindow의 비즈니스 로직을 담당한다. */
class ToolWindowLogic(private val context: ToolWindowContext) {
    private val ui = context.ui
    private val project = context.toolWindow.project
    private val tokenDnDHelper = TokenDnDHelper()

    /** 도메인 콤보박스를 갱신한다. */
    fun refreshDomainCombo(domains: List<Domain>) {
        val names = mutableListOf("선택 안 함")
        names.addAll(domains.map { it.name })
        ui.domainCombo.model = DefaultComboBoxModel(names.toTypedArray())
    }

    /** 도메인 이름을 기준으로 콤보 선택값을 변경한다. */
    fun selectDomainByName(name: String?) {
        val target = name?.trim().orEmpty()
        if (target.isBlank()) return
        ui.domainCombo.selectedItem = target
    }

    /** 데이터셋 메타 정보를 UI에 반영한다. */
    fun refreshMeta() {
        val dataset = context.datasetRepository.getDataset()
        refreshDomainCombo(dataset.domains)
    }

    /** 검색 결과를 갱신한다. */
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

    /** Builder 프리뷰와 토큰 UI를 갱신한다. */
    fun updateBuilderPreview() {
        val name = context.builder.buildName()
        val text = if (name.isBlank()) "" else name
        ui.builderPreview.text = text
        ui.builderPreview.toolTipText = if (text.isBlank()) null else text
        ui.tokensPanel.removeAll()
        val tokens = context.builder.getTokens()
        if (tokens.isEmpty()) {
            val emptyHeight = javax.swing.JButton("A").preferredSize.height
            ui.tokensPanel.add(javax.swing.Box.createRigidArea(Dimension(0, emptyHeight)))
        }
        tokens.forEachIndexed { index, word ->
            val rawLabel = word.abbr ?: word.koName
            val tokenHeight = ui.addColumnButton.preferredSize.height
            val chip = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                putClientProperty("tokenIndex", index)
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                        javax.swing.UIManager.getColor("Component.borderColor") ?: java.awt.Color.GRAY,
                        1,
                        true
                    ),
                    JBUI.Borders.empty(1, 4, 1, 2)
                )
                isOpaque = true
                background = this@ToolWindowLogic.ui.tokensPanel.background
                toolTipText = rawLabel
            }
            val handleLabel = JBLabel(TOKEN_HANDLE_PREFIX).apply {
                foreground = javax.swing.UIManager.getColor("Label.disabledForeground")
                    ?: javax.swing.UIManager.getColor("Label.foreground")
                alignmentY = Component.CENTER_ALIGNMENT
            }
            val textLabel = JBLabel(rawLabel).apply {
                alignmentY = Component.CENTER_ALIGNMENT
            }
            val removeButton = JButton(AllIcons.Actions.Close).apply {
                isFocusable = false
                isContentAreaFilled = false
                border = JBUI.Borders.empty()
                preferredSize = JBUI.size(14, 14)
                minimumSize = JBUI.size(14, 14)
                cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                toolTipText = "제거"
                alignmentY = Component.CENTER_ALIGNMENT
                addActionListener {
                    context.builder.removeAt(index)
                    updateBuilderPreview()
                }
            }
            chip.add(Box.createHorizontalStrut(JBUI.scale(2)))
            chip.add(handleLabel)
            chip.add(Box.createHorizontalStrut(JBUI.scale(2)))
            chip.add(textLabel)
            chip.add(Box.createHorizontalStrut(JBUI.scale(2)))
            chip.add(removeButton)
            chip.add(Box.createHorizontalStrut(JBUI.scale(1)))
            val currentPref = chip.preferredSize
            val chipSize = Dimension(currentPref.width, tokenHeight)
            chip.preferredSize = chipSize
            chip.minimumSize = chipSize

            val dragListener = TokenDragListener(index)
            chip.addMouseListener(dragListener)
            chip.addMouseMotionListener(dragListener)
            handleLabel.addMouseListener(dragListener)
            handleLabel.addMouseMotionListener(dragListener)
            textLabel.addMouseListener(dragListener)
            textLabel.addMouseMotionListener(dragListener)
            ui.tokensPanel.add(chip)
        }
        ui.tokensPanel.revalidate()
        ui.tokensPanel.repaint()
        ui.builderClearButton.isEnabled = context.builder.getTokens().isNotEmpty()
        ui.addColumnButton.isEnabled = name.isNotBlank()
        ui.builderPreview.revalidate()
        ui.builderPreview.repaint()
    }

    /** 토큰 드래그 앤 드롭 계산/인디케이터 표시를 담당한다. */
    private inner class TokenDnDHelper {
        private val dropIndicator = JPanel().apply {
            val focusColor = javax.swing.UIManager.getColor("Component.focusColor")
                ?: javax.swing.UIManager.getColor("Focus.color")
                ?: DEFAULT_DROP_INDICATOR_COLOR
            background = focusColor
            isOpaque = true
            preferredSize = Dimension(JBUI.scale(2), JBUI.scale(24))
        }

        fun resolveInsertionIndex(point: Point): Int {
            val tokenChips = ui.tokensPanel.components
                .filterIsInstance<JPanel>()
                .filter { it.getClientProperty("tokenIndex") is Int }
            if (tokenChips.isEmpty()) return 0
            val ordered = tokenChips.sortedBy { it.x }
            ordered.forEachIndexed { idx, chip ->
                val centerX = chip.x + (chip.width / 2)
                if (point.x < centerX) return idx
            }
            return ordered.size
        }

        fun showDropIndicator(insertionIndex: Int) {
            ui.tokensPanel.remove(dropIndicator)
            val count = ui.tokensPanel.componentCount
            val safeIndex = insertionIndex.coerceIn(0, count)
            ui.tokensPanel.add(dropIndicator, safeIndex)
            ui.tokensPanel.revalidate()
            ui.tokensPanel.repaint()
        }

        fun hideDropIndicator() {
            if (dropIndicator.parent == ui.tokensPanel) {
                ui.tokensPanel.remove(dropIndicator)
                ui.tokensPanel.revalidate()
                ui.tokensPanel.repaint()
            }
        }
    }

    /** 토큰 칩 드래그를 처리한다. */
    private inner class TokenDragListener(
        private val tokenIndex: Int,
    ) : java.awt.event.MouseAdapter() {
        private var startPoint: Point? = null
        private val dragThreshold = JBUI.scale(4)

        override fun mousePressed(e: java.awt.event.MouseEvent) {
            startPoint = SwingUtilities.convertPoint(e.component, e.point, ui.tokensPanel)
        }

        override fun mouseDragged(e: java.awt.event.MouseEvent) {
            val currentPoint = SwingUtilities.convertPoint(e.component, e.point, ui.tokensPanel)
            if (!isDragged(currentPoint)) return
            val insertionIndex = tokenDnDHelper.resolveInsertionIndex(currentPoint)
            tokenDnDHelper.showDropIndicator(insertionIndex)
        }

        override fun mouseReleased(e: java.awt.event.MouseEvent) {
            tokenDnDHelper.hideDropIndicator()
            val currentPoint = SwingUtilities.convertPoint(e.component, e.point, ui.tokensPanel)
            if (!isDragged(currentPoint)) {
                startPoint = null
                return
            }
            val insertionIndex = tokenDnDHelper.resolveInsertionIndex(currentPoint)
            var targetIndex = insertionIndex
            if (targetIndex > tokenIndex) targetIndex -= 1
            if (targetIndex != tokenIndex) {
                context.builder.move(tokenIndex, targetIndex)
                updateBuilderPreview()
            }
            startPoint = null
        }

        private fun isDragged(currentPoint: Point): Boolean {
            val start = startPoint ?: return false
            return abs(currentPoint.x - start.x) >= dragThreshold ||
                abs(currentPoint.y - start.y) >= dragThreshold
        }
    }

    /** 선택 상태에 따라 컨텍스트 메뉴 활성화를 갱신한다. */
    fun updateActionButtons(addBuilderMenuItem: JMenuItem?) {
        val item = ui.resultList.selectedValue
        val canAdd = ui.builderModeCheck.isSelected &&
                (item?.type == SearchItemType.WORD || item?.type == SearchItemType.TERM)
        addBuilderMenuItem?.isEnabled = canAdd
    }

    /** 텍스트를 클립보드에 복사한다. */
    fun copyText(text: String) {
        if (text.isBlank()) return
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    /** 현재 에디터 커서 위치에 텍스트를 삽입한다. */
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

    /** 선택 항목의 복사/삽입 대상 텍스트를 결정한다. */
    fun resolveSelectedText(item: SearchItem): String {
        return when (item.type) {
            SearchItemType.TERM -> item.abbr ?: item.primaryEn ?: item.titleKo
            SearchItemType.WORD -> item.abbr ?: item.primaryEn ?: item.titleKo
            SearchItemType.DOMAIN -> item.titleKo
        }
    }

    /** 선택 항목을 Builder에 추가한다. */
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

    /** 도메인 이름으로 Domain을 찾는다. */
    fun findDomainByName(name: String?): Domain? {
        val key = name?.trim().orEmpty()
        if (key.isBlank()) return null
        return context.datasetRepository.getDataset().domains.firstOrNull { it.name == key }
    }

    /** 콤보에서 선택된 도메인을 반환한다. */
    fun selectedDomainFromCombo(): Domain? {
        val selected = ui.domainCombo.selectedItem as? String ?: return null
        if (selected == "선택 안 함") return null
        return findDomainByName(selected)
    }

    /** Builder 기준으로 사용할 도메인을 결정한다. */
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

    /** Builder 결과로 컬럼 엔트리를 생성한다. */
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

    /** 스테이징된 컬럼 목록으로 컬럼 SQL을 생성한다. */
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

    /** 스테이징된 컬럼 목록으로 CREATE TABLE SQL을 생성한다. */
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

    /** Output 영역에 표시할 SQL을 생성한다. */
    fun buildOutputSql(): String = buildCreateTableFromColumns()

    /** Output 라벨에 현재 DB 방언을 반영한다. */
    fun updateOutputLabel() {
        ui.outputLabel.text = "Output (SQL - ${context.settings.state.dbDialect})"
    }

    /** Output 영역을 즉시 갱신한다. */
    fun updateOutputPreview() {
        updateOutputLabel()
        ui.outputArea.text = buildOutputSql()
    }

    /** Output 영역 갱신을 디바운스한다. */
    fun scheduleOutputRefresh() {
        context.outputAlarm.cancelAllRequests()
        context.outputAlarm.addRequest({ updateOutputPreview() }, 300)
    }

    /** DB 방언 변경에 맞춰 컬럼 정의를 재생성한다. */
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

    /** 복사 완료 알림을 표시한다. */
    fun showCopiedToast(text: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("StdNamingHound")
            .createNotification("${text} 복사됨", NotificationType.INFORMATION)
            .notify(project)
    }

    /** 팝업 메뉴에 테마 색을 적용한다. */
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

    /** 기본 도메인을 반환한다. */
    fun defaultDomain(): Domain = DEFAULT_DOMAIN

    /** 컬럼 정의/코멘트 SQL 분리 결과다. */
    private data class ColumnParts(
        val definition: String,
        val commentSql: String?,
    )

    /** 단일 컬럼 SQL을 정의/코멘트로 분리한다. */
    private fun splitColumnSql(sql: String): ColumnParts {
        val lines = sql.split("\n")
        val definition = lines.firstOrNull()?.trim()?.removeSuffix(";").orEmpty()
        val comment = lines.drop(1).joinToString("\n").trim().ifBlank { null }
        return ColumnParts(definition, comment)
    }

    companion object {
        private const val TOKEN_HANDLE_PREFIX = "\u2261"
        private val DEFAULT_DROP_INDICATOR_COLOR = java.awt.Color(0x4A90E2)

        /** 도메인이 없을 때 사용하는 기본 도메인이다. */
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
