package com.github.ydj515.stdnaminghound.toolWindow.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Icon
import javax.swing.JComponent

class ToolbarIconButton(
    targetComponent: JComponent,
    text: String,
    description: String,
    icon: Icon,
) : JBPanel<ToolbarIconButton>(BorderLayout()) {
    private val listeners = mutableListOf<ActionListener>()
    private val action = object : DumbAwareAction(text, description, icon) {
        override fun actionPerformed(e: AnActionEvent) {
            if (!isEnabled) return
            val event = ActionEvent(this@ToolbarIconButton, ActionEvent.ACTION_PERFORMED, text)
            listeners.forEach { it.actionPerformed(event) }
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = isEnabled
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
    private val toolbar = ActionManager.getInstance().createActionToolbar(
        ActionPlaces.TOOLBAR,
        DefaultActionGroup(action),
        true,
    )

    init {
        isOpaque = false
        border = JBUI.Borders.empty()

        toolbar.setTargetComponent(targetComponent)
        toolbar.setMiniMode(true)
        toolbar.setMinimumButtonSize(JBUI.size(BUTTON_SIZE, BUTTON_SIZE))

        add(toolbar.component.apply {
            isOpaque = false
            border = JBUI.Borders.empty()
        }, BorderLayout.CENTER)
    }

    fun addActionListener(listener: ActionListener) {
        listeners += listener
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        toolbar.updateActionsAsync()
    }

    companion object {
        private const val BUTTON_SIZE = 28
    }
}
