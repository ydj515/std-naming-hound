package com.github.ydj515.stdnaminghound.toolWindow.handlers

import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettingsListener
import com.github.ydj515.stdnaminghound.builder.WordBuilder
import com.github.ydj515.stdnaminghound.toolWindow.context.ToolWindowContext
import com.github.ydj515.stdnaminghound.toolWindow.logic.ToolWindowLogic
import com.github.ydj515.stdnaminghound.toolWindow.ui.drag.ColumnReorderTransferHandler
import com.github.ydj515.stdnaminghound.toolWindow.util.containsName
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import java.awt.Toolkit
import java.awt.event.KeyEvent
import javax.swing.JComponent

/** UI 이벤트를 로직으로 연결한다. */
class ToolWindowEventBinder(
    private val logic: ToolWindowLogic,
    private val context: ToolWindowContext,
) {
    private val ui = context.ui

    /** UI 컴포넌트에 리스너를 바인딩한다. */
    fun bind() {
        val columnsList = ui.columnsList
        val columnsModel = ui.columnsModel
        val resultList = ui.resultList

        columnsList.transferHandler = ColumnReorderTransferHandler(columnsList) {
            logic.scheduleOutputRefresh()
        }

        val columnsPopupGroup = DefaultActionGroup().apply {
            add(object : DumbAwareAction("컬럼 삭제", null, AllIcons.Actions.DeleteTag) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = columnsList.selectedIndex >= 0
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val index = columnsList.selectedIndex
                    if (index < 0) return
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
            })
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
                    showActionPopup(
                        place = "StdNamingHound.ColumnsPopup",
                        group = columnsPopupGroup,
                        invoker = columnsList,
                        x = e.x,
                        y = e.y,
                    )
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

        ui.builderModeCheck.addActionListener {
            logic.updateResults(ui.searchField.text)
        }
        ui.termFilterCheck.addActionListener { logic.updateResults(ui.searchField.text) }
        ui.wordFilterCheck.addActionListener { logic.updateResults(ui.searchField.text) }

        val resultPopupGroup = DefaultActionGroup().apply {
            add(object : DumbAwareAction("Add to Builder", null, AllIcons.General.Add) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    val item = resultList.selectedValue
                    e.presentation.isEnabled = ui.builderModeCheck.isSelected &&
                        item != null &&
                        logic.canAddToBuilder(item)
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val item = resultList.selectedValue ?: return
                    logic.addSelectedItemToBuilder(item)
                }
            })
            add(object : DumbAwareAction("Insert Name", null, AllIcons.Actions.Edit) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = resultList.selectedValue != null
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val item = resultList.selectedValue ?: return
                    logic.insertText(logic.resolveSelectedText(item))
                }
            })
            add(object : DumbAwareAction("Copy Name", null, AllIcons.Actions.Copy) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = resultList.selectedValue != null
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val item = resultList.selectedValue ?: return
                    logic.copyText(logic.resolveSelectedText(item))
                }
            })
            add(object : DumbAwareAction("Copy SQL Column", null, AllIcons.Actions.Copy) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = resultList.selectedValue?.let(logic::canCopySqlColumn) == true
                }

                override fun actionPerformed(e: AnActionEvent) {
                    val item = resultList.selectedValue ?: return
                    val sql = logic.buildColumnSqlForTerm(item) ?: return
                    logic.copyText(sql)
                    logic.showCopiedToast("SQL Column")
                }
            })
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
                    showActionPopup(
                        place = "StdNamingHound.ResultPopup",
                        group = resultPopupGroup,
                        invoker = resultList,
                        x = e.x,
                        y = e.y,
                    )
                }
            }
        })

        ui.resultList.addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
            override fun mouseMoved(e: java.awt.event.MouseEvent) {
                val index = ui.resultList.locationToIndex(e.point)
                if (index >= 0 && index != ui.resultList.selectedIndex) {
                    ui.resultList.selectedIndex = index
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

        (ui.builderPreview as LinkLabel<*>).setListener(LinkListener { _, _ ->
            val text = context.builder.buildName()
            logic.copyText(text)
            logic.showCopiedToast(text)
        }, null)

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
        logic.updateOutputPreview()
    }

    /** IntelliJ Action System 팝업을 표시해 IDE 기본 메뉴 hover/테마 스타일을 사용한다. */
    private fun showActionPopup(
        place: String,
        group: DefaultActionGroup,
        invoker: JComponent,
        x: Int,
        y: Int,
    ) {
        ActionManager.getInstance()
            .createActionPopupMenu(place, group)
            .component
            .show(invoker, x, y)
    }
}
