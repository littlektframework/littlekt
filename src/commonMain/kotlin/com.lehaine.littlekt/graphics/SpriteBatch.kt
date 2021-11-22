package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.gl.BlendFactor
import com.lehaine.littlekt.graphics.gl.DrawMode
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.fragment.TexturedFragmentShader
import com.lehaine.littlekt.graphics.shader.vertex.TexturedQuadShader
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.ortho
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class SpriteBatch(
    val application: Application,
    val size: Int = 1000,
    val shader: ShaderProgram = ShaderProgram(application.graphics.gl, TexturedQuadShader(), TexturedFragmentShader())
) : Disposable {
    companion object {
        private const val VERTEX_SIZE = 2 + 1 + 2
        private const val SPRITE_SIZE = 4 * VERTEX_SIZE
    }

    private val gl get() = application.graphics.gl

    var renderCalls = 0
        private set
    var totalRenderCalls = 0
        private set
    var maxSpritesInBatch = 0
        private set

    private var drawing = false
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
    var projectionMatrix = ortho(
        l = 0f,
        r = application.graphics.width.toFloat(),
        b = 0f,
        t = application.graphics.height.toFloat(),
        n = -1f,
        f = 1f
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
    private var combinedMatrix = Mat4()

    private val mesh = Mesh(
        gl,
        false,
        size * 4,
        size * 6,
        VertexAttribute.POSITION,
        VertexAttribute.COLOR_PACKED,
        VertexAttribute.TEX_COORDS(0)
    )

    private val vertices = FloatArray(size * SPRITE_SIZE)

    private var lastTexture: Texture? = null
    private var idx = 0
    private var invTexWidth = 0f
    private var invTexHeight = 0f

    private val color = Color.WHITE
    private val colorPacked = color.toFloatBits()

    init {
        val len = size * 6
        val indices = ShortArray(len)
        var i = 0
        var j = 0
        while (i < len) {
            indices[i] = j.toShort()
            indices[i + 1] = (j + 1).toShort()
            indices[i + 2] = (j + 2).toShort()
            indices[i + 3] = (j + 2).toShort()
            indices[i + 4] = (j + 3).toShort()
            indices[i + 5] = j.toShort()
            i += 6
            j += 4
        }
        mesh.setIndices(indices)
    }

    fun begin(projectionMatrix: Mat4? = null) {
        if (drawing) {
            throw IllegalStateException("SpriteBatch.end must be called before begin.")
        }
        renderCalls = 0

        gl.depthMask(false)

        projectionMatrix?.let {
            this.projectionMatrix = it
        }
        shader.bind()
        setupMatrices()

        drawing = true
    }

    fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        width: Float = texture.width.toFloat(),
        height: Float = texture.height.toFloat(),
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Float = 0f
    ) {
        if (!drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        if (texture != lastTexture) {
            switchTexture(texture)
        } else if (idx == vertices.size) {
            flush()
        }

        val worldOriginX = x + originX
        val worldOriginY = y + originY
        var fx = -originX
        var fy = -originY
        var fx2 = width - originX
        var fy2 = height - originY

        if (scaleX != 1f || scaleY != 1f) {
            fx *= scaleX
            fy *= scaleY
            fx2 *= scaleX
            fy2 *= scaleY
        }

        val p1x = fx
        val p1y = fy
        val p2x = fx
        val p2y = fy2
        val p3x = fx2
        val p3y = fy2
        val p4x = fx2
        val p4y = fy

        var x1: Float
        var y1: Float
        var x2: Float
        var y2: Float
        var x3: Float
        var y3: Float
        var x4: Float
        var y4: Float

        if (rotation == 0f) {
            x1 = p1x
            y1 = p1y

            x2 = p2x
            y2 = p2y

            x3 = p3x
            y3 = p3y

            x4 = p4x
            y4 = p4y
        } else {
            val cos = cos(rotation)
            val sin = sin(rotation)

            x1 = cos * p1x - sin * p1y
            y1 = sin * p1x + cos * p1y

            x2 = cos * p2x - sin * p2y
            y2 = sin * p2x + cos * p2y

            x3 = cos * p3x - sin * p3y
            y3 = sin * p3x + cos * p3y

            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        }

        x1 += worldOriginX
        y1 += worldOriginY
        x2 += worldOriginX
        y2 += worldOriginY
        x3 += worldOriginX
        y3 += worldOriginY
        x4 += worldOriginX
        y4 += worldOriginY

        val u = 0f
        val v = 1f
        val u2 = 1f
        val v2 = 0f

        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = colorPacked
        vertices[idx + 3] = u
        vertices[idx + 4] = v

        vertices[idx + 5] = x2
        vertices[idx + 6] = y2
        vertices[idx + 7] = colorPacked
        vertices[idx + 8] = u
        vertices[idx + 9] = v2

        vertices[idx + 10] = x3
        vertices[idx + 11] = y3
        vertices[idx + 12] = colorPacked
        vertices[idx + 13] = u2
        vertices[idx + 14] = v2

        vertices[idx + 15] = x4
        vertices[idx + 16] = y4
        vertices[idx + 17] = colorPacked
        vertices[idx + 18] = u2
        vertices[idx + 19] = v
        idx += SPRITE_SIZE
    }

    fun end() {
        if (!drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before end.")
        }
        if (idx > 0) {
            flush()
        }
        lastTexture = null
        drawing = false
        gl.depthMask(true)
        gl.disable(State.BLEND)
    }

    fun flush() {
        if (idx == 0) {
            return
        }
        renderCalls++
        totalRenderCalls++
        val spritesInBatch = idx / 20
        if (spritesInBatch > maxSpritesInBatch) {
            maxSpritesInBatch = spritesInBatch
        }
        val count = spritesInBatch * 6
        lastTexture?.bind()
        mesh.setVertices(vertices, 0, idx)
        mesh.indicesBuffer.apply {
            position = 0
            limit = count
        }
        gl.enable(State.BLEND)
        gl.blendFuncSeparate(
            BlendFactor.SRC_ALPHA,
            BlendFactor.ONE_MINUS_SRC_ALPHA,
            BlendFactor.SRC_ALPHA,
            BlendFactor.ONE_MINUS_SRC_ALPHA
        )
        mesh.render(shader, DrawMode.TRIANGLES, 0, count)
        idx = 0
    }

    private fun switchTexture(texture: Texture) {
        flush()
        lastTexture = texture
        invTexWidth = 1f / texture.width
        invTexHeight = 1f / texture.height
    }

    private fun setupMatrices() {
        combinedMatrix = projectionMatrix * transformMatrix
        shader.vertexShader.uProjTrans.apply(shader, combinedMatrix)
        shader.fragmentShader.uTexture.apply(shader)
    }

    override fun dispose() {
        mesh.dispose()
        shader.dispose()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun SpriteBatch.use(projectionMatrix: Mat4? = null, action: (SpriteBatch) -> Unit) {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    begin(projectionMatrix)
    action(this)
    end()
}