package com.github.ydj515.stdnaminghound.toolWindow.util

import com.github.ydj515.stdnaminghound.toolWindow.ColumnEntry
import javax.swing.DefaultListModel

/** 컬럼 이름 중복 여부를 검사한다. */
fun DefaultListModel<ColumnEntry>.containsName(name: String): Boolean {
    return (0 until size).any {
        getElementAt(it).name == name
    }
}
