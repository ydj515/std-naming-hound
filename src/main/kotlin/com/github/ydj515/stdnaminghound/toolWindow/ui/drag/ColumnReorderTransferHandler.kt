package com.github.ydj515.stdnaminghound.toolWindow.ui.drag

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.DefaultListModel
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

/** 컬럼 목록의 드래그 재정렬을 처리한다. */
class ColumnReorderTransferHandler(
    private val list: JList<ColumnEntry>,
    private val onChanged: () -> Unit,
) : TransferHandler() {
    private val flavor = DataFlavor(ColumnEntry::class.java, "ColumnEntry")
    private var fromIndex = -1

    /** 드래그 가능한 액션을 정의한다. */
    override fun getSourceActions(c: JComponent): Int = MOVE

    /** 드래그 시작 시 전송 객체를 생성한다. */
    override fun createTransferable(c: JComponent): Transferable? {
        fromIndex = list.selectedIndex
        val value = list.selectedValue ?: return null
        return ColumnEntryTransferable(value, flavor)
    }

    /** 드롭 가능 여부를 판단한다. */
    override fun canImport(support: TransferSupport): Boolean {
        return support.isDrop && support.isDataFlavorSupported(flavor)
    }

    /** 드롭 처리로 리스트 순서를 변경한다. */
    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) return false
        val dropLocation = support.dropLocation as JList.DropLocation
        val toIndex = dropLocation.index
        val model = list.model as DefaultListModel<ColumnEntry>
        val data = support.transferable.getTransferData(flavor) as ColumnEntry
        if (fromIndex < 0 || toIndex < 0) return false
        model.remove(fromIndex)
        val insertIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
        model.add(insertIndex, data)
        list.selectedIndex = insertIndex
        fromIndex = -1
        onChanged()
        return true
    }
}
