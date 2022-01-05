package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.GlyphLayout
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.util.datastructure.FloatArrayList

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
class GpuFontCache {
    private val layouts = mutableListOf<GlyphLayout>()
    private var glyphCount = 0
    private var x: Float = 0f
    private var y: Float = 0f
    private val vertices = FloatArrayList()


    fun translate(tx: Float, ty: Float) {
        if (tx == 0f && ty == 0f) return

        x += tx
        y += ty

        for (i in vertices.indices step 5) {
            vertices[i] += tx
            vertices[i + 1] += ty
        }
    }


    fun setText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        clear()
        addText(font, text, x, y, scale, rotation, targetWidth, align, wrap)
    }

    fun addText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        val layout = GlyphLayout() // TODO use pool
        layout.setText(font, text, targetWidth, scale, align, wrap)
        addText(font, text, x, y, scale, rotation, targetWidth, align, wrap)
    }

    fun addText(font: Font, layout: GlyphLayout, x: Float, y: Float, rotation: Angle) {
        addToCache(layout, x, y + font.ascender, rotation)
    }

    fun clear() {
        layouts.clear()
        x = 0f
        y = 0f
        vertices.clear()
    }

    private fun addToCache(layout: GlyphLayout, x: Float, y: Float, rotation: Angle) {
        layouts += layout
        layout.runs.forEach { run ->
            var tx = x + run.x
            val ty = y + run.y
            run.glyphs.forEachIndexed { index, glyph ->
                tx += run.advances[index]
                addGlyph(glyph, tx, ty, rotation)
            }
        }
    }

    private fun addGlyph(glyph: Glyph, x: Float, y: Float, rotation: Angle) {

    }
}