package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.Batch
import com.littlekt.math.Mat4
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.cosine
import com.littlekt.math.geom.normalized
import com.littlekt.math.geom.sine
import com.littlekt.math.isFuzzyZero
import com.littlekt.util.datastructure.FloatArrayList
import com.littlekt.util.datastructure.Pool

/**
 * Caches glyph geometry to provide a fast way to render static textures without having recompute
 * the glyph each time. This is a base class and can be extended to add additional functionality to
 * specific fonts.
 *
 * @see BitmapFontCache
 * @author Colton Daily
 * @date 1/5/2022
 */
open class FontCache(val pages: Int = 1) {
    private val temp4 = Mat4() // used for rotating text
    private val layouts = mutableListOf<GlyphLayout>()
    private val tempGlyphCount = IntArray(pages)

    private val layoutPool = Pool(reset = { it.reset() }) { GlyphLayout() }
    private val pooledLayouts = mutableListOf<GlyphLayout>()

    private var lastX = 0f
    private var lastY = 0f

    protected var x: Float = 0f
    protected var y: Float = 0f
    protected val pageVertices = Array(pages) { FloatArrayList() }
    protected var currentTint = Color.WHITE

    /**
     * Draws the text using the specified batch and list of textures. The [textures] list size must
     * be >= [pages]
     *
     * @param batch the batch to draw with
     * @param textures the textures to use for drawing
     */
    fun draw(batch: Batch, textures: List<Texture>) {
        pageVertices.forEachIndexed { index, vertices ->
            if (vertices.isNotEmpty()) {
                batch.draw(textures[index], vertices.data, 0, vertices.size)
            }
        }
    }

    /**
     * Moves the position of the text by the specified amount.
     *
     * @param tx the amount to move the x position
     * @param ty the amount to move the y position
     */
    fun translate(tx: Float, ty: Float) {
        if (tx == 0f && ty == 0f) return

        x += tx
        y += ty

        pageVertices.forEach { vertices ->
            for (i in vertices.indices step 9) {
                vertices[i] += tx
                vertices[i + 1] += ty
            }
        }
    }

    /**
     * Tints the existing text in the cache.
     *
     * @param tint the tint color
     */
    fun tint(tint: Color) {
        if (tint == currentTint) return
        currentTint = tint
        tempGlyphCount.fill(0)
        layouts.forEach { layout ->
            val colors = layout.colors
            var colorsIndex = 0
            var nextColorGlyphIndex = 0
            var glyphIndex = 0
            var lastColor = Color.BLACK
            layout.runs.forEach { run ->
                val glyphs = run.glyphs
                glyphs.forEach {
                    if (glyphIndex++ == nextColorGlyphIndex) {
                        tempColor.setAbgr888(colors[++colorsIndex])
                        lastColor = tempColor.mul(tint)
                        nextColorGlyphIndex =
                            if (++colorsIndex < colors.size) colors[colorsIndex] else -1
                    }
                    val offset = tempGlyphCount[it.page] * 36 + 2
                    tempGlyphCount[it.page]++
                    val vertices = pageVertices[it.page]
                    vertices[offset] = lastColor.r
                    vertices[offset + 1] = lastColor.g
                    vertices[offset + 2] = lastColor.b
                    vertices[offset + 3] = lastColor.a
                    vertices[offset + 8] = lastColor.r
                    vertices[offset + 9] = lastColor.g
                    vertices[offset + 10] = lastColor.b
                    vertices[offset + 11] = lastColor.a
                    vertices[offset + 16] = lastColor.r
                    vertices[offset + 17] = lastColor.g
                    vertices[offset + 18] = lastColor.b
                    vertices[offset + 19] = lastColor.a
                    vertices[offset + 24] = lastColor.r
                    vertices[offset + 25] = lastColor.g
                    vertices[offset + 26] = lastColor.b
                    vertices[offset + 27] = lastColor.a
                }
            }
        }
    }

    /**
     * Sets the position of the text.
     *
     * @param px the x coordinate
     * @param py the y coordinate
     */
    fun setPosition(px: Float, py: Float) = translate(px - x, py - y)

    /**
     * Clears any existing glyphs of previous text and adds the new glyphs of the specified string
     * of [text].
     *
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
        wrap: Boolean = false,
        truncate: String? = null,
    ) {
        clear()
        addText(
            font,
            text,
            x,
            y,
            scaleX,
            scaleY,
            rotation,
            color,
            targetWidth,
            align,
            wrap,
            truncate
        )
    }

    /**
     * Clears any existing glyphs of previous text and uses [layout] to compile the glyphs to cache.
     *
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
     *
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
     * @param truncate the string to display when the text is too long to fit within [targetWidth]
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
        wrap: Boolean = false,
        truncate: String? = null,
    ) {
        val layout = layoutPool.alloc()
        pooledLayouts += layout
        layout.setText(font, text, color, targetWidth, scaleX, scaleY, align, wrap, truncate)
        addToCache(layout, x, y, scaleX, scaleY, rotation, color)
    }

    /** Clears any existing glyphs from the cache. */
    fun clear() {
        layoutPool.free(pooledLayouts)
        pooledLayouts.clear()
        layouts.clear()
        x = 0f
        y = 0f
        currentTint = Color.WHITE
        pageVertices.forEach { it.clear() }
    }

    private fun addToCache(
        layout: GlyphLayout,
        x: Float,
        y: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        color: Color,
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
                temp4.rotate(Angle.ZERO, Angle.ZERO, rotation)
            }
            run.glyphs.forEachIndexed { index, glyph ->
                tx += run.advances[index]
                addGlyph(glyph, tx, ty, scaleX, scaleY, rotation, color)
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
        val mx = (if (rotation.normalized.radians.isFuzzyZero()) tx else temp4[12])
        val my = (if (rotation.normalized.radians.isFuzzyZero()) ty else temp4[13])

        val p1x = glyph.left * scaleX
        val p1y = glyph.bottom * scaleY
        val p2x = glyph.right * scaleX
        val p2y = glyph.top * scaleY

        var tlX: Float = p1x
        var tlY: Float = p2y
        var trX: Float = p2x
        var trY: Float = p2y
        var brX: Float = p2x
        var brY: Float = p1y
        var blX: Float = p1x
        var blY: Float = p1y
        if (rotation != Angle.ZERO) {
            val cos = rotation.cosine
            val sin = rotation.sine

            tlX = cos * p1x - sin * p1y
            tlY = sin * p1x + cos * p1y

            trX = cos * p2x - sin * p1y
            trY = sin * p2x + cos * p1y

            brX = cos * p2x - sin * p2y
            brY = sin * p2x + cos * p2y

            blX = tlX + (brX - trX)
            blY = brY - (trY - tlY)
        }
        tlX += mx
        tlY += my
        trX += mx
        trY += my
        brX += mx
        brY += my
        blX += mx
        blY += my

        val u0 = glyph.u0
        val v0 = glyph.v0
        val u1 = glyph.u1
        val v1 = glyph.v1

        val vertices = pageVertices[glyph.page]

        vertices.run { // top left
            add(tlX)
            add(tlY)
            add(0f)
            add(color.r)
            add(color.g)
            add(color.b)
            add(color.a)
            add(u0)
            add(v0)
        }
        vertices.run { // top right
            add(trX)
            add(trY)
            add(0f)
            add(color.r)
            add(color.g)
            add(color.b)
            add(color.a)
            add(u1)
            add(v0)
        }
        vertices.run { // bottom right
            add(brX)
            add(brY)
            add(0f)
            add(color.r)
            add(color.g)
            add(color.b)
            add(color.a)
            add(u1)
            add(v1)
        }
        vertices.run { // bottom left
            add(blX)
            add(blY)
            add(0f)
            add(color.r)
            add(color.g)
            add(color.b)
            add(color.a)
            add(u0)
            add(v1)
        }
    }

    companion object {
        private val tempColor = MutableColor()
    }
}
