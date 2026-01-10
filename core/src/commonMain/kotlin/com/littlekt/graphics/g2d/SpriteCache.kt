package com.littlekt.graphics.g2d

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.*
import com.littlekt.graphics.g2d.util.CameraSpriteBuffers
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.util.CameraBuffersViaMatrix
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.math.Mat4
import com.littlekt.math.MutableVec2f
import com.littlekt.math.MutableVec4f
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.radians
import com.littlekt.util.UniqueId
import com.littlekt.util.datastructure.fastForEach

/**
 * Stores Sprite data in a cache that must be updated and managed from an outside source. Sprites
 * are not cleared per frame and are only removed when explicitly invoked to. Each sprite is
 * instanced by the id of the texture slice. The cache is separated into two buffers: a static and a
 * dynamic. In its current state, the uvs are considered dynamic to handle animations. Everything
 * else is considered static. The idea is to prevent many static updates, if possible. Updating
 * static may cause worse performance then using a standard [SpriteBatch].
 *
 * @author Colton Daily
 * @date 4/23/2024
 */
class SpriteCache(
    val device: Device,
    val format: TextureFormat,
    size: Int = 1000,
    cameraBuffers: CameraBuffersViaMatrix? = null,
) : Releasable {
    private val ownCameraBuffers = cameraBuffers == null
    private val cameraBuffers: CameraBuffersViaMatrix = cameraBuffers ?: CameraSpriteBuffers(device)

    /** Holds the sprites: `position (2) + scale (2) + size (2) + rotation (1) + color (4)`. */
    private var staticData = FloatBuffer(size * STATIC_COMPONENTS_PER_SPRITE)
    private var dynamicData = FloatBuffer(size * DYNAMIC_COMPONENTS_PER_SPRITE)

    private val mesh =
        indexedMesh(
            device,
            listOf(
                VertexAttribute(
                    format = VertexFormat.FLOAT32x3,
                    offset = 0,
                    shaderLocation = 0,
                    usage = VertexAttrUsage.POSITION,
                ),
                VertexAttribute(
                    format = VertexFormat.FLOAT32x2,
                    offset = VertexFormat.FLOAT32x3.bytes.toLong(),
                    shaderLocation = 1,
                    usage = VertexAttrUsage.UV,
                ),
            ),
            40,
        ) {
            indicesAsQuad()
            // normalize coordinates
            val minX = -0.5f
            val minY = -0.5f
            val maxX = 0.5f
            val maxY = 0.5f
            addVertex { // top left
                position.x = minX
                position.y = maxY
            }
            addVertex { // top right
                position.x = maxX
                position.y = maxY
            }
            addVertex { // bottom right
                position.x = maxX
                position.y = minY
            }
            addVertex { // bottom left
                position.x = minX
                position.y = minY
            }
        }
    private val shader: SpriteCacheShader =
        SpriteCacheShader(device, staticData.capacity, dynamicData.capacity)

    private val bindGroupByTextureId: MutableMap<Int, BindGroup> = mutableMapOf()
    private val drawCalls: MutableList<DrawCall> = mutableListOf()

    private var blendState = BlendState.NonPreMultiplied
    private val renderPipeline =
        device.createRenderPipeline(createRenderPipelineDescriptor(shader, blendState))

    /** The textures used by sprites in insert order. */
    private val textures = mutableSetOf<Texture>()
    private val spriteBuffer =
        device.createGPUFloatBuffer(
            "sprite buffer",
            staticData,
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )
    private val spriteIndices = mutableMapOf<SpriteId, Int>()
    private val spriteView = SpriteView()
    private var spriteCount = 0
    private var staticDirty = false
    private var dynamicDirty = false
    private val dirty: Boolean
        get() = staticDirty || dynamicDirty

    private val lastDynamicMeshOffsets: List<Long> = listOf(0L)

    /**
     * Create a new sprite and add it to the cache. The order the sprites are added are the order
     * they are drawn in, just like a [SpriteBatch].
     *
     * This dirties the static and dynamic buffers.
     *
     * @param slice the texture slice to render the sprite
     * @param action the lambda to set the sprite data using a [SpriteView].
     * @return a [SpriteId], which is just an integer, that can be used in [updateSprite] and
     *   [remove], if needed.
     */
    fun add(slice: TextureSlice, action: SpriteView.() -> Unit): SpriteId {
        var textureIdx = textures.indexOf(slice.texture)
        if (textureIdx == -1) {
            textures += slice.texture
            textureIdx = textures.size - 1
        }
        spriteView.resetToZero()
        spriteView.size.set(slice.width.toFloat(), slice.height.toFloat())
        spriteView.uvs.set(slice.u, slice.v, slice.u1, slice.v1)
        spriteView.action()

        val id = UniqueId.next<SpriteCache>()
        insertId(id, spriteCount)

        copySpriteViewToData(
            index = spriteCount,
            textureIdx = textureIdx,
            rotated = slice.rotated,
            view = spriteView,
        )
        spriteCount++

        staticDirty = true
        dynamicDirty = true
        return id
    }

    /**
     * Remove a sprite from the cache. This dirties the static & dynamic buffers.
     *
     * @param id the sprite to remove
     */
    fun remove(id: SpriteId) {
        val removeIdx = spriteIndices[id]
        if (removeIdx == null) {
            logger.warn { "Sprite $id does not exist or has already been removed!" }
            return
        }

        removeId(id, removeIdx)
        spriteCount--
        staticDirty = true
        dynamicDirty = true
    }

    /**
     * Renders all the sprites in the cache.
     *
     * @param encoder the render pass encoder to draw sprites
     * @param viewProjection the view-projection matrix to use for rendering
     */
    fun render(encoder: RenderPassEncoder, viewProjection: Mat4? = null) {
        if (spriteCount == 0) return
        if (dirty) {
            if (mesh.geometry.dirty) {
                mesh.update()
                mesh.clearVertices()
            }
            if (dynamicDirty) {
                drawCalls.clear()
                ensureDrawCalls()
                dynamicData.limit = spriteCount * DYNAMIC_COMPONENTS_PER_SPRITE
                shader.updateSpriteDynamicStorage(dynamicData)
            }
            if (staticDirty) {
                staticData.limit = spriteCount * STATIC_COMPONENTS_PER_SPRITE
                shader.updateSpriteStaticStorage(staticData)
            }
            staticDirty = false
            dynamicDirty = false
        }
        encoder.setIndexBuffer(mesh.ibo, IndexFormat.UINT16)
        encoder.setVertexBuffer(0, mesh.vbo)
        var lastPipelineSet: RenderPipeline? = null
        var lastCombinedMatrixSet: Mat4? = null
        var lastBindGroupsSet: BindGroup? = null
        var instanceIdx = 0
        drawCalls.fastForEach { drawCall ->
            // ensure shader bind groups are created
            val texture = drawCall.texture
            val textureBindGroup =
                bindGroupByTextureId.getOrPut(texture.id) {
                    shader.createBindGroup(
                        BindingUsage.TEXTURE,
                        drawCall.texture.view,
                        drawCall.texture.sampler,
                    )
                        ?: error(
                            "SpriteCache requires $shader to create a BindGroup for BindingUsage.TEXTURE but it failed to do so."
                        )
                }
            if (viewProjection != null && lastCombinedMatrixSet != viewProjection) {
                lastCombinedMatrixSet = viewProjection
                cameraBuffers.update(viewProjection)
            }
            if (lastPipelineSet != renderPipeline) {
                encoder.setPipeline(renderPipeline)
                lastPipelineSet = renderPipeline
            }
            if (lastBindGroupsSet != textureBindGroup) {
                lastBindGroupsSet = textureBindGroup
                shader.setBindGroup(
                    encoder,
                    cameraBuffers.getOrCreateBindGroup(shader),
                    cameraBuffers.bindingUsage,
                    lastDynamicMeshOffsets,
                )
                shader.setBindGroup(encoder, textureBindGroup, BindingUsage.TEXTURE)
                shader.setBindGroups(encoder)
            }
            EngineStats.extra(QUAD_STATS_NAME, 1)
            EngineStats.extra(INSTANCES_STATS_NAME, drawCall.instances)
            encoder.drawIndexed(
                indexCount = 6,
                instanceCount = drawCall.instances,
                firstInstance = instanceIdx,
            )
            instanceIdx += drawCall.instances
        }
    }

    private fun ensureDrawCalls() {
        if (spriteCount == 0) return
        var currentTextureIdx = -1
        repeat(spriteCount) { i ->
            val textureIdx = dynamicData[i * DYNAMIC_COMPONENTS_PER_SPRITE + 4].toInt() and 0xFFFF
            if (textureIdx != currentTextureIdx) {
                drawCalls += DrawCall(0, textures.elementAt(textureIdx))
                currentTextureIdx = textureIdx
            }
            drawCalls.last().instances++
        }
    }

    /**
     * Update an existing sprite in the cache using a [SpriteView] for easy manipulation. Consider,
     * updating only the supported dynamic data, uvs, to prevent a drop in performance. Updating
     * static data may cause worse performance than using a standard [SpriteBatch].
     *
     * @param id the id of the sprite
     * @param slice the optional texture slice, used only when the slice must change to calculate
     *   the sort value.
     * @param action the update action
     */
    fun updateSprite(id: SpriteId, slice: TextureSlice? = null, action: SpriteView.() -> Unit) {
        val spriteIdx =
            spriteIndices[id] ?: error("SpriteId $id does not exist when trying to update!")

        spriteView.resetToZero()
        copyDataToSpriteView(spriteIdx, spriteView)
        spriteView.clean()
        var textureIdx = calculateTextureIndexFromData(spriteIdx)
        val rotation = slice?.rotated ?: (calculateRotationFromData(spriteIdx) == 1)
        // if zIndex or sprite prevTextureIdx changes, we must calculate the order all over again
        // so we must remove it from that data and then reinsert it.
        if (slice != null) {
            if (!textures.contains(slice.texture)) {
                textures += slice.texture
                textureIdx = textures.size - 1
            }
            spriteView.uvs.set(slice.u, slice.v, slice.u1, slice.v1)
        }
        spriteView.action()

        copySpriteViewToData(
            index = id,
            textureIdx = textureIdx,
            rotated = rotation,
            view = spriteView,
        )

        if (!staticDirty) {
            staticDirty = spriteView.staticDirty
        }
        if (!dynamicDirty) {
            dynamicDirty = spriteView.dynamicDirty
        }
    }

    /**
     * Update an existing sprite in the cache using a [SpriteView] for easy manipulation. The
     * `zIndex` of a sprite may not be changed unless the texture slice is passed back in. If not,
     * then, an error is thrown if the `zIndex` changes. This is due to losing the texture slice id
     * when calculating the sort value of the sprite. If the `zIndex` must be changed without the
     * [slice] then call [remove] to remove the sprite and [add] to add it back with the `zIndex`.
     *
     * @param slice the optional texture slice, used only when the `zIndex` must change to calculate
     *   the sort value.
     * @param action the update action
     */
    fun SpriteId.update(slice: TextureSlice? = null, action: SpriteView.() -> Unit) =
        updateSprite(this, slice, action)

    private fun insertId(id: SpriteId, insertIdx: Int) {
        val staticEndIdx = spriteCount * STATIC_COMPONENTS_PER_SPRITE
        if (staticData.capacity <= staticEndIdx) {
            logger.debug {
                "Static Data buffer has run out of room. Increasing size by a factor of 2."
            }
            val newData = FloatBuffer(staticData.capacity * 2)
            newData.put(staticData)
            staticData = newData
            staticData.position = 0
        }

        val dynamicEndIdx = spriteCount * DYNAMIC_COMPONENTS_PER_SPRITE
        if (dynamicData.capacity <= dynamicEndIdx) {
            logger.debug {
                "Dynamic Data buffer has run out of room. Increasing size by a factor of 2."
            }
            val newData = FloatBuffer(dynamicData.capacity * 2)
            newData.put(dynamicData)
            dynamicData = newData
            dynamicData.position = 0
        }

        var shouldShift = false
        spriteIndices.forEach { (spriteId, idx) ->
            if (idx >= insertIdx) {
                shouldShift = true
                spriteIndices[spriteId] = idx + 1
            }
        }
        // shift data down from insertIdx to spriteCount-1
        if (shouldShift) {
            val staticOffset = (insertIdx + 1) * STATIC_COMPONENTS_PER_SPRITE
            val staticStartIdx = insertIdx * STATIC_COMPONENTS_PER_SPRITE
            staticData.shiftDataDown(staticStartIdx, staticEndIdx, staticOffset)

            val dynamicOffset = (insertIdx + 1) * DYNAMIC_COMPONENTS_PER_SPRITE
            val dynamicStartIdx = insertIdx * DYNAMIC_COMPONENTS_PER_SPRITE
            dynamicData.shiftDataDown(dynamicStartIdx, dynamicEndIdx, dynamicOffset)
        }

        spriteIndices[id] = insertIdx
    }

    private fun removeId(id: SpriteId, removeIdx: Int) {
        // shift up the sprites after remove location by 1
        spriteIndices.forEach { (spriteId, idx) ->
            if (idx >= removeIdx) {
                spriteIndices[spriteId] = idx - 1
            }
        }

        // shift up all the data in the buffer from removeIdx to spriteCount-1
        val staticOffset = removeIdx * STATIC_COMPONENTS_PER_SPRITE
        staticData.put(
            staticData,
            dstOffset = staticOffset,
            srcOffset = (removeIdx + 1) * STATIC_COMPONENTS_PER_SPRITE,
            len = spriteCount * STATIC_COMPONENTS_PER_SPRITE,
        )

        val dynamicOffset = removeIdx * DYNAMIC_COMPONENTS_PER_SPRITE
        dynamicData.put(
            dynamicData,
            dstOffset = dynamicOffset,
            srcOffset = (removeIdx + 1) * DYNAMIC_COMPONENTS_PER_SPRITE,
            len = spriteCount * DYNAMIC_COMPONENTS_PER_SPRITE,
        )

        spriteIndices.remove(id)
        staticDirty = true
    }

    private fun copySpriteViewToData(
        index: Int,
        textureIdx: Int,
        rotated: Boolean,
        view: SpriteView,
    ) {
        val staticOffset = index * STATIC_COMPONENTS_PER_SPRITE
        staticData[staticOffset] = view.position.x
        staticData[staticOffset + 1] = view.position.y
        staticData[staticOffset + 2] = view.scale.x
        staticData[staticOffset + 3] = view.scale.y
        staticData[staticOffset + 4] = view.size.x
        staticData[staticOffset + 5] = view.size.y
        staticData[staticOffset + 6] = view.rotation.radians
        staticData[staticOffset + 7] = 0f // padding
        staticData[staticOffset + 8] = view.color.r
        staticData[staticOffset + 9] = view.color.g
        staticData[staticOffset + 10] = view.color.b
        staticData[staticOffset + 11] = view.color.a

        val dynamicOffset = index * DYNAMIC_COMPONENTS_PER_SPRITE
        dynamicData[dynamicOffset] = view.uvs.x
        dynamicData[dynamicOffset + 1] = view.uvs.y
        dynamicData[dynamicOffset + 2] = view.uvs.z
        dynamicData[dynamicOffset + 3] = view.uvs.w
        //   rotated       can be a value up to 255 (8 bits)
        //   textureId  can be a value up to 65,535 (16 bits)
        val rotationInt = if (rotated) 1 else 0
        dynamicData[dynamicOffset + 4] =
            ((rotationInt shl 16 and 0xFF0000) or (textureIdx and 0xFFFF)).toFloat()
    }

    private fun calculateTextureIndexFromData(index: Int): Int {
        val offset = index * DYNAMIC_COMPONENTS_PER_SPRITE
        return dynamicData[offset + 4].toInt() and 0xFFFF
    }

    private fun calculateRotationFromData(index: Int): Int {
        val offset = index * DYNAMIC_COMPONENTS_PER_SPRITE
        return dynamicData[offset + 4].toInt() shr 16 and 0xFF
    }

    private fun copyDataToSpriteView(index: Int, view: SpriteView) {
        val staticOffset = index * STATIC_COMPONENTS_PER_SPRITE
        view.position.set(staticData[staticOffset], staticData[staticOffset + 1])
        view.scale.set(staticData[staticOffset + 2], staticData[staticOffset + 3])
        view.size.set(staticData[staticOffset + 4], staticData[staticOffset + 5])
        view.rotation = staticData[staticOffset + 6].radians
        // offset + 7 is padding
        view.color.set(
            staticData[staticOffset + 8],
            staticData[staticOffset + 9],
            staticData[staticOffset + 10],
            staticData[staticOffset + 11],
        )

        val dynamicOffset = index * DYNAMIC_COMPONENTS_PER_SPRITE
        view.uvs.set(
            dynamicData[dynamicOffset],
            dynamicData[dynamicOffset + 1],
            dynamicData[dynamicOffset + 2],
            dynamicData[dynamicOffset + 3],
        )
    }

    private fun createRenderPipelineDescriptor(
        shader: Shader,
        blendState: BlendState,
    ): RenderPipelineDescriptor {
        return RenderPipelineDescriptor(
            layout = shader.getOrCreatePipelineLayout(),
            vertex =
                VertexState(
                    module = shader.shaderModule,
                    entryPoint = shader.vertexEntryPoint,
                    buffer = mesh.geometry.layout.gpuVertexBufferLayout,
                ),
            fragment =
                FragmentState(
                    module = shader.shaderModule,
                    entryPoint = shader.fragmentEntryPoint,
                    target =
                        ColorTargetState(
                            format = format,
                            blendState = blendState,
                            writeMask = ColorWriteMask.ALL,
                        ),
                ),
            primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
            depthStencil = null,
            multisample =
                MultisampleState(count = 1, mask = 0xFFFFFFF, alphaToCoverageEnabled = false),
        )
    }

    /**
     * Clear the current index map and sets the total sprite count back to zero and dirties the
     * buffer.
     */
    fun clear() {
        spriteIndices.clear()
        spriteCount = 0
        staticDirty = true
    }

    override fun release() {
        spriteBuffer.release()
        staticData.clear()
        spriteIndices.clear()
        if (ownCameraBuffers) {
            cameraBuffers.release()
        }
    }

    private fun FloatBuffer.shiftDataDown(startIdx: Int, endIdx: Int, dstOffset: Int) {
        if (endIdx == 0) return
        var idx = endIdx - 1
        for (i in endIdx - 1 downTo startIdx) {
            this[dstOffset + idx--] = this[i]
        }
    }

    companion object {
        private const val QUAD_STATS_NAME = "SpriteCache Quads"
        private const val INSTANCES_STATS_NAME = "SpriteCache Instances"

        /** position (2) + scale (2) + size (2) + rotation (1) + padding (1) + color (4) . */
        private const val STATIC_COMPONENTS_PER_SPRITE = 12

        /** uvs(min & max) (4) + texture/uvRotation(1) + padding (3) */
        private const val DYNAMIC_COMPONENTS_PER_SPRITE = 8

        private val logger = Logger<SpriteCache>()
    }

    /** A view into a singular sprite in a [SpriteCache]. */
    class SpriteView {
        private val spriteData =
            FloatArray(STATIC_COMPONENTS_PER_SPRITE + DYNAMIC_COMPONENTS_PER_SPRITE)

        private val _position = Vec2fView(0)

        /** The position of the sprite. */
        val position: MutableVec2f
            get() = _position

        private val _scale = Vec2fView(2)

        /** The scale of the sprite. Defaults to (1f,1f) */
        val scale: MutableVec2f
            get() = _scale

        private val _size = Vec2fView(4)

        /** The width and height of this sprite. Defaults to the texture slices width & height. */
        val size: MutableVec2f
            get() = _size

        private val _rotation = AngleView(6)

        /** The rotation of the sprite. */
        var rotation: Angle
            set(value) {
                _rotation.value = value
            }
            get() = _rotation.value

        private val _color = ColorWrapView(Vec4fView(7))

        /** The color / tint of the sprite. Defaults to white (1f, 1f, 1f, 1f). */
        val color: MutableColor
            get() = _color

        private val _uvs = Vec4fView(11)

        /** The uvs of the sprite. Defaults to the texture slices uv coords. */
        val uvs: MutableVec4f
            get() = _uvs

        /** @return true if either [position], [scale], [size], [rotation], or [color] is dirty. */
        val staticDirty: Boolean
            get() =
                _position.dirty || _scale.dirty || _size.dirty || _rotation.dirty || _color.dirty

        /** @return true if either [uvs] is dirty. */
        val dynamicDirty: Boolean
            get() = _uvs.dirty

        /**
         * Resets all views values to zero except for scale & color which reset to values to one.
         */
        fun resetToZero() {
            position.set(0f, 0f)
            scale.set(1f, 1f)
            size.set(0f, 0f)
            color.set(1f, 1f, 1f, 1f)
            rotation = Angle.ZERO
            clean()
        }

        /** Resets all views to a non-dirty state. */
        fun clean() {
            _position.dirty = false
            _scale.dirty = false
            _size.dirty = false
            _color.vecView.dirty = false
            _rotation.dirty = false
        }

        private inner class AngleView(private val attribOffset: Int) {
            var dirty = false
            var value: Angle
                get() =
                    if (attribOffset < 0) {
                        Angle.ZERO
                    } else {
                        spriteData[attribOffset].radians
                    }
                set(value) {
                    if (attribOffset >= 0) {
                        dirty = true
                        spriteData[attribOffset] = value.radians
                    }
                }
        }

        private inner class Vec2fView(private val attribOffset: Int) : MutableVec2f() {
            var dirty = false

            override operator fun get(i: Int): Float {
                return if (attribOffset >= 0 && i in 0..1) {
                    spriteData[attribOffset + i]
                } else {
                    0f
                }
            }

            override operator fun set(i: Int, v: Float) {
                if (attribOffset >= 0 && i in 0..1) {
                    dirty = true
                    spriteData[attribOffset + i] = v
                }
            }
        }

        private inner class Vec4fView(private val attribOffset: Int) : MutableVec4f() {
            var dirty = false

            override operator fun get(i: Int): Float {
                return if (attribOffset >= 0 && i in 0..3) {
                    spriteData[attribOffset + i]
                } else {
                    0f
                }
            }

            override operator fun set(i: Int, v: Float) {
                if (attribOffset >= 0 && i in 0..3) {
                    dirty = true
                    spriteData[attribOffset + i] = v
                }
            }
        }

        private inner class ColorWrapView(val vecView: Vec4fView) : MutableColor() {
            val dirty: Boolean
                get() = vecView.dirty

            override operator fun get(i: Int) = vecView[i]

            override operator fun set(i: Int, v: Float) {
                vecView[i] = v
            }
        }
    }

    private data class DrawCall(var instances: Int, var texture: Texture)
}

/** An identifier used to track sprites in a [SpriteCache] */
internal typealias SpriteId = Int
