package com.littlekt.resources

/**
 * Info related to allocated resources.
 *
 * @author Colton Daily
 * @date 4/14/2024
 */
abstract class ResourceInfo {
    val id = lastId++

    abstract fun delete()

    companion object {
        private var lastId = 0L
    }
}
