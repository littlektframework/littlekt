package com.lehaine.littlekt.graph.node.component.ui

import com.lehaine.littlekt.graphics.TextureSlice

/**
 * @author Colton Daily
 * @date 1/19/2022
 */
class StyleBoxTexture(val slice: TextureSlice) : StyleBox() {
    var hAxis: AxisStretchMode = AxisStretchMode.STRETCH
    var vAxis: AxisStretchMode = AxisStretchMode.STRETCH

    enum class AxisStretchMode {
        STRETCH,
        TILE,
        TILE_FIT
    }
}