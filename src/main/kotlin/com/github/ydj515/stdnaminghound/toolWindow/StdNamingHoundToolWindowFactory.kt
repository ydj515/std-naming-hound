package com.github.ydj515.stdnaminghound.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** ToolWindow 콘텐츠를 등록하는 팩토리다. */
class StdNamingHoundToolWindowFactory : ToolWindowFactory {

    /** ToolWindow 본문 UI를 생성해 등록한다. */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = StdNamingHoundToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    /** ToolWindow 사용 가능 여부를 반환한다. */
    override fun shouldBeAvailable(project: Project) = true
}
