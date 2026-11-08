package com.littlekt.util.datastructure

/**
 * An open addressing map of [Int] keys to values that doesn't box its keys.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class IntMap<V : Any>(initialCapacity: Int = 16) {
    private var keys = IntArray(tableSizeFor(initialCapacity))
    private var values = arrayOfNulls<Any>(keys.size)
    private var mask = keys.size - 1

    /** The number of entries in this map. */
    var size: Int = 0
        private set

    @Suppress("UNCHECKED_CAST")
    operator fun get(key: Int): V? {
        var index = hash(key) and mask
        while (true) {
            val value = values[index] ?: return null
            if (keys[index] == key) return value as V
            index = (index + 1) and mask
        }
    }

    operator fun set(key: Int, value: V) {
        if ((size + 1) * 2 > keys.size) {
            resize(keys.size * 2)
        }
        var index = hash(key) and mask
        while (true) {
            if (values[index] == null) {
                keys[index] = key
                values[index] = value
                size++
                return
            }
            if (keys[index] == key) {
                values[index] = value
                return
            }
            index = (index + 1) and mask
        }
    }

    /** @return the existing value for [key] or puts the result of [defaultValue] and returns it. */
    inline fun getOrPut(key: Int, defaultValue: () -> V): V {
        return get(key) ?: defaultValue().also { set(key, it) }
    }

    /** Invokes [action] on each value. */
    @Suppress("UNCHECKED_CAST")
    inline fun forEachValue(action: (V) -> Unit) {
        val values = valuesArray
        for (i in values.indices) {
            val value = values[i] ?: continue
            action(value as V)
        }
    }

    /** Removes all entries. */
    fun clear() {
        keys.fill(0)
        values.fill(null)
        size = 0
    }

    @PublishedApi
    internal val valuesArray: Array<Any?>
        get() = values

    private fun resize(newSize: Int) {
        val oldKeys = keys
        val oldValues = values
        keys = IntArray(newSize)
        values = arrayOfNulls(newSize)
        mask = newSize - 1
        size = 0
        for (i in oldKeys.indices) {
            val value = oldValues[i] ?: continue
            @Suppress("UNCHECKED_CAST")
            set(oldKeys[i], value as V)
        }
    }

    private fun hash(key: Int): Int {
        val h = key * -0x61c88647
        return h xor (h ushr 16)
    }

    private companion object {
        fun tableSizeFor(capacity: Int): Int {
            var size = 2
            while (size < capacity * 2) size = size shl 1
            return size
        }
    }
}
