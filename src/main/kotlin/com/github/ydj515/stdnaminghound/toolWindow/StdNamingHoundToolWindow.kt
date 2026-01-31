package com.github.ydj515.stdnaminghound.toolWindow

import com.github.ydj515.stdnaminghound.builder.WordBuilder
import com.github.ydj515.stdnaminghound.search.SearchEngine
import com.github.ydj515.stdnaminghound.search.SearchIndexRepository
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.github.ydj515.stdnaminghound.sql.SqlGenerator
import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.github.ydj515.stdnaminghound.toolWindow.context.ToolWindowContext
import com.github.ydj515.stdnaminghound.toolWindow.handlers.ToolWindowEventBinder
import com.github.ydj515.stdnaminghound.toolWindow.logic.ToolWindowLogic
import com.github.ydj515.stdnaminghound.toolWindow.ui.ToolWindowUiBuilder
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBPanel
import com.intellij.util.Alarm

/** ToolWindow의 초기 구성과 의존성 연결을 담당한다. */
class StdNamingHoundToolWindow(private val toolWindow: ToolWindow) {
    private val datasetRepository = service<DatasetRepository>()
    private val searchIndexRepository = service<SearchIndexRepository>()
    private val searchEngine = SearchEngine(searchIndexRepository)
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, toolWindow.disposable)
    private val outputAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, toolWindow.disposable)
    private val builder = WordBuilder()
    private val settings = service<StdNamingHoundSettings>()
    private val sqlGenerator = SqlGenerator()

    /** ToolWindow 루트 UI를 생성해 반환한다. */
    fun getContent(): JBPanel<JBPanel<*>> {
        val ui = ToolWindowUiBuilder().build(toolWindow.project)
        val context = ToolWindowContext(
            toolWindow = toolWindow,
            datasetRepository = datasetRepository,
            searchIndexRepository = searchIndexRepository,
            searchEngine = searchEngine,
            settings = settings,
            sqlGenerator = sqlGenerator,
            builder = builder,
            alarm = alarm,
            outputAlarm = outputAlarm,
            ui = ui,
        )
        val logic = ToolWindowLogic(context)
        ToolWindowEventBinder(logic, context).bind()
        return ui.root
    }
}
