package com.littlekt.graphics.g2d.shape

import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.math.Mat4

internal class BatchManager(private val batch: Batch, private val slice: TextureSlice) {

    var color: Color = Color.WHITE
    private var verts = FloatArray(3600)
    private var vertexCount = 0
    var pixelSize = 1f
    val halfPixelSize
        get() = pixelSize * 0.5f

    val offset
        get() = 0.001f * pixelSize

    private val verticesArrayIndex: Int
        get() = VERTEX_SIZE * vertexCount

    private val verticesRemaining: Int
        get() = (verts.size - QUAD_PUSH_SIZE * vertexCount) / VERTEX_SIZE

    var cachingDraws = false
        private set

    init {
        setTextureSliceUV()
    }

    fun startCaching(): Boolean {
        val wasCaching = cachingDraws
        cachingDraws = true
        return wasCaching
    }

    fun endCaching() {
        cachingDraws = false
        if (vertexCount > 0) {
            pushToBatch()
        }
    }

    fun pushVertex() {
        vertexCount++
    }

    fun pushQuad() {
        vertexCount += 4
    }

    fun pushTriangle() {
        x4(x3())
        y4(y3())
        pushQuad()
    }

    fun ensureSpaceForTriangle() {
        ensureSpace(4)
    }

    fun ensureSpaceForQuad() {
        ensureSpace(4)
    }

    fun ensureSpace(vertices: Int) {
        if (vertices * VERTEX_SIZE > verts.size) {
            increaseCacheSize(vertices * VERTEX_SIZE)
        } else if (verticesRemaining < vertices) {
            pushToBatch()
        }
    }

    fun increaseCacheSize(minSize: Int) {
        pushToBatch()
        var newSize = verts.size
        while (minSize > newSize) {
            newSize *= 2
        }
        verts = FloatArray(newSize)
        setTextureSliceUV()
    }

    fun pushToBatch() {
        if (vertexCount == 0) return
        batch.draw(slice.texture, verts, 0, verticesArrayIndex)
        vertexCount = 0
    }

    fun x1(x1: Float) {
        verts[verticesArrayIndex + X1] = x1
    }

    fun y1(y1: Float) {
        verts[verticesArrayIndex + Y1] = y1
    }

    fun x2(x2: Float) {
        verts[verticesArrayIndex + X2] = x2
    }

    fun y2(y2: Float) {
        verts[verticesArrayIndex + Y2] = y2
    }

    fun x3(x3: Float) {
        verts[verticesArrayIndex + X3] = x3
    }

    fun y3(y3: Float) {
        verts[verticesArrayIndex + Y3] = y3
    }

    fun x4(x4: Float) {
        verts[verticesArrayIndex + X4] = x4
    }

    fun y4(y4: Float) {
        verts[verticesArrayIndex + Y4] = y4
    }

    fun x1(): Float {
        return verts[verticesArrayIndex + X1]
    }

    fun y1(): Float {
        return verts[verticesArrayIndex + Y1]
    }

    fun x2(): Float {
        return verts[verticesArrayIndex + X2]
    }

    fun y2(): Float {
        return verts[verticesArrayIndex + Y2]
    }

    fun x3(): Float {
        return verts[verticesArrayIndex + X3]
    }

    fun y3(): Float {
        return verts[verticesArrayIndex + Y3]
    }

    fun x4(): Float {
        return verts[verticesArrayIndex + X4]
    }

    fun y4(): Float {
        return verts[verticesArrayIndex + Y4]
    }

    fun color1(color: Color) {
        color1r(color.r)
        color1g(color.g)
        color1b(color.b)
        color1a(color.a)
    }

    fun color1r(c: Float) {
        verts[verticesArrayIndex + C1R] = c
    }

    fun color1g(c: Float) {
        verts[verticesArrayIndex + C1G] = c
    }

    fun color1b(c: Float) {
        verts[verticesArrayIndex + C1B] = c
    }

    fun color1a(c: Float) {
        verts[verticesArrayIndex + C1A] = c
    }

    fun color2(color: Color) {
        color2r(color.r)
        color2g(color.g)
        color2b(color.b)
        color2a(color.a)
    }

    fun color2r(c: Float) {
        verts[verticesArrayIndex + C2R] = c
    }

    fun color2g(c: Float) {
        verts[verticesArrayIndex + C2G] = c
    }

    fun color2b(c: Float) {
        verts[verticesArrayIndex + C2B] = c
    }

    fun color2a(c: Float) {
        verts[verticesArrayIndex + C2A] = c
    }

    fun color3(color: Color) {
        color3r(color.r)
        color3g(color.g)
        color3b(color.b)
        color3a(color.a)
    }

    fun color3r(c: Float) {
        verts[verticesArrayIndex + C3R] = c
    }

    fun color3g(c: Float) {
        verts[verticesArrayIndex + C3G] = c
    }

    fun color3b(c: Float) {
        verts[verticesArrayIndex + C3B] = c
    }

    fun color3a(c: Float) {
        verts[verticesArrayIndex + C3A] = c
    }

    fun color4(color: Color) {
        color4r(color.r)
        color4g(color.g)
        color4b(color.b)
        color4a(color.a)
    }

    fun color4r(c: Float) {
        verts[verticesArrayIndex + C4R] = c
    }

    fun color4g(c: Float) {
        verts[verticesArrayIndex + C4G] = c
    }

    fun color4b(c: Float) {
        verts[verticesArrayIndex + C4B] = c
    }

    fun color4a(c: Float) {
        verts[verticesArrayIndex + C4A] = c
    }

    private fun setTextureSliceUV() {
        val u = 0.5f * (slice.u + slice.u1)
        val v = 0.5f * (slice.v + slice.v1)
        for (i in verts.indices step VERTEX_SIZE) {
            verts[i + U1] = u
            verts[i + V1] = v
        }
    }

    companion object {
        private const val VERTEX_SIZE = 9
        private const val QUAD_PUSH_SIZE = 4 * VERTEX_SIZE
        private val mat4 = Mat4()

        private const val X1 = 0
        private const val Y1 = 1
        private const val Z1 = 2
        private const val C1R = 3
        private const val C1G = 4
        private const val C1B = 5
        private const val C1A = 6
        private const val U1 = 7
        private const val V1 = 8
        private const val X2 = 9
        private const val Y2 = 10
        private const val Z2 = 11
        private const val C2R = 12
        private const val C2G = 13
        private const val C2B = 14
        private const val C2A = 15
        private const val U2 = 16
        private const val V2 = 17
        private const val X3 = 18
        private const val Y3 = 19
        private const val Z3 = 20
        private const val C3R = 21
        private const val C3G = 22
        private const val C3B = 23
        private const val C3A = 24
        private const val U3 = 25
        private const val V3 = 26
        private const val X4 = 27
        private const val Y4 = 28
        private const val Z4 = 29
        private const val C4R = 30
        private const val C4G = 31
        private const val C4B = 32
        private const val C4A = 33
        private const val U4 = 34
        private const val V4 = 35
    }
}
