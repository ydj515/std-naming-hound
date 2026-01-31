package com.github.ydj515.stdnaminghound.services

import com.github.ydj515.stdnaminghound.StdNamingHoundBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/** 프로젝트 단위 서비스로 초기 로그를 남긴다. */
@Service(Service.Level.PROJECT)
class StdNamingHoundProjectService(project: Project) {

    init {
        thisLogger().info(StdNamingHoundBundle.message("projectService", project.name))
    }
}
