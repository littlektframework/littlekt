package com.littlekt.mesh.layout

import com.littlekt.graphics.VertexAttrUsage
import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.webgpu.VertexFormat

/** A test list of vertex attributes that hits all the VertexAttrUsage types. */
val testVertexAttributes: List<VertexAttribute> = run {
    val vertexAttributes = mutableListOf<VertexAttribute>()

    var offset: Long = 0
    vertexAttributes += VertexAttribute(VertexFormat.FLOAT32x3, offset, 0, VertexAttrUsage.POSITION)
    offset += VertexFormat.FLOAT32x3.bytes
    vertexAttributes += VertexAttribute(VertexFormat.FLOAT32x3, offset, 1, VertexAttrUsage.NORMAL)
    offset += VertexFormat.FLOAT32x3.bytes

    vertexAttributes += VertexAttribute(VertexFormat.FLOAT32x4, offset, 2, VertexAttrUsage.COLOR)
    offset += VertexFormat.FLOAT32x4.bytes

    vertexAttributes += VertexAttribute(VertexFormat.FLOAT32x2, offset, 3, VertexAttrUsage.UV)
    offset += VertexFormat.FLOAT32x2.bytes

    vertexAttributes += VertexAttribute(VertexFormat.FLOAT32x4, offset, 4, VertexAttrUsage.TANGENT)
    offset += VertexFormat.FLOAT32x4.bytes

    vertexAttributes += VertexAttribute(VertexFormat.SINT32x4, offset, 5, VertexAttrUsage.JOINT)
    offset += VertexFormat.SINT32x4.bytes

    vertexAttributes += VertexAttribute(VertexFormat.FLOAT32x4, offset, 6, VertexAttrUsage.WEIGHT)
    offset += VertexFormat.FLOAT32x4.bytes
    vertexAttributes.toList()
}
