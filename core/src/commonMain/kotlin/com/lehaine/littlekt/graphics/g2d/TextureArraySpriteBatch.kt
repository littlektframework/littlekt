package com.lehaine.littlekt.graphics.g2d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.*
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.TextureArrayFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.TextureArrayVertexShader
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.geom.normalized
import com.lehaine.littlekt.math.geom.sine
import com.lehaine.littlekt.math.isFuzzyZero

/**
 * **Requires GLES 3.0!**
 *
 * Creates a new [TextureArraySpriteBatch] that is an optimized version of the [SpriteBatch] that maintains
 * a texture-cache inside a [GL.TEXTURE_2D_ARRAY] to combine draw calls with different textures effectively.
 * @param context the context
 * @param size the max number of sprites in a single batch. Max of 8191.
 * @param maxTextureSlots the expected number of textures to be in use.
 * @param maxTextureWidth the width of the largest texture
 * @param maxTextureHeight the height of the largest texture
 * @param textureArrayMagFilter the mag filter to use for the texture array
 * @param textureArrayMinFilter the min filter to use for the texture array
 * @author Colton Daily
 * @date 2/8/2022
 */
class TextureArraySpriteBatch(
    val context: Context,
    val size: Int = 1000,
    private val maxTextureSlots: Int,
    private val maxTextureWidth: Int,
    private val maxTextureHeight: Int,
    private var textureArrayMagFilter: TexMagFilter = TexMagFilter.NEAREST,
    private var textureArrayMinFilter: TexMinFilter = TexMinFilter.NEAREST,
) : Batch {
    companion object {
        private const val VERTEX_SIZE = 2 + 1 + 2 + 1
        private const val SPRITE_SIZE = 4 * VERTEX_SIZE
    }

    private val gl get() = context.graphics.gl

    override var color = Color.WHITE
        set(value) {
            if (field == value) return
            field = value
            colorBits = field.toFloatBits()
        }
    override var colorBits = color.toFloatBits()

    val defaultShader =
        ShaderProgram(TextureArrayVertexShader(), TextureArrayFragmentShader()).also { it.prepare(context) }
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

    private var currentTextureLFUSize = 0
    private var currentTextureLFUSwaps = 0

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

    private val textureIndexAttribute = VertexAttribute(
        usage = VertexAttrUsage.GENERIC,
        numComponents = 1,
        alias = "a_textureIndex"
    )

    private val mesh = mesh(
        context.gl,
        listOf(
            VertexAttribute.POSITION, VertexAttribute.COLOR_PACKED, VertexAttribute.TEX_COORDS(0),
            textureIndexAttribute
        )
    ) {
        geometry.indicesAsQuad()
    }


    override val drawing: Boolean get() = _drawing
    private var _drawing = false

    private var idx = 0

    private var invTexWidth = 0f
    private var invTexHeight = 0f
    private val invMaxTexWidth = 1f / maxTextureWidth
    private val invMaxTexHeight = 1f / maxTextureHeight
    private var subImageScaleWidth = 0f
    private var subImageScaleHeight = 0f

    private val usedTextures = arrayOfNulls<Texture>(maxTextureSlots)

    private var usedTexturesNextSwapSlot = 0

    private val textureArrayHandle: GlTexture = initializeArrayTexture()
    private val useMipMaps =
        (textureArrayMagFilter.glFlag >= GL.NEAREST_MIPMAP_NEAREST && textureArrayMagFilter.glFlag <= GL.LINEAR_MIPMAP_LINEAR)
                || (textureArrayMinFilter.glFlag >= GL.NEAREST_MIPMAP_NEAREST || textureArrayMinFilter.glFlag <= GL.LINEAR_MIPMAP_LINEAR)

    private var mipMapsDirty: Boolean = false

    private var prevBlendSrcFunc = BlendFactor.SRC_ALPHA
    private var prevBlendDstFunc = BlendFactor.ONE_MINUS_SRC_ALPHA
    private var prevBlendSrcFuncAlpha = BlendFactor.SRC_ALPHA
    private var prevBlendDstFuncAlpha = BlendFactor.ONE_MINUS_SRC_ALPHA
    private var blendSrcFunc = prevBlendSrcFunc
    private var blendDstFunc = prevBlendDstFunc
    private var blendSrcFuncAlpha = prevBlendSrcFuncAlpha
    private var blendDstFuncAlpha = prevBlendDstFuncAlpha

    private val copyFrameBuffer = FrameBuffer(maxTextureWidth, maxTextureHeight).also { it.prepare(context) }

    private fun initializeArrayTexture(): GlTexture {
        val texture = gl.createTexture()
        gl.bindTexture(TextureTarget._2D_ARRAY, texture)
        gl.texImage3D(
            TextureTarget._2D_ARRAY,
            0,
            TextureFormat.RGBA,
            TextureFormat.RGBA,
            maxTextureWidth,
            maxTextureHeight,
            maxTextureSlots,
            DataType.UNSIGNED_BYTE
        )
        gl.texParameteri(TextureTarget._2D_ARRAY, TexParameter.MAG_FILTER, textureArrayMagFilter.glFlag)
        gl.texParameteri(TextureTarget._2D_ARRAY, TexParameter.MIN_FILTER, textureArrayMinFilter.glFlag)

        gl.texParameteri(TextureTarget._2D_ARRAY, TexParameter.WRAP_S, TexWrap.REPEAT.glFlag)
        gl.texParameteri(TextureTarget._2D_ARRAY, TexParameter.WRAP_T, TexWrap.REPEAT.glFlag)

        gl.bindDefaultTexture(TextureTarget._2D_ARRAY)

        mipMapsDirty = useMipMaps
        return texture
    }

    override fun begin(projectionMatrix: Mat4?) {
        if (_drawing) {
            throw IllegalStateException("SpriteBatch.end must be called before begin.")
        }
        renderCalls = 0
        currentTextureLFUSwaps = 0

        gl.depthMask(false)
        gl.activeTexture(GL.TEXTURE0)
        gl.bindTexture(TextureTarget._2D_ARRAY, textureArrayHandle)

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

        flushIfFull()

        val ti = activateTexture(slice.texture).toFloat()

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

        val u = (if (flipX) slice.u2 else slice.u) * subImageScaleWidth
        val v = (if (flipY) slice.v else slice.v2) * subImageScaleHeight
        val u2 = (if (flipX) slice.u else slice.u2) * subImageScaleWidth
        val v2 = (if (flipY) slice.v2 else slice.v) * subImageScaleHeight

        mesh.geometry.run {
            addVertex { // bottom left
                this.x = x1
                position.y = y1
                colorPacked.value = colorBits
                texCoords.x = u
                texCoords.y = if (slice.rotated) v2 else v
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // top left
                this.x = x2
                position.y = y2
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u2 else u
                texCoords.y = v2
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // top right
                this.x = x3
                position.y = y3
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = if (slice.rotated) v else v2
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // bottom right
                this.x = x4
                position.y = y4
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u else u2
                texCoords.y = v
                getFloatAttribute(textureIndexAttribute)?.value = ti
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

        flushIfFull()

        val ti = activateTexture(slice.texture).toFloat()

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

        var u = srcX * invTexWidth * subImageScaleWidth
        var v = srcY * invTexHeight * subImageScaleHeight
        var u2 = (srcX + srcWidth) * invTexWidth * subImageScaleWidth
        var v2 = (srcY + srcHeight) * invTexHeight * subImageScaleHeight

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
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // top left
                position.x = x2
                position.y = y2
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u2 else u
                texCoords.y = v2
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // top right
                position.x = x3
                position.y = y3
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = if (slice.rotated) v else v2
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // bottom right
                position.x = x4
                position.y = y4
                colorPacked.value = colorBits
                texCoords.x = if (slice.rotated) u else u2
                texCoords.y = v
                getFloatAttribute(textureIndexAttribute)?.value = ti
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

        flushIfFull()

        val ti = activateTexture(texture).toFloat()

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
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // top left
                position.x = x2
                position.y = y2
                colorPacked.value = colorBits
                texCoords.x = u
                texCoords.y = v2
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // top right
                position.x = x3
                position.y = y3
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = v2
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
            addVertex { // bottom right
                position.x = x4
                position.y = y4
                colorPacked.value = colorBits
                texCoords.x = u2
                texCoords.y = v
                getFloatAttribute(textureIndexAttribute)?.value = ti
            }
        }

        idx += SPRITE_SIZE
    }

    override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        if (!_drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before draw.")
        }
        flushIfFull()

        val ti = activateTexture(texture).toFloat()

        for (srcPos in 0 until count step VERTEX_SIZE - 1) {
            mesh.geometry.add(spriteVertices, srcPos, idx, VERTEX_SIZE - 1)
            idx += VERTEX_SIZE - 3
            mesh.geometry.vertices[idx++] *= subImageScaleWidth
            mesh.geometry.vertices[idx++] *= subImageScaleHeight
            mesh.geometry.vertices += ti
            idx++
        }
    }

    override fun end() {
        if (!_drawing) {
            throw IllegalStateException("SpriteBatch.begin must be called before end.")
        }
        if (idx > 0) {
            flush()
        }
        _drawing = false
        gl.depthMask(true)
        gl.disable(State.BLEND)
    }

    private fun flushIfFull() {
        if (mesh.geometry.vertices.capacity - idx < SPRITE_SIZE / VERTEX_SIZE) {
            flush()
        }
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

        if (useMipMaps && mipMapsDirty) {
            gl.generateMipmap(TextureTarget._2D_ARRAY)
            mipMapsDirty = false
        }

        gl.enable(State.BLEND)
        gl.blendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha)
        mesh.render(shader, DrawMode.TRIANGLES, 0, count)
        idx = 0
    }

    /**
     * Assigns space on the texture array, sets up texture scaling, and manages the LFU cache.
     * @param texture the texture to get from or load into the cache
     * @return the texture slot allocated for the specified texture
     */
    private fun activateTexture(texture: Texture): Int {
        subImageScaleWidth = texture.width * invMaxTexWidth
        subImageScaleHeight = texture.height * invMaxTexHeight

        if (subImageScaleWidth > 1f || subImageScaleHeight > 1f) {
            throw IllegalStateException("Texture ${texture.glTexture} is larger than the Array Texture: [${texture.width},${texture.height}] > [$maxTextureWidth,$maxTextureHeight]")
        }

        invTexWidth = subImageScaleWidth / texture.width
        invTexHeight = subImageScaleHeight / texture.height

        val textureSlot = findTextureCacheIndex(texture)
        if (textureSlot >= 0) {
            // We don't want to throw out a texture we just used
            if (textureSlot == usedTexturesNextSwapSlot) {
                usedTexturesNextSwapSlot = (usedTexturesNextSwapSlot + 1) % currentTextureLFUSize
            }
            return textureSlot
        }
        // If a free texture unit is available we just use it
        // If not we have to flush and then throw out the least accessed one.
        if (currentTextureLFUSize < maxTextureSlots) {
            usedTextures[currentTextureLFUSize] = texture
            copyTextureIntoArrayTexture(texture, currentTextureLFUSize)
            currentTextureLFUSwaps++
            return currentTextureLFUSize++
        } else {
            // We have to flush if there is something in the pipeline using this texture already,
            // otherwise the texture index of previously rendered sprites gets invalidated
            if (idx > 0) {
                flush()
            }
            val slot = usedTexturesNextSwapSlot
            usedTexturesNextSwapSlot = (usedTexturesNextSwapSlot + 1) % currentTextureLFUSize
            usedTextures[slot] = texture
            copyTextureIntoArrayTexture(texture, slot)

            currentTextureLFUSwaps++
            return slot
        }
    }

    private fun findTextureCacheIndex(texture: Texture): Int {
        val handle = texture.glTexture
        for (i in 0 until currentTextureLFUSize) {
            if (handle == usedTextures[i]?.glTexture) return i
        }
        return -1
    }

    /**
     * Copies a texture into the texture array.
     * @param texture the texture to copy
     * @param slot the slot of the texture array to copy the texture to
     */
    private fun copyTextureIntoArrayTexture(texture: Texture, slot: Int) {
        copyFrameBuffer.use {
            gl.frameBufferTexture2D(
                GL.READ_FRAMEBUFFER,
                FrameBufferRenderBufferAttachment.COLOR_ATTACHMENT(0),
                texture.glTexture ?: error("Texture doesn't have a 'glTexture' handle!"),
                0
            )
            gl.readBuffer(FrameBufferRenderBufferAttachment.COLOR_ATTACHMENT(0).glFlag)
            gl.copyTexSubImage3D(
                TextureTarget._2D_ARRAY,
                0,
                0,
                0,
                slot,
                0,
                0,
                copyFrameBuffer.width,
                copyFrameBuffer.height
            )
        }

        if (useMipMaps) {
            mipMapsDirty = true
        }
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

    private fun setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix)
        shader.uProjTrans?.apply(shader, combinedMatrix)
    }

    override fun dispose() {
        gl.bindDefaultTexture(TextureTarget._2D_ARRAY)
        gl.deleteTexture(textureArrayHandle)
        copyFrameBuffer.dispose()
        mesh.dispose()
        shader.dispose()
    }
}