package com.littlekt.graphics.g2d

import com.littlekt.EngineStats
import com.littlekt.Graphics
import com.littlekt.graphics.Color
import com.littlekt.graphics.IndexedMesh
import com.littlekt.graphics.Texture
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.shader.SpriteShader
import com.littlekt.graphics.textureIndexedMesh
import com.littlekt.graphics.util.CommonIndexedMeshGeometry
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.math.Mat4
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.cosine
import com.littlekt.math.geom.normalized
import com.littlekt.math.geom.sine
import com.littlekt.math.isFuzzyZero
import com.littlekt.util.LazyMat4
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.pool

/**
 * Draws batched quads using indices.
 *
 * @param device the device.
 * @param initWidth the initial width to use when calculating the projection matrix.
 * @param initHeight the initial height to sue when calculating the projection matrix.
 * @param format the texture format to be used for each [RenderPipeline].
 * @param size the initial number of quads to be used by the buffers in the internal mesh. The mesh
 *   will automatically grow if needed.
 * @param cameraDynamicSize the size in which the underlying [defaultShader] should be multiplied by
 *   to handle dynamic camera uniform values.
 * @author Colton Daily
 * @date 4/11/2024
 */
class SpriteBatch(
    val device: Device,
    initWidth: Int,
    initHeight: Int,
    val format: TextureFormat,
    private val size: Int = 1000,
    private val cameraDynamicSize: Int = 50
) : Batch {

    constructor(
        device: Device,
        graphics: Graphics,
        format: TextureFormat,
        size: Int = 1000,
        cameraDynamicSize: Int = 50
    ) : this(device, graphics.width, graphics.height, format, size, cameraDynamicSize)

    /**
     * The transform matrix that can be used to multiply against the [viewProjection] matrix. This
     * should be set directly instead of manipulating the underlying matrix.
     */
    override var transformMatrix = Mat4()
        set(value) {
            field = value
            combinedMatrix.isDirty = true
        }

    /**
     * The view projection matrix to be used when rendering. This should be set directly instead of
     * manipulating the underlying matrix.
     */
    override var viewProjection =
        Mat4()
            .setToOrthographic(
                left = -initWidth * 0.5f,
                right = initWidth * 0.5f,
                bottom = -initHeight * 0.5f,
                top = initHeight * 0.5f,
                near = 0f,
                far = 1f
            )
        set(value) {
            field = value
            combinedMatrix.isDirty = true
        }

    override var drawing: Boolean = false

    override var lastMeshIdx: Int = 0

    private var combinedMatrix = LazyMat4 { it.set(viewProjection).mul(transformMatrix) }
    private val matPool = pool(reset = { it.setToIdentity() }) { Mat4() }

    private val meshes: MutableList<IndexedMesh<CommonIndexedMeshGeometry>> =
        mutableListOf(textureIndexedMesh(device, size) { indicesAsQuad() })
    private val mesh: IndexedMesh<CommonIndexedMeshGeometry>
        get() = meshes[lastMeshIdx]

    override val defaultShader: Shader = SpriteBatchShader(device, cameraDynamicSize)

    override var shader: Shader = defaultShader

    private val bindGroupsByTextureId: MutableMap<TextureRenderInfo, List<BindGroup>> =
        mutableMapOf()
    private val drawCalls: MutableList<DrawCall> = mutableListOf()

    private var blendState = BlendState.NonPreMultiplied
    private var prevBlendState = blendState

    private val renderPipelineByBlendState: MutableMap<RenderInfo, RenderPipeline> =
        mutableMapOf(
            RenderInfo(shader, blendState) to
                device.createRenderPipeline(
                    createRenderPipelineDescriptor(RenderInfo(shader, blendState))
                )
        )

    private val spriteIndices = mutableMapOf(lastMeshIdx to 0)
    private var spriteIdx: Int
        get() = spriteIndices.getOrPut(lastMeshIdx) { 0 }
        set(value) {
            spriteIndices[lastMeshIdx] = value
        }

    private var lastTexture: Texture? = null
    private var lastBlendState: BlendState = blendState
    private var lastShader: Shader = shader
    private val lastCombinedMatrix: Mat4 = matPool.alloc().set(combinedMatrix.get())
    private var invTexWidth = 0f
    private var invTexHeight = 0f

    private val dataMap = mutableMapOf<String, Any>()
    private val lastDynamicMeshOffsets: MutableList<Long> = MutableList(1) { 0L }
    private val shaderDynamicOffsets = mutableMapOf<Shader, Long>()

    init {
        check(size > 0) { "A batch must be greater than zero sprites!" }
        check(cameraDynamicSize >= 1) { "SpriteBatch: 'cameraDynamicSize' must be >= 1!" }
    }

    override fun begin(viewProjection: Mat4?) {
        check(!drawing) { "SpriteBatch.end must be called before begin." }
        viewProjection?.let { this.viewProjection = it }
        drawing = true
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
        color: Color,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(slice.texture)

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

        var blX: Float
        var blY: Float
        var tlX: Float
        var tlY: Float
        var trX: Float
        var trY: Float
        var brX: Float
        var brY: Float

        if (rotation.normalized.radians.isFuzzyZero()) {
            blX = p1x
            blY = p1y

            tlX = p2x
            tlY = p2y

            trX = p3x
            trY = p3y

            brX = p4x
            brY = p4y
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine

            blX = cos * p1x - sin * p1y
            blY = sin * p1x + cos * p1y

            tlX = cos * p2x - sin * p2y
            tlY = sin * p2x + cos * p2y

            trX = cos * p3x - sin * p3y
            trY = sin * p3x + cos * p3y

            brX = blX + (trX - tlX)
            brY = trY - (tlY - blY)
        }

        blX += x
        blY += y
        tlX += x
        tlY += y
        trX += x
        trY += y
        brX += x
        brY += y

        val u0 = if (flipX) slice.u1 else slice.u
        val v0 = if (flipY) slice.v1 else slice.v
        val u1 = if (flipX) slice.u else slice.u1
        val v1 = if (flipY) slice.v else slice.v1

        mesh.geometry.run {
            addVertex { // top left
                position.x = tlX
                position.y = tlY
                this.color.set(color)
                texCoords.x = if (slice.rotated) u1 else u0
                texCoords.y = v0
            }
            addVertex { // top right
                position.x = trX
                position.y = trY
                this.color.set(color)
                texCoords.x = u1
                texCoords.y = if (slice.rotated) v1 else v0
            }
            addVertex { // bottom right
                position.x = brX
                position.y = brY
                this.color.set(color)
                texCoords.x = if (slice.rotated) u0 else u1
                texCoords.y = v1
            }
            addVertex { // bottom left
                position.x = blX
                position.y = blY
                this.color.set(color)
                texCoords.x = u0
                texCoords.y = if (slice.rotated) v0 else v1
            }
        }

        drawCalls.last().instances++
        spriteIdx++
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
        color: Color,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(slice.texture)

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

        var blX: Float
        var blY: Float
        var tlX: Float
        var tlY: Float
        var trX: Float
        var trY: Float
        var brR: Float
        var byR: Float

        if (rotation.normalized.radians.isFuzzyZero()) {
            blX = p1x
            blY = p1y

            tlX = p2x
            tlY = p2y

            trX = p3x
            trY = p3y

            brR = p4x
            byR = p4y
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine

            blX = cos * p1x - sin * p1y
            blY = sin * p1x + cos * p1y

            tlX = cos * p2x - sin * p2y
            tlY = sin * p2x + cos * p2y

            trX = cos * p3x - sin * p3y
            trY = sin * p3x + cos * p3y

            brR = blX + (trX - tlX)
            byR = trY - (tlY - blY)
        }

        blX += x
        blY += y
        tlX += x
        tlY += y
        trX += x
        trY += y
        brR += x
        byR += y

        var u0 = srcX * invTexWidth
        var v0 = srcY * invTexHeight
        var u1 = (srcX + srcWidth) * invTexWidth
        var v1 = (srcY + srcHeight) * invTexHeight

        u0 = if (flipX) u1 else u0
        v0 = if (flipY) v1 else v0
        u1 = if (flipX) u0 else u1
        v1 = if (flipY) v0 else v1

        mesh.geometry.run {
            addVertex { // top left
                position.x = tlX
                position.y = tlY
                this.color.set(color)
                texCoords.x = if (slice.rotated) u1 else u0
                texCoords.y = v0
            }
            addVertex { // top right
                position.x = trX
                position.y = trY
                this.color.set(color)
                texCoords.x = u1
                texCoords.y = if (slice.rotated) v1 else v0
            }
            addVertex { // bottom right
                position.x = brR
                position.y = byR
                this.color.set(color)
                texCoords.x = if (slice.rotated) u0 else u1
                texCoords.y = v1
            }
            addVertex { // bottom left
                position.x = blX
                position.y = blY
                this.color.set(color)
                texCoords.x = u0
                texCoords.y = if (slice.rotated) v0 else v1
            }
        }

        drawCalls.last().instances++
        spriteIdx++
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
        color: Color,
        flipX: Boolean,
        flipY: Boolean,
    ) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(texture)

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

        var blX: Float
        var blY: Float
        var tlX: Float
        var tlY: Float
        var trX: Float
        var trY: Float
        var brX: Float
        var brY: Float

        if (rotation.normalized.radians.isFuzzyZero()) {
            blX = p1x
            blY = p1y

            tlX = p2x
            tlY = p2y

            trX = p3x
            trY = p3y

            brX = p4x
            brY = p4y
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine

            blX = cos * p1x - sin * p1y
            blY = sin * p1x + cos * p1y

            tlX = cos * p2x - sin * p2y
            tlY = sin * p2x + cos * p2y

            trX = cos * p3x - sin * p3y
            trY = sin * p3x + cos * p3y

            brX = blX + (trX - tlX)
            brY = trY - (tlY - blY)
        }

        blX += x
        blY += y
        tlX += x
        tlY += y
        trX += x
        trY += y
        brX += x
        brY += y

        var u0 = srcX * invTexWidth
        var v0 = srcY * invTexHeight
        var u1 = (srcX + srcWidth) * invTexWidth
        var v1 = (srcY + srcHeight) * invTexHeight

        if (flipX) {
            val tmp = u0
            u0 = u1
            u1 = tmp
        }

        if (flipY) {
            val tmp = v0
            v0 = v1
            v1 = tmp
        }

        mesh.geometry.run {
            addVertex { // top left
                position.x = tlX
                position.y = tlY
                this.color.set(color)
                texCoords.x = u0
                texCoords.y = v0
            }
            addVertex { // top right
                position.x = trX
                position.y = trY
                this.color.set(color)
                texCoords.x = u1
                texCoords.y = v0
            }
            addVertex { // bottom right
                position.x = brX
                position.y = brY
                this.color.set(color)
                texCoords.x = u1
                texCoords.y = v1
            }
            addVertex { // bottom left
                position.x = blX
                position.y = blY
                this.color.set(color)
                texCoords.x = u0
                texCoords.y = v1
            }
        }

        drawCalls.last().instances++
        spriteIdx++
    }

    override fun draw(texture: Texture, spriteVertices: FloatArray, offset: Int, count: Int) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        ensureDrawCall(texture)

        val stride = mesh.geometry.layout.arrayStride.toInt()

        mesh.geometry.add(spriteVertices, offset, spriteIdx * stride, count)
        val total = count / stride
        drawCalls.last().instances += total
        spriteIdx += total
    }

    override fun flush(renderPassEncoder: RenderPassEncoder, viewProjection: Mat4?) {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        viewProjection?.let { this.viewProjection = it }
        if (spriteIdx == 0) return

        mesh.update()
        renderPassEncoder.setIndexBuffer(mesh.ibo, IndexFormat.UINT16)
        renderPassEncoder.setVertexBuffer(0, mesh.vbo)
        var lastPipelineSet: RenderPipeline? = null
        var lastCombinedMatrixSet: Mat4? = null
        var lastBindGroupsSet: List<BindGroup>? = null
        var lastShader: Shader? = null
        drawCalls.fastForEach { drawCall ->
            val shader = drawCall.renderInfo.shader
            var lastDynamicOffsetIndex = shaderDynamicOffsets.getOrPut(shader) { -1L }
            val renderPipeline =
                renderPipelineByBlendState.getOrPut(drawCall.renderInfo) {
                    device.createRenderPipeline(createRenderPipelineDescriptor(drawCall.renderInfo))
                }
            // ensure shader bind groups are created
            val bindGroups =
                bindGroupsByTextureId.getOrPut(drawCall.textureRenderInfo) {
                    dataMap.clear()
                    dataMap[SpriteShader.TEXTURE] = drawCall.texture
                    shader.createBindGroups(dataMap)
                }
            if (lastCombinedMatrixSet != drawCall.combinedMatrix || lastShader != shader) {
                dataMap.clear()
                dataMap[SpriteShader.VIEW_PROJECTION] = drawCall.combinedMatrix
                if (lastDynamicOffsetIndex < cameraDynamicSize - 1) {
                    lastDynamicOffsetIndex++
                } else {
                    logger.warn {
                        "SpriteBatch wants to update the SpriteShader.CAMERA_UNIFORM_DYNAMIC_OFFSET but is unable to due to SpriteBatch.cameraDynamicSize being too small! If you are setting the Batch.viewProjection multiple times per render pass then increase this value!"
                    }
                }
                dataMap[SpriteShader.CAMERA_UNIFORM_DYNAMIC_OFFSET] = lastDynamicOffsetIndex
                shaderDynamicOffsets[shader] = lastDynamicOffsetIndex
                shader.update(dataMap)
            }
            if (lastPipelineSet != renderPipeline) {
                renderPassEncoder.setPipeline(renderPipeline)
                lastPipelineSet = renderPipeline
            }
            if (
                lastBindGroupsSet != bindGroups ||
                    lastShader != shader ||
                    lastCombinedMatrixSet != drawCall.combinedMatrix
            ) {
                lastBindGroupsSet = bindGroups
                lastDynamicMeshOffsets[0] =
                    lastDynamicOffsetIndex * device.limits.minUniformBufferOffsetAlignment
                shader.setBindGroups(renderPassEncoder, bindGroups, lastDynamicMeshOffsets)
                lastShader = shader
                lastCombinedMatrixSet = drawCall.combinedMatrix
            }
            val indexCount = drawCall.instances * 6
            EngineStats.extra(QUAD_STATS_NAME, drawCall.instances)
            renderPassEncoder.drawIndexed(indexCount, 1, firstIndex = drawCall.offset * 6)

            matPool.free(drawCall.combinedMatrix)
        }
        drawCalls.clear()
        dataMap.clear()
        lastTexture = null
        lastMeshIdx++
    }

    override fun end() {
        check(drawing) { "SpriteBatch.begin must be called before draw." }
        lastMeshIdx = 0
        shaderDynamicOffsets.clear()
        meshes.forEach { it.clearVertices() }
        spriteIndices.keys.forEach { spriteIndices[it] = 0 }
        drawing = false
    }

    override fun setBlendState(newBlendState: BlendState) {
        if (blendState == newBlendState) return
        prevBlendState = blendState
        blendState = newBlendState
    }

    override fun swapToPreviousBlendState() {
        if (blendState == prevBlendState) return
        setBlendState(prevBlendState)
    }

    override fun useDefaultShader() {
        if (shader != defaultShader) {
            shader = defaultShader
        }
    }

    private fun ensureDrawCall(texture: Texture) {
        ensureMesh()
        ensureTexture(texture)
        ensureRenderInfo()
        ensureMatrices()
    }

    private fun ensureMesh() {
        if (lastMeshIdx + 1 > meshes.size) {
            addMesh()
        }
    }

    private fun ensureTexture(texture: Texture) {
        if (texture != lastTexture) {
            lastTexture = texture
            invTexWidth = 1f / texture.width
            invTexHeight = 1f / texture.height

            createDrawCall()
        }
    }

    private fun ensureRenderInfo() {
        if (blendState != lastBlendState || shader != lastShader) {
            lastBlendState = blendState
            lastShader = shader

            createDrawCall()
        }
    }

    private fun ensureMatrices() {
        if (combinedMatrix.get() != lastCombinedMatrix) {
            lastCombinedMatrix.set(combinedMatrix.get())
            createDrawCall()
        }
    }

    private fun createDrawCall() {
        val texture = lastTexture ?: error("Attempting to create a draw call with no texture!")
        if (drawCalls.isNotEmpty() && drawCalls.last().instances == 0) {
            // we created a new draw call, but we haven't drawn yet. So we can just
            // update the last created draw call and update its info
            drawCalls.removeLast().also { matPool.free(it.combinedMatrix) }
            drawCalls +=
                DrawCall(
                    texture = texture,
                    renderInfo = RenderInfo(lastShader, lastBlendState),
                    combinedMatrix = matPool.alloc().set(lastCombinedMatrix),
                    offset = spriteIdx
                )
        } else {
            drawCalls +=
                DrawCall(
                    texture = texture,
                    renderInfo = RenderInfo(lastShader, lastBlendState),
                    combinedMatrix = matPool.alloc().set(lastCombinedMatrix),
                    offset = spriteIdx
                )
        }
    }

    private fun createRenderPipelineDescriptor(renderInfo: RenderInfo): RenderPipelineDescriptor {
        val (shader, blendState) = renderInfo
        return RenderPipelineDescriptor(
            layout = shader.pipelineLayout,
            vertex =
                VertexState(
                    module = shader.shaderModule,
                    entryPoint = shader.vertexEntryPoint,
                    mesh.geometry.layout.gpuVertexBufferLayout
                ),
            fragment =
                FragmentState(
                    module = shader.shaderModule,
                    entryPoint = shader.fragmentEntryPoint,
                    target =
                        ColorTargetState(
                            format = format,
                            blendState = blendState,
                            writeMask = ColorWriteMask.ALL
                        )
                ),
            primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
            depthStencil = null,
            multisample =
                MultisampleState(count = 1, mask = 0xFFFFFFF, alphaToCoverageEnabled = false)
        )
    }

    private fun addMesh(): IndexedMesh<CommonIndexedMeshGeometry> {
        val mesh = textureIndexedMesh(device, size) { indicesAsQuad() }
        meshes += mesh
        return mesh
    }

    override fun release() {
        lastTexture = null
        mesh.release()
        defaultShader.release()
        renderPipelineByBlendState.values.forEach { it.release() }
        renderPipelineByBlendState.clear()
        drawCalls.clear()
    }

    private data class DrawCall(
        val texture: Texture,
        val renderInfo: RenderInfo,
        val combinedMatrix: Mat4,
        val offset: Int = 0,
    ) {
        var instances: Int = 0
        val textureRenderInfo = TextureRenderInfo(texture.id, renderInfo)
    }

    companion object {
        private const val QUAD_STATS_NAME = "SpriteBatch Quads"
        private val logger = Logger<SpriteBatch>()
    }

    private data class RenderInfo(val shader: Shader, val blendState: BlendState)

    private data class TextureRenderInfo(val textureId: Int, val renderInfo: RenderInfo)
}
