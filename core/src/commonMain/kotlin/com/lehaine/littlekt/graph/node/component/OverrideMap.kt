package com.lehaine.littlekt.graph.node.component

/**
 * @author Colton Daily
 * @date 2/3/2022
 */
class OverrideMap<K, V>(private val onValueChange: (() -> Unit)? = null) : MutableMap<K, V> {
    private val map = mutableMapOf<K, V>()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries
    override val keys: MutableSet<K> get() = map.keys
    override val values: MutableCollection<V> get() = map.values
    override val size: Int get() = map.size

    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun isEmpty(): Boolean = map.isEmpty()

    override fun put(key: K, value: V): V? {
        val result = map.put(key, value)
        onValueChange?.invoke()
        return result
    }

    override fun putAll(from: Map<out K, V>) {
        map.putAll(from)
        onValueChange?.invoke()
    }

    override fun remove(key: K): V? {
        val result = map.remove(key)
        onValueChange?.invoke()
        return result
    }

    override fun get(key: K): V? = map[key]

    override fun clear() = map.clear()
}