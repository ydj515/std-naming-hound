package com.github.ydj515.stdnaminghound.startup

import com.github.ydj515.stdnaminghound.StdNamingHoundBundle
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class StdNamingHoundProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().info(StdNamingHoundBundle.message("projectService", project.name))
    }
}
