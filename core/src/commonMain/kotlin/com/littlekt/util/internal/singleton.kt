package com.littlekt.util.internal

import kotlin.concurrent.Volatile

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
internal open class SingletonBase<T, A>(private val constructor: (A) -> T) {
    @Volatile private var instance: T? = null
    private var mockConstructor: ((A) -> T)? = null
    protected var errorMessageOnFailed = "Instance has not been created!"

    /**
     * Get the instance of the singleton if it exists or else throws an error if it hasn't been
     * created yet.
     */
    val INSTANCE: T
        get() = instance ?: error(errorMessageOnFailed)

    /**
     * Create a new instance of the single with the provided arguments, replacing any that might
     * have previously existed.
     *
     * @param args: Arguments used to construct instance.
     */
    fun createInstance(args: A): T {
        val newInstance = mockConstructor?.invoke(args) ?: constructor(args)
        instance = newInstance
        return newInstance
    }

    /** Get instance of singleton if it exists, otherwise return null. */
    fun getInstanceOrNull(): T? = instance

    /**
     * Get instance of singleton if it exists, otherwise create it with the provided arguments.
     *
     * @param args: Arguments used to construct instance.
     */
    fun getInstanceOrCreate(args: A): T =
        instance ?: lock(this) { instance ?: createInstance(args) }
}
