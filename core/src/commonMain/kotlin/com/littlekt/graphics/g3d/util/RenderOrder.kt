package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.g3d.util.RenderOrder.Companion.DEFAULT
import com.littlekt.graphics.g3d.util.RenderOrder.Companion.FIRST
import com.littlekt.graphics.g3d.util.RenderOrder.Companion.LAST
import com.littlekt.graphics.g3d.util.RenderOrder.Companion.SKYBOX
import com.littlekt.graphics.g3d.util.RenderOrder.Companion.TRANSPARENT
import kotlin.jvm.JvmInline

/**
 * A value class that specifies the render order for pipelines when rendering meshes. Custom render
 * order may be created with [order] set to the desired order. Some render orders are already used
 * (see below) but custom order values may be used between each. For example, the [DEFAULT] render
 * order has a value of `10` and the [LAST] render order has a value of `100`. One could add custom
 * render orders to handle anything in between, or after.
 *
 * @see FIRST
 * @see DEFAULT
 * @see SKYBOX
 * @see TRANSPARENT
 * @see LAST
 * @author Colton Daily
 * @date 12/12/2024
 */
@JvmInline
value class RenderOrder(val order: Int) {
    companion object {
        /** Renders first before any other. [order] is set to `0`. */
        val FIRST = RenderOrder(0)

        /** Default render order. [order] is set to `10`. */
        val DEFAULT = RenderOrder(10)

        /** Render order for skyboxes. [order] is set to `20`. */
        val SKYBOX = RenderOrder(20)

        /** Render order for rendering transparency. [order] is set to `30`. */
        val TRANSPARENT = RenderOrder(30)

        /** Renders last after all others. [order] is set to `100`. */
        val LAST = RenderOrder(100)
    }
}
