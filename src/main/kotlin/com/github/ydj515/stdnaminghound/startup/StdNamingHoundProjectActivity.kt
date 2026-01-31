package com.github.ydj515.stdnaminghound.startup

import com.github.ydj515.stdnaminghound.StdNamingHoundBundle
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** 프로젝트 시작 시 로그를 남기는 액티비티다. */
class StdNamingHoundProjectActivity : ProjectActivity {

    /** 프로젝트 로딩 후 호출되는 진입점이다. */
    override suspend fun execute(project: Project) {
        thisLogger().info(StdNamingHoundBundle.message("projectService", project.name))
    }
}
