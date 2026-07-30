package com.kairos.os.domain.session

data class AppSession(
    val packageName: String,
    val displayName: String,
    val appSlug: String,
    val grantedAtMs: Long,
    val expiresAtMs: Long,
    val minutes: Int
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAtMs

    val remainingMs: Long
        get() = (expiresAtMs - System.currentTimeMillis()).coerceAtLeast(0)
}

enum class SessionEndReason {
    EXPIRED,
    REPLACED,
    MANUAL
}
