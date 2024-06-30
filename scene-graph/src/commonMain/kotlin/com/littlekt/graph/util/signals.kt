package com.littlekt.graph.util

import com.littlekt.util.datastructure.fastForEach

/**
 * An event class that emits a callback to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Signal {
    private val connectedNodes = mutableMapOf<Any, () -> Unit>()
    private val connections = mutableListOf<() -> Unit>()

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: () -> Unit) {
        connectedNodes[node] = slot
    }

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    /** Emit this signal to all connections and connected nodes. */
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

    /** Clear all connections and connected nodes. */
    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/**
 * An event class that emits a callback, with a single parameter, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class SingleSignal<T> {
    private val connectedNodes = mutableMapOf<Any, (T) -> Unit>()
    private val connections = mutableListOf<(T) -> Unit>()

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (T) -> Unit) {
        connectedNodes[node] = slot
    }

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    /** Emit this signal to all connections and connected nodes. */
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

    /** Clear all connections and connected nodes. */
    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/**
 * An event class that emits a callback, with two parameters, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class DoubleSignal<A, B> {
    private val connectedNodes = mutableMapOf<Any, (A, B) -> Unit>()
    private val connections = mutableListOf<(A, B) -> Unit>()

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (A, B) -> Unit) {
        connectedNodes[node] = slot
    }

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    /** Emit this signal to all connections and connected nodes. */
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

    /** Clear all connections and connected nodes. */
    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/**
 * An event class that emits a callback, with three parameters, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class TripleSignal<A, B, C> {
    private val connectedNodes = mutableMapOf<Any, (A, B, C) -> Unit>()
    private val connections = mutableListOf<(A, B, C) -> Unit>()

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (A, B, C) -> Unit) {
        connectedNodes[node] = slot
    }

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    /** Emit this signal to all connections and connected nodes. */
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

    /** Clear all connections and connected nodes. */
    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/**
 * An event class that emits a callback, with four parameters, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class QuadrupleSignal<A, B, C, D> {
    private val connectedNodes = mutableMapOf<Any, (A, B, C, D) -> Unit>()
    private val connections = mutableListOf<(A, B, C, D) -> Unit>()

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (A, B, C, D) -> Unit) {
        connectedNodes[node] = slot
    }

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) {
        connectedNodes.remove(node)
    }

    /** Emit this signal to all connections and connected nodes. */
    fun emit(value1: A, value2: B, value3: C, value4: D) {
        connectedNodes.forEach { (_, slot) -> slot(value1, value2, value3, value4) }
        connections.fastForEach { it(value1, value2, value3, value4) }
    }

    operator fun plusAssign(slot: (A, B, C, D) -> Unit) {
        connections += slot
    }

    operator fun minusAssign(slot: (A, B, C, D) -> Unit) {
        connections -= slot
    }

    /** Clear all connections and connected nodes. */
    fun clear() {
        connectedNodes.clear()
        connections.clear()
    }
}

/** Create a new [Signal]. */
fun signal() = Signal()

/** Create a new signal, [SingleSignal], that contains a single parameter in its callback. */
fun <A> signal1v() = SingleSignal<A>()

/** Create a new signal, [DoubleSignal], that contains two parameters in its callback. */
fun <A, B> signal2v() = DoubleSignal<A, B>()

/** Create a new signal, [TripleSignal], that contains three parameters in its callback. */
fun <A, B, C> signal3v() = TripleSignal<A, B, C>()

/** Create a new signal, [QuadrupleSignal], that contains four parameters in its callback. */
fun <A, B, C, D> signal4v() = QuadrupleSignal<A, B, C, D>()
