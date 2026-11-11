package com.littlekt.graphics.g3d.util

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal interface ReleaseListener {
    fun onReleased(id: Int)
}

/**
 * Notifies each added [ReleaseListener] once when its owner is released. Listeners are removed
 * after being notified and [version] is incremented.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
internal class ReleaseNotifier {
    private val lock = SynchronizedObject()
    private val listeners = mutableListOf<ReleaseListener>()
    private var _version = 0

    val version: Int
        get() = synchronized(lock) { _version }

    val listenerCount: Int
        get() = synchronized(lock) { listeners.size }

    fun add(listener: ReleaseListener) {
        synchronized(lock) {
            for (i in listeners.indices) if (listeners[i] === listener) return
            listeners += listener
        }
    }

    fun remove(listener: ReleaseListener) {
        synchronized(lock) {
            for (i in listeners.indices) {
                if (listeners[i] === listener) {
                    listeners.removeAt(i)
                    return
                }
            }
        }
    }

    fun notifyReleased(id: Int) {
        val notify =
            synchronized(lock) {
                _version++
                if (listeners.isEmpty()) return
                listeners.toTypedArray().also { listeners.clear() }
            }
        for (i in notify.indices) notify[i].onReleased(id)
    }
}
