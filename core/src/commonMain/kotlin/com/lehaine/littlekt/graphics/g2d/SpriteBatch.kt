package com.lehaine.littlekt.graphics.g2d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.gl.BlendFactor
import com.lehaine.littlekt.graphics.gl.DrawMode
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.DefaultFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.DefaultVertexShader
import com.lehaine.littlekt.graphics.textureMesh
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.normalized
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.math.isFuzzyZero
import kotlin.math.min

/**
 * Draws batched quads using indices.
 * @param context the context
 * @param size the max number of sprites in a single batch. Max of 8191.
 * @author Colton Daily
 * @date 11/7/2021
 */
class SpriteBatch(
    val context: Context,
    val size: Int = 1000,
) : Batch {
    companion object {
        private const val VERTEX_SIZE = 2 + 1 + 2
        private const val SPRITE_SIZE = 4 * VERTEX_SIZE
    }

    private val maxVertices = size * SPRITE_SIZE

    private val gl get() = context.graphics.gl
    val defaultShader = ShaderProgram(DefaultVertexShader(), DefaultFragmentShader()).also { it.prepare(context) }
    override var shader: ShaderProgram<*, *> = defaultShader
        set(value) {
            if (_drawing) {
                flush()
            }
            field = value
            if (_drawing) {
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

    override val drawing: Boolean get() = _drawing
    private var _drawing = false

    override var transformMatrix = Mat4()
        set(value) {
            if (_drawing) {
                flush()
            }
            field = value
            if (_drawing) {
                setupMatrices()
            }
        }
    override var projectionMatrix = Mat4().setToOrthographic(
        left = 0f,
        right = context.graphics.width.toFloat(),
        bottom = 0f,
        top = context.graphics.height.toFloat(),
        near = -1f,
        far = 1f
    )
        set(value) {
            if (_drawing) {
                flush()
            }
            field = value
            if (_drawing) {
                setupMatrices()
            }
        }
    private var combinedMatrix = Mat4()

    private val mesh = textureMesh(context.gl, maxVertices) {
        geometry.indicesAsQuad()
    }

    private var lastTexture: Texture? = null
    private var idx = 0
    private var invTexWidth = 0f
    private var invTexHeight = 0f

    override var color = Color.WHITE
        set(value) {
            if (field == value) return
            field = value
            colorBits = field.toFloatBits()
        }
    override var colorBits = color.toFloatBits()

    private var prevBlendSrcFunc = BlendFactor.SRC_ALPHA
    private var prevBlendDstFunc = BlendFactor.ONE_MINUS_SRC_ALPHA
    private var prevBlendSrcFuncAlpha = BlendFactor.SRC_ALPHA
    private var prevBlendDstFuncAlpha = BlendFactor.ONE_MINUS_SRC_ALPHA
    private var blendSrcFunc = prevBlendSrcFunc
    private var blendDstFunc = prevBlendDstFunc
    private var blendSrcFuncAlpha = prevBlendSrcFuncAlpha
    private var blendDstFuncAlpha = prevBlendDstFuncAlpha

    init {
        // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max
        check(size <= 8191) { "A batch must be 8191 sprites or fewer: $size" }
    }

    override fun begin(projectionMatrix: Mat4?) {
        if (_drawing) {
            throw IllegalStateException("SpriteBatch.end must be called before begin.")
        }
        renderCalls = 0

        gl.depthMask(false)

        projectionMatrix?.let {
            this.projectionMatrix = it
        }
        shader.bind()
        setupMatrices()

        _drawing = true
    }

    override fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        colorBits: Float,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        if (!_drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        if (slice.texture != lastTexture) {
            switchTexture(slice.texture)
        } else if (idx >= maxVertices) {
            flush()
        }

        var fx = -(originX - slice.offsetX)
        var fy = -(originY - slice.offsetY)
        val w = if (slice.rotated) height else width
        val h = if (slice.rotated) width else height
        var fx2 = w + fx
        var fy2 = h + fy

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

        if (rotation.normalized.radians.isFuzzyZero()) {
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

        x1 += x
        y1 += y
        x2 += x
        y2 += y
        x3 += x
        y3 += y
        x4 += x
        y4 += y

        val u = if (flipX) slice.u2 else slice.u
        val v = if (flipY) slice.v else slice.v2
        val u2 = if (flipX) slice.u else slice.u2
        val v2 = if (flipY) slice.v2 else slice.v

        mesh.geometry.run {
            addVertex { // bottom left
                position.x = x1
                position.y = y1
                colorPacked.value = colorBits
                texCoords.x = u
                texCoords.y = if (slice.rotated) v2 else v
            }
            addVertex { // top left
                position.x = x2
                position.y = y2
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u2 else u
                texCoords.y = v2
            }
            addVertex { // top right
                position.x = x3
                position.y = y3
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = if (slice.rotated) v else v2
            }
            addVertex { // bottom right
                position.x = x4
                position.y = y4
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u else u2
                texCoords.y = v
            }
        }

        idx += SPRITE_SIZE
    }

    override fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        colorBits: Float,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        if (!_drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        if (slice.texture != lastTexture) {
            switchTexture(slice.texture)
        } else if (idx >= maxVertices) {
            flush()
        }

        var fx = -(originX - slice.offsetX)
        var fy = -(originY - slice.offsetY)
        val w = if (slice.rotated) height else width
        val h = if (slice.rotated) width else height
        var fx2 = w + fx
        var fy2 = h + fy

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

        if (rotation.normalized.radians.isFuzzyZero()) {
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

        x1 += x
        y1 += y
        x2 += x
        y2 += y
        x3 += x
        y3 += y
        x4 += x
        y4 += y

        var u = srcX * invTexWidth
        var v = srcY * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = (srcY + srcHeight) * invTexHeight

        u = if (flipX) u2 else u
        v = if (flipY) v2 else v
        u2 = if (flipX) u else u2
        v2 = if (flipY) v else v2

        mesh.geometry.run {
            addVertex { // bottom left
                position.x = x1
                position.y = y1
                colorPacked.value = colorBits
                texCoords.x = u
                texCoords.y = if (slice.rotated) v2 else v
            }
            addVertex { // top left
                position.x = x2
                position.y = y2
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u2 else u
                texCoords.y = v2
            }
            addVertex { // top right
                position.x = x3
                position.y = y3
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = if (slice.rotated) v else v2
            }
            addVertex { // bottom right
                position.x = x4
                position.y = y4
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u else u2
                texCoords.y = v
            }
        }

        idx += SPRITE_SIZE
    }

    override fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        colorBits: Float,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        if (!_drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        if (texture != lastTexture) {
            switchTexture(texture)
        } else if (idx >= maxVertices) {
            flush()
        }

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

        if (rotation.normalized.radians.isFuzzyZero()) {
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

        x1 += x
        y1 += y
        x2 += x
        y2 += y
        x3 += x
        y3 += y
        x4 += x
        y4 += y

        var u = srcX * invTexWidth
        var v = srcY * invTexHeight
        var u2 = (srcX + srcWidth) * invTexWidth
        var v2 = (srcY + srcHeight) * invTexHeight

        if (flipX) {
            val tmp = u
            u = u2
            u2 = tmp
        }

        if (flipY) {
            val tmp = v
            v = v2
            v2 = tmp
        }


        mesh.geometry.run {
            addVertex { // bottom left
                position.x = x1
                position.y = y1
                colorPacked.value = colorBits
                texCoords.x = u
                texCoords.y = v
            }
            addVertex { // top left
                position.x = x2
                position.y = y2
                colorPacked.value = colorBits
                texCoords.x = u
                texCoords.y = v2
            }
            addVertex { // top right
                position.x = x3
                position.y = y3
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = v2
            }
            addVertex { // bottom right
                position.x = x4
                position.y = y4
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = v
            }
        }

        idx += SPRITE_SIZE
    }

    override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        if (!_drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        val verticesLength: Int = mesh.geometry.vertices.capacity
        var remainingVertices = verticesLength

        if (texture != lastTexture) {
            switchTexture(texture)
        } else {
            remainingVertices -= idx
            if (remainingVertices == 0) {
                flush()
                remainingVertices = verticesLength
            }
        }

        var copyCount = min(remainingVertices, count)
        mesh.geometry.add(spriteVertices, offset, idx, copyCount)
        idx += copyCount

        var remainingCount = count - copyCount
        var currOffset = offset
        while (remainingCount > 0) {
            currOffset += copyCount
            flush()
            copyCount = min(verticesLength, remainingCount)
            mesh.geometry.add(spriteVertices, currOffset, 0, copyCount)
            idx += copyCount
            remainingCount -= copyCount
        }
    }

    override fun end() {
        if (!_drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before end.")
        }
        if (idx > 0) {
            flush()
        }
        lastTexture = null
        _drawing = false
        gl.depthMask(true)
        gl.disable(State.BLEND)
    }

    override fun flush() {
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

    override fun setBlendFunction(src: BlendFactor, dst: BlendFactor) {
        setBlendFunctionSeparate(src, dst, src, dst)
    }

    override fun setBlendFunctionSeparate(
        srcFuncColor: BlendFactor,
        dstFuncColor: BlendFactor,
        srcFuncAlpha: BlendFactor,
        dstFuncAlpha: BlendFactor,
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

    override fun setToPreviousBlendFunction() {
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

    override fun useDefaultShader() {
        if (shader != defaultShader) {
            shader = defaultShader
        }
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