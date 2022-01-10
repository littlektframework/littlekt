package com.lehaine.littlekt

/**
 * A [ContextListener] is the base of an [Context] when it is created, rendering, or destroyed.
 * @author Colton Daily
 * @date 9/29/2021
 */
abstract class ContextListener(val context: Context) {

    /**
     * Invoked once the [Context] is ready. Add all the rendering, updating, dispose, and other game logic here.
     * @see [Context.onRender]
     * @see [Context.onPostRender]
     * @see [Context.onResize]
     * @see [Context.onDispose]
     */
    open suspend fun Context.start() {}
}