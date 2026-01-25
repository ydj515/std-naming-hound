package com.github.ydj515.stdnaminghound.settings

interface StdNamingHoundSettingsListener {
    fun settingsChanged(state: StdNamingHoundSettings.State)
}
