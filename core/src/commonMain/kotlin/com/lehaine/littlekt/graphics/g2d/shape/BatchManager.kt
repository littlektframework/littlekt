package com.lehaine.littlekt.graphics.g2d.shape

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.g2d.Batch
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.math.Mat4

internal class BatchManager(private val batch: Batch, private val slice: TextureSlice) {

    var colorBits: Float = Color.WHITE.toFloatBits()
    private var verts = FloatArray(2000)
    private var vertexCount = 0
    var pixelSize = 1f
    val halfPixelSize get() = pixelSize * 0.5f
    val offset get() = 0.001f * pixelSize

    private val verticesArrayIndex: Int get() = VERTEX_SIZE * vertexCount
    private val verticesRemaining: Int get() = (verts.size - QUAD_PUSH_SIZE * vertexCount) / VERTEX_SIZE

    var cachingDraws = false
        private set

    init {
        setTextureSliceUV()
    }

    fun updatePixelSize(context: Context) {
        val trans = batch.transformMatrix
        val proj = batch.projectionMatrix
        mat4.set(proj).mul(trans)
        val scaleX = mat4.scaleX
        val worldWidth = 2f / scaleX
        val newPixelSize = worldWidth / context.graphics.width
        pixelSize = newPixelSize
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
        verts[verticesArrayIndex + Batch.X1] = x1
    }

    fun y1(y1: Float) {
        verts[verticesArrayIndex + Batch.Y1] = y1
    }

    fun x2(x2: Float) {
        verts[verticesArrayIndex + Batch.X2] = x2
    }

    fun y2(y2: Float) {
        verts[verticesArrayIndex + Batch.Y2] = y2
    }

    fun x3(x3: Float) {
        verts[verticesArrayIndex + Batch.X3] = x3
    }

    fun y3(y3: Float) {
        verts[verticesArrayIndex + Batch.Y3] = y3
    }

    fun x4(x4: Float) {
        verts[verticesArrayIndex + Batch.X4] = x4
    }

    fun y4(y4: Float) {
        verts[verticesArrayIndex + Batch.Y4] = y4
    }

    fun x1(): Float {
        return verts[verticesArrayIndex + Batch.X1]
    }

    fun y1(): Float {
        return verts[verticesArrayIndex + Batch.Y1]
    }

    fun x2(): Float {
        return verts[verticesArrayIndex + Batch.X2]
    }

    fun y2(): Float {
        return verts[verticesArrayIndex + Batch.Y2]
    }

    fun x3(): Float {
        return verts[verticesArrayIndex + Batch.X3]
    }

    fun y3(): Float {
        return verts[verticesArrayIndex + Batch.Y3]
    }

    fun x4(): Float {
        return verts[verticesArrayIndex + Batch.X4]
    }

    fun y4(): Float {
        return verts[verticesArrayIndex + Batch.Y4]
    }

    fun color1(c: Float) {
        verts[verticesArrayIndex + Batch.C1] = c
    }

    fun color2(c: Float) {
        verts[verticesArrayIndex + Batch.C2] = c
    }

    fun color3(c: Float) {
        verts[verticesArrayIndex + Batch.C3] = c
    }

    fun color4(c: Float) {
        verts[verticesArrayIndex + Batch.C4] = c
    }

    private fun setTextureSliceUV() {
        val u = 0.5f * (slice.u + slice.u2)
        val v = 0.5f * (slice.v + slice.v2)
        for (i in verts.indices step VERTEX_SIZE) {
            verts[i + Batch.U1] = u
            verts[i + Batch.V1] = v
        }
    }

    companion object {
        private const val VERTEX_SIZE = 5
        private const val QUAD_PUSH_SIZE = 4 * VERTEX_SIZE
        private val mat4 = Mat4()
    }
}