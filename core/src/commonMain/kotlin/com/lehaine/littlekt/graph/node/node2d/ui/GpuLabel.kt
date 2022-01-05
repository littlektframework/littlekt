package com.lehaine.littlekt.graph.node.node2d.ui

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.graphics.GlyphLayout
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.TtfFont

/**
 * @author Colton Daily
 * @date 1/4/2022
 */
open class GpuLabel : Control() {
    private val layout = GlyphLayout()
    private var textDirty = false

    var pxSize: Int = 16
    var font: TtfFont? = null
    var gpuFont: GpuFont? = null

    var text: String = ""
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            gpuFont?.addText(field, x, y, pxSize)
            onMinimumSizeChanged()
        }

    var verticalAlign: VAlign = VAlign.TOP
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }
    var horizontalAlign: HAlign = HAlign.LEFT
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }

    var wrap: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }

    var uppercase: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            textDirty = true
            onMinimumSizeChanged()
        }


    override fun render(batch: SpriteBatch, camera: Camera) {
        gpuFont?.draw(batch)
    }
}