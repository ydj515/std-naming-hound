package com.github.ydj515.stdnaminghound.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "StdNamingHoundSettings",
    storages = [Storage("std-naming-hound.xml")],
)
@Service(Service.Level.APP)
class StdNamingHoundSettings : PersistentStateComponent<StdNamingHoundSettings.State> {
    data class State(
        var useCustomOnly: Boolean = false,
        var enableFuzzy: Boolean = true,
        var dbDialect: String = "Postgres",
        var mergePolicy: String = com.github.ydj515.stdnaminghound.storage.MergePolicy.CUSTOM_FIRST.name,
        var customDatasetJson: String? = null,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }
}
