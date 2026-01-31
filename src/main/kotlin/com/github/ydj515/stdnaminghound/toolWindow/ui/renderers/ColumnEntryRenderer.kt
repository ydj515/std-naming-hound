package com.github.ydj515.stdnaminghound.toolWindow.ui.renderers

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import javax.swing.JList
import javax.swing.ListCellRenderer
import com.intellij.ui.components.JBLabel
import javax.swing.BorderFactory

class ColumnEntryRenderer : ListCellRenderer<ColumnEntry> {
    private val label = JBLabel()

    override fun getListCellRendererComponent(
        list: JList<out ColumnEntry>,
        value: ColumnEntry,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): java.awt.Component {
        label.text = value.definition
        label.foreground = if (isSelected) list.selectionForeground else list.foreground
        label.background = if (isSelected) list.selectionBackground else list.background
        label.isOpaque = true
        label.border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        return label
    }
}
