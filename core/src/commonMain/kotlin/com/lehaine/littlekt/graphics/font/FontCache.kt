package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.util.datastructure.FloatArrayList

/**
 * @author Colton Daily
 * @date 1/5/2022
 */
open class FontCache {
    private val temp4 = Mat4() // used for rotating text
    private val layouts = mutableListOf<GlyphLayout>()

    private var lastX = 0f
    private var lastY = 0f

    protected var x: Float = 0f
    protected var y: Float = 0f
    protected val vertices = FloatArrayList()
    protected var currentTint = Color.WHITE.toFloatBits()


    fun draw(batch: SpriteBatch, texture: Texture) {
        batch.draw(texture, vertices.data, 0, vertices.size)
    }

    fun translate(tx: Float, ty: Float) {
        if (tx == 0f && ty == 0f) return

        x += tx
        y += ty

        for (i in vertices.indices step 5) {
            vertices[i] += tx
            vertices[i + 1] += ty
        }
    }

    fun tint(tint: Color) {
        val newTint = tint.toFloatBits()
        if (newTint == currentTint) return
        currentTint = newTint

        layouts.forEach { layout ->
            val colors = layout.colors
            var colorsIndex = 0
            var nextColorGlyphIndex = 0
            var glyphIndex = 0
            var lastColorFloatBits = 0f
            layout.runs.forEach { run ->
                val glyphs = run.glyphs
                glyphs.forEach {
                    if (glyphIndex++ == nextColorGlyphIndex) {
                        tempColor.setAbgr8888(colors[++colorsIndex])
                        lastColorFloatBits = tempColor.mul(tint).toFloatBits()
                        nextColorGlyphIndex = if (++colorsIndex < colors.size) colors[colorsIndex] else -1
                    }
                    val offset = 2
                    vertices[offset] = lastColorFloatBits
                    vertices[offset + 5] = lastColorFloatBits
                    vertices[offset + 10] = lastColorFloatBits
                    vertices[offset + 15] = lastColorFloatBits
                }
            }
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
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        clear()
        addText(font, text, x, y, scale, rotation, color, targetWidth, align, wrap)
    }

    fun setText(
        layout: GlyphLayout,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
    ) {
        clear()
        addToCache(layout, x, y, scale, rotation, color)
    }

    fun addText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        scale: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        val layout = GlyphLayout() // TODO use pool
        layout.setText(font, text, color, targetWidth, scale, align, wrap)
        addToCache(layout, x, y, scale, rotation, color)
    }

    fun clear() {
        layouts.clear()
        x = 0f
        y = 0f
        vertices.clear()
    }

    private fun addToCache(
        layout: GlyphLayout, x: Float, y: Float, scale: Float, rotation: Angle, color: Color
    ) {
        layouts += layout
        layout.runs.forEach { run ->
            var tx = x + run.x
            val ty = y + run.y
            lastX = tx
            lastY = ty
            if (rotation != Angle.ZERO) {
                temp4.setToIdentity()
                temp4.translate(tx, ty, 0f)
                temp4.rotate(0f, 0f, rotation.degrees)
            }
            run.glyphs.forEachIndexed { index, glyph ->
                tx += run.advances[index]
                addGlyph(glyph, tx + glyph.left - glyph.right, ty + glyph.top + glyph.height, scale, rotation, color)
            }
        }
    }

    private fun addGlyph(
        glyph: GlyphMetrics,
        tx: Float,
        ty: Float,
        scale: Float,
        rotation: Angle,
        color: Color,
    ) {
        if (rotation != Angle.ZERO) {
            temp4.translate(tx - lastX, ty - lastY, 0f)
        }
        lastX = tx
        lastY = ty
        val mx = (if (rotation == Angle.ZERO) tx else temp4[12])
        val my = (if (rotation == Angle.ZERO) ty else temp4[13])
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
        val u = glyph.u
        val v = glyph.v2
        val u2 = glyph.u2
        val v2 = glyph.v

        vertices.run { // bottom left
            add(x1)
            add(y1)
            add(colorBits)
            add(u)
            add(v)
        }

        vertices.run { // top left
            add(x4)
            add(y4)
            add(colorBits)
            add(u)
            add(v2)
        }
        vertices.run { // top right
            add(x3)
            add(y3)
            add(colorBits)
            add(u2)
            add(v2)
        }
        vertices.run { // bottom right
            add(x2)
            add(y2)
            add(colorBits)
            add(u2)
            add(v)
        }
    }

    companion object {
        private val tempColor = MutableColor()
    }
}