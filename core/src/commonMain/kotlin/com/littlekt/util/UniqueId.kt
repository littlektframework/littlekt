package com.littlekt.util

import kotlin.reflect.KClass
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic

/**
 * Generate a unique id, incrementally & atomically, based on the type.
 *
 * @author Colton Daily
 * @date 2/4/2025
 */
object UniqueId {
    private val counters = mutableMapOf<KClass<*>, AtomicInt>()

    /**
     * Gets the current id, for [T], and increments the value, atomically.
     *
     * When creating unique ids, think which base class type ids will be generated from.For example,
     * if we have an interface that requires an id, and we have multiple implements, those
     * implementations should use the interface as the base type for generating ids, not it's
     * implementation type.
     */
    fun <T : Any> next(type: KClass<T>): Int {
        return counters.getOrPut(type) { atomic(0) }.incrementAndGet()
    }

    /**
     * Gets the current id, for [T], and increments the value, atomically.
     *
     * When creating unique ids, think which base class type ids will be generated from.For example,
     * if we have an interface that requires an id, and we have multiple implements, those
     * implementations should use the interface as the base type for generating ids, not it's
     * implementation type.
     */
    inline fun <reified T : Any> next(): Int = next(T::class)
}
