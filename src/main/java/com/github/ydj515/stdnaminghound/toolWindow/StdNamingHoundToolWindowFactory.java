package com.github.ydj515.stdnaminghound.toolWindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

/** ToolWindow 콘텐츠를 등록하는 팩토리다. */
public final class StdNamingHoundToolWindowFactory implements ToolWindowFactory {

    /** ToolWindow 사용 가능 여부를 비동기 권장 API로 반환한다. */
    @Override
    public Object isApplicableAsync(@NotNull Project project, @NotNull Continuation<? super Boolean> $completion) {
        return Boolean.TRUE;
    }

    /** ToolWindow 본문 UI를 생성해 등록한다. */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        StdNamingHoundToolWindow toolWindowContent = new StdNamingHoundToolWindow(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContent(), null, false);
        toolWindow.getContentManager().addContent(content);
    }
}
