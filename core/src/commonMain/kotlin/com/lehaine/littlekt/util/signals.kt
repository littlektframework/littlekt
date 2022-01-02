package com.lehaine.littlekt.util

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Signal {
    private val connections = mutableMapOf<Any, () -> Unit>()

    fun connect(node: Any, slot: () -> Unit) {
        connections[node] = slot
    }

    fun disconnect(node: Any) {
        connections.remove(node)
    }

    fun emit() {
        connections.forEach { (_, slot) -> slot() }
    }
}

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class TypedSignal<T> {
    private val connections = mutableMapOf<Any, (T) -> Unit>()

    fun connect(node: Any, slot: (T) -> Unit) {
        connections[node] = slot
    }

    fun disconnect(node: Any) {
        connections.remove(node)
    }

    fun emit(value: T) {
        connections.forEach { (_, slot) -> slot(value) }
    }
}
