package com.github.ydj515.stdnaminghound.toolWindow.context

import com.github.ydj515.stdnaminghound.builder.WordBuilder
import com.github.ydj515.stdnaminghound.search.SearchEngine
import com.github.ydj515.stdnaminghound.search.SearchIndexRepository
import com.github.ydj515.stdnaminghound.settings.StdNamingHoundSettings
import com.github.ydj515.stdnaminghound.sql.SqlGenerator
import com.github.ydj515.stdnaminghound.storage.DatasetRepository
import com.github.ydj515.stdnaminghound.toolWindow.ui.ToolWindowUi
import com.intellij.openapi.wm.ToolWindow
import com.intellij.util.Alarm

/** ToolWindow에서 공유하는 서비스/상태/컴포넌트 묶음이다. */
class ToolWindowContext(
    val toolWindow: ToolWindow,
    val datasetRepository: DatasetRepository,
    val searchIndexRepository: SearchIndexRepository,
    val searchEngine: SearchEngine,
    val settings: StdNamingHoundSettings,
    val sqlGenerator: SqlGenerator,
    val builder: WordBuilder,
    val alarm: Alarm,
    val outputAlarm: Alarm,
    val ui: ToolWindowUi,
) {
    /** 현재 적용 중인 케이스 스타일을 보관한다. */
    var currentCaseStyle: WordBuilder.CaseStyle = WordBuilder.CaseStyle.SNAKE_UPPER
}
