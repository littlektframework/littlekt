package com.lehaine.littlekt.util


/**
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Signal {
    private val connectedNodes = mutableMapOf<Any, () -> Unit>()
    private val connections = mutableListOf<() -> Unit>()

    fun connect(node: Any, slot: () -> Unit) {
        connectedNodes[node] = slot
    }

    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    fun emit() {
        connectedNodes.forEach { (_, slot) -> slot() }
        connections.fastForEach { it() }
    }

    operator fun plusAssign(slot: () -> Unit) {
        connections += slot
    }

    operator fun minusAssign(slot: () -> Unit) {
        connections -= slot
    }

    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class SingleSignal<T> {
    private val connectedNodes = mutableMapOf<Any, (T) -> Unit>()
    private val connections = mutableListOf<(T) -> Unit>()

    fun connect(node: Any, slot: (T) -> Unit) {
        connectedNodes[node] = slot
    }

    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    fun emit(value: T) {
        connectedNodes.forEach { (_, slot) -> slot(value) }
        connections.fastForEach { it(value) }
    }

    operator fun plusAssign(slot: (T) -> Unit) {
        connections += slot
    }

    operator fun minusAssign(slot: (T) -> Unit) {
        connections -= slot
    }

    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class DoubleSignal<A, B> {
    private val connectedNodes = mutableMapOf<Any, (A, B) -> Unit>()
    private val connections = mutableListOf<(A, B) -> Unit>()

    fun connect(node: Any, slot: (A, B) -> Unit) {
        connectedNodes[node] = slot
    }

    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    fun emit(value1: A, value2: B) {
        connectedNodes.forEach { (_, slot) -> slot(value1, value2) }
        connections.fastForEach { it(value1, value2) }
    }

    operator fun plusAssign(slot: (A, B) -> Unit) {
        connections += slot
    }

    operator fun minusAssign(slot: (A, B) -> Unit) {
        connections -= slot
    }

    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class TripleSignal<A, B, C> {
    private val connectedNodes = mutableMapOf<Any, (A, B, C) -> Unit>()
    private val connections = mutableListOf<(A, B, C) -> Unit>()

    fun connect(node: Any, slot: (A, B, C) -> Unit) {
        connectedNodes[node] = slot
    }

    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    fun emit(value1: A, value2: B, value3: C) {
        connectedNodes.forEach { (_, slot) -> slot(value1, value2, value3) }
        connections.fastForEach { it(value1, value2, value3) }
    }

    operator fun plusAssign(slot: (A, B, C) -> Unit) {
        connections += slot
    }

    operator fun minusAssign(slot: (A, B, C) -> Unit) {
        connections -= slot
    }

    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}


fun signal() = Signal()
fun <A> signal1v() = SingleSignal<A>()
fun <A, B> signal2v() = DoubleSignal<A, B>()
fun <A, B, C> signal3v() = TripleSignal<A, B, C>()