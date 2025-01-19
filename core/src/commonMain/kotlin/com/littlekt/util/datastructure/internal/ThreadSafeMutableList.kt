package com.littlekt.util.datastructure.internal

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/**
 * A [MutableList] that uses a [reentrantLock] for thread safety.
 *
 * @author Colton Daily
 * @date 1/17/2025
 */
internal class ThreadSafeMutableList<T>(private val delegate: MutableList<T> = mutableListOf()) :
    MutableList<T> by delegate {
    private val lock = reentrantLock()

    override val size: Int
        get() = lock.withLock { delegate.size }

    override fun isEmpty(): Boolean = lock.withLock { delegate.isEmpty() }

    override fun contains(element: T): Boolean = lock.withLock { delegate.contains(element) }

    override fun containsAll(elements: Collection<T>): Boolean =
        lock.withLock { delegate.containsAll(elements) }

    override fun get(index: Int): T = lock.withLock { delegate[index] }

    override fun indexOf(element: T): Int = lock.withLock { delegate.indexOf(element) }

    override fun lastIndexOf(element: T): Int = lock.withLock { delegate.lastIndexOf(element) }

    override fun add(element: T): Boolean = lock.withLock { delegate.add(element) }

    override fun add(index: Int, element: T) = lock.withLock { delegate.add(index, element) }

    override fun addAll(index: Int, elements: Collection<T>): Boolean =
        lock.withLock { delegate.addAll(index, elements) }

    override fun addAll(elements: Collection<T>): Boolean =
        lock.withLock { delegate.addAll(elements) }

    override fun clear() = lock.withLock { delegate.clear() }

    override fun remove(element: T): Boolean = lock.withLock { delegate.remove(element) }

    override fun removeAll(elements: Collection<T>): Boolean =
        lock.withLock { delegate.removeAll(elements) }

    override fun removeAt(index: Int): T = lock.withLock { delegate.removeAt(index) }

    override fun retainAll(elements: Collection<T>): Boolean =
        lock.withLock { delegate.retainAll(elements) }

    override fun set(index: Int, element: T): T = lock.withLock { delegate.set(index, element) }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
        lock.withLock { ThreadSafeMutableList(delegate.subList(fromIndex, toIndex)) }

    override fun iterator(): MutableIterator<T> =
        lock.withLock { ThreadSafeIterator(delegate.iterator()) }

    override fun listIterator(): MutableListIterator<T> =
        lock.withLock { ThreadSafeListIterator(delegate.listIterator()) }

    override fun listIterator(index: Int): MutableListIterator<T> =
        lock.withLock { ThreadSafeListIterator(delegate.listIterator(index)) }

    private inner class ThreadSafeIterator(private val iterator: MutableIterator<T>) :
        MutableIterator<T> {
        override fun hasNext(): Boolean = lock.withLock { iterator.hasNext() }

        override fun next(): T = lock.withLock { iterator.next() }

        override fun remove() = lock.withLock { iterator.remove() }
    }

    private inner class ThreadSafeListIterator(private val iterator: MutableListIterator<T>) :
        MutableListIterator<T> {
        override fun hasNext(): Boolean = lock.withLock { iterator.hasNext() }

        override fun next(): T = lock.withLock { iterator.next() }

        override fun remove() = lock.withLock { iterator.remove() }

        override fun hasPrevious(): Boolean = lock.withLock { iterator.hasPrevious() }

        override fun nextIndex(): Int = lock.withLock { iterator.nextIndex() }

        override fun previous(): T = lock.withLock { iterator.previous() }

        override fun previousIndex(): Int = lock.withLock { iterator.previousIndex() }

        override fun set(element: T) = lock.withLock { iterator.set(element) }

        override fun add(element: T) = lock.withLock { iterator.add(element) }
    }
}

internal inline fun <T> threadSafeMutableListOf(): ThreadSafeMutableList<T> =
    ThreadSafeMutableList()

internal inline fun <T> threadSafeMutableListOf(vararg elements: T): ThreadSafeMutableList<T> =
    ThreadSafeMutableList(mutableListOf(*elements))
