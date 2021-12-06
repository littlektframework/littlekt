package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.*
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.RectBuilder
import com.lehaine.littlekt.math.Vec2f

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
class GPUFont(val font: TtfFont) : Preparable {
    private lateinit var glyphRenderer: GlyphRenderer

    private lateinit var glyphShader: ShaderProgram<GlyphVertexShader, GlyphFragmentShader>
    private lateinit var glyphOffscreenShader: ShaderProgram<GlyphVertexShader, GlyphOffscreenFragmentShader>

    private lateinit var textShader: ShaderProgram<TextVertexShader, TextFragmentShader>
    private lateinit var glyphMesh: Mesh
    private lateinit var gl: GL

    private var isPrepared = false
    private val fbo = FrameBuffer(960, 540)
    private val temp = Mat4()
    private val textBuilder = TextBuilder()

    override val prepared: Boolean
        get() = isPrepared

    override fun prepare(application: Application) {
        gl = application.gl
        glyphShader = ShaderProgram(application.gl, GlyphVertexShader(), GlyphFragmentShader())
        glyphOffscreenShader = ShaderProgram(application.gl, GlyphVertexShader(), GlyphOffscreenFragmentShader())
        textShader = ShaderProgram(application.gl, TextVertexShader(), TextFragmentShader())

        glyphMesh = textureMesh(application.gl) {
            maxVertices = 5000
        }
        glyphRenderer = GlyphRenderer(glyphMesh)
        fbo.prepare(application)
        isPrepared = true
    }


    /**
     * @param text the string of text to render
     * @param x the x coord position to render the text at
     * @param y the y coord position to tender the text at
     * @param color the color to render the text as. The color is only taken into account if [flush] is used.
     * This is due to able to render directly to the vertices vs having to use blending to get the desired results.
     */
    fun text(text: String, x: Float, y: Float, color: Color = Color.BLACK) {
        check(isPrepared) { "GPUFont has not been prepared yet! Please call prepare() before using!" }
        renderText(text, x, y, color)
    }

    fun buildText(x: Float, y: Float, build: TextBuilder.() -> Unit) {
        check(isPrepared) { "GPUFont has not been prepared yet! Please call prepare() before using!" }
        textBuilder.build()
        renderText(textBuilder.text, x, y, textBuilder.colors)
    }


    /**
     * Renders the text to a stencil buffer in order to flip the pixels the correct way and then renders to the
     * color buffer. This does not use any antialiasing. The color of the text passed in [text] will be rendered.
     * @param viewProjection the combined view projection matrix to render the text
     */
    fun flush(viewProjection: Mat4) {
        // if not using FBO - then lets use the stencil buffer
        gl.enable(State.STENCIL_TEST)
        gl.colorMask(red = false, green = false, blue = false, alpha = false)
        gl.clear(ClearBufferMask.STENCIL_BUFFER_BIT)
        gl.stencilFunc(CompareFunction.ALWAYS, 1, 1)
        gl.stencilOp(StencilAction.KEEP, StencilAction.KEEP, StencilAction.INVERT)

        glyphShader.bind()
        glyphShader.uProjTrans?.apply(glyphShader, viewProjection)
        glyphMesh.render(glyphShader)

        gl.colorMask(red = true, green = true, blue = true, alpha = true)
        gl.stencilFunc(CompareFunction.EQUAL, 1, 1)
        gl.stencilOp(StencilAction.KEEP, StencilAction.KEEP, StencilAction.INVERT)

        glyphMesh.render(glyphShader)

        gl.disable(State.STENCIL_TEST)
        textBuilder.clear()
    }

    /**
     * Renders the text offscreen in order to determine antialiasing and then renders the FBO texture to the [batch].
     * @param batch the batch to render the results to
     * @param viewProjection the combined view projection matrix to render the text
     * @param useJitter whether or not to use the jitter pattern to display text. If set to `true` this will allow for
     * cleaner and crisper text but at the cost of **6** higher draw calls. Otherwise, the text will be rendered offscreen
     * once.
     * @param color the color to render the text as. If any color BUT [Color.BLACK] is used, it will result in one extra
     * draw call due.
     */
    fun flush(batch: SpriteBatch, viewProjection: Mat4, color: Color = Color.BLACK, useJitter: Boolean = true) {
        fbo.begin()
        gl.clearColor(Color.CLEAR)
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        gl.enable(State.BLEND)
        gl.blendEquation(BlendEquationMode.FUNC_ADD)
        gl.blendFunc(BlendFactor.ONE, BlendFactor.ONE)

        glyphOffscreenShader.bind()
        if (useJitter) {
            JITTER_PATTERN.forEachIndexed { idx, pattern ->
                temp.set(viewProjection)
                temp.translate(pattern.x, pattern.y, 0f)
                if (idx % 2 == 0) {
                    glyphOffscreenShader.fragmentShader.uColor.apply(
                        glyphOffscreenShader,
                        if (idx == 0) 1f else 0f,
                        if (idx == 2) 1f else 0f,
                        if (idx == 4) 1f else 0f,
                        0f
                    )
                }
                glyphOffscreenShader.vertexShader.uProjTrans.apply(glyphOffscreenShader, temp)
                glyphMesh.render(glyphOffscreenShader)
            }
        } else {
            temp.set(viewProjection)
            glyphOffscreenShader.fragmentShader.uColor.apply(
                glyphOffscreenShader,
                1f,
                1f,
                1f,
                0f
            )
            glyphOffscreenShader.vertexShader.uProjTrans.apply(glyphOffscreenShader, temp)
            glyphMesh.render(glyphOffscreenShader)
        }

        fbo.end()
        batch.shader = textShader
        batch.setBlendFunction(BlendFactor.ZERO, BlendFactor.SRC_COLOR)
        batch.use(viewProjection) {
            textShader.fragmentShader.uColor.apply(textShader, Color.CLEAR)
            it.draw(fbo.colorBufferTexture, 0f, 0f, flipY = true)
            it.setToPreviousBlendFunction()

            if (color != Color.BLACK) {
                it.setBlendFunction(BlendFactor.ONE, BlendFactor.ONE)
                textShader.fragmentShader.uColor.apply(textShader, color)
                it.draw(fbo.colorBufferTexture, 0f, 0f, flipY = true)
            }
        }
        batch.shader = batch.defaultShader
        batch.setToPreviousBlendFunction()
        textBuilder.clear()
    }

    private fun renderText(text: String, x: Float, y: Float, color: Color) {
        var tx = x
        val scale = 1f /// unitsPerEm * 72f
        text.forEach { char ->
            val code = char.code
            val glyph = font.glyphs[code] ?: error("Unable to find glyph for '$char'!")
            if (char != ' ') {
                glyphRenderer.begin(glyph, color)
                glyph.path.commands.forEach { cmd ->
                    when (cmd.type) {
                        GlyphPath.CommandType.MOVE_TO -> glyphRenderer.moveTo(cmd.x * scale + tx, -cmd.y * scale + y)
                        GlyphPath.CommandType.LINE_TO -> glyphRenderer.lineTo(cmd.x * scale + tx, -cmd.y * scale + y)
                        GlyphPath.CommandType.QUADRATIC_CURVE_TO -> glyphRenderer.curveTo(
                            cmd.x1 * scale + tx,
                            -cmd.y1 * scale + y,
                            cmd.x * scale + tx,
                            -cmd.y * scale + y
                        )
                        GlyphPath.CommandType.CLOSE -> glyphRenderer.close()
                        else -> {
                            // do nothing with bezier curves - only want the quadratic curves
                        }
                    }
                }
                glyphRenderer.end()
            }
            tx += glyph.advanceWidth * 0.075f * scale
        }
    }

    private fun renderText(texts: List<String>, x: Float, y: Float, colors: List<Color>) {
        var tx = x
        val scale = 1f /// unitsPerEm * 72f
        texts.forEachIndexed { index, text ->
            text.forEach { char ->
                val code = char.code
                val glyph = font.glyphs[code] ?: error("Unable to find glyph for '$char'!")
                if (char != ' ') {
                    glyphRenderer.begin(glyph, colors[index])
                    glyph.path.commands.forEach { cmd ->
                        when (cmd.type) {
                            GlyphPath.CommandType.MOVE_TO -> glyphRenderer.moveTo(
                                cmd.x * scale + tx,
                                -cmd.y * scale + y
                            )
                            GlyphPath.CommandType.LINE_TO -> glyphRenderer.lineTo(
                                cmd.x * scale + tx,
                                -cmd.y * scale + y
                            )
                            GlyphPath.CommandType.QUADRATIC_CURVE_TO -> glyphRenderer.curveTo(
                                cmd.x1 * scale + tx,
                                -cmd.y1 * scale + y,
                                cmd.x * scale + tx,
                                -cmd.y * scale + y
                            )
                            GlyphPath.CommandType.CLOSE -> glyphRenderer.close()
                            else -> {
                                // do nothing with bezier curves - only want the quadratic curves
                            }
                        }
                    }
                    glyphRenderer.end()
                }
                tx += glyph.advanceWidth * 0.075f * scale
            }
        }
    }

    companion object {
        private val logger = Logger<GPUFont>()
        private val JITTER_PATTERN = listOf(
            Vec2f(-1f / 12f, -5f / 12f),
            Vec2f(1f / 12f, 1f / 12f),
            Vec2f(3f / 12f, -1f / 12f),
            Vec2f(5f / 12f, 5f / 12f),
            Vec2f(7f / 12f, -3f / 12f),
            Vec2f(9f / 12f, 3f / 12f)
        )
    }
}

class TextBuilder() {
    internal val text: MutableList<String> = mutableListOf()
    internal val colors: MutableList<Color> = mutableListOf()

    fun append(color: Color = Color.BLACK, action: () -> String) {
        text += action()
        colors += color
    }

    internal fun clear() {
        text.clear()
        colors.clear()
    }
}

internal class GlyphRenderer(val mesh: Mesh) {

    private enum class TriangleType {
        SOLID,
        QUADRATIC_CURVE
    }

    private val vertices = mutableListOf<Float>()
    private var firstX = 0f
    private var firstY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var contourCount = 0
    private var glyph: Glyph? = null
    private var rectBuilder = RectBuilder()
    private var color: Color = Color.WHITE
    private var colorBits = color.toFloatBits()

    fun begin(glyph: Glyph, color: Color) {
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

    fun end() {
        // TODO - determine if we really need to keep track of the glyph bounds. If so build it here.
    }

    private fun appendTriangle(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        triangleType: TriangleType
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
        mesh.setVertex {
            this.x = x
            this.y = y
            this.u = u
            this.v = v
            this.colorPacked = colorBits
        }
    }
}