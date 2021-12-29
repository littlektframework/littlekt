package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.ByteBuffer
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.file.vfs.writePixmap
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.BlendFactor
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.gl.State
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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max
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
     * The max amount of vertices to use for the text mesh. Increase or decrease this based on the amount of text
     * that will be displayed.
     */
    maxVertices: Int = 10000
) {
    var transformMatrix = Mat4()
        set(value) {
            if (drawing) {
                flush()
            }
            field = value
            if (drawing) {
                setupMatrices()
            }
        }
    var projectionMatrix = Mat4().setOrthographic(
        left = 0f,
        right = context.graphics.width.toFloat(),
        bottom = 0f,
        top = context.graphics.height.toFloat(),
        near = -1f,
        far = 1f
    )
        set(value) {
            if (drawing) {
                flush()
            }
            field = value
            if (drawing) {
                setupMatrices()
            }
        }

    var debug = false

    private val combinedMatrix = Mat4()

    private val compiler = GlyphCompiler()
    private val atlases = mutableListOf<AtlasGroup>()
    private val instances = mutableListOf<GpuGlyph>()
    private val compiledGlyphs = mutableMapOf<TtfFont, MutableMap<Int, GpuGlyph>>(defaultFont to mutableMapOf())
    private val vertices = FloatArrayList(maxVertices)

    private var drawing = false

    private val temp4 = Mat4() // used for rotating text

    private val mesh: Mesh = textureMesh(context.gl) {
        this.maxVertices = maxVertices
        useBatcher = false
    }.also { it.indicesAsQuad() }
    private val shader: ShaderProgram<GpuTextVertexShader, GpuTextFragmentShader> = ShaderProgram(
        GpuTextVertexShader(),
        GpuTextFragmentShader()
    ).also { it.prepare(context) }

    private val store: VfsFile get() = context.storageVfs
    private val gl: GL get() = context.gl

    init {
        shader.bind()
        shader.vertexShader.uTexelSize.apply(shader, Vec2f(1f / atlasWidth, 1f / atlasHeight))
        shader.fragmentShader.uTextureWidth.apply(shader, atlasWidth)
    }

    /**
     * Indicates to the GpuFont to start drawing. Binds the shader and prepares the matrices for drawing.
     * @param projectionMatrix the projection matrix to use in the text vertex shader.
     */
    fun begin(projectionMatrix: Mat4? = null) {
        check(!drawing) { "end() must be called before begin." }

        gl.depthMask(false)

        projectionMatrix?.let {
            this.projectionMatrix = it
        }

        shader.bind()
        setupMatrices()

        drawing = true
    }

    /**
     * Draws the specified string of text at the specified location and rotaion.
     * @param text the string of text to draw
     * @param x the x coord position
     * @param y the y coord position
     * @param pxSize the size of text in pixels
     * @param rotation the rotation to draw the text
     * @param color the color of the text
     */
    fun drawText(
        text: String,
        x: Float,
        y: Float,
        pxSize: Int,
        rotation: Angle = Angle.ZERO,
        color: Color = Color.BLACK,
        font: TtfFont = defaultFont
    ) {
        check(drawing) { "begin() must be called before drawText." }

        val scale = 1f / font.unitsPerEm * pxSize
        var tx = x
        var ty = y
        var lastX = tx
        var lastY = ty
        if (rotation != Angle.ZERO) {
            temp4.setIdentity()
            temp4.translate(tx, ty, 0f)
            temp4.rotate(0f, 0f, rotation.degrees)
        }
        text.forEach {
            if (it == '\r') {
                return@forEach
            }
            if (it == '\n') {
                ty += font.ascender * scale
                tx = x
                return@forEach
            }
            if (it == '\t') {
                tx += 2000 * scale
                return@forEach
            }

            val glyph = glyph(it, font)
            if (it != ' ') {
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
            tx += glyph.advanceWidth * scale
        }

        mesh.setVertices(vertices.data, 0, vertices.size)
    }

    /**
     * Indicates to the GpuFont to finish drawing and will [flush] any draws.
     */
    fun end() {
        check(drawing) { "begin() must be called before end." }
        if (instances.isNotEmpty()) {
            flush()
            vertices.clear()
            instances.clear()
        }
        drawing = false
        gl.depthMask(true)
        gl.disable(State.BLEND)
    }

    /**
     * Flushes the text mesh to be rendered.
     */
    fun flush() {
        atlases.forEach {
            if (it.uploaded) return@forEach
            it.texture.prepare(context)
            it.uploaded = true
        }
        if (atlases.isNotEmpty()) {
            // TODO handle multiple atlases
            gl.blendFunc(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA)
            gl.enable(State.BLEND)
            atlases[0].texture.bind()
            shader.vertexShader.uTexture.apply(shader)
            mesh.render(shader, count = vertices.size / 20 * 6)
            gl.disable(State.BLEND)
        }
    }


    private fun setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix)
        shader.uProjTrans?.apply(shader, combinedMatrix)
        shader.uTexture?.apply(shader)
    }

    private fun glyph(char: Char, font: TtfFont): GpuGlyph {
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
        writeGlyphToBuffer(
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

    private fun writeGlyphToBuffer(
        buffer: ByteBuffer,
        curves: List<Bezier>,
        glyphWidth: Int,
        glyphHeight: Int,
        gridX: Short,
        gridY: Short,
        gridWidth: Short,
        gridHeight: Short
    ) {
        buffer.putUShort(gridX).putUShort(gridY).putUShort(gridWidth).putUShort(gridHeight)
        curves.forEach {
            writeBezierToBuffer(buffer, it, glyphWidth, glyphHeight)
        }
    }

    /**
     * A [Bezier] is written as 6 16-bit integers (12 bytes). Increments buffer by the number of bytes written (always 12).
     * Coords are scaled from [0, glyphSize] to [o, UShort.MAX_VALUE]
     */
    private fun writeBezierToBuffer(buffer: ByteBuffer, bezier: Bezier, glyphWidth: Int, glyphHeight: Int) {
        buffer.apply {
            putUShort((bezier.p0.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUShort((bezier.p0.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
            putUShort((bezier.control.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUShort((bezier.control.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
            putUShort((bezier.p1.x * UShort.MAX_VALUE.toInt() / glyphWidth).toInt().toShort())
            putUShort((bezier.p1.y * UShort.MAX_VALUE.toInt() / glyphHeight).toInt().toShort())
        }
    }

    private fun getOpenAtlasGroup(): AtlasGroup {
        if (atlases.isEmpty() || atlases.last().full) {
            val atlas = AtlasGroup().apply {

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

@OptIn(ExperimentalContracts::class)
inline fun GpuFont.use(projectionMatrix: Mat4? = null, action: (GpuFont) -> Unit) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    begin(projectionMatrix)
    action(this)
    end()
}

private data class GpuGlyph(
    val width: Int,
    val height: Int,
    val offsetX: Int,
    val offsetY: Int,
    val bezierAtlasPosX: Int,
    val bezierAtlasPosY: Int,
    val atlasIdx: Int,
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




