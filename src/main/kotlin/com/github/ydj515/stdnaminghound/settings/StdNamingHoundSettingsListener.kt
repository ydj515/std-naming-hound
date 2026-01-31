package com.github.ydj515.stdnaminghound.settings

/** 설정 변경 이벤트를 수신하는 리스너다. */
interface StdNamingHoundSettingsListener {
    /** 설정 변경 시 호출된다. */
    fun settingsChanged(state: StdNamingHoundSettings.State)
}
