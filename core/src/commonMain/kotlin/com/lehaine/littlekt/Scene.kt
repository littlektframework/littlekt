package com.lehaine.littlekt

import kotlin.time.Duration


/**
 * A base scene that extends [AssetProvider]. Does nothing on it's own and will need to be managed
 * by either a [ContextListener] or [Game].
 * @see Game
 * @see ContextListener
 * @author Colton Daily
 * @date 12/26/2021
 */
abstract class Scene(context: Context) : AssetProvider(context), Context by context, Disposable {
    fun show() = Unit
    fun render(dt: Duration) = Unit
    fun resize(width: Int, height: Int) = Unit
    fun pause() = Unit
    fun resume() = Unit
    fun hide() = Unit
    override fun dispose() = Unit

}