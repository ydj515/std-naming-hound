package com.github.ydj515.stdnaminghound.toolWindow.ui

import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import com.github.ydj515.stdnaminghound.toolWindow.ui.renderers.ColumnEntryRenderer
import com.github.ydj515.stdnaminghound.toolWindow.ui.renderers.SearchItemRenderer
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.DefaultListModel
import javax.swing.JSplitPane
import javax.swing.DropMode
import javax.swing.JButton
import com.intellij.openapi.fileTypes.PlainTextFileType

/** ToolWindow UI 컴포넌트를 생성한다. */
class ToolWindowUiBuilder {
    /** 프로젝트 컨텍스트를 사용해 UI를 구성한다. */
    fun build(project: Project): ToolWindowUi {
        val root = JBPanel<JBPanel<*>>().apply { layout = BorderLayout() }

        val searchField = SearchTextField()
        val listModel = DefaultListModel<SearchItem>()
        val resultList = JBList(listModel)
        val termFallbackHint = JBLabel("표준 용어(TERM)가 없습니다. 표준 단어(WORD)를 조합해 생성하세요.").apply {
            icon = AllIcons.General.ContextHelp
            foreground = UIUtil.getContextHelpForeground()
            border = JBUI.Borders.empty(JBUI.scale(4), JBUI.scale(12), JBUI.scale(4), 0)
            isVisible = false
        }
        val termFilterCheck = JBCheckBox("용어", true)
        val wordFilterCheck = JBCheckBox("단어", true)
        val builderModeCheck = JBCheckBox("Builder")
        val settingsButton = JButton(AllIcons.General.GearPlain).apply {
            toolTipText = "open settings"
            isFocusable = false
            preferredSize = JBUI.size(28, 28)
        }
        val columnsModel = DefaultListModel<ColumnEntry>()
        val columnsList = JBList(columnsModel).apply {
            cellRenderer = ColumnEntryRenderer()
            dragEnabled = true
            dropMode = DropMode.INSERT
        }
        val builderPreview = LinkLabel<Any?>().apply {
            text = ""
            toolTipText = ""
        }
        val tokensPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0))
        val domainCombo = ComboBox<String>()
        domainCombo.preferredSize = JBUI.size(165, domainCombo.preferredSize.height)
        domainCombo.maximumSize = JBUI.size(165, domainCombo.preferredSize.height)
        val previewHeight = 24
        builderPreview.preferredSize = JBUI.size(180, previewHeight)
        builderPreview.minimumSize = JBUI.size(120, previewHeight)
        builderPreview.maximumSize = java.awt.Dimension(Int.MAX_VALUE, JBUI.scale(previewHeight))
        val builderClearButton = JButton(AllIcons.Actions.GC).apply { toolTipText = "Clear Builder" }
        val addColumnButton = JButton(AllIcons.General.Add).apply { toolTipText = "Add Stage" }
        val outputArea = EditorTextField("", project, PlainTextFileType.INSTANCE).apply {
            isViewer = true
            setOneLineMode(false)
            preferredSize = JBUI.size(600, 160)
        }
        val outputCopyButton = JButton(AllIcons.Actions.Copy).apply { toolTipText = "SQL 복사" }
        val outputLabel = JBLabel()
        val clearColumnsButton = JButton(AllIcons.Actions.GC).apply {
            toolTipText = "Stage 전체 비우기"
            isFocusable = false
            preferredSize = JBUI.size(28, 28)
        }

        val renderer = SearchItemRenderer()
        resultList.cellRenderer = renderer

        val buttonRow = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            add(termFilterCheck)
            add(wordFilterCheck)
            add(builderModeCheck)
            add(settingsButton)
        }
        val buttonBar = JBPanel<JBPanel<*>>(java.awt.GridBagLayout()).apply {
            add(
                buttonRow,
                java.awt.GridBagConstraints().apply { anchor = java.awt.GridBagConstraints.CENTER }
            )
        }
        val topBar = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(searchField, BorderLayout.CENTER)
            add(buttonBar, BorderLayout.EAST)
        }
        val searchHeader = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(topBar, BorderLayout.NORTH)
            add(termFallbackHint, BorderLayout.SOUTH)
        }
        val center = JBScrollPane(resultList)
        val leftInset = 12
        val iconColumnWidth = 20
        val iconContentGap = 8
        tokensPanel.border = JBUI.Borders.empty()
        val builderControls = JBPanel<JBPanel<*>>().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            val row2 = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                val iconPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    add(JBLabel(AllIcons.Actions.Preview), BorderLayout.CENTER)
                }.apply {
                    preferredSize = JBUI.size(iconColumnWidth, preferredSize.height)
                    minimumSize = JBUI.size(iconColumnWidth, minimumSize.height)
                    maximumSize = JBUI.size(iconColumnWidth, maximumSize.height)
                }
                val contentPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                    add(builderPreview, BorderLayout.CENTER)
                }
                border = JBUI.Borders.empty(0, leftInset, 0, 0)
                add(iconPanel, BorderLayout.WEST)
                add(contentPanel, BorderLayout.CENTER)
            }
            val row3 = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                val iconPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    add(JBLabel(AllIcons.Nodes.DataTables), BorderLayout.CENTER)
                }.apply {
                    preferredSize = JBUI.size(iconColumnWidth, preferredSize.height)
                    minimumSize = JBUI.size(iconColumnWidth, minimumSize.height)
                    maximumSize = JBUI.size(iconColumnWidth, maximumSize.height)
                }
                val buttonPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
                    add(addColumnButton)
                    add(builderClearButton)
                }
                val inlinePanel = object : JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)) {
                    override fun doLayout() {
                        val requiredWidth = domainCombo.preferredSize.width + buttonPanel.preferredSize.width + JBUI.scale(8)
                        buttonPanel.isVisible = width >= requiredWidth
                        super.doLayout()
                    }
                }.apply {
                    add(domainCombo)
                    add(buttonPanel)
                }
                val contentPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    border = JBUI.Borders.empty(0, iconContentGap, 0, 0)
                    add(inlinePanel, BorderLayout.WEST)
                }
                border = JBUI.Borders.empty(0, leftInset, 0, 0)
                add(iconPanel, BorderLayout.WEST)
                add(contentPanel, BorderLayout.CENTER)
            }
            add(row2)
            add(row3)
        }
        val builderPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val tokensRow = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                val iconPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                    val iconLabel = JBLabel(AllIcons.General.FitContent)
                    add(iconLabel, BorderLayout.CENTER)
                }
                iconPanel.preferredSize = JBUI.size(iconColumnWidth, iconPanel.preferredSize.height)
                iconPanel.minimumSize = JBUI.size(iconColumnWidth, iconPanel.minimumSize.height)
                iconPanel.maximumSize = JBUI.size(iconColumnWidth, iconPanel.maximumSize.height)
                val contentPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
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
        val columnsPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val stageLabel = JBLabel("Stage")
            val header = object : JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, JBUI.scale(10), 0)) {
                override fun doLayout() {
                    val requiredWidth = stageLabel.preferredSize.width + clearColumnsButton.preferredSize.width + JBUI.scale(12)
                    clearColumnsButton.isVisible = width >= requiredWidth
                    super.doLayout()
                }
            }.apply {
                add(stageLabel)
                add(clearColumnsButton)
            }
            val scroll = JBScrollPane(columnsList).apply {
                preferredSize = JBUI.size(600, 140)
            }
            add(header, BorderLayout.NORTH)
            add(scroll, BorderLayout.CENTER)
        }
        val stagingPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(builderPanel, BorderLayout.NORTH)
            add(columnsPanel, BorderLayout.CENTER)
        }
        val outputPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            val header = object : JBPanel<JBPanel<*>>(BorderLayout()) {
                override fun doLayout() {
                    val requiredWidth = outputLabel.preferredSize.width + outputCopyButton.preferredSize.width + JBUI.scale(12)
                    outputCopyButton.isVisible = width >= requiredWidth
                    super.doLayout()
                }
            }.apply {
                add(outputLabel, BorderLayout.WEST)
                add(outputCopyButton, BorderLayout.EAST)
            }
            add(header, BorderLayout.NORTH)
            add(outputArea, BorderLayout.CENTER)
        }
        val topPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(searchHeader, BorderLayout.NORTH)
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

        root.add(mainSplit, BorderLayout.CENTER)

        val gap = JBUI.scale(6)
        topPanel.border = JBUI.Borders.empty(0, 0, gap, 0)
        stagingPanel.border = JBUI.Borders.empty(gap, 0, gap, 0)
        outputPanel.border = JBUI.Borders.empty(gap, 0, 0, 0)

        return ToolWindowUi(
            root = root,
            searchField = searchField,
            listModel = listModel,
            resultList = resultList,
            termFallbackHint = termFallbackHint,
            termFilterCheck = termFilterCheck,
            wordFilterCheck = wordFilterCheck,
            builderModeCheck = builderModeCheck,
            settingsButton = settingsButton,
            columnsModel = columnsModel,
            columnsList = columnsList,
            builderPreview = builderPreview,
            tokensPanel = tokensPanel,
            domainCombo = domainCombo,
            builderClearButton = builderClearButton,
            addColumnButton = addColumnButton,
            outputArea = outputArea,
            outputCopyButton = outputCopyButton,
            clearColumnsButton = clearColumnsButton,
            outputLabel = outputLabel,
            columnsPanel = columnsPanel,
            stagingPanel = stagingPanel,
            outputPanel = outputPanel,
            topPanel = topPanel,
            mainComponent = mainSplit,
            renderer = renderer,
        )
    }
}
