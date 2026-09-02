package com.oleksandr.fastflow.domain.repository

/**
 * Pokes the home-screen widget after a state change.
 *
 * Kept behind an interface so use cases stay free of Glance (SPEC 3.6).
 */
interface WidgetUpdater {
    suspend fun refresh()
}
