package com.lehaine.littlekt.util.datastructure


/**
 * Creates a [Pool] instance and then generates the object by passing in the Pool instance.
 * @param preallocate the number of objects to preallocate
 * @param gen the object generate function to create a new object when needed
 */
fun <T> pool(preallocate: Int = 0, gen: Pool<T>.(Int) -> T): Pool<T> {
    val pool = Pool<T>()
    pool.preAlloc(preallocate, gen)
    return pool
}

/**
 * Structure containing a set of reusable objects.
 *
 * The method [alloc] retrieves from the pool or allocates a new object,
 * while the [free] method pushes back one element to the pool and resets it to reuse it.
 */
class Pool<T> internal constructor() {
    private var reset: (T) -> Unit = {}
    private var gen: ((Int) -> T)? = null

    /**
     * Structure containing a set of reusable objects.
     * @param reset the function that reset an existing object to its initial state
     * @param preallocate the number of objects to preallocate
     * @param gen the object generate function to create a new object when needed
     */
    constructor(reset: (T) -> Unit = {}, preallocate: Int = 0, gen: (Int) -> T) : this() {
        this.reset = reset
        this.gen = gen
        preAlloc(preallocate, gen)
    }

    /**
     * Structure containing a set of reusable objects.
     * @param preallocate the number of objects to preallocate
     * @param gen the object generate function to create a new object when needed
     */
    constructor(preallocate: Int = 0, gen: (Int) -> T) : this({}, preallocate, gen)

    private val items = Stack<T>()
    private var lastId = 0

    val totalAllocatedItems get() = lastId
    val totalItemsInUse get() = totalAllocatedItems - itemsInPool
    val itemsInPool: Int get() = items.size

    private fun preAlloc(preallocate: Int, gen: (Int) -> T) {
        for (n in 0 until preallocate) items.push(gen(lastId++))
    }

    internal fun preAlloc(preallocate: Int, gen: Pool<T>.(Int) -> T) {
        for (n in 0 until preallocate) items.push(gen(lastId++))
    }

    fun alloc(): T {
        return if (items.isNotEmpty()) items.pop() else gen?.invoke(lastId++)
            ?: error("Pool<T> was not instantiated with a generator function!")
    }

    fun free(element: T) {
        reset(element)
        items.push(element)
    }

    fun free(vararg elements: T) {
        elements.forEach { free(it) }
    }

    fun free(elements: Iterable<T>) {
        for (element in elements) free(element)
    }

    fun free(elements: List<T>) {
        elements.forEach { free(it) }
    }

    inline operator fun <R> invoke(callback: (T) -> R): R = alloc(callback)

    inline fun <R> alloc(callback: (T) -> R): R {
        val temp = alloc()
        return callback(temp)
    }

    inline fun <R> allocMultiple(
        count: Int,
        temp: MutableList<T> = mutableListOf(),
        callback: (MutableList<T>) -> R,
    ): R {
        temp.clear()
        for (n in 0 until count) temp.add(alloc())
        return callback(temp)
    }

    inline fun <R> allocThis(callback: T.() -> R): R {
        val temp = alloc()
        return callback(temp)
    }

    override fun hashCode(): Int = items.hashCode()
    override fun equals(other: Any?): Boolean =
        (other is Pool<*>) && this.items == other.items && this.itemsInPool == other.itemsInPool
}