package com.github.ydj515.stdnaminghound.services

import com.github.ydj515.stdnaminghound.StdNamingHoundBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class StdNamingHoundProjectService(project: Project) {

    init {
        thisLogger().info(StdNamingHoundBundle.message("projectService", project.name))
    }
}
