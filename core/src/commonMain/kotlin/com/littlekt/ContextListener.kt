package com.littlekt

/**
 * A [ContextListener] is the base of an [Context] when it is created, rendering, or destroyed.
 *
 * @author Colton Daily
 * @date 9/29/2021
 */
abstract class ContextListener(val context: Context) {

    /**
     * Invoked once the [Context] is ready. Add all the rendering, updating, release, and other game
     * logic here.
     *
     * @see [Context.onUpdate]
     * @see [Context.onPostUpdate]
     * @see [Context.onResize]
     * @see [Context.onRelease]
     */
    open suspend fun Context.start() {}
}
