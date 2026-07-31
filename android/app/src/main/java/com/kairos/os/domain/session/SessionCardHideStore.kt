package com.kairos.os.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCardHideStore @Inject constructor() {

    private val hiddenKeys = mutableSetOf<String>()
    private val _hiddenKeysFlow = MutableStateFlow<Set<String>>(emptySet())
    val hiddenKeysFlow: StateFlow<Set<String>> = _hiddenKeysFlow.asStateFlow()

    fun sessionKey(session: AppSession): String = "${session.packageName}:${session.grantedAtMs}"

    fun isHidden(session: AppSession): Boolean = sessionKey(session) in hiddenKeys

    fun hide(session: AppSession) {
        hiddenKeys.add(sessionKey(session))
        _hiddenKeysFlow.value = hiddenKeys.toSet()
    }

    fun clearAll() {
        hiddenKeys.clear()
        _hiddenKeysFlow.value = emptySet()
    }
}
