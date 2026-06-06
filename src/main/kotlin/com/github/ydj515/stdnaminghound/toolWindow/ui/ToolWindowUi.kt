package com.github.ydj515.stdnaminghound.toolWindow.ui

import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EditorTextField
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.LinkLabel
import java.awt.Component
import javax.swing.DefaultListModel
import javax.swing.JButton

class ToolWindowUi(
    val root: JBPanel<JBPanel<*>>,
    val searchField: SearchTextField,
    val listModel: DefaultListModel<SearchItem>,
    val resultList: JBList<SearchItem>,
    val termFallbackHint: JBLabel,
    val termFilterCheck: JBCheckBox,
    val wordFilterCheck: JBCheckBox,
    val builderModeCheck: JBCheckBox,
    val settingsButton: JButton,
    val columnsModel: DefaultListModel<ColumnEntry>,
    val columnsList: JBList<ColumnEntry>,
    val builderPreview: LinkLabel<Any?>,
    val tokensPanel: JBPanel<JBPanel<*>>,
    val domainCombo: ComboBox<String>,
    val builderClearButton: JButton,
    val addColumnButton: JButton,
    val outputArea: EditorTextField,
    val outputCopyButton: JButton,
    val clearColumnsButton: JButton,
    val outputLabel: JBLabel,
    val columnsPanel: JBPanel<JBPanel<*>>,
    val stagingPanel: JBPanel<JBPanel<*>>,
    val outputPanel: JBPanel<JBPanel<*>>,
    val topPanel: JBPanel<JBPanel<*>>,
    val mainComponent: Component,
    val renderer: com.github.ydj515.stdnaminghound.toolWindow.ui.renderers.SearchItemRenderer,
)
