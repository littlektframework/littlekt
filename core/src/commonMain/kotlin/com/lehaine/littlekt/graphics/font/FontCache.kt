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
 * Caches glyph geometry to provide a fast way to render static textures without having recompute the glyph each time.
 * This is a base class and can be extended to add additional functionality to specific fonts.
 * @see BitmapFontCache
 * @author Colton Daily
 * @date 1/5/2022
 */
open class FontCache(val pages: Int = 1) {
    private val temp4 = Mat4() // used for rotating text
    private val layouts = mutableListOf<GlyphLayout>()

    private var lastX = 0f
    private var lastY = 0f

    protected var x: Float = 0f
    protected var y: Float = 0f
    protected val pageVertices = Array(pages) { FloatArrayList() }
    protected var currentTint = Color.WHITE.toFloatBits()


    /**
     * Draws the text using the specified batch and list of textures.
     * The [textures] list size must be >= [pages]
     * @param batch the batch to draw with
     * @param textures the textures to use for drawing
     */
    fun draw(batch: SpriteBatch, textures: List<Texture>) {
        pageVertices.forEachIndexed { index, vertices ->
            if (vertices.isNotEmpty()) {
                batch.draw(textures[index], vertices.data, 0, vertices.size)
            }
        }
    }

    /**
     * Moves the position of the text by the specified amount.
     * @param tx the amount to move the x position
     * @param ty the amount to move the y position
     */
    fun translate(tx: Float, ty: Float) {
        if (tx == 0f && ty == 0f) return

        x += tx
        y += ty

        pageVertices.forEach { vertices ->
            for (i in vertices.indices step 5) {
                vertices[i] += tx
                vertices[i + 1] += ty
            }
        }
    }

    /**
     * Tints the existing text in the cache.
     * @param tint the tint color
     */
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
                    val vertices = pageVertices[it.page]
                    vertices[offset] = lastColorFloatBits
                    vertices[offset + 5] = lastColorFloatBits
                    vertices[offset + 10] = lastColorFloatBits
                    vertices[offset + 15] = lastColorFloatBits
                }
            }
        }
    }

    /**
     * Sets the position of the text.
     * @param px the x coordinate
     * @param py the y coordinate
     */
    fun setPosition(px: Float, py: Float) = translate(px - x, py - y)

    /**
     * Clears any existing glyphs of previous text and adds the new glyphs of the specified string of [text].
     * @param font the font that contains the glyphs to use
     * @param text the string of text to draw
     * @param x the x position to draw the text
     * @param y the y position to draw the text
     * @param scaleX the scale of the x component of the glyphs
     * @param scaleY the scale of the y component of the glyphs
     * @param rotation the rotation of the text to draw
     * @param color the color of the text to draw
     * @param targetWidth the width of the area the text will be drawn, for wrapping or truncation
     * @param align the horizontal alignment of the text, see [HAlign]
     * @param wrap if true, the text will be wrapped within the [targetWidth]
     */
    fun setText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        clear()
        addText(font, text, x, y, scaleX, scaleY, rotation, color, targetWidth, align, wrap)
    }

    /**
     * Clears any existing glyphs of previous text and uses [layout] to compile the glyphs to cache.
     * @param layout the glyph layout to cache
     * @param x the x position to draw the text
     * @param y the y position to draw the text
     * @param scaleX the scale of the x component of the glyphs
     * @param scaleY the scale of the y component of the glyphs
     * @param rotation the rotation of the text to draw
     * @param color the color of the text to draw
     */
    fun setText(
        layout: GlyphLayout,
        x: Float,
        y: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
    ) {
        clear()
        addToCache(layout, x, y, scaleX, scaleY, rotation, color)
    }

    /**
     * Adds new glyphs of the specified string of [text] on top of any existing glyphs.
     * @param font the font that contains the glyphs to use
     * @param text the string of text to draw
     * @param x the x position to draw the text
     * @param y the y position to draw the text
     * @param scaleX the scale of the x component of the glyphs
     * @param scaleY the scale of the y component of the glyphs
     * @param rotation the rotation of the text to draw
     * @param color the color of the text to draw
     * @param targetWidth the width of the area the text will be drawn, for wrapping or truncation
     * @param align the horizontal alignment of the text, see [HAlign]
     * @param wrap if true, the text will be wrapped within the [targetWidth]
     */
    fun addText(
        font: Font,
        text: CharSequence,
        x: Float,
        y: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.WHITE,
        targetWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false
    ) {
        val layout = GlyphLayout() // TODO use pool
        layout.setText(font, text, color, targetWidth, scaleX, scaleY, align, wrap)
        addToCache(layout, x, y, scaleX, scaleY, rotation, color)
    }

    /**
     * Clears any existing glyphs from the cache.
     */
    fun clear() {
        layouts.clear()
        x = 0f
        y = 0f
        pageVertices.forEach {
            it.clear()
        }
    }

    private fun addToCache(
        layout: GlyphLayout, x: Float, y: Float, scaleX: Float, scaleY: Float, rotation: Angle, color: Color
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
                addGlyph(
                    glyph,
                    tx + (glyph.left - glyph.right) * scaleX,
                    ty + (glyph.top + glyph.height) * scaleY,
                    scaleX,
                    scaleY,
                    rotation,
                    color
                )
            }
        }
    }

    private fun addGlyph(
        glyph: GlyphMetrics,
        tx: Float,
        ty: Float,
        scaleX: Float,
        scaleY: Float,
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
        val p1y = -glyph.height * scaleY
        val p2x = glyph.width * scaleX
        val p2y = 0f

        var x1: Float = p1x
        var y1: Float = p1y
        var x2: Float = p2x
        var y2: Float = p1y
        var x3: Float = p2x
        var y3: Float = p2y
        var x4: Float = p1x
        var y4: Float = p2y
        if (rotation != Angle.ZERO) {
            val cos = rotation.cosine
            val sin = rotation.sine

            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y

            x2 = cos * p2x - sin * p1y
            y2 = sin * p2x + cos * p1y

            x3 = cos * p2x - sin * p2y
            y3 = sin * p2x + cos * p2y

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

        val vertices = pageVertices[glyph.page]

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