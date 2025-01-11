package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Drawable
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Adds a [Panel] to the current [Node] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [Panel] context in order to initialize any
 *   values
 * @return the newly created [Panel]
 */
@OptIn(ExperimentalContracts::class)
inline fun Node.panel(callback: @SceneGraphDslMarker Panel.() -> Unit = {}): Panel {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Panel().also(callback).addTo(this)
}

/**
 * Adds a [Panel] to the [SceneGraph.root] as a child and then triggers the [callback]
 *
 * @param callback the callback that is invoked with a [Panel] context in order to initialize any
 *   values
 * @return the newly created [Panel]
 */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.panel(callback: @SceneGraphDslMarker Panel.() -> Unit = {}): Panel {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.panel(callback)
}

/**
 * A [Control] that provides an opaque background.
 *
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

    private val panelThemeVar
        get() = if (mode == Mode.BACKGROUND) themeVars.panel else themeVars.panelForeground

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        panel.let {
            it.draw(
                batch = batch,
                x = globalX - originX,
                y = globalY - originY,
                originX = originX,
                originY = originY,
                width = width,
                height = height,
                scaleX = globalScaleX,
                scaleY = globalScaleY,
                rotation = globalRotation,
                color = it.tint,
            )
        }
    }

    enum class Mode {
        BACKGROUND,
        FOREGROUND,
    }

    class ThemeVars {
        val panel = "panel"
        val panelForeground = "panelFg"
    }

    companion object {
        val themeVars = ThemeVars()
    }
}
