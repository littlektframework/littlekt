package com.lehaine.littlekt.graphics.g2d.font

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Experimental
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.CompareFunction
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.gl.StencilAction
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.GlyphFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.GlyphVertexShader
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.RectBuilder
import com.lehaine.littlekt.util.datastructure.FloatArrayList
import kotlin.time.measureTime

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
@Experimental
class VectorFont(private val font: TtfFont) : Preparable {
    private lateinit var glyphRenderer: GlyphRenderer

    private lateinit var glyphShader: ShaderProgram<GlyphVertexShader, GlyphFragmentShader>

    private lateinit var glyphMesh: Mesh
    private lateinit var gl: GL
    private lateinit var fbo: FrameBuffer
    private lateinit var fboSlice: TextureSlice

    private var isPrepared = false

    // text block cache -  textBlock.id to textBlock.hashCode
    private val textBlockCache = linkedMapOf<Int, Pair<Int, Pair<Float, Float>>>()

    // text block text cache - textBlock.id to mapOf text.id to text.hashCode
    private val textCache = mutableMapOf<Int, MutableMap<Int, Int>>()

    // text data cache - text.id to TextData object
    private val textDataCache = mutableMapOf<Int, TextData>()

    private val instances = mutableListOf<TextBlock>()
    private val instancesHash = linkedSetOf<Int>()
    private val lastInstancesHash = linkedSetOf<Int>()

    private val vertices = FloatArrayList(500000)
    private var offset = 0

    override val prepared: Boolean
        get() = isPrepared


    override fun prepare(context: Context) {
        gl = context.gl
        glyphShader = ShaderProgram(GlyphVertexShader(), GlyphFragmentShader()).also { it.prepare(context) }

        glyphMesh = textureMesh(context.gl, vertices.capacity) {}
        glyphRenderer = GlyphRenderer()
        fbo = FrameBuffer(
            context.graphics.width,
            context.graphics.height,
        ).apply { prepare(context) }
        fboSlice = fbo.colorBufferTexture.slice()
        isPrepared = true
    }

    /**
     * Resizes the internal frame buffer.
     */
    fun resize(width: Int, height: Int, context: Context) {
        if (!prepared) return
        fbo.dispose()
        fbo = FrameBuffer(
            width, height
        ).apply { prepare(context) }
        fboSlice = fbo.colorBufferTexture.slice()
    }

    /**
     * Queue a [TextBlock] to render.
     * @param text the [TextBlock] to render
     */
    fun queue(text: TextBlock) {
        check(isPrepared) { "VectorFont has not been prepared yet! Please call prepare() before using!" }

        measureTime {
            renderText(text)
            instancesHash += text.hashCode()
            instances += text
        }
    }


    /**
     * Renders the text to a stencil buffer in order to flip the pixels the correct way and then renders to the
     * color buffer. This does not use any antialiasing. The color of the text passed in [queue] will be rendered.
     * @param viewProjection the combined view projection matrix to render the text
     */
    fun flush(viewProjection: Mat4) {
        // if not using FBO - then lets use the stencil buffer
        gl.enable(State.STENCIL_TEST)
        gl.colorMask(red = false, green = false, blue = false, alpha = false)
        gl.clear(ClearBufferMask.STENCIL_BUFFER_BIT)
        gl.stencilFunc(CompareFunction.ALWAYS, 1, 1)
        gl.stencilOp(StencilAction.KEEP, StencilAction.KEEP, StencilAction.INVERT)

        updateGlyphMeshVertices()

        glyphShader.bind()
        glyphShader.uProjTrans?.apply(glyphShader, viewProjection)
        glyphMesh.render(glyphShader, count = offset / 5)

        gl.colorMask(red = true, green = true, blue = true, alpha = true)
        gl.stencilFunc(CompareFunction.EQUAL, 1, 1)
        gl.stencilOp(StencilAction.KEEP, StencilAction.KEEP, StencilAction.INVERT)

        glyphMesh.render(glyphShader, count = offset / 5)

        gl.disable(State.STENCIL_TEST)

        reset()
    }

    private fun updateGlyphMeshVertices() {
        if (instancesHash.size == lastInstancesHash.size && lastInstancesHash.containsAll(instancesHash)) {
            instances.forEach { block ->
                block.text.forEach {
                    offset += textDataCache[it.id]?.vertices?.size ?: 0
                }
            }
            // no need to upload vertices or set them again
        } else {
            val iterator = lastInstancesHash.iterator()

            run checkInstances@{
                instancesHash.forEachIndexed { index, hash ->
                    if (iterator.hasNext()) {
                        val last = iterator.next()
                        if (last != hash) {
                            uploadTextBlockVertices(instances.drop(index))
                            return@checkInstances
                        } else {
                            instances[index].text.forEach {
                                offset += textDataCache[it.id]?.vertices?.size ?: 0
                            }
                        }
                    } else if (index < instances.size) {
                        uploadTextBlockVertices(instances.drop(index))
                        return@checkInstances
                    }
                }
            }
            lastInstancesHash.clear()
            lastInstancesHash.addAll(instancesHash)
            glyphMesh.geometry.add(vertices.toFloatArray())
        }
    }

    private fun reset() {
        offset = 0
        instances.clear()
        instancesHash.clear()
    }

    private fun renderText(textBlock: TextBlock) {
        val cachedTextBlockHash = textBlockCache[textBlock.id]

        if (cachedTextBlockHash != null) {
            if (cachedTextBlockHash.first != textBlock.hashCode()) {
                if (textBlock.x != cachedTextBlockHash.second.first || textBlock.y != cachedTextBlockHash.second.second) {
                    textBlockCache[textBlock.id] = textBlock.hashCode() to (textBlock.x to textBlock.y)
                    var startX = textBlock.x
                    var startY = textBlock.y
                    textBlock.text.forEach { text ->
                        val data = compileGlyphs(text, startX, startY)
                        startX = data.endX
                        startY = data.endY
                        textCache[textBlock.id]?.let {
                            it[text.id] = text.hashCode()
                        }
                        textDataCache[text.id] = data
                    }
                } else {
                    var recalc = false
                    var startX = textBlock.x
                    var startY = textBlock.y
                    textBlock.text.forEach { text ->
                        if (!recalc) {
                            val cachedTextHash = textCache[textBlock.id]?.get(text.id) ?: -1
                            if (cachedTextHash != text.hashCode()) {
                                recalc = true
                            } else {
                                startX = textDataCache[text.id]?.endX ?: startX
                                startY = textDataCache[text.id]?.endY ?: startY
                            }
                        }
                        if (recalc) {
                            val data = compileGlyphs(text, startX, startY)
                            textCache[textBlock.id]?.let {
                                it[text.id] = text.hashCode()
                            }
                            textDataCache[text.id] = data
                        }
                    }
                }
            }
        } else {
            textBlockCache[textBlock.id] = textBlock.hashCode() to (textBlock.x to textBlock.y)
            textCache[textBlock.id] = mutableMapOf()
            var startX = textBlock.x
            var startY = textBlock.y
            textBlock.text.forEach { text ->
                val data = compileGlyphs(text, startX, startY)
                startX = data.endX
                startY = data.endY
                textCache[textBlock.id]?.let {
                    it[text.id] = text.hashCode()
                }
                textDataCache[text.id] = data
            }
        }
    }

    private fun uploadTextBlockVertices(blocks: Collection<TextBlock>) {
        blocks.forEach blocks@{ block ->
            block.text.forEach text@{ text ->
                val data = textDataCache[text.id] ?: return@text
                var j = 0
                for (i in offset until offset + data.vertices.size) {
                    vertices[i] = data.vertices[j++]
                }
                offset += j
            }
        }
    }

    private val tempVertices = FloatArrayList(1000)
    private fun compileGlyphs(text: Text, startX: Float, startY: Float): TextData {
        tempVertices.clear()
        var tx = startX
        var ty = startY
        val pathScale = 1f / font.unitsPerEm * text.pxScale

        text.text.forEach { char ->
            val code = char.code
            if (char == '\n') {
                ty += font.metrics.ascent * text.pxScale
                tx = startX
                return@forEach
            }
            val glyph = font.glyphs[code] ?: error("Unable to find glyph for '$char'!")
            if (char != ' ') {
                glyphRenderer.begin(glyph, text.color)
                glyph.path.commands.forEach { cmd ->
                    when (cmd.type) {
                        GlyphPath.CommandType.MOVE_TO -> glyphRenderer.moveTo(
                            cmd.x * pathScale + tx,
                            -cmd.y * pathScale + ty
                        )

                        GlyphPath.CommandType.LINE_TO -> glyphRenderer.lineTo(
                            cmd.x * pathScale + tx,
                            -cmd.y * pathScale + ty
                        )

                        GlyphPath.CommandType.CURVE_TO -> {
                            // possibly future use to convert cubic beziers to quadratic beziers
                        }

                        GlyphPath.CommandType.QUADRATIC_CURVE_TO -> {
                            glyphRenderer.curveTo(
                                cmd.x1 * pathScale + tx,
                                -cmd.y1 * pathScale + ty,
                                cmd.x * pathScale + tx,
                                -cmd.y * pathScale + ty
                            )
                        }

                        GlyphPath.CommandType.CLOSE -> glyphRenderer.close()

                    }
                }
                tempVertices += glyphRenderer.end()
            }
            tx += glyph.advanceWidth * pathScale
        }
        return TextData(tx, ty, tempVertices.toFloatArray())
    }


    data class TextBlock(
        var x: Float = 0f,
        var y: Float = 0f,
        val text: MutableList<Text> = mutableListOf(),
    ) {
        val id = genId

        companion object {
            private var genId = 1
                get() = field++
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as TextBlock

            return id == other.id
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + text.hashCode()
            result = 31 * result + id
            return result
        }

    }

    data class Text(
        var text: String = "",
        var pxScale: Int = 16,
        var color: Color = Color.BLACK,
    ) {
        val id = genId

        companion object {
            private var genId = 1
                get() = field++
        }
    }
}

private class TextData(val endX: Float, val endY: Float, val vertices: FloatArray)

internal class GlyphRenderer {

    private enum class TriangleType {
        SOLID,
        QUADRATIC_CURVE
    }

    private var firstX = 0f
    private var firstY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var contourCount = 0
    private var glyph: TtfGlyph? = null
    private var rectBuilder = RectBuilder()
    private var color: Color = Color.WHITE
    private var colorBits = color.toFloatBits()
    private val vertices = FloatArrayList(100)

    fun begin(glyph: TtfGlyph, color: Color) {
        if (this.color != color) {
            this.color = color
            colorBits = color.toFloatBits()
        }
        this.glyph = glyph
        rectBuilder.reset()
        vertices.clear()
    }

    fun moveTo(x: Float, y: Float) {
        firstX = x
        currentX = x
        firstY = y
        currentY = y
        contourCount = 0
    }

    fun lineTo(x: Float, y: Float) {
        if (++contourCount >= 2) {
            appendTriangle(firstX, firstY, currentX, currentY, x, y, TriangleType.SOLID)
        }
        currentX = x
        currentY = y
    }

    fun curveTo(cx: Float, cy: Float, x: Float, y: Float) {
        if (++contourCount >= 2) {
            appendTriangle(firstX, firstY, currentX, currentY, x, y, TriangleType.SOLID)
        }
        appendTriangle(currentX, currentY, cx, cy, x, y, TriangleType.QUADRATIC_CURVE)
        currentX = x
        currentY = y
    }

    fun close() {
        currentX = firstX
        currentY = firstY
        contourCount = 0
    }

    fun end(): FloatArray {
        return vertices.toFloatArray()
    }

    private fun appendTriangle(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        triangleType: TriangleType,
    ) {
        when (triangleType) {
            TriangleType.SOLID -> {
                appendVertex(ax, ay, 0f, 0f)
                appendVertex(bx, by, 0f, 0f)
                appendVertex(cx, cy, 0f, 0f)
            }

            TriangleType.QUADRATIC_CURVE -> {
                appendVertex(ax, ay, 0f, 0f)
                appendVertex(bx, by, 0.5f, 0f)
                appendVertex(cx, cy, 1f, 1f)
            }
        }
    }

    private fun appendVertex(x: Float, y: Float, u: Float, v: Float) {
        rectBuilder.include(x, y)
        vertices.run {
            add(x)
            add(y)
            add(colorBits)
            add(u)
            add(v)
        }
    }
}