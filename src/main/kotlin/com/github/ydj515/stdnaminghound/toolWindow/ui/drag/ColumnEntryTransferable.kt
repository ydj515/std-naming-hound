package com.github.ydj515.stdnaminghound.toolWindow.ui.drag

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

class ColumnEntryTransferable(
    private val entry: ColumnEntry,
    private val flavor: DataFlavor,
) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(flavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == this.flavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) {
            throw java.awt.datatransfer.UnsupportedFlavorException(flavor)
        }
        return entry
    }
}
