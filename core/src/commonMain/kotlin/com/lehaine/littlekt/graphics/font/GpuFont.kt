package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.FileHandler
import com.lehaine.littlekt.file.MixedBuffer
import com.lehaine.littlekt.file.createMixedBuffer
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.VertexAttrType
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.GpuTextFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.GpuTextVertexShader
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.math.max
import kotlin.time.measureTimedValue

/**
 * @author Colton Daily
 * @date 12/9/2021
 */

class GpuFont(
    private val font: TtfFont,
    private val atlasWidth: Int = 256,
    private val atlasHeight: Int = 512
) {
    private val compiler = GlyphCompiler()
    private val atlases = mutableListOf<AtlasGroup>()
    private val instances = mutableListOf<GpuGlyph>()
    private val compiledGlyphs = mutableMapOf<TtfFont, MutableMap<Int, GpuGlyph>>(font to mutableMapOf())
    private val vertices = FloatArrayList(1000)
    private var text = StringBuilder("")

    private lateinit var mesh: Mesh
    private lateinit var shader: ShaderProgram<GpuTextVertexShader, GpuTextFragmentShader>
    private lateinit var context: Context

    private val fileHandler: FileHandler get() = context.fileHandler
    private val gl: GL get() = context.gl


    fun prepare(context: Context) {
        this.context = context
        mesh = mesh(
            context.gl, listOf(
                VertexAttribute.POSITION_2D,
                VertexAttribute.COLOR_PACKED,
                VertexAttribute(
                    usage = VertexAttrUsage.TEX_COORDS,
                    numComponents = 2,
                    alias = ShaderProgram.TEXCOORD_ATTRIBUTE + 0,
                    type = VertexAttrType.FLOAT,
                    unit = 0
                )
            )
        ) {
            maxVertices = 10000
            useBatcher = false
        }.also { it.indicesAsQuad() }
        shader = ShaderProgram(GpuTextVertexShader(), GpuTextFragmentShader()).also { it.prepare(context) }
        shader.bind()
        shader.vertexShader.uTexelSize.apply(shader, Vec2f(1f / atlasWidth, 1f / atlasHeight))
    }

    fun insertText(text: String, x: Float, y: Float, pxSize: Int, color: Color = Color.BLACK) {
        val lastSize = vertices.size
        this.text.append(text)
        val scale = 1f / font.unitsPerEm * pxSize
        var tx = x
        var ty = y
        text.forEach {
            if (it == '\r') {
                return@forEach
            }
            if (it == '\n') {
                ty -= font.ascender
                tx = x
                return@forEach
            }
            if (it == '\t') {
                tx += 2000 * scale
                return@forEach
            }

            val glyph = glyph(it)
            if (it != ' ') {
                val bx = glyph.bezierAtlasPosX * 2
                val by = glyph.bezierAtlasPosY * 2
                vertices.run { // bottom left
                    add(tx)
                    add(ty)
                    add(color.toFloatBits())
                    add(0f + bx)
                    add(0f + by)
                }
                vertices.run { // bottom right
                    add(glyph.width * scale + tx)
                    add(ty)
                    add(color.toFloatBits())
                    add(1f + bx)
                    add(0f + by)
                }
                vertices.run { // top right
                    add(glyph.width * scale + tx)
                    add(glyph.height * scale + ty)
                    add(color.toFloatBits())
                    add(1f + bx)
                    add(1f + by)
                }
                vertices.run { // top left
                    add(tx)
                    add(glyph.height * scale + ty)
                    add(color.toFloatBits())
                    add(0f + bx)
                    add(1f + by)
                }
                instances += glyph
            }
            tx += glyph.advanceWidth * scale
        }

        mesh.setVertices(vertices.data, 0, vertices.size)
    }

    fun render(viewProjection: Mat4? = null) {
        atlases.forEach {
            if (it.uploaded) return@forEach
            it.texture.prepare(context)
            it.uploaded = true
        }
        if (atlases.isNotEmpty()) {
            shader.bind()
            atlases[0].texture.bind(0)
            viewProjection?.let {
                shader.vertexShader.uProjTrans.apply(shader, viewProjection)
            }
            shader.vertexShader.uTexture.apply(shader)
            mesh.render(shader)
        }
    }

    private fun glyph(char: Char): GpuGlyph {
        // if already compiled -- return the glyph
        compiledGlyphs[font]?.get(char.code)?.also { return it }

        var atlas = getOpenAtlasGroup()
        val glyph = font.glyphs[char.code] ?: error("Glyph for $char doesn't exist!")
        val curves =
            measureTimedValue { compiler.compile(glyph) }.also { logger.debug { "Took ${it.duration} to compile $char glyph." } }.value
        val grid = VGrid(curves, glyph.width, glyph.height, GRID_MAX_SIZE, GRID_MAX_SIZE)

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
                glyph.yMax,
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

        if (atlas.gridX + GRID_MAX_SIZE > atlasWidth) {
            atlas.gridY += GRID_MAX_SIZE
            atlas.gridX = 0
            if (atlas.gridY >= atlasHeight) {
                atlas.full = true
                atlas.uploaded = false
                atlas = getOpenAtlasGroup()
            }
        }

        val buffer = atlas.pixmap.pixels
        buffer.position = atlas.glyphDataBufOffset * ATLAS_CHANNELS
        writeGlyphToBuffer(
            buffer, curves, glyph.width, glyph.height, atlas.gridX.toShort(), atlas.gridY.toShort(),
            GRID_MAX_SIZE.toShort(), GRID_MAX_SIZE.toShort()
        )
        VGridAtlas.writeVGridAt(
            grid = grid,
            data = buffer,
            offset = atlasWidth * (atlasHeight / 2) * ATLAS_CHANNELS,
            tx = atlas.gridX,
            ty = atlas.gridY,
            width = atlasWidth,
            height = atlasHeight,
            depth = ATLAS_CHANNELS
        )

        val gpuGlyph = GpuGlyph(
            glyph.width,
            glyph.height,
            glyph.leftSideBearing,
            glyph.yMax,
            atlas.glyphDataBufOffset % atlasWidth,
            atlas.glyphDataBufOffset / atlasWidth,
            atlases.size - 1,
            glyph.advanceWidth.toInt()
        )
        println(gpuGlyph)
        compiledGlyphs[font]?.put(char.code, gpuGlyph)

        atlas.glyphDataBufOffset += bezierPixelLength
        atlas.gridX += GRID_MAX_SIZE
        atlas.uploaded = false

        // TODO - unique atlas names
        writeBMP("atlas.bmp", atlasWidth, atlasHeight, ATLAS_CHANNELS, buffer)
        return gpuGlyph
    }

    private fun writeBMP(name: String, width: Int, height: Int, channels: Int, buffer: MixedBuffer) {
        val bmpBuffer = createMixedBuffer(60 + buffer.capacity)
        bmpBuffer.run {
            putInt8('B'.code.toByte())
            putInt8('M'.code.toByte())
            putUint32(54 + width * height * channels) // size
            putUint16(0) // res1
            putUint16(0) // res2
            putUint32(54) // offset
            putUint32(40) // biSize
            putUint32(width)
            putUint32(height)
            putUint16(1.toShort()) // planes
            putUint16((8 * channels).toShort()) // bitCount
            putUint32(0) // compression
            putUint32(width * height * channels) // image size bytes
            putUint32(0) // x pixels per meter
            putUint32(0) // y pixels per meter
            putUint32(0) // clr used
            putUint32(0) //clr important
            putInt8(buffer.toArray(), 0, buffer.capacity)
        }
        fileHandler.store(name, bmpBuffer.toArray())
    }

    private fun writeGlyphToBuffer(
        buffer: MixedBuffer,
        curves: List<Bezier>,
        glyphWidth: Int,
        glyphHeight: Int,
        gridX: Short,
        gridY: Short,
        gridWidth: Short,
        gridHeight: Short
    ) {
        buffer.putUint16(gridX).putUint16(gridY).putUint16(gridWidth).putUint16(gridHeight)
        curves.forEach {
            writeBezierToBuffer(buffer, it, glyphWidth, glyphHeight)
        }
    }

    /**
     * A [Bezier] is written as 6 16-bit integers (12 bytes). Increments buffer by the number of bytes written (always 12).
     * Coords are scaled from [0, glyphSize] to [o, UShort.MAX_VALUE]
     */
    private fun writeBezierToBuffer(buffer: MixedBuffer, bezier: Bezier, glyphWidth: Int, glyphHeight: Int) {
        buffer.apply {
            putUint16((bezier.p0.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUint16((bezier.p0.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
            putUint16((bezier.control.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUint16((bezier.control.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
            putUint16((bezier.p1.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUint16((bezier.p1.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
        }
    }

    private fun getOpenAtlasGroup(): AtlasGroup {
        if (atlases.isEmpty() || atlases.last().full) {
            val atlas = AtlasGroup().apply {
                pixmap = Pixmap(atlasWidth, atlasHeight, createMixedBuffer(atlasWidth * atlasHeight * ATLAS_CHANNELS))
                uploaded = true
                texture.prepare(context)
            }
            atlases += atlas
        }
        return atlases.last()
    }

    companion object {
        private const val GRID_MAX_SIZE = 10 // should this be 20?
        private const val ATLAS_CHANNELS = 4 // Must be 4 (RGBA)

        private val logger = Logger<GpuFont>()
    }
}

private data class GpuGlyph(
    val width: Int,
    val height: Int,
    val offsetX: Int,
    val offsetY: Int,
    val bezierAtlasPosX: Int,
    val bezierAtlasPosY: Int,
    val bezierAtlasPosZ: Int,
    val advanceWidth: Int
)

private class AtlasGroup {
    var pixmap = Pixmap(0, 0)
        set(value) {
            field = value
            textureData = PixmapTextureData(field, false)
        }
    var textureData = PixmapTextureData(pixmap, false)
        set(value) {
            field = value
            texture = Texture(field)
        }
    var texture = Texture(textureData)
    var gridX = 0
    var gridY = 0
    var full = false
    var uploaded = false

    var glyphDataBufOffset = 0

    override fun toString(): String {
        return "AtlasGroup(x=$gridX, y=$gridY, full=$full, uploaded=$uploaded, glyphDataBufOffset=$glyphDataBufOffset)"
    }
}

private class GlyphCompiler {

    fun compile(glyph: Glyph): List<Bezier> {
        // Tolerance for error when approximating cubic beziers with quadratics.
        // Too low and many quadratics are generated (slow), too high and not
        // enough are generated (looks bad). 5% works pretty well.
        val c2qResolution = max((((glyph.width + glyph.height) / 2) * 0.05f).toInt(), 1)
        val beziers = decompose(glyph, c2qResolution)

        if (glyph.xMin != 0 || glyph.yMin != 0) {
            translateBeziers(beziers, glyph.xMin, glyph.yMin)
        }

        // TODO calculate if glyph orientation is clockwise or counter clockwise. If, CCW then we need to flip the beziers
        val counterClockwise = false //glyph.orientation == FILL_LEFT
        if (counterClockwise) {
            flipBeziers(beziers)
        }
        return beziers
    }

    private fun flipBeziers(beziers: ArrayList<Bezier>) {
        beziers.forEach { bezier ->
            bezier.p0.x = bezier.p1.x.also { bezier.p1.x = bezier.p0.x }
            bezier.p0.y = bezier.p1.y.also { bezier.p1.y = bezier.p0.y }
        }
    }

    private fun decompose(glyph: Glyph, c2qResolution: Int): ArrayList<Bezier> {
        if (glyph.path.isEmpty() || glyph.numberOfContours <= 0) {
            return ArrayList()
        }
        val curves = ArrayList<Bezier>(glyph.numberOfContours)
        val quadBeziers = Array(24) { QuadraticBezier(0f, 0f, 0f, 0f, 0f, 0f) }

        var startX = 0f
        var startY = 0f
        var prevX = 0f
        var prevY = 0f
        glyph.path.commands.forEach { cmd ->
            when (cmd.type) {
                GlyphPath.CommandType.MOVE_TO -> {
                    startX = cmd.x
                    startY = cmd.y
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.LINE_TO -> {
                    curves += Bezier().apply {
                        p0.set(prevX, prevY)
                        control.set(prevX, prevY)
                        p1.set(cmd.x, cmd.y)
                    }
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.CURVE_TO -> {
                    val cubicBezier = CubicBezier(prevX, prevY, cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y)

                    val totalBeziers = 6 * cubicBezier.convertToQuadBezier(c2qResolution, quadBeziers)
                    for (i in 0 until totalBeziers step 6) {
                        val quadBezier = quadBeziers[i]
                        curves += Bezier().apply {
                            p0.set(quadBezier.p1x, quadBezier.p1y)
                            control.set(quadBezier.c1x, quadBezier.c1y)
                            p1.set(quadBezier.p2x, quadBezier.p2y)
                        }
                    }
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.QUADRATIC_CURVE_TO -> {
                    curves += Bezier().apply {
                        p0.set(prevX, prevY)
                        control.set(cmd.x1, cmd.y1)
                        p1.set(cmd.x, cmd.y)
                    }
                    prevX = cmd.x
                    prevY = cmd.y
                }
                GlyphPath.CommandType.CLOSE -> {
                    prevX = startX
                    prevY = startY
                }
            }
        }
        return curves
    }


    private fun translateBeziers(beziers: ArrayList<Bezier>, xMin: Int, yMin: Int) {
        beziers.forEach {
            it.p0.x -= xMin
            it.p0.y -= yMin
            it.p1.x -= xMin
            it.p1.y -= yMin
            it.control.x -= xMin
            it.control.y -= yMin

        }
    }
}




