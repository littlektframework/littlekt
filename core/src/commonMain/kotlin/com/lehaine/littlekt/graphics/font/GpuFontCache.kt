package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.GlyphLayout
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.internal.GpuGlyph
import com.lehaine.littlekt.graphics.internal.InternalResources
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.util.datastructure.FloatArrayList

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
class GpuFontCache {
    private val layouts = mutableListOf<GlyphLayout>()
    private var x: Float = 0f
    private var y: Float = 0f
    private val vertices = FloatArrayList()
    private val temp4 = Mat4() // used for rotating text

    private var lastX = 0f
    private var lastY = 0f

    fun translate(tx: Float, ty: Float) {
        if (tx == 0f && ty == 0f) return

        x += tx
        y += ty

        for (i in vertices.indices step 5) {
            vertices[i] += tx
            vertices[i + 1] += ty
        }
    }

    fun setPosition(px: Float, py: Float) = translate(px - x, py - y)

    fun setText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.BLACK,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        clear()
        addText(font, text, x, y, scale, rotation, color, targetWidth, align, wrap)
    }

    fun setText(
        font: Font,
        layout: GlyphLayout,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.BLACK,
    ) {
        clear()
        addToCache(font, layout, x, y, scale, rotation, color)
    }

    fun addText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.BLACK,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        val layout = GlyphLayout() // TODO use pool
        layout.setText(font, text, targetWidth, scale, align, wrap)
        addToCache(font, layout, x, y, scale, rotation, color)
    }

    fun render(batch: SpriteBatch) {
        if (vertices.isEmpty()) return

        if (batch.shader != InternalResources.INSTANCE.gpuFontShader) {
            batch.shader = InternalResources.INSTANCE.gpuFontShader
        }

        batch.draw(InternalResources.INSTANCE.gpuAtlas.texture, vertices.data, 0, vertices.size)
    }

    fun clear() {
        layouts.clear()
        x = 0f
        y = 0f
        vertices.clear()
    }

    private fun addToCache(
        font: Font, layout: GlyphLayout, x: Float, y: Float, scale: Float, rotation: Angle, color: Color
    ) {
        layouts += layout
        layout.runs.forEach { run ->
            var tx = x + run.x
            val ty = y + run.y
            lastX = tx
            lastY = ty
            run.glyphs.forEachIndexed { index, glyph ->
                tx += run.advances[index]
                val gpuGlyph = InternalResources.INSTANCE.compileGlyph(glyph.unicode.toChar(), font)
                addGlyph(gpuGlyph, tx, ty, scale, rotation, color)
            }
        }
    }

    private fun addGlyph(
        glyph: GpuGlyph,
        tx: Float,
        ty: Float,
        scale: Float,
        rotation: Angle,
        color: Color,
    ) {
        val bx = glyph.bezierAtlasPosX shl 1
        val by = glyph.bezierAtlasPosY shl 1
        val offsetX = glyph.offsetX * scale
        val offsetY = glyph.offsetY * scale
        if (rotation != Angle.ZERO) {
            temp4.translate(tx + offsetX - lastX, ty - offsetY - lastY, 0f)
        }
        lastX = tx + offsetX
        lastY = ty - offsetY
        val mx = (if (rotation == Angle.ZERO) tx + offsetX else temp4[12])
        val my = (if (rotation == Angle.ZERO) ty - offsetY else temp4[13])
        val p1x = 0f
        val p1y = -glyph.height * scale
        val p2x = glyph.width * scale
        val p3y = 0f
        var x1: Float = p1x
        var y1: Float = p1y
        var x2: Float = p2x
        var y2: Float = p1y
        var x3: Float = p2x
        var y3: Float = p3y
        var x4: Float = p1x
        var y4: Float = p3y
        if (rotation != Angle.ZERO) {
            val cos = rotation.cosine
            val sin = rotation.sine

            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y

            x2 = cos * p2x - sin * p1y
            y2 = sin * p2x + cos * p1y

            x3 = cos * p2x - sin * p3y
            y3 = sin * p2x + cos * p3y

            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        }
        x1 += mx
        y1 += my
        x2 += mx
        y2 += my
        x3 += mx
        y3 += my
        x4 += mx
        y4 += my

        val colorBits = color.toFloatBits()

        vertices.run { // bottom left
            add(x1)
            add(y1)
            add(colorBits)
            add(0f + bx)
            add(1f + by)
        }

        vertices.run { // top left
            add(x4)
            add(y4)
            add(colorBits)
            add(0f + bx)
            add(0f + by)
        }
        vertices.run { // top right
            add(x3)
            add(y3)
            add(colorBits)
            add(1f + bx)
            add(0f + by)
        }
        vertices.run { // bottom right
            add(x2)
            add(y2)
            add(colorBits)
            add(1f + bx)
            add(1f + by)
        }
    }
}