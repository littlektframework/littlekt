package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.util.datastructure.IntMap
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * A cache of resources, such as bind groups, by owner id. A cached resource is released and
 * evicted on [sync] once its owner's [ReleaseNotifier] notifies. [get], [put], and [sync] must be
 * called from the render thread; [markPrepared] and [addPending] may be called from any thread.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
internal class BindGroupCache<V : Releasable> : ReleaseListener {
    private val lock = SynchronizedObject()
    private val values = IntMap<V>()
    private val notifiers = IntMap<ReleaseNotifier>()
    private val prepared = mutableSetOf<Int>()
    private val pendingIds = mutableListOf<Int>()
    private val pendingNotifiers = mutableListOf<ReleaseNotifier>()
    private val pendingValues = mutableListOf<V>()
    private val evictedIds = mutableListOf<Int>()
    private val hasPending = atomic(false)

    val size: Int
        get() = values.size

    operator fun get(id: Int): V? = values[id]

    fun put(id: Int, notifier: ReleaseNotifier, version: Int, value: V): V? {
        notifier.add(this)
        if (notifier.version != version) {
            value.release()
            return null
        }
        return insert(id, notifier, value)
    }

    fun markPrepared(id: Int): Boolean = synchronized(lock) { prepared.add(id) }

    fun addPending(id: Int, notifier: ReleaseNotifier, version: Int, value: V) {
        notifier.add(this)
        synchronized(lock) {
            if (notifier.version != version) {
                prepared.remove(id)
                value.release()
                return
            }
            pendingIds += id
            pendingNotifiers += notifier
            pendingValues += value
            hasPending.value = true
        }
    }

    override fun onReleased(id: Int) {
        synchronized(lock) {
            prepared.remove(id)
            var i = 0
            while (i < pendingIds.size) {
                if (pendingIds[i] == id) {
                    pendingIds.removeAt(i)
                    pendingNotifiers.removeAt(i)
                    pendingValues.removeAt(i).release()
                } else {
                    i++
                }
            }
            evictedIds += id
            hasPending.value = true
        }
    }

    fun sync() {
        if (!hasPending.value) return
        synchronized(lock) {
            for (i in evictedIds.indices) {
                val id = evictedIds[i]
                values.remove(id)?.release()
                notifiers.remove(id)
            }
            evictedIds.clear()
            for (i in pendingIds.indices) {
                insert(pendingIds[i], pendingNotifiers[i], pendingValues[i])
            }
            pendingIds.clear()
            pendingNotifiers.clear()
            pendingValues.clear()
            hasPending.value = false
        }
    }

    fun release() {
        sync()
        synchronized(lock) {
            notifiers.forEachValue { it.remove(this) }
            notifiers.clear()
            values.forEachValue { it.release() }
            values.clear()
            prepared.clear()
        }
    }

    private fun insert(id: Int, notifier: ReleaseNotifier, value: V): V {
        val existing = values[id]
        if (existing != null) {
            if (existing !== value) value.release()
            return existing
        }
        values[id] = value
        notifiers[id] = notifier
        return value
    }
}
