package com.solar.ev.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LogoutEventManager {
    private val _logoutEvent = MutableSharedFlow<Unit>(replay = 0) // replay=0 for one-time event
    val logoutEvent = _logoutEvent.asSharedFlow()

    suspend fun triggerLogout() {
        _logoutEvent.emit(Unit)
    }
}
