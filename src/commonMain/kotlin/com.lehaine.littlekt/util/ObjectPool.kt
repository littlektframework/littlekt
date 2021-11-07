package com.lehaine.littlekt.util

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
abstract class ObjectPool<T>(private val size: Int) {

    private val pool = mutableListOf<T>()

    @Suppress("UNCHECKED_CAST")
    fun obtain(): T {
        if (pool.isEmpty()) {
            for (it in 0..size) {
                free(newInstance())
            }
        }
        val result = pool[pool.size - 1]
        pool.removeAt(pool.size - 1)
        return result
    }

    fun free(obj: T) {
        destroyInstance(obj)
        pool.add(obj)
    }

    fun free(objs: Iterable<T>) {
        objs.forEach { destroyInstance(it) }
        pool.addAll(objs)
    }

    abstract fun newInstance(): T

    abstract fun destroyInstance(obj: T)
}