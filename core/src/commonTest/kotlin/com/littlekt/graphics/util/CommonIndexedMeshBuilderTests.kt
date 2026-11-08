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

class CommonIndexedMeshBuilderTests {

    private fun geometry(): CommonIndexedMeshGeometry {
        val attributes =
            listOf(
                VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION),
                VertexAttribute(VertexFormat.FLOAT32x3, 12, 1, VertexAttrUsage.NORMAL),
                VertexAttribute(VertexFormat.FLOAT32x2, 24, 2, VertexAttrUsage.UV),
            )
        return CommonIndexedMeshGeometry(
            VertexBufferLayout(attributes.calculateStride().toLong(), VertexStepMode.VERTEX, attributes)
        )
    }

    @Test
    fun test_uv_sphere() {
        val geometry = geometry()
        CommonIndexedMeshBuilder(geometry).uvSphere {
            radius = 2f
            rings = 4
            sectors = 8
        }
        assertEquals(5 * 9, geometry.numVertices)
        assertEquals((4 * 8 - 8) * 6, geometry.numIndices)
        for (i in 0 until geometry.numVertices) {
            val vertex = geometry[i]
            assertTrue(abs(vertex.position.length() - 2f) < 1e-4f)
            assertTrue(abs(vertex.normal.length() - 1f) < 1e-4f)
        }
        assertEquals(-2f, geometry.bounds.min.y, 1e-4f)
        assertEquals(2f, geometry.bounds.max.y, 1e-4f)
    }
}
