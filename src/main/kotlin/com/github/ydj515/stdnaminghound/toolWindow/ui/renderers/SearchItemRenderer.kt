package com.github.ydj515.stdnaminghound.toolWindow.ui.renderers

import com.github.ydj515.stdnaminghound.model.SearchItem
import com.github.ydj515.stdnaminghound.model.SearchItemType
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.BorderFactory

/** 검색 결과 리스트의 렌더링을 담당한다. */
class SearchItemRenderer : ListCellRenderer<SearchItem> {
    private val panel = JBPanel<JBPanel<*>>(BorderLayout())
    private val title = SimpleColoredComponent()
    private val sub = SimpleColoredComponent()
    var hoverIndex: Int = -1

    /** 리스트 셀 UI를 구성해 반환한다. */
    override fun getListCellRendererComponent(
        list: JList<out SearchItem>,
        value: SearchItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): java.awt.Component {
        val badge = when (value.type) {
            SearchItemType.TERM -> "[용어]"
            SearchItemType.WORD -> "[단어]"
            SearchItemType.DOMAIN -> "[도메인]"
        }
        val koName = value.titleKo
        val abbr = value.abbr
        val en = value.primaryEn
        val desc = value.subText.orEmpty()

        title.clear()
        title.append(badge, com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
        title.append(" ", com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES)
        title.append(koName, com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        if (!abbr.isNullOrBlank() && abbr != koName) {
            title.append("  ", com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES)
            title.append("[$abbr]", com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }

        sub.clear()
        val subText = buildString {
            if (!en.isNullOrBlank()) append(en)
            if (desc.isNotBlank()) {
                if (isNotEmpty()) append("  |  ")
                append(ellipsize(desc, 80))
            }
        }
        sub.append(subText, com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
        sub.toolTipText = desc.ifBlank { null }
        val hover = index == hoverIndex && !isSelected
        val fg = when {
            isSelected || hover -> list.selectionForeground
            else -> list.foreground
        }
        title.foreground = fg
        sub.foreground = if (isSelected || hover) fg else JBColor.GRAY
        panel.background = when {
            isSelected || hover -> list.selectionBackground
            else -> list.background
        }
        panel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        panel.removeAll()
        panel.add(title, BorderLayout.NORTH)
        panel.add(sub, BorderLayout.SOUTH)
        return panel
    }

    /** 지정 길이를 넘는 텍스트를 말줄임 처리한다. */
    private fun ellipsize(text: String, max: Int): String {
        val trimmed = text.trim()
        if (max <= 0) return ""
        if (max <= 3) return trimmed.take(max)
        if (trimmed.length <= max) return trimmed
        return trimmed.substring(0, max - 3) + "..."
    }
}
