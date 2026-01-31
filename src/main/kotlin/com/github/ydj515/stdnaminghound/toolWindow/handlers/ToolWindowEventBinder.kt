package com.github.ydj515.stdnaminghound.toolWindow.handlers

import com.github.ydj515.stdnaminghound.settings.StdNamingHoundConfigurable
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettingsListener
import com.github.ydj515.stdnaminghound.builder.WordBuilder
import com.github.ydj515.stdnaminghound.toolWindow.context.ToolWindowContext
import com.github.ydj515.stdnaminghound.toolWindow.logic.ToolWindowLogic
import com.github.ydj515.stdnaminghound.toolWindow.ui.drag.ColumnReorderTransferHandler
import com.github.ydj515.stdnaminghound.toolWindow.util.containsName
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.ui.Messages
import java.awt.Toolkit
import java.awt.event.KeyEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JComponent

class ToolWindowEventBinder(
    private val logic: ToolWindowLogic,
    private val context: ToolWindowContext,
) {
    private val ui = context.ui
    private var addBuilderMenuItem: JMenuItem? = null

    fun bind() {
        val columnsList = ui.columnsList
        val columnsModel = ui.columnsModel
        val resultList = ui.resultList

        columnsList.transferHandler = ColumnReorderTransferHandler(columnsList) {
            logic.scheduleOutputRefresh()
        }

        val columnsPopup = JPopupMenu().apply {
            val removeItem = JMenuItem("컬럼 삭제", AllIcons.Actions.DeleteTag)
            add(removeItem)
            removeItem.addActionListener {
                val index = columnsList.selectedIndex
                if (index < 0) return@addActionListener
                val entry = columnsModel.getElementAt(index)
                val result = Messages.showYesNoDialog(
                    context.toolWindow.project,
                    "${entry.name} 컬럼을 삭제하시겠습니까?",
                    "컬럼 삭제",
                    null
                )
                if (result == Messages.YES) {
                    columnsModel.remove(index)
                    logic.scheduleOutputRefresh()
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
                    logic.applyPopupTheme(columnsPopup)
                    columnsPopup.updateUI()
                    columnsPopup.show(columnsList, e.x, e.y)
                }
            }
        })

        ui.searchField.textEditor.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = schedule()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = schedule()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = schedule()

            private fun schedule() {
                context.alarm.cancelAllRequests()
                context.alarm.addRequest({
                    val query = ui.searchField.text
                    ApplicationManager.getApplication().invokeLater {
                        logic.updateResults(query)
                    }
                }, 200)
            }
        })

        columnsModel.addListDataListener(object : javax.swing.event.ListDataListener {
            override fun intervalAdded(e: javax.swing.event.ListDataEvent?) = logic.scheduleOutputRefresh()
            override fun intervalRemoved(e: javax.swing.event.ListDataEvent?) = logic.scheduleOutputRefresh()
            override fun contentsChanged(e: javax.swing.event.ListDataEvent?) = logic.scheduleOutputRefresh()
        })

        ui.resultList.addListSelectionListener {
            ui.resultList.selectedValue ?: return@addListSelectionListener
            logic.updateActionButtons(addBuilderMenuItem)
        }

        ui.builderModeCheck.addActionListener {
            logic.updateResults(ui.searchField.text)
            logic.updateActionButtons(addBuilderMenuItem)
        }
        ui.termFilterCheck.addActionListener { logic.updateResults(ui.searchField.text) }
        ui.wordFilterCheck.addActionListener { logic.updateResults(ui.searchField.text) }

        val popupMenu = JPopupMenu().apply {
            addBuilderMenuItem = JMenuItem("Add to Builder", AllIcons.General.Add)
            val insertItem = JMenuItem("Insert Name", AllIcons.Actions.Edit)
            val copyItem = JMenuItem("Copy Name", AllIcons.Actions.Copy)
            add(addBuilderMenuItem)
            add(insertItem)
            add(copyItem)
            copyItem.addActionListener {
                val item = resultList.selectedValue ?: return@addActionListener
                logic.copyText(logic.resolveSelectedText(item))
            }
            insertItem.addActionListener {
                val item = resultList.selectedValue ?: return@addActionListener
                logic.insertText(logic.resolveSelectedText(item))
            }
            addBuilderMenuItem?.addActionListener {
                val item = resultList.selectedValue ?: return@addActionListener
                logic.addSelectedItemToBuilder(item)
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
                if (e.clickCount == 2 && ui.builderModeCheck.isSelected) {
                    val index = resultList.locationToIndex(e.point)
                    if (index >= 0) {
                        resultList.selectedIndex = index
                        val item = resultList.selectedValue ?: return
                        logic.addSelectedItemToBuilder(item)
                    }
                }
            }

            private fun showPopup(e: java.awt.event.MouseEvent) {
                val index = resultList.locationToIndex(e.point)
                if (index >= 0) {
                    resultList.selectedIndex = index
                    logic.updateActionButtons(addBuilderMenuItem)
                    logic.applyPopupTheme(popupMenu)
                    popupMenu.updateUI()
                    popupMenu.show(resultList, e.x, e.y)
                }
            }
        })

        ui.resultList.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
            override fun mouseMoved(e: java.awt.event.MouseEvent) {
                val index = ui.resultList.locationToIndex(e.point)
                if (index >= 0 && index != ui.resultList.selectedIndex) {
                    ui.resultList.selectedIndex = index
                    logic.updateActionButtons(addBuilderMenuItem)
                }
                if (ui.renderer.hoverIndex != index) {
                    ui.renderer.hoverIndex = index
                    ui.resultList.repaint()
                }
            }
        })

        ui.resultList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseExited(e: java.awt.event.MouseEvent) {
                if (ui.renderer.hoverIndex != -1) {
                    ui.renderer.hoverIndex = -1
                    ui.resultList.repaint()
                }
            }
        })

        ui.builderClearButton.addActionListener {
            context.builder.clear()
            ui.domainCombo.selectedItem = "선택 안 함"
            logic.updateBuilderPreview()
        }

        ui.addColumnButton.addActionListener {
            val entry = logic.buildColumnEntryForBuilder()
            if (entry != null) {
                if (ui.columnsModel.containsName(entry.name)) {
                    Messages.showInfoMessage(context.toolWindow.project, "이미 추가된 컬럼입니다: ${entry.name}", "컬럼 추가")
                    return@addActionListener
                }
                ui.columnsModel.addElement(entry)
                context.builder.clear()
                ui.domainCombo.selectedItem = "선택 안 함"
                logic.updateBuilderPreview()
            }
        }

        ui.builderPreview.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.button != java.awt.event.MouseEvent.BUTTON1) return
                val text = context.builder.buildName()
                logic.copyText(text)
                logic.showCopiedToast(text)
            }
        })

        val menuMask = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        val inputMap = ui.resultList.getInputMap(JComponent.WHEN_FOCUSED)
        val actionMap = ui.resultList.actionMap
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "insertSelected")
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuMask), "copySelected")
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK), "addBuilder")
        actionMap.put("insertSelected", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val item = ui.resultList.selectedValue ?: return
                logic.insertText(logic.resolveSelectedText(item))
            }
        })
        actionMap.put("copySelected", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val item = ui.resultList.selectedValue ?: return
                logic.copyText(logic.resolveSelectedText(item))
            }
        })
        actionMap.put("addBuilder", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val item = ui.resultList.selectedValue ?: return
                logic.addSelectedItemToBuilder(item)
            }
        })

        ui.outputCopyButton.addActionListener {
            val text = logic.buildOutputSql()
            logic.copyText(text)
            if (text.isNotBlank()) {
                logic.showCopiedToast(text)
            }
        }

        ui.settingsButton.addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(context.toolWindow.project, StdNamingHoundConfigurable::class.java)
        }

        ui.clearColumnsButton.addActionListener {
            if (ui.columnsModel.size == 0) return@addActionListener
            val result = Messages.showYesNoDialog(
                context.toolWindow.project,
                "모든 컬럼을 삭제하시겠습니까?",
                "컬럼 삭제",
                null
            )
            if (result == Messages.YES) {
                ui.columnsModel.removeAllElements()
                logic.scheduleOutputRefresh()
            }
        }

        val connection = ApplicationManager.getApplication().messageBus.connect(context.toolWindow.disposable)
        connection.subscribe(StdNamingHoundSettings.TOPIC, object : StdNamingHoundSettingsListener {
            override fun settingsChanged(state: StdNamingHoundSettings.State) {
                val style = runCatching {
                    WordBuilder.CaseStyle.valueOf(state.defaultCaseStyle)
                }.getOrDefault(WordBuilder.CaseStyle.SNAKE_UPPER)
                context.currentCaseStyle = style
                context.builder.setCaseStyle(context.currentCaseStyle)
                logic.updateBuilderPreview()
                logic.updateOutputLabel()
                logic.rebuildColumnEntriesForDialect()
                logic.scheduleOutputRefresh()
            }
        })

        logic.refreshMeta()
        context.currentCaseStyle = runCatching {
            WordBuilder.CaseStyle.valueOf(context.settings.state.defaultCaseStyle)
        }.getOrDefault(WordBuilder.CaseStyle.SNAKE_UPPER)
        context.builder.setCaseStyle(context.currentCaseStyle)
        logic.updateBuilderPreview()
        logic.updateActionButtons(addBuilderMenuItem)
        logic.updateOutputPreview()
    }
}
