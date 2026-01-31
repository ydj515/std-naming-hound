package com.github.ydj515.stdnaminghound.toolWindow.ui.drag

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.DefaultListModel
import javax.swing.DropMode
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

class ColumnReorderTransferHandler(
    private val list: JList<ColumnEntry>,
    private val onChanged: () -> Unit,
) : TransferHandler() {
    private val flavor = DataFlavor(ColumnEntry::class.java, "ColumnEntry")
    private var fromIndex = -1

    override fun getSourceActions(c: JComponent): Int = MOVE

    override fun createTransferable(c: JComponent): Transferable? {
        fromIndex = list.selectedIndex
        val value = list.selectedValue ?: return null
        return ColumnEntryTransferable(value, flavor)
    }

    override fun canImport(support: TransferSupport): Boolean {
        return support.isDrop && support.isDataFlavorSupported(flavor)
    }

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
