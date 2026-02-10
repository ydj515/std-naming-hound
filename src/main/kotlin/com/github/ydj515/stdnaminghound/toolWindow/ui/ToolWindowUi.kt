package com.github.ydj515.stdnaminghound.toolWindow.ui

import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import com.intellij.ui.EditorTextField
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.LinkLabel
import java.awt.Component
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel

class ToolWindowUi(
    val root: JBPanel<JBPanel<*>>,
    val searchField: SearchTextField,
    val listModel: DefaultListModel<SearchItem>,
    val resultList: JBList<SearchItem>,
    val termFilterCheck: JCheckBox,
    val wordFilterCheck: JCheckBox,
    val builderModeCheck: JCheckBox,
    val settingsButton: JButton,
    val columnsModel: DefaultListModel<ColumnEntry>,
    val columnsList: JBList<ColumnEntry>,
    val builderPreview: LinkLabel<Any?>,
    val tokensPanel: JPanel,
    val domainCombo: JComboBox<String>,
    val builderClearButton: JButton,
    val addColumnButton: JButton,
    val outputArea: EditorTextField,
    val outputCopyButton: JButton,
    val clearColumnsButton: JButton,
    val outputLabel: JBLabel,
    val columnsPanel: JPanel,
    val stagingPanel: JPanel,
    val outputPanel: JPanel,
    val topPanel: JPanel,
    val mainComponent: Component,
    val renderer: com.github.ydj515.stdnaminghound.toolWindow.ui.renderers.SearchItemRenderer,
)
