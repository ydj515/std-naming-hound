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
    var currentCaseStyle: WordBuilder.CaseStyle = WordBuilder.CaseStyle.SNAKE_UPPER
}
