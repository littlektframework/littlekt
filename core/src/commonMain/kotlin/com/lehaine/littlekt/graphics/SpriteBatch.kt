package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.graphics.gl.BlendFactor
import com.lehaine.littlekt.graphics.gl.DrawMode
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.DefaultFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultVertexShader
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.radians
import com.lehaine.littlekt.math.geom.sine
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * @author Colton Daily
 * @date 11/7/2021
 */
class SpriteBatch(
    val context: Context,
    val size: Int = 1000,
) : Disposable {
    companion object {
        private const val VERTEX_SIZE = 2 + 1 + 2
        private const val SPRITE_SIZE = 4 * VERTEX_SIZE
    }

    private val gl get() = context.graphics.gl
    val defaultShader = ShaderProgram(DefaultVertexShader(), DefaultFragmentShader()).also { it.prepare(context) }
    var shader: ShaderProgram<*, *> = defaultShader
        set(value) {
            if (drawing) {
                flush()
            }
            field = value
            if (drawing) {
                field.bind()
                setupMatrices()
            }
        }
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
    private var combinedMatrix = Mat4()

    private val mesh = context.textureMesh {
        isStatic = false
        maxVertices = size * 4
    }.apply {
        indicesAsQuad()
    }

    private var lastTexture: Texture? = null
    private var idx = 0
    private var invTexWidth = 0f
    private var invTexHeight = 0f

    var color = Color.WHITE
        set(value) {
            if (field == value) return
            field = value
            colorBits = field.toFloatBits()
        }
    private var colorBits = color.toFloatBits()

    private var prevBlendSrcFunc = BlendFactor.SRC_ALPHA
    private var prevBlendDstFunc = BlendFactor.ONE_MINUS_SRC_ALPHA
    private var prevBlendSrcFuncAlpha = BlendFactor.SRC_ALPHA
    private var prevBlendDstFuncAlpha = BlendFactor.ONE_MINUS_SRC_ALPHA
    private var blendSrcFunc = prevBlendSrcFunc
    private var blendDstFunc = prevBlendDstFunc
    private var blendSrcFuncAlpha = prevBlendSrcFuncAlpha
    private var blendDstFuncAlpha = prevBlendDstFuncAlpha

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
        rotation: Angle = 0f.radians,
        flipX: Boolean = false,
        flipY: Boolean = false,
    ) {
        if (!drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        if (texture != lastTexture) {
            switchTexture(texture)
        } else if (idx == mesh.maxVertices) {
            flush()
        }

        val worldOriginX = x + originX
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

        var x1: Float = p1x
        var y1: Float = p1y
        var x2: Float = p2x
        var y2: Float = p2y
        var x3: Float = p3x
        var y3: Float = p3y
        var x4: Float = p4x
        var y4: Float = p4y

        if (rotation != 0f.radians) {
            val cos = rotation.cosine
            val sin = rotation.sine

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
        y1 += y
        x2 += worldOriginX
        y2 += y
        x3 += worldOriginX
        y3 += y
        x4 += worldOriginX
        y4 += y

        val u = if (flipX) 1f else 0f
        val v = if (flipY) 0f else 0f
        val u2 = if (flipX) 0f else 1f
        val v2 = if (flipY) 1f else 1f

        mesh.run {
            setVertex { // bottom left
                this.x = x1
                this.y = y1
                this.colorPacked = colorBits
                this.u = u
                this.v = v
            }

            setVertex { // top left
                this.x = x2
                this.y = y2
                this.colorPacked = colorBits
                this.u = u
                this.v = v2
            }

            setVertex { // top right
                this.x = x3
                this.y = y3
                this.colorPacked = colorBits
                this.u = u2
                this.v = v2
            }

            setVertex { // bottom right
                this.x = x4
                this.y = y4
                this.colorPacked = colorBits
                this.u = u2
                this.v = v
            }
        }

        idx += SPRITE_SIZE
    }

    fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float = 0f,
        originY: Float = 0f,
        width: Float = slice.width.toFloat(),
        height: Float = slice.height.toFloat(),
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        rotation: Angle = 0f.radians,
        flipX: Boolean = false,
        flipY: Boolean = false,
    ) {
        if (!drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        if (slice.texture != lastTexture) {
            switchTexture(slice.texture)
        } else if (idx == mesh.maxVertices) {
            flush()
        }

        val worldOriginX = x + originX
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

        if (rotation == 0f.radians) {
            x1 = p1x
            y1 = p1y

            x2 = p2x
            y2 = p2y

            x3 = p3x
            y3 = p3y

            x4 = p4x
            y4 = p4y
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine

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
        y1 += y
        x2 += worldOriginX
        y2 += y
        x3 += worldOriginX
        y3 += y
        x4 += worldOriginX
        y4 += y

        val u = if (flipX) slice.u2 else slice.u
        val v = if (flipY) slice.v else slice.v2
        val u2 = if (flipX) slice.u else slice.u2
        val v2 = if (flipY) slice.v2 else slice.v

        mesh.run {
            setVertex { // bottom left
                this.x = x1
                this.y = y1
                this.colorPacked = colorBits
                this.u = u
                this.v = v
            }
            setVertex { // top left
                this.x = x2
                this.y = y2
                this.colorPacked = colorBits
                this.u = u
                this.v = v2
            }
            setVertex { // top right
                this.x = x3
                this.y = y3
                this.colorPacked = colorBits
                this.u = u2
                this.v = v2
            }
            setVertex { // bottom right
                this.x = x4
                this.y = y4
                this.colorPacked = colorBits
                this.u = u2
                this.v = v
            }
        }

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
        val spritesInBatch = idx / SPRITE_SIZE
        if (spritesInBatch > maxSpritesInBatch) {
            maxSpritesInBatch = spritesInBatch
        }
        val count = spritesInBatch * 6
        lastTexture?.bind()
        gl.enable(State.BLEND)
        gl.blendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
        mesh.render(shader, DrawMode.TRIANGLES, 0, count)
        idx = 0
    }

    fun setBlendFunction(src: BlendFactor, dst: BlendFactor) {
        setBlendFunctionSeparate(src, dst, src, dst)
    }

    fun setBlendFunctionSeparate(
        srcFuncColor: BlendFactor,
        dstFuncColor: BlendFactor,
        srcFuncAlpha: BlendFactor,
        dstFuncAlpha: BlendFactor
    ) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor
            && blendSrcFuncAlpha == srcFuncAlpha && blendDstFuncAlpha == dstFuncAlpha
        ) {
            return
        }
        flush()
        prevBlendSrcFunc = blendSrcFunc
        prevBlendDstFunc = blendDstFunc
        prevBlendSrcFuncAlpha = blendSrcFuncAlpha
        prevBlendDstFuncAlpha = blendDstFuncAlpha
        blendSrcFunc = srcFuncColor
        blendDstFunc = dstFuncColor
        blendSrcFuncAlpha = srcFuncAlpha
        blendDstFuncAlpha = dstFuncAlpha
    }

    fun setToPreviousBlendFunction() {
        if (blendSrcFunc == prevBlendSrcFunc && blendDstFunc == prevBlendDstFunc
            && blendSrcFuncAlpha == prevBlendSrcFuncAlpha && blendDstFuncAlpha == prevBlendDstFuncAlpha
        ) {
            return
        }

        flush()
        blendSrcFunc = prevBlendSrcFunc
        blendDstFunc = prevBlendDstFunc
        blendSrcFuncAlpha = prevBlendSrcFuncAlpha
        blendDstFuncAlpha = prevBlendDstFuncAlpha
    }

    private fun switchTexture(texture: Texture) {
        flush()
        lastTexture = texture
        invTexWidth = 1f / texture.width
        invTexHeight = 1f / texture.height
    }

    private fun setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix)
        shader.uProjTrans?.apply(shader, combinedMatrix)
        shader.uTexture?.apply(shader)
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