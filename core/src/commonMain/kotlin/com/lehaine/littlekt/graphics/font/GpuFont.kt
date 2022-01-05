package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.writePixmap
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.internal.GpuAtlas
import com.lehaine.littlekt.graphics.font.internal.GpuGlyph
import com.lehaine.littlekt.graphics.font.internal.GpuGlyphCompiler
import com.lehaine.littlekt.graphics.font.internal.GpuGlyphWriter
import com.lehaine.littlekt.graphics.font.internal.VGrid
import com.lehaine.littlekt.graphics.font.internal.VGridAtlas
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.GpuTextFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.GpuTextVertexShader
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.time.measureTimedValue

/**
 * @author Colton Daily
 * @date 12/9/2021
 */

class GpuFont(
    private val context: Context,
    /**
     * The default font to use if a font face isn't passed in during drawing of text.
     */
    private val defaultFont: TtfFont,
    /**
     * The width of the combined atlas that is generated for each GpuFont.
     */
    private val atlasWidth: Int = 256,
    /**
     * The height of the combined atlast the is generated for each GpuFont. The bezier and the grid will each share
     * half of this total. The grid will take the bottom half while the beziers will take the top.
     */
    private val atlasHeight: Int = 512,
    /**
     * The size of the grid that is generated for each glyph. Try increasing this value if some glyphs are rendering
     * badly. Beware that increasing this will impact the amount of space is taken up when saving to the atlas texture,
     * which will require increasing either the [atlasWidth] or [atlasHeight] to accommodate the extra space.
     */
    private val gridSize: Int = 10,
    /**
     * The max amount of vertices to use for cache the text Increase or decrease this based on the amount of text
     * that will be displayed.
     */
    maxVertices: Int = 10000
) {
    var debug = false

    private val compiler = GpuGlyphCompiler()
    private val atlases = mutableListOf<GpuAtlas>()
    private val instances = mutableListOf<GpuGlyph>()
    private val compiledGlyphs = mutableMapOf<TtfFont, MutableMap<Int, GpuGlyph>>(defaultFont to mutableMapOf())
    private val vertices = FloatArrayList(maxVertices)

    private val temp4 = Mat4() // used for rotating text

    private val shader: ShaderProgram<GpuTextVertexShader, GpuTextFragmentShader> = ShaderProgram(
        GpuTextVertexShader(),
        GpuTextFragmentShader()
    ).also { it.prepare(context) }

    private val store: VfsFile get() = context.storageVfs
    private val gl: GL get() = context.gl
    private val layout = GlyphLayout()

    init {
        shader.bind()
        shader.vertexShader.uTexelSize.apply(shader, Vec2f(1f / atlasWidth, 1f / atlasHeight))
        shader.fragmentShader.uTextureWidth.apply(shader, atlasWidth)
    }

    /**
     * Draws the specified string of text at the specified location and rotation.
     * @param text the string of text to draw
     * @param x the x coord position
     * @param y the y coord position
     * @param pxSize the size of text in pixels
     * @param rotation the rotation to draw the text
     * @param color the color of the text
     */
    fun addText(
        text: String,
        x: Float,
        y: Float,
        pxSize: Int,
        maxWidth: Float = 0f,
        align: HAlign = HAlign.LEFT,
        wrap: Boolean = false,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.BLACK,
        font: TtfFont = defaultFont
    ) {
        val scale = font.pxScale(pxSize)
        layout.setText(font, text, maxWidth, scale, align, wrap)

        layout.runs.forEach { run ->
            var tx = x + run.x
            val ty = y + run.y
            var lastX = tx
            var lastY = ty
            if (rotation != Angle.ZERO) {
                temp4.setToIdentity()
                temp4.translate(tx, ty, 0f)
                temp4.rotate(0f, 0f, rotation.degrees)
            }
            run.glyphs.forEachIndexed { index, runGlyph ->
                val char = runGlyph.unicode.toChar()

                if (!font.isWhitespace(char)) {
                    val glyph = compileGlyph(char, font)
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

                    vertices.run { // bottom left
                        add(x1)
                        add(y1)
                        add(color.toFloatBits())
                        add(0f + bx)
                        add(1f + by)
                    }

                    vertices.run { // top left
                        add(x4)
                        add(y4)
                        add(color.toFloatBits())
                        add(0f + bx)
                        add(0f + by)
                    }
                    vertices.run { // top right
                        add(x3)
                        add(y3)
                        add(color.toFloatBits())
                        add(1f + bx)
                        add(0f + by)
                    }
                    vertices.run { // bottom right
                        add(x2)
                        add(y2)
                        add(color.toFloatBits())
                        add(1f + bx)
                        add(1f + by)
                    }
                    instances += glyph
                }
                tx += run.advances[index]
            }
        }
    }

    /**
     * Flushes the text mesh to be rendered.
     */
    fun draw(batch: SpriteBatch) {
        atlases.forEach {
            if (it.uploaded) return@forEach
            it.texture.prepare(context)
            it.uploaded = true
        }
        if (atlases.isNotEmpty()) {
            // TODO handle multiple atlases
            batch.draw(atlases[0].texture, vertices.data, count = vertices.size)
        }
    }

    /**
     * Sets the specified [batch] to use this GPU fonts shader in order to render.
     * @param batch the batch to set the shader to
     */
    fun useShaderWith(batch: SpriteBatch) {
        batch.shader = shader
    }

    /**
     * Clear any cached vertices and glyph instances.
     */
    fun clear() {
        vertices.clear()
        instances.clear()
    }

    private fun compileGlyph(char: Char, font: TtfFont): GpuGlyph {
        // if already compiled -- return the glyph
        compiledGlyphs.getOrPut(font) { mutableMapOf() }[char.code]?.also { return it }

        var atlas = getOpenAtlasGroup()
        val glyph = font.glyphs[char.code] ?: error("Glyph for $char doesn't exist!")
        val curves =
            measureTimedValue { compiler.compile(glyph) }.also { logger.debug { "Took ${it.duration} to compile $char (${char.code}) glyph." } }.value
        val grid = VGrid(curves, glyph.width, glyph.height, gridSize, gridSize)

        // Although the data is represented as a 32bit texture, it's actually
        // two 16bit ints per pixel, each with an x and y coordinate for
        // the bezier. Every six 16bit ints (3 pixels) is a full bezier
        // plus two pixels for grid position information
        val bezierPixelLength = 2 + curves.size * 3

        val tooManyCurves = bezierPixelLength > atlasWidth * atlasHeight

        if (curves.isEmpty() || tooManyCurves) {
            if (tooManyCurves) {
                logger.warn { "Glyph '$char' has too many curves!" }
            }
            // TODO do what then if its empty or too many curves?
            val gpuGlyph = GpuGlyph(
                glyph.width,
                glyph.height,
                glyph.leftSideBearing,
                glyph.yMax - glyph.height,
                0,
                0,
                -1,
                glyph.advanceWidth.toInt()
            )
            compiledGlyphs[font]?.put(char.code, gpuGlyph)
            return gpuGlyph
        }

        if (atlas.glyphDataBufOffset + bezierPixelLength > atlasWidth * atlasHeight) {
            atlas.full = true
            atlas.uploaded = true
            atlas = getOpenAtlasGroup()
        }

        if (atlas.gridX + gridSize > atlasWidth) {
            atlas.gridY += gridSize
            atlas.gridX = 0
            if (atlas.gridY >= atlasHeight) {
                atlas.full = true
                atlas.uploaded = false
                atlas = getOpenAtlasGroup()
            }
        }

        val buffer = atlas.pixmap.pixels

        VGridAtlas.writeVGridAt(
            grid = grid,
            data = buffer,
            tx = atlas.gridX,
            ty = atlas.gridY,
            width = atlasWidth,
            height = atlasHeight,
            depth = 4
        )

        buffer.position = atlas.glyphDataBufOffset * ATLAS_CHANNELS + atlasWidth * (atlasHeight / 2) * ATLAS_CHANNELS
        GpuGlyphWriter.writeGlyphToBuffer(
            buffer, curves, glyph.width, glyph.height, atlas.gridX.toShort(), atlas.gridY.toShort(),
            gridSize.toShort(), gridSize.toShort()
        )

        val gpuGlyph = GpuGlyph(
            glyph.width,
            glyph.height,
            glyph.leftSideBearing,
            glyph.yMax - glyph.height,
            atlas.glyphDataBufOffset % atlasWidth,
            atlas.glyphDataBufOffset / atlasWidth + atlasHeight / 2,
            atlases.size - 1,
            glyph.advanceWidth.toInt()
        )

        compiledGlyphs[font]?.put(char.code, gpuGlyph)

        atlas.glyphDataBufOffset += bezierPixelLength
        atlas.gridX += gridSize
        atlas.uploaded = false

        if (debug) {
            store.vfs.launch {
                store["atlas.bmp"].writePixmap(Pixmap(atlasWidth, atlasHeight, buffer))
            }
        }
        return gpuGlyph
    }

    private fun getOpenAtlasGroup(): GpuAtlas {
        if (atlases.isEmpty() || atlases.last().full) {
            val atlas = GpuAtlas().apply {

                pixmap = Pixmap(atlasWidth, atlasHeight, createByteBuffer(atlasWidth * atlasHeight * ATLAS_CHANNELS))
                uploaded = true
                texture.prepare(context)
            }
            atlases += atlas
        }
        return atlases.last()
    }

    companion object {
        private const val ATLAS_CHANNELS = 4 // Must be 4 (RGBA)

        private val logger = Logger<GpuFont>()
    }
}




