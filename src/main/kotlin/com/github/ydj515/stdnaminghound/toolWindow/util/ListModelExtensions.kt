package com.github.ydj515.stdnaminghound.toolWindow.util

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import javax.swing.DefaultListModel

fun DefaultListModel<ColumnEntry>.containsName(name: String): Boolean {
    for (i in 0 until size) {
        if (getElementAt(i).name == name) return true
    }
    return false
}
