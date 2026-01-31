package com.github.ydj515.stdnaminghound.toolWindow.ui.renderers

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import javax.swing.JList
import javax.swing.ListCellRenderer
import com.intellij.ui.components.JBLabel
import javax.swing.BorderFactory

/** 컬럼 누적 목록의 렌더링을 담당한다. */
class ColumnEntryRenderer : ListCellRenderer<ColumnEntry> {
    private val label = JBLabel()

    /** 컬럼 리스트 셀 UI를 구성해 반환한다. */
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
