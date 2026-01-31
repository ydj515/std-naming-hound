package com.github.ydj515.stdnaminghound.toolWindow.ui

import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import com.github.ydj515.stdnaminghound.toolWindow.ui.renderers.ColumnEntryRenderer
import com.github.ydj515.stdnaminghound.toolWindow.ui.renderers.SearchItemRenderer
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Box
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.DropMode
import com.intellij.openapi.fileTypes.PlainTextFileType

/** ToolWindow UI 컴포넌트를 생성한다. */
class ToolWindowUiBuilder {
    /** 프로젝트 컨텍스트를 사용해 UI를 구성한다. */
    fun build(project: Project): ToolWindowUi {
        val root = JBPanel<JBPanel<*>>().apply { layout = BorderLayout() }

        val searchField = SearchTextField()
        val listModel = DefaultListModel<SearchItem>()
        val resultList = JBList(listModel)
        val termFilterCheck = JCheckBox("용어", true)
        val wordFilterCheck = JCheckBox("단어", true)
        val builderModeCheck = JCheckBox("Builder")
        val settingsButton = JButton(AllIcons.General.GearPlain).apply {
            toolTipText = "open settings"
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
        val builderPreview = LinkLabel<Any?>().apply {
            text = ""
            toolTipText = ""
        }
        val tokensPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        val domainCombo = JComboBox<String>()
        domainCombo.preferredSize = JBUI.size(JBUI.scale(165), domainCombo.preferredSize.height)
        domainCombo.maximumSize = JBUI.size(JBUI.scale(165), domainCombo.preferredSize.height)
        val previewHeight = JBUI.scale(24)
        builderPreview.preferredSize = JBUI.size(JBUI.scale(220), previewHeight)
        builderPreview.minimumSize = JBUI.size(JBUI.scale(120), previewHeight)
        builderPreview.maximumSize = java.awt.Dimension(Int.MAX_VALUE, previewHeight)
        val builderClearButton = JButton(AllIcons.Actions.GC).apply { toolTipText = "Clear Builder" }
        val addColumnButton = JButton(AllIcons.General.Add).apply { toolTipText = "Add Stage" }
        val outputArea = EditorTextField("", project, PlainTextFileType.INSTANCE).apply {
            isViewer = true
            setOneLineMode(false)
            preferredSize = Dimension(600, 160)
        }
        val outputCopyButton = JButton(AllIcons.Actions.Copy).apply { toolTipText = "SQL 복사" }
        val outputLabel = JBLabel()
        val clearColumnsButton = JButton(AllIcons.Actions.GC).apply { toolTipText = "Clear Stage" }

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
                java.awt.GridBagConstraints().apply { anchor = java.awt.GridBagConstraints.CENTER }
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
            val header = JPanel(BorderLayout()).apply {
                add(JBLabel("Stage"), BorderLayout.WEST)
                add(clearColumnsButton, BorderLayout.EAST)
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
        val outputPanel = JPanel(BorderLayout()).apply {
            val header = JPanel(BorderLayout()).apply {
                add(outputLabel, BorderLayout.WEST)
                add(outputCopyButton, BorderLayout.EAST)
            }
            add(header, BorderLayout.NORTH)
            add(outputArea, BorderLayout.CENTER)
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
            termFilterCheck = termFilterCheck,
            wordFilterCheck = wordFilterCheck,
            builderModeCheck = builderModeCheck,
            settingsButton = settingsButton,
            columnsModel = columnsModel,
            columnsList = columnsList,
            metaLabel = metaLabel,
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
