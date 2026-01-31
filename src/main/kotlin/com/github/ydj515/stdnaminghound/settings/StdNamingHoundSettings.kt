package com.github.ydj515.stdnaminghound.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.github.ydj515.stdnaminghound.builder.WordBuilder

@State(
    name = "StdNamingHoundSettings",
    storages = [Storage("std-naming-hound.xml")],
)
/** 플러그인 전역 설정을 저장/로드한다. */
@Service(Service.Level.APP)
class StdNamingHoundSettings : PersistentStateComponent<StdNamingHoundSettings.State> {
    /** 영속화되는 설정 값 묶음이다. */
    data class State(
        var useCustomOnly: Boolean = false,
        var enableFuzzy: Boolean = true,
        var dbDialect: String = "Postgres",
        var mergePolicy: String = com.github.ydj515.stdnaminghound.storage.MergePolicy.CUSTOM_FIRST.name,
        var defaultCaseStyle: String = WordBuilder.CaseStyle.SNAKE_UPPER.name,
        var customDatasetJson: String? = null,
    )

    private var state = State()

    /** 현재 설정 상태를 반환한다. */
    override fun getState(): State = state

    /** 설정 상태를 로드한다. */
    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        /** 설정 변경 이벤트를 전달하는 메시지 토픽이다. */
        val TOPIC: Topic<StdNamingHoundSettingsListener> =
            Topic.create("StdNamingHoundSettings", StdNamingHoundSettingsListener::class.java)
    }
}
