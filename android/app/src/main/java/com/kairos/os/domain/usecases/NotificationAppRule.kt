package com.kairos.os.domain.usecases

enum class NotificationAppRule {
    ALLOWED,     // Whitelisted: always delivered directly on-device
    BLOCKED,     // Blacklisted: silently dismissed without saving to digest
    KAI_DECIDES  // Default smart AI + heuristic classification
}
