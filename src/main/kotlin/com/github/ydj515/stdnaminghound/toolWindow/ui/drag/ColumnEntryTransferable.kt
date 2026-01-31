package com.github.ydj515.stdnaminghound.toolWindow.ui.drag

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

/** 드래그 앤 드롭을 위한 ColumnEntry 전송 객체다. */
class ColumnEntryTransferable(
    private val entry: ColumnEntry,
    private val flavor: DataFlavor,
) : Transferable {
    /** 지원하는 DataFlavor 목록을 반환한다. */
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(flavor)

    /** 지원 여부를 검사한다. */
    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == this.flavor

    /** 실제 전송 데이터를 반환한다. */
    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) {
            throw java.awt.datatransfer.UnsupportedFlavorException(flavor)
        }
        return entry
    }
}
