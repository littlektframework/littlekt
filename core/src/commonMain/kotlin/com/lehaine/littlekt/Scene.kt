package com.lehaine.littlekt

import kotlin.time.Duration


/**
 * A base scene that extends [AssetProvider]. Does nothing on its own and will need to be managed
 * by either a [ContextListener] or [Game].
 * @see Game
 * @see ContextListener
 * @author Colton Daily
 * @date 12/26/2021
 */
abstract class Scene(val context: Context) : Disposable {
    open suspend fun Context.show() = Unit
    open suspend fun Context.render(dt: Duration) = Unit
    open suspend fun Context.resize(width: Int, height: Int) = Unit
    open suspend fun Context.hide() = Unit

    final override fun dispose() {
        context.dispose()
    }

    open fun Context.dispose() = Unit
}