package com.littlekt.graphics.g3d

import com.littlekt.EngineStats
import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.file.IntBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.Color
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.g2d.font.GlyphLayout
import com.littlekt.graphics.g2d.font.MsdfShader
import com.littlekt.graphics.g2d.font.TextureFont
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.math.geom.Angle
import com.littlekt.math.geom.cosine
import com.littlekt.math.geom.sine
import com.littlekt.resources.Textures
import com.littlekt.util.align
import com.littlekt.util.datastructure.IntMap
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Draws textured quads, text and strips in a 3D scene, such as health bars, damage numbers, icons
 * over units, beams and trails. Quads & text face the camera, and strips are ribbons through a list
 * of points that turn to face it. Draws are batched by texture and drawn on [flush].
 *
 * Set [colorFormat], [depthFormat] & [sampleCount] to match the render pass. Draws are tested
 * against the depth buffer without writing to it, unless [depthTest] is `false`, so draw them after
 * opaque geometry.
 *
 * ```
 * billboards.begin(camera)
 * billboards.draw(icon, x, y + 2f, z, 0.5f, 0.5f)
 * billboards.drawText(font, "-120", x, y + 2.5f, z, 0.4f, Color.RED)
 * billboards.flush(renderPass)
 * billboards.end()
 * ```
 *
 * @param device the device to create buffers & pipelines with
 * @param initialQuads the number of quads the buffers start with room for. They grow as needed.
 * @author Colton Daily
 * @date 11/15/2026
 */
class BillboardBatch(val device: Device, initialQuads: Int = 1024) : Releasable {

    /** The format of the color target. */
    var colorFormat: TextureFormat = TextureFormat.BGRA8_UNORM_SRGB

    /** The format of the depth target, or `null` if the render pass has none. */
    var depthFormat: TextureFormat? = TextureFormat.DEPTH24_PLUS_STENCIL8

    /** The number of samples of the render pass targets, `1` or `4`. */
    var sampleCount: Int = 1
        set(value) {
            require(value == 1 || value == 4) { "BillboardBatch.sampleCount must be 1 or 4!" }
            field = value
        }

    /** The blending of the following draws. */
    var blendState: BlendState = BlendState.NonPreMultiplied

    /** If `true`, the following draws are hidden behind closer geometry. */
    var depthTest: Boolean = true

    /** The most flushes in a single frame. */
    var maxFlushesPerFrame: Int = 8
        set(value) {
            require(value > 0) { "maxFlushesPerFrame must be greater than 0!" }
            if (value == field) return
            field = value
            recreateCameraBuffer()
        }

    /** `true` between [begin] and [end]. */
    var drawing: Boolean = false
        private set

    private val cameraStride =
        (16 * Float.SIZE_BYTES).align(device.limits.minUniformBufferOffsetAlignment)
    private val cameraLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(
                        0,
                        ShaderStage.VERTEX,
                        BufferBindingLayout(hasDynamicOffset = true, minBindingSize = 64L),
                    )
                ),
                "BillboardBatch camera layout",
            )
        )
    private val textureLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(
                    BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                    BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                ),
                "BillboardBatch texture layout",
            )
        )
    private val styleLayout =
        device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                listOf(BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, BufferBindingLayout())),
                "BillboardBatch msdf style layout",
            )
        )
    private val shader = device.createShaderModule(WGSL)
    private val msdfShader = device.createShaderModule(MSDF_WGSL)
    private val pipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(listOf(cameraLayout, textureLayout), "BillboardBatch")
        )
    private val msdfPipelineLayout =
        device.createPipelineLayout(
            PipelineLayoutDescriptor(
                listOf(cameraLayout, textureLayout, styleLayout),
                "BillboardBatch msdf",
            )
        )
    private val pipelines = mutableListOf<PipelineEntry>()
    private val textureBindGroups = IntMap<BindGroup>()
    private val styleBindGroups = IntMap<BindGroup>()

    private var cameraBuffer = createCameraBuffer()
    private var cameraBindGroup = createCameraBindGroup()
    private val cameraData = FloatBuffer(16)

    private var vertexCapacity = max(initialQuads, 1) * 4
    private var indexCapacity = max(initialQuads, 1) * 6
    private var vertexBuffer = createVertexBuffer(vertexCapacity)
    private var indexBuffer = createIndexBuffer(indexCapacity)
    private val retired = mutableListOf<GPUBuffer>()
    private var vertices = FloatBuffer(vertexCapacity * FLOATS_PER_VERTEX)
    private var indices = IntBuffer(indexCapacity)
    private var vertexCount = 0
    private var indexCount = 0
    private var gpuVertexOffset = 0
    private var gpuIndexOffset = 0
    private var frame = -1L
    private var flushIndex = 0

    private val draws = mutableListOf<DrawCall>()
    private var drawCount = 0

    private var camera: Camera? = null
    private var rightX = 1f
    private var rightY = 0f
    private var rightZ = 0f
    private var upX = 0f
    private var upY = 1f
    private var upZ = 0f
    private var backX = 0f
    private var backY = 0f
    private var backZ = 1f

    private val layout = GlyphLayout()
    private var strip = FloatArray(STRIP_FLOATS * 64)
    private var stripCount = 0
    private var stripSlice: TextureSlice? = null

    /** `true` once released. */
    var isReleased: Boolean = false
        private set

    /** Begins drawing with [camera], which quads, text & strips turn to face. */
    fun begin(camera: Camera) {
        check(!drawing) { "BillboardBatch.end must be called before begin." }
        drawing = true
        this.camera = camera
        val view = camera.view.data
        setRight(view[0], view[4], view[8])
        setUp(view[1], view[5], view[9])
        setBack(view[2], view[6], view[10])
    }

    /**
     * Draws [slice] as a quad facing the camera.
     *
     * @param x the world x position of the quad's origin
     * @param y the world y position of the quad's origin
     * @param z the world z position of the quad's origin
     * @param width the width in world units, or pixels if [sizeInPixels]
     * @param height the height in world units, or pixels if [sizeInPixels]
     * @param color the tint
     * @param rotation the rotation around the view direction
     * @param originX the origin from the left, from `0` to `1`
     * @param originY the origin from the bottom, from `0` to `1`
     * @param mode how the quad faces the camera
     * @param sizeInPixels `true` to keep the same size on screen at any distance
     */
    fun draw(
        slice: TextureSlice,
        x: Float,
        y: Float,
        z: Float,
        width: Float,
        height: Float,
        color: Color = Color.WHITE,
        rotation: Angle = Angle.ZERO,
        originX: Float = 0.5f,
        originY: Float = 0.5f,
        mode: BillboardMode = BillboardMode.SPHERICAL,
        sizeInPixels: Boolean = false,
    ) {
        drawQuad(
            slice.texture,
            slice.u,
            slice.v,
            slice.u1,
            slice.v1,
            x,
            y,
            z,
            width,
            height,
            color,
            rotation,
            originX,
            originY,
            mode,
            sizeInPixels,
        )
    }

    /** Draws all of [texture] as a quad facing the camera. @see draw */
    fun draw(
        texture: Texture,
        x: Float,
        y: Float,
        z: Float,
        width: Float,
        height: Float,
        color: Color = Color.WHITE,
        rotation: Angle = Angle.ZERO,
        originX: Float = 0.5f,
        originY: Float = 0.5f,
        mode: BillboardMode = BillboardMode.SPHERICAL,
        sizeInPixels: Boolean = false,
    ) {
        drawQuad(
            texture,
            0f,
            0f,
            1f,
            1f,
            x,
            y,
            z,
            width,
            height,
            color,
            rotation,
            originX,
            originY,
            mode,
            sizeInPixels,
        )
    }

    /**
     * Draws [text] facing the camera. Use an [com.littlekt.graphics.g2d.font.MsdfFont] to keep it
     * sharp at any distance.
     *
     * @param size the font size in world units, or pixels if [sizeInPixels]
     * @param originX the origin from the left of the text, from `0` to `1`
     * @param originY the origin from the bottom of the text's capital letters, from `0` to `1`
     * @see draw
     */
    fun drawText(
        font: TextureFont,
        text: CharSequence,
        x: Float,
        y: Float,
        z: Float,
        size: Float,
        color: Color = Color.WHITE,
        originX: Float = 0.5f,
        originY: Float = 0.5f,
        mode: BillboardMode = BillboardMode.SPHERICAL,
        sizeInPixels: Boolean = false,
    ) {
        checkDrawing()
        if (text.isEmpty() || font.metrics.size <= 0f) return
        val worldSize = if (sizeInPixels) size * worldPerPixel(x, y, z) else size
        val scale = worldSize / font.metrics.size
        layout.setText(font, text, scaleX = scale, scaleY = scale)
        if (layout.runs.isEmpty()) return
        val metrics = font.metrics
        val first = layout.runs.first()
        val last = layout.runs.last()
        val top = first.y - metrics.baseline * scale + metrics.capHeight * scale
        val bottom = last.y - metrics.baseline * scale
        val offsetX = -layout.width * originX
        val offsetY = -(bottom + (top - bottom) * originY)
        val style = font.shader as? MsdfShader
        selectAxes(mode)
        for (r in layout.runs.indices) {
            val run = layout.runs[r]
            var tx = run.x
            for (i in run.glyphs.indices) {
                tx += run.advances[i]
                val glyph = run.glyphs[i]
                if (glyph.bounds.width <= 0f || glyph.bounds.height <= 0f) continue
                val texture = font.textures.getOrNull(glyph.page) ?: continue
                val left = tx + glyph.left * scale + offsetX
                val right = tx + glyph.right * scale + offsetX
                val bottomY = run.y + glyph.bottom * scale + offsetY
                val topY = run.y + glyph.top * scale + offsetY
                ensureRoom(4, 6)
                useDraw(texture, style)
                val base = vertexCount
                vertex(x, y, z, left, bottomY, color, glyph.u0, glyph.v1)
                vertex(x, y, z, right, bottomY, color, glyph.u1, glyph.v1)
                vertex(x, y, z, right, topY, color, glyph.u1, glyph.v0)
                vertex(x, y, z, left, topY, color, glyph.u0, glyph.v0)
                quadIndices(base)
            }
        }
    }

    /**
     * Begins a strip: a ribbon through the points added with [stripPoint] that turns to face the
     * camera. The texture is stretched along its length.
     *
     * @param slice the texture of the strip
     */
    fun beginStrip(slice: TextureSlice = Textures.white) {
        checkDrawing()
        check(stripSlice == null) { "BillboardBatch.endStrip must be called before beginStrip." }
        stripSlice = slice
        stripCount = 0
    }

    /** Adds a point of [width] world units and [color] to the current strip. */
    fun stripPoint(x: Float, y: Float, z: Float, width: Float, color: Color = Color.WHITE) {
        check(stripSlice != null) { "BillboardBatch.beginStrip must be called before stripPoint." }
        val offset = stripCount * STRIP_FLOATS
        if (offset + STRIP_FLOATS > strip.size) strip = strip.copyOf(strip.size * 2)
        strip[offset] = x
        strip[offset + 1] = y
        strip[offset + 2] = z
        strip[offset + 3] = width
        strip[offset + 4] = color.r
        strip[offset + 5] = color.g
        strip[offset + 6] = color.b
        strip[offset + 7] = color.a
        stripCount++
    }

    /** Ends the current strip and adds it to the batch. */
    fun endStrip() {
        val slice = stripSlice ?: error("BillboardBatch.beginStrip must be called before endStrip.")
        stripSlice = null
        val count = stripCount
        if (count < 2) return
        var total = 0f
        for (i in 1 until count) total += distance(i - 1, i)
        if (total <= 0f) return
        ensureRoom(count * 2, (count - 1) * 6)
        useDraw(slice.texture, null)
        val base = vertexCount
        var travelled = 0f
        for (i in 0 until count) {
            if (i > 0) travelled += distance(i - 1, i)
            val prev = max(i - 1, 0) * STRIP_FLOATS
            val next = minOf(i + 1, count - 1) * STRIP_FLOATS
            var tx = strip[next] - strip[prev]
            var ty = strip[next + 1] - strip[prev + 1]
            var tz = strip[next + 2] - strip[prev + 2]
            val tl = sqrt(tx * tx + ty * ty + tz * tz).takeIf { it > 0f } ?: 1f
            tx /= tl
            ty /= tl
            tz /= tl
            var sx = ty * backZ - tz * backY
            var sy = tz * backX - tx * backZ
            var sz = tx * backY - ty * backX
            val sl = sqrt(sx * sx + sy * sy + sz * sz)
            if (sl > 0.0001f) {
                sx /= sl
                sy /= sl
                sz /= sl
            } else {
                sx = upX
                sy = upY
                sz = upZ
            }
            val o = i * STRIP_FLOATS
            val half = strip[o + 3] * 0.5f
            val u = slice.u + (slice.u1 - slice.u) * (travelled / total)
            putVertex(
                strip[o] - sx * half,
                strip[o + 1] - sy * half,
                strip[o + 2] - sz * half,
                strip[o + 4],
                strip[o + 5],
                strip[o + 6],
                strip[o + 7],
                u,
                slice.v1,
            )
            putVertex(
                strip[o] + sx * half,
                strip[o + 1] + sy * half,
                strip[o + 2] + sz * half,
                strip[o + 4],
                strip[o + 5],
                strip[o + 6],
                strip[o + 7],
                u,
                slice.v,
            )
        }
        for (i in 0 until count - 1) {
            val a = base + i * 2
            putIndex(a)
            putIndex(a + 2)
            putIndex(a + 3)
            putIndex(a)
            putIndex(a + 3)
            putIndex(a + 1)
        }
    }

    /** Draws everything added since the last flush within [renderPassEncoder]. */
    fun flush(renderPassEncoder: RenderPassEncoder) {
        checkDrawing()
        if (drawCount == 0) return
        val camera = camera ?: return
        nextFrameCheck()
        closeDraws()
        if (gpuVertexOffset + vertexCount > vertexCapacity || gpuIndexOffset + indexCount > indexCapacity) {
            growGpuBuffers()
        }
        device.queue.writeBuffer(
            vertexBuffer,
            vertices,
            offset = gpuVertexOffset.toLong() * VERTEX_BYTES,
            size = vertexCount.toLong() * FLOATS_PER_VERTEX,
        )
        device.queue.writeBuffer(
            indexBuffer,
            indices,
            offset = gpuIndexOffset.toLong() * Int.SIZE_BYTES,
            size = indexCount.toLong(),
        )
        if (flushIndex >= maxFlushesPerFrame) {
            logger.warn {
                "BillboardBatch has exceeded maxFlushesPerFrame ($maxFlushesPerFrame). Reusing the last camera slot which will overwrite its data!"
            }
            flushIndex = maxFlushesPerFrame - 1
        }
        val slot = flushIndex++
        val viewProjection = camera.viewProjection.data
        for (i in 0 until 16) cameraData[i] = viewProjection[i]
        device.queue.writeBuffer(cameraBuffer, cameraData, offset = slot.toLong() * cameraStride)

        renderPassEncoder.setVertexBuffer(0, vertexBuffer)
        renderPassEncoder.setIndexBuffer(indexBuffer, IndexFormat.UINT32)
        var lastPipeline: RenderPipeline? = null
        var lastTexture: BindGroup? = null
        var lastStyle: BindGroup? = null
        for (i in 0 until drawCount) {
            val draw = draws[i]
            val style = draw.style
            val pipeline = pipeline(style != null, draw.blend, draw.depthTest)
            if (pipeline !== lastPipeline) {
                renderPassEncoder.setPipeline(pipeline)
                renderPassEncoder.setBindGroup(0, cameraBindGroup, slot * cameraStride)
                lastPipeline = pipeline
                lastTexture = null
                lastStyle = null
            }
            val textureGroup = textureBindGroup(draw.texture)
            if (textureGroup !== lastTexture) {
                renderPassEncoder.setBindGroup(1, textureGroup)
                lastTexture = textureGroup
            }
            if (style != null) {
                val styleGroup = styleBindGroup(style)
                if (styleGroup !== lastStyle) {
                    renderPassEncoder.setBindGroup(2, styleGroup)
                    lastStyle = styleGroup
                }
            }
            renderPassEncoder.drawIndexed(
                draw.indexCount,
                1,
                firstIndex = gpuIndexOffset + draw.firstIndex,
                baseVertex = gpuVertexOffset,
            )
        }
        gpuVertexOffset += vertexCount
        gpuIndexOffset += indexCount
        vertexCount = 0
        indexCount = 0
        drawCount = 0
    }

    /** Ends drawing. Anything not flushed is dropped. */
    fun end() {
        checkDrawing()
        check(stripSlice == null) { "BillboardBatch.endStrip must be called before end." }
        drawing = false
        camera = null
        vertexCount = 0
        indexCount = 0
        drawCount = 0
    }

    private fun drawQuad(
        texture: Texture,
        u0: Float,
        v0: Float,
        u1: Float,
        v1: Float,
        x: Float,
        y: Float,
        z: Float,
        width: Float,
        height: Float,
        color: Color,
        rotation: Angle,
        originX: Float,
        originY: Float,
        mode: BillboardMode,
        sizeInPixels: Boolean,
    ) {
        checkDrawing()
        val scale = if (sizeInPixels) worldPerPixel(x, y, z) else 1f
        val w = width * scale
        val h = height * scale
        val left = -w * originX
        val bottom = -h * originY
        val right = left + w
        val top = bottom + h
        ensureRoom(4, 6)
        useDraw(texture, null)
        selectAxes(mode)
        val base = vertexCount
        if (rotation == Angle.ZERO) {
            vertex(x, y, z, left, bottom, color, u0, v1)
            vertex(x, y, z, right, bottom, color, u1, v1)
            vertex(x, y, z, right, top, color, u1, v0)
            vertex(x, y, z, left, top, color, u0, v0)
        } else {
            val cos = rotation.cosine
            val sin = rotation.sine
            vertex(x, y, z, left * cos - bottom * sin, left * sin + bottom * cos, color, u0, v1)
            vertex(x, y, z, right * cos - bottom * sin, right * sin + bottom * cos, color, u1, v1)
            vertex(x, y, z, right * cos - top * sin, right * sin + top * cos, color, u1, v0)
            vertex(x, y, z, left * cos - top * sin, left * sin + top * cos, color, u0, v0)
        }
        quadIndices(base)
    }

    private var axisRightX = 0f
    private var axisRightY = 0f
    private var axisRightZ = 0f
    private var axisUpX = 0f
    private var axisUpY = 0f
    private var axisUpZ = 0f

    private fun selectAxes(mode: BillboardMode) {
        if (mode == BillboardMode.AXIS_Y) {
            var rx = backZ
            var rz = -backX
            val length = sqrt(rx * rx + rz * rz)
            if (length > 0.0001f) {
                rx /= length
                rz /= length
                axisRightX = rx
                axisRightY = 0f
                axisRightZ = rz
                axisUpX = 0f
                axisUpY = 1f
                axisUpZ = 0f
                return
            }
        }
        axisRightX = rightX
        axisRightY = rightY
        axisRightZ = rightZ
        axisUpX = upX
        axisUpY = upY
        axisUpZ = upZ
    }

    private fun vertex(
        x: Float,
        y: Float,
        z: Float,
        localX: Float,
        localY: Float,
        color: Color,
        u: Float,
        v: Float,
    ) {
        putVertex(
            x + axisRightX * localX + axisUpX * localY,
            y + axisRightY * localX + axisUpY * localY,
            z + axisRightZ * localX + axisUpZ * localY,
            color.r,
            color.g,
            color.b,
            color.a,
            u,
            v,
        )
    }

    private fun putVertex(
        x: Float,
        y: Float,
        z: Float,
        r: Float,
        g: Float,
        b: Float,
        a: Float,
        u: Float,
        v: Float,
    ) {
        val o = vertexCount * FLOATS_PER_VERTEX
        vertices[o] = x
        vertices[o + 1] = y
        vertices[o + 2] = z
        vertices[o + 3] = r
        vertices[o + 4] = g
        vertices[o + 5] = b
        vertices[o + 6] = a
        vertices[o + 7] = u
        vertices[o + 8] = v
        vertexCount++
    }

    private fun quadIndices(base: Int) {
        putIndex(base)
        putIndex(base + 1)
        putIndex(base + 2)
        putIndex(base)
        putIndex(base + 2)
        putIndex(base + 3)
    }

    private fun putIndex(index: Int) {
        indices[indexCount++] = index
    }

    private fun ensureRoom(vertexAmount: Int, indexAmount: Int) {
        val neededVertices = (vertexCount + vertexAmount) * FLOATS_PER_VERTEX
        if (neededVertices > vertices.capacity) {
            val grown = FloatBuffer(max(vertices.capacity * 2, neededVertices))
            for (i in 0 until vertexCount * FLOATS_PER_VERTEX) grown[i] = vertices[i]
            vertices = grown
        }
        val neededIndices = indexCount + indexAmount
        if (neededIndices > indices.capacity) {
            val grown = IntBuffer(max(indices.capacity * 2, neededIndices))
            for (i in 0 until indexCount) grown[i] = indices[i]
            indices = grown
        }
    }

    private fun useDraw(texture: Texture, style: MsdfShader?) {
        val last = if (drawCount > 0) draws[drawCount - 1] else null
        if (
            last != null &&
                last.texture === texture &&
                last.style === style &&
                last.blend == blendState &&
                last.depthTest == depthTest
        ) {
            return
        }
        if (drawCount == draws.size) draws += DrawCall()
        val draw = draws[drawCount++]
        draw.texture = texture
        draw.style = style
        draw.blend = blendState
        draw.depthTest = depthTest
        draw.firstIndex = indexCount
        draw.indexCount = 0
    }

    private fun closeDraws() {
        for (i in 0 until drawCount) {
            val draw = draws[i]
            val end = if (i + 1 < drawCount) draws[i + 1].firstIndex else indexCount
            draw.indexCount = end - draw.firstIndex
        }
    }

    private fun distance(a: Int, b: Int): Float {
        val oa = a * STRIP_FLOATS
        val ob = b * STRIP_FLOATS
        val dx = strip[ob] - strip[oa]
        val dy = strip[ob + 1] - strip[oa + 1]
        val dz = strip[ob + 2] - strip[oa + 2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun worldPerPixel(x: Float, y: Float, z: Float): Float {
        val camera = camera ?: return 1f
        val vp = camera.viewProjection.data
        val w =
            if (camera is OrthographicCamera) 1f
            else max(vp[3] * x + vp[7] * y + vp[11] * z + vp[15], 0.0001f)
        val projectionY = camera.projection.data[5]
        val height = camera.virtualHeight
        if (projectionY == 0f || height <= 0f) return 1f
        return 2f * w / (projectionY * height)
    }

    private fun setRight(x: Float, y: Float, z: Float) {
        val l = sqrt(x * x + y * y + z * z).takeIf { it > 0f } ?: 1f
        rightX = x / l
        rightY = y / l
        rightZ = z / l
    }

    private fun setUp(x: Float, y: Float, z: Float) {
        val l = sqrt(x * x + y * y + z * z).takeIf { it > 0f } ?: 1f
        upX = x / l
        upY = y / l
        upZ = z / l
    }

    private fun setBack(x: Float, y: Float, z: Float) {
        val l = sqrt(x * x + y * y + z * z).takeIf { it > 0f } ?: 1f
        backX = x / l
        backY = y / l
        backZ = z / l
    }

    private fun checkDrawing() {
        check(drawing) { "BillboardBatch.begin must be called first." }
    }

    private fun nextFrameCheck() {
        val current = EngineStats.frame.value
        if (current == frame) return
        frame = current
        gpuVertexOffset = 0
        gpuIndexOffset = 0
        flushIndex = 0
        retired.forEach { it.release() }
        retired.clear()
    }

    private fun growGpuBuffers() {
        retired += vertexBuffer
        retired += indexBuffer
        vertexCapacity = max(vertexCapacity * 2, vertexCount * 2)
        indexCapacity = max(indexCapacity * 2, indexCount * 2)
        vertexBuffer = createVertexBuffer(vertexCapacity)
        indexBuffer = createIndexBuffer(indexCapacity)
        gpuVertexOffset = 0
        gpuIndexOffset = 0
    }

    private fun textureBindGroup(texture: Texture): BindGroup =
        textureBindGroups[texture.id]
            ?: device
                .createBindGroup(
                    BindGroupDescriptor(
                        textureLayout,
                        listOf(BindGroupEntry(0, texture.view), BindGroupEntry(1, texture.sampler)),
                        label = "BillboardBatch texture bind group",
                    )
                )
                .also { textureBindGroups[texture.id] = it }

    private fun styleBindGroup(style: MsdfShader): BindGroup =
        styleBindGroups[style.id]
            ?: device
                .createBindGroup(
                    BindGroupDescriptor(
                        styleLayout,
                        listOf(BindGroupEntry(0, BufferBinding(style.styleBuffer))),
                        label = "BillboardBatch msdf style bind group",
                    )
                )
                .also { styleBindGroups[style.id] = it }

    private fun pipeline(msdf: Boolean, blend: BlendState, depthTest: Boolean): RenderPipeline {
        val depth = depthFormat
        for (i in pipelines.indices) {
            val entry = pipelines[i]
            if (
                entry.msdf == msdf &&
                    entry.blend == blend &&
                    entry.depthTest == depthTest &&
                    entry.colorFormat == colorFormat &&
                    entry.depthFormat == depth &&
                    entry.sampleCount == sampleCount
            ) {
                return entry.pipeline
            }
        }
        val module = if (msdf) msdfShader else shader
        val pipeline =
            device.createRenderPipeline(
                RenderPipelineDescriptor(
                    layout = if (msdf) msdfPipelineLayout else pipelineLayout,
                    vertex =
                        VertexState(
                            module,
                            "vs_main",
                            listOf(
                                WebGPUVertexBufferLayout(
                                    VERTEX_BYTES.toLong(),
                                    VertexStepMode.VERTEX,
                                    listOf(
                                        WebGPUVertexAttribute(VertexFormat.FLOAT32x3, 0L, 0),
                                        WebGPUVertexAttribute(VertexFormat.FLOAT32x4, 12L, 1),
                                        WebGPUVertexAttribute(VertexFormat.FLOAT32x2, 28L, 2),
                                    ),
                                )
                            ),
                        ),
                    fragment =
                        FragmentState(module, "fs_main", listOf(ColorTargetState(colorFormat, blend))),
                    depthStencil =
                        depth?.let {
                            DepthStencilState(
                                format = it,
                                depthWriteEnabled = false,
                                depthCompare =
                                    if (depthTest) CompareFunction.LESS_EQUAL
                                    else CompareFunction.ALWAYS,
                            )
                        },
                    multisample = MultisampleState(count = sampleCount),
                    label = "BillboardBatch pipeline",
                )
            )
        pipelines += PipelineEntry(msdf, blend, depthTest, colorFormat, depth, sampleCount, pipeline)
        return pipeline
    }

    private fun createVertexBuffer(vertexCount: Int): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "BillboardBatch vertices",
                vertexCount.toLong() * VERTEX_BYTES,
                BufferUsage.VERTEX or BufferUsage.COPY_DST,
                false,
            )
        )

    private fun createIndexBuffer(indexCount: Int): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "BillboardBatch indices",
                indexCount.toLong() * Int.SIZE_BYTES,
                BufferUsage.INDEX or BufferUsage.COPY_DST,
                false,
            )
        )

    private fun createCameraBuffer(): GPUBuffer =
        device.createBuffer(
            BufferDescriptor(
                "BillboardBatch camera",
                maxFlushesPerFrame.toLong() * cameraStride,
                BufferUsage.UNIFORM or BufferUsage.COPY_DST,
                false,
            )
        )

    private fun createCameraBindGroup(): BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                cameraLayout,
                listOf(BindGroupEntry(0, BufferBinding(cameraBuffer, 0, 64L))),
                label = "BillboardBatch camera bind group",
            )
        )

    private fun recreateCameraBuffer() {
        retired += cameraBuffer
        cameraBindGroup.release()
        cameraBuffer = createCameraBuffer()
        cameraBindGroup = createCameraBindGroup()
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        pipelines.forEach { it.pipeline.release() }
        pipelines.clear()
        textureBindGroups.forEachValue { it.release() }
        textureBindGroups.clear()
        styleBindGroups.forEachValue { it.release() }
        styleBindGroups.clear()
        retired.forEach { it.release() }
        retired.clear()
        vertexBuffer.release()
        indexBuffer.release()
        cameraBindGroup.release()
        cameraBuffer.release()
        pipelineLayout.release()
        msdfPipelineLayout.release()
        shader.release()
        msdfShader.release()
        cameraLayout.release()
        textureLayout.release()
        styleLayout.release()
    }

    private class DrawCall {
        lateinit var texture: Texture
        var style: MsdfShader? = null
        var blend: BlendState = BlendState.NonPreMultiplied
        var depthTest = true
        var firstIndex = 0
        var indexCount = 0
    }

    private class PipelineEntry(
        val msdf: Boolean,
        val blend: BlendState,
        val depthTest: Boolean,
        val colorFormat: TextureFormat,
        val depthFormat: TextureFormat?,
        val sampleCount: Int,
        val pipeline: RenderPipeline,
    )

    private companion object {
        const val FLOATS_PER_VERTEX = 9
        const val VERTEX_BYTES = FLOATS_PER_VERTEX * 4
        const val STRIP_FLOATS = 8
        val logger = Logger<BillboardBatch>()

        const val VERTEX_WGSL =
            """
struct Camera {
    view_proj: mat4x4f,
};
@group(0) @binding(0) var<uniform> camera: Camera;
@group(1) @binding(0) var billboard_texture: texture_2d<f32>;
@group(1) @binding(1) var billboard_sampler: sampler;

struct VertexOutput {
    @builtin(position) position: vec4f,
    @location(0) color: vec4f,
    @location(1) uv: vec2f,
};

@vertex
fn vs_main(@location(0) position: vec3f, @location(1) color: vec4f, @location(2) uv: vec2f) -> VertexOutput {
    var output: VertexOutput;
    output.position = camera.view_proj * vec4f(position, 1.0);
    output.color = color;
    output.uv = uv;
    return output;
}
"""

        const val WGSL =
            VERTEX_WGSL +
                """
@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4f {
    let color = textureSample(billboard_texture, billboard_sampler, input.uv) * input.color;
    if (color.a <= 0.002) {
        discard;
    }
    return color;
}
"""

        const val MSDF_WGSL =
            VERTEX_WGSL +
                """
struct Style {
    params: vec4f,
    outline_color: vec4f,
    glow_color: vec4f,
};
@group(2) @binding(0) var<uniform> style: Style;

fn median(r: f32, g: f32, b: f32) -> f32 {
    return max(min(r, g), min(max(r, g), b));
}

@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4f {
    let s = textureSample(billboard_texture, billboard_sampler, input.uv);
    let unit_range = vec2f(style.params.x) / vec2f(textureDimensions(billboard_texture));
    let screen_size = vec2f(1.0) / fwidth(input.uv);
    let px_range = max(0.5 * dot(unit_range, screen_size), 1.0);
    let weight = style.params.y;
    let fill = clamp((median(s.r, s.g, s.b) - 0.5 + weight) * px_range + 0.5, 0.0, 1.0);
    var color = input.color.rgb;
    var alpha = fill * input.color.a;
    let outline_width = style.params.z;
    if (outline_width > 0.0) {
        let outline = clamp((s.a - 0.5 + weight + outline_width) * px_range + 0.5, 0.0, 1.0)
            * style.outline_color.a * input.color.a;
        color = mix(style.outline_color.rgb, input.color.rgb, fill);
        alpha = alpha + outline * (1.0 - alpha);
    }
    let glow_width = style.params.w;
    if (glow_width > 0.0) {
        let glow_distance = s.a - 0.5 + weight + outline_width;
        let glow = smoothstep(-glow_width, 0.0, glow_distance) * style.glow_color.a * input.color.a;
        let combined = alpha + glow * (1.0 - alpha);
        color = (color * alpha + style.glow_color.rgb * glow * (1.0 - alpha)) / max(combined, 0.0001);
        alpha = combined;
    }
    if (alpha <= 0.002) {
        discard;
    }
    return vec4f(color, alpha);
}
"""
    }
}

/**
 * How a [BillboardBatch] quad or text faces the camera.
 *
 * @author Colton Daily
 * @date 11/15/2026
 */
enum class BillboardMode {
    /** Faces the camera fully. */
    SPHERICAL,

    /** Stays upright and only turns around the world y axis, such as for trees & name plates. */
    AXIS_Y,
}
