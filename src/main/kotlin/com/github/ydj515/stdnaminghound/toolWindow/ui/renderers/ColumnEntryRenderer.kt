package com.github.ydj515.stdnaminghound.toolWindow.ui.renderers

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.ListCellRenderer
import com.intellij.ui.components.JBLabel
import javax.swing.BorderFactory
import javax.swing.JPanel

/** 컬럼 누적 목록의 렌더링을 담당한다. */
class ColumnEntryRenderer : ListCellRenderer<ColumnEntry> {
    private val panel = JPanel(BorderLayout())
    private val handleLabel = JBLabel("\u2261")
    private val textLabel = JBLabel()

    init {
        panel.add(handleLabel, BorderLayout.WEST)
        panel.add(textLabel, BorderLayout.CENTER)
    }

    /** 컬럼 리스트 셀 UI를 구성해 반환한다. */
    override fun getListCellRendererComponent(
        list: JList<out ColumnEntry>,
        value: ColumnEntry,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): java.awt.Component {
        val fg = if (isSelected) list.selectionForeground else list.foreground
        val bg = if (isSelected) list.selectionBackground else list.background

        textLabel.text = value.definition
        textLabel.foreground = fg
        textLabel.background = bg
        textLabel.isOpaque = true
        textLabel.border = BorderFactory.createEmptyBorder(4, 4, 4, 8)

        handleLabel.foreground = fg
        handleLabel.background = bg
        handleLabel.isOpaque = true
        handleLabel.border = BorderFactory.createEmptyBorder(4, 8, 4, 2)

        panel.background = bg
        panel.isOpaque = true
        return panel
    }
}
