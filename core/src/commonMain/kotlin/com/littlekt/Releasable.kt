package com.littlekt

/**
 * An object that holds memory, that can be released.
 *
 * @author Colton Daily
 * @date 11/19/2021
 */
interface Releasable {
    /** Release any memory held by this object. */
    fun release()
}
