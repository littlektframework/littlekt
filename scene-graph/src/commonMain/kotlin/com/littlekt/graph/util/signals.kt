package com.littlekt.graph.util

/**
 * Tracks the slots connected to a signal. Connected objects are matched by identity and every
 * slot is stored in a list so emitting doesn't allocate.
 */
internal class SignalSlots<S> {
    private val keys = mutableListOf<Any>()
    internal val keyedSlots = mutableListOf<S>()
    internal val slots = mutableListOf<S>()

    val isEmpty: Boolean
        get() = keys.isEmpty() && slots.isEmpty()

    fun connect(node: Any, slot: S) {
        val index = indexOf(node)
        if (index >= 0) {
            keyedSlots[index] = slot
        } else {
            keys += node
            keyedSlots += slot
        }
    }

    fun disconnect(node: Any) {
        val index = indexOf(node)
        if (index < 0) return
        keys.removeAt(index)
        keyedSlots.removeAt(index)
    }

    fun add(slot: S) {
        slots += slot
    }

    fun remove(slot: S) {
        slots -= slot
    }

    inline fun forEach(action: (S) -> Unit) {
        var i = 0
        while (i < keyedSlots.size) {
            action(keyedSlots[i])
            i++
        }
        i = 0
        while (i < slots.size) {
            action(slots[i])
            i++
        }
    }

    fun clear() {
        keys.clear()
        keyedSlots.clear()
        slots.clear()
    }

    private fun indexOf(node: Any): Int {
        for (i in keys.indices) {
            if (keys[i] === node) return i
        }
        return -1
    }
}

/**
 * An event class that emits a callback to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
open class Signal {
    private val slots = SignalSlots<() -> Unit>()

    /** `true` if nothing is connected to this signal. */
    val isEmpty: Boolean
        get() = slots.isEmpty

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: () -> Unit) = slots.connect(node, slot)

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) = slots.disconnect(node)

    /** Emit this signal to all connections and connected nodes. */
    fun emit() {
        if (slots.isEmpty) return
        slots.forEach { it() }
    }

    /** Adds the [slot] as a connection. Same as [plusAssign]. */
    operator fun invoke(slot: () -> Unit) = slots.add(slot)

    operator fun plusAssign(slot: () -> Unit) = slots.add(slot)

    operator fun minusAssign(slot: () -> Unit) = slots.remove(slot)

    /** Clear all connections and connected nodes. */
    fun clear() = slots.clear()
}

/**
 * An event class that emits a callback, with a single parameter, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class SingleSignal<T> {
    private val slots = SignalSlots<(T) -> Unit>()

    /** `true` if nothing is connected to this signal. */
    val isEmpty: Boolean
        get() = slots.isEmpty

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (T) -> Unit) = slots.connect(node, slot)

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) = slots.disconnect(node)

    /** Emit this signal to all connections and connected nodes. */
    fun emit(value: T) {
        if (slots.isEmpty) return
        slots.forEach { it(value) }
    }

    /** Adds the [slot] as a connection. Same as [plusAssign]. */
    operator fun invoke(slot: (T) -> Unit) = slots.add(slot)

    operator fun plusAssign(slot: (T) -> Unit) = slots.add(slot)

    operator fun minusAssign(slot: (T) -> Unit) = slots.remove(slot)

    /** Clear all connections and connected nodes. */
    fun clear() = slots.clear()
}

/**
 * An event class that emits a callback, with two parameters, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class DoubleSignal<A, B> {
    private val slots = SignalSlots<(A, B) -> Unit>()

    /** `true` if nothing is connected to this signal. */
    val isEmpty: Boolean
        get() = slots.isEmpty

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (A, B) -> Unit) = slots.connect(node, slot)

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) = slots.disconnect(node)

    /** Emit this signal to all connections and connected nodes. */
    fun emit(value1: A, value2: B) {
        if (slots.isEmpty) return
        slots.forEach { it(value1, value2) }
    }

    /** Adds the [slot] as a connection. Same as [plusAssign]. */
    operator fun invoke(slot: (A, B) -> Unit) = slots.add(slot)

    operator fun plusAssign(slot: (A, B) -> Unit) = slots.add(slot)

    operator fun minusAssign(slot: (A, B) -> Unit) = slots.remove(slot)

    /** Clear all connections and connected nodes. */
    fun clear() = slots.clear()
}

/**
 * An event class that emits a callback, with three parameters, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class TripleSignal<A, B, C> {
    private val slots = SignalSlots<(A, B, C) -> Unit>()

    /** `true` if nothing is connected to this signal. */
    val isEmpty: Boolean
        get() = slots.isEmpty

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (A, B, C) -> Unit) = slots.connect(node, slot)

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) = slots.disconnect(node)

    /** Emit this signal to all connections and connected nodes. */
    fun emit(value1: A, value2: B, value3: C) {
        if (slots.isEmpty) return
        slots.forEach { it(value1, value2, value3) }
    }

    /** Adds the [slot] as a connection. Same as [plusAssign]. */
    operator fun invoke(slot: (A, B, C) -> Unit) = slots.add(slot)

    operator fun plusAssign(slot: (A, B, C) -> Unit) = slots.add(slot)

    operator fun minusAssign(slot: (A, B, C) -> Unit) = slots.remove(slot)

    /** Clear all connections and connected nodes. */
    fun clear() = slots.clear()
}

/**
 * An event class that emits a callback, with four parameters, to any connections it has gained.
 *
 * @author Colton Daily
 * @date 1/2/2022
 */
class QuadrupleSignal<A, B, C, D> {
    private val slots = SignalSlots<(A, B, C, D) -> Unit>()

    /** `true` if nothing is connected to this signal. */
    val isEmpty: Boolean
        get() = slots.isEmpty

    /** Connect an object to this signal. */
    fun connect(node: Any, slot: (A, B, C, D) -> Unit) = slots.connect(node, slot)

    /** Disconnect an object from this signal. */
    fun disconnect(node: Any) = slots.disconnect(node)

    /** Emit this signal to all connections and connected nodes. */
    fun emit(value1: A, value2: B, value3: C, value4: D) {
        if (slots.isEmpty) return
        slots.forEach { it(value1, value2, value3, value4) }
    }

    /** Adds the [slot] as a connection. Same as [plusAssign]. */
    operator fun invoke(slot: (A, B, C, D) -> Unit) = slots.add(slot)

    operator fun plusAssign(slot: (A, B, C, D) -> Unit) = slots.add(slot)

    operator fun minusAssign(slot: (A, B, C, D) -> Unit) = slots.remove(slot)

    /** Clear all connections and connected nodes. */
    fun clear() = slots.clear()
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
