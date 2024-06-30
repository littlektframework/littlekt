package com.littlekt.graphics.g2d.tilemap.tiled.internal

import com.littlekt.math.geom.Angle

/**
 * @author Colton Daily
 * @date 3/2/2022
 */
internal data class TileData(
    var id: Int = 0,
    var flipX: Boolean = false,
    var flipY: Boolean = false,
    var rotation: Angle = Angle.ZERO
)
