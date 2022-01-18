package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.math.Rect

/**
 * @author Colton Daily
 * @date 1/18/2022
 */
class NinePatchRect : Control() {

    var drawable: TextureSlice = Textures.white

    var drawCenter = true
    var region = Rect()
    var hAxis: AxisStretchMode = AxisStretchMode.STRETCH
    var vAxis: AxisStretchMode = AxisStretchMode.STRETCH

    var regionMarginLeft = 0f
    var regionMarginRight = 0f
    var regionMarginTop = 0f
    var regionMarginBottom = 0f

    enum class AxisStretchMode {
        STRETCH,
        TILE,
        TILE_FIT
    }

}