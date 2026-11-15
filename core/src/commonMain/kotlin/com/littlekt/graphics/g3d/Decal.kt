package com.littlekt.graphics.g3d

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.degrees
import com.littlekt.resources.Textures

/**
 * A [Node3D] that projects a texture onto the geometry inside its box, such as a targeting circle,
 * a scorch mark or a puddle. The box is `1` unit on each axis around the node, so size it with the
 * node's scale, and the texture is projected down the node's local y axis. Drawn with a
 * [DecalRenderer] after opaque geometry.
 *
 * ```
 * val scorch = Decal(scorchTexture).apply {
 *     position(2f, 0f, 3f)
 *     scaling(3f, 1f, 3f)
 * }
 * ```
 *
 * @param slice the texture projected onto geometry
 * @param color the tint, including its alpha
 * @author Colton Daily
 * @date 11/15/2026
 */
class Decal(var slice: TextureSlice = Textures.white, var color: Color = Color.WHITE) : Node3D() {

    /**
     * Surfaces turned further than this from facing the projection fade out, so the decal doesn't
     * smear down walls.
     */
    var angleFade: Angle = 70.degrees

    /** How much of the box's height fades out at its top & bottom, from `0` to `0.5`. */
    var edgeFade: Float = 0.1f
        set(value) {
            field = value.coerceIn(0f, 0.5f)
        }

    init {
        name = "Decal"
    }
}
