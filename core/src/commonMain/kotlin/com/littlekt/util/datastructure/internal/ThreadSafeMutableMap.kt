package com.littlekt.util.datastructure.internal

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * A [MutableMap] that uses a [reentrantLock] for thread safety.
 *
 * @author Colton Daily
 * @date 1/17/2025
 */
internal class ThreadSafeMutableMap<K, V>(private val delegate: MutableMap<K, V> = mutableMapOf()) :
    MutableMap<K, V> {
    private val lock = reentrantLock()

    override val size: Int
        get() = lock.withLock { delegate.size }

    override fun isEmpty(): Boolean = lock.withLock { delegate.isEmpty() }

    override fun containsKey(key: K): Boolean = lock.withLock { delegate.containsKey(key) }

    override fun containsValue(value: V): Boolean = lock.withLock { delegate.containsValue(value) }

    override fun get(key: K): V? = lock.withLock { delegate[key] }

    override fun put(key: K, value: V): V? = lock.withLock { delegate.put(key, value) }

    override fun remove(key: K): V? = lock.withLock { delegate.remove(key) }

    override fun putAll(from: Map<out K, V>) = lock.withLock { delegate.putAll(from) }

    override fun clear() = lock.withLock { delegate.clear() }

    override val keys: MutableSet<K>
        get() = lock.withLock { delegate.keys.toMutableSet() }

    override val values: MutableCollection<V>
        get() = lock.withLock { delegate.values.toMutableList() }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = lock.withLock { delegate.entries.map { it.toMutableEntry() }.toMutableSet() }

    private fun <K, V> Map.Entry<K, V>.toMutableEntry(): MutableMap.MutableEntry<K, V> {
        return object : MutableMap.MutableEntry<K, V> {
            override val key: K
                get() = this@toMutableEntry.key

            override val value: V
                get() = this@toMutableEntry.value

            override fun setValue(newValue: V): V {
                throw UnsupportedOperationException(
                    "This map does not allow modifying entries directly."
                )
            }
        }
    }
}

internal inline fun <K, V> threadSafeMutableMapOf(): ThreadSafeMutableMap<K, V> =
    ThreadSafeMutableMap()

internal inline fun <K, V> threadSafeMutableMapOf(
    vararg pairs: Pair<K, V>
): ThreadSafeMutableMap<K, V> = ThreadSafeMutableMap(mutableMapOf(*pairs))
