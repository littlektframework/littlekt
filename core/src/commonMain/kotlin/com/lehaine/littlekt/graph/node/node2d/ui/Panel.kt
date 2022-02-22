package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.lehaine.littlekt.graph.node.component.Drawable
import com.lehaine.littlekt.graphics.Batch
import com.lehaine.littlekt.graphics.Camera

/**
 * Adds a [Panel] to the current [Node] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Panel] context in order to initialize any values
 * @return the newly created [Panel]
 */
inline fun Node.panel(callback: @SceneGraphDslMarker Panel.() -> Unit = {}) =
    Panel().also(callback).addTo(this)

/**
 * Adds a [Panel] to the  [SceneGraph.root] as a child and then triggers the [callback]
 * @param callback the callback that is invoked with a [Panel] context in order to initialize any values
 * @return the newly created [Panel]
 */
inline fun SceneGraph<*>.panel(callback: @SceneGraphDslMarker Panel.() -> Unit = {}) =
    root.panel(callback)


/**
 * A [Control] that provides an opaque background.
 * @author Colton Daily
 * @date 1/21/2022
 */
open class Panel : Control() {

    var mode: Mode = Mode.BACKGROUND

    var panel: Drawable
        get() = getThemeDrawable(panelThemeVar)
        set(value) {
            drawableOverrides[panelThemeVar] = value
        }

    private val panelThemeVar get() = if (mode == Mode.BACKGROUND) themeVars.panel else themeVars.panelForeground

    override fun render(batch: Batch, camera: Camera) {
        panel.let {
            it.draw(batch, globalX, globalY, width, height, scaleX, scaleY, rotation, it.modulate)
        }
    }

    enum class Mode {
        BACKGROUND,
        FOREGROUND
    }

    class ThemeVars {
        val panel = "panel"
        val panelForeground = "panelFg"
    }

    companion object {
        val themeVars = ThemeVars()
    }
}