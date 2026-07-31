package com.kairos.os.domain.models

import com.kairos.os.domain.session.AppSession
import com.kairos.os.domain.session.SessionCardHideStore

sealed class HomeActivityItem {
    abstract val sortKey: Long

    data class Agent(val agent: RunningAgent) : HomeActivityItem() {
        override val sortKey: Long = agent.createdAt
    }

    data class AppGrant(val session: AppSession) : HomeActivityItem() {
        override val sortKey: Long = session.grantedAtMs
    }
}

fun buildHomeActivityItems(
    agents: List<RunningAgent>,
    session: AppSession?,
    hideStore: SessionCardHideStore
): List<HomeActivityItem> {
    val items = mutableListOf<HomeActivityItem>()
    items.addAll(agents.map { HomeActivityItem.Agent(it) })
    if (session != null && !session.isExpired && !hideStore.isHidden(session)) {
        items.add(HomeActivityItem.AppGrant(session))
    }
    return items.sortedByDescending { it.sortKey }
}
