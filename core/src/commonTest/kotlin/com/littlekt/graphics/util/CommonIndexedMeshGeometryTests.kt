package com.littlekt.graphics.util

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.VertexBufferLayout
import com.littlekt.graphics.calculateStride
import com.littlekt.graphics.webgpu.VertexFormat
import com.littlekt.graphics.webgpu.VertexStepMode
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonIndexedMeshGeometryTests {

    private fun geometry(): CommonIndexedMeshGeometry {
        val attributes =
            listOf(
                VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION),
                VertexAttribute(VertexFormat.FLOAT32x3, 12, 1, VertexAttrUsage.NORMAL),
                VertexAttribute(VertexFormat.FLOAT32x2, 24, 2, VertexAttrUsage.UV),
                VertexAttribute(VertexFormat.FLOAT32x4, 32, 3, VertexAttrUsage.TANGENT),
            )
        return CommonIndexedMeshGeometry(
            VertexBufferLayout(attributes.calculateStride().toLong(), VertexStepMode.VERTEX, attributes)
        )
    }

    private fun CommonIndexedMeshGeometry.floorQuad(uv: (x: Float, z: Float) -> Pair<Float, Float>) {
        val corners = listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f)
        corners.forEach { (x, z) ->
            addVertex {
                position.set(x, 0f, z)
                normal.set(0f, 1f, 0f)
                val (u, v) = uv(x, z)
                this.uv.set(u, v)
            }
        }
        addIndices(0, 2, 1, 0, 3, 2)
    }

    private fun CommonIndexedMeshGeometry.assertValidTangents() {
        for (i in 0 until numVertices) {
            val vertex = this[i]
            val t = vertex.tangent
            assertTrue(t.x.isFinite() && t.y.isFinite() && t.z.isFinite(), "tangent $i is $t")
            assertEquals(1f, kotlin.math.sqrt(t.x * t.x + t.y * t.y + t.z * t.z), 1e-4f)
            assertTrue(abs(t.x * vertex.normal.x + t.y * vertex.normal.y + t.z * vertex.normal.z) < 1e-4f)
            assertTrue(t.w == 1f || t.w == -1f, "tangent $i has a w of ${t.w}")
        }
    }

    @Test
    fun test_generate_tangents_keeps_default_handedness() {
        val geometry = geometry()
        geometry.floorQuad { x, z -> x to z }
        geometry.generateTangents()
        geometry.assertValidTangents()
        for (i in 0 until geometry.numVertices) {
            val t = geometry[i].tangent
            assertEquals(1f, t.x, 1e-4f)
            assertEquals(1f, t.w)
        }
    }

    @Test
    fun test_generate_tangents_flips_mirrored_uvs() {
        val geometry = geometry()
        geometry.floorQuad { x, z -> x to -z }
        geometry.generateTangents()
        geometry.assertValidTangents()
        for (i in 0 until geometry.numVertices) {
            assertEquals(-1f, geometry[i].tangent.w)
        }
    }

    @Test
    fun test_generate_tangents_without_uv_variation() {
        val geometry = geometry()
        geometry.floorQuad { x, _ -> x to 0f }
        geometry.generateTangents()
        geometry.assertValidTangents()

        val flat = geometry()
        flat.floorQuad { _, _ -> 0f to 0f }
        flat.generateTangents()
        flat.assertValidTangents()
    }
}
