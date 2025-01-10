package com.littlekt.graphics

import com.littlekt.ContextListener
import com.littlekt.graphics.util.CommonMeshGeometry
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.VertexFormat
import com.littlekt.graphics.webgpu.VertexStepMode

/**
 * Creates a new mesh using the list of [VertexAttribute].
 *
 * @see indexedMesh
 */
inline fun mesh(
    device: Device,
    attributes: List<VertexAttribute>,
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {},
): Mesh<CommonMeshGeometry> {
    val geometry =
        CommonMeshGeometry(
            VertexBufferLayout(
                attributes.calculateStride().toLong(),
                VertexStepMode.VERTEX,
                attributes,
            ),
            size,
        )
    geometry.generate()
    return Mesh(device, geometry)
}

/**
 * Creates a mesh with [VertexAttrUsage.POSITION] and [VertexAttrUsage.COLOR] attributes.
 *
 * @see colorIndexedMesh
 */
fun colorMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit,
): Mesh<CommonMeshGeometry> {
    return mesh(
        device,
        listOf(
            VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION),
            VertexAttribute(
                VertexFormat.FLOAT32x4,
                VertexFormat.FLOAT32x3.bytes.toLong(),
                1,
                VertexAttrUsage.COLOR,
            ),
        ),
        size,
        generate,
    )
}

/**
 * Creates a mesh with [VertexAttrUsage.POSITION], [VertexAttrUsage.COLOR], and [VertexAttrUsage.UV]
 * attributes.
 *
 * @see textureIndexedMesh
 */
fun textureMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {},
): Mesh<CommonMeshGeometry> {
    return mesh(
        device,
        listOf(
            VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION),
            VertexAttribute(
                VertexFormat.FLOAT32x4,
                VertexFormat.FLOAT32x3.bytes.toLong(),
                1,
                VertexAttrUsage.COLOR,
            ),
            VertexAttribute(
                VertexFormat.FLOAT32x2,
                VertexFormat.FLOAT32x4.bytes.toLong() + VertexFormat.FLOAT32x3.bytes.toLong(),
                2,
                VertexAttrUsage.UV,
            ),
        ),
        size,
        generate,
    )
}

/**
 * Creates a mesh with [VertexAttrUsage.POSITION] attribute.
 *
 * @see positionIndexedMesh
 */
fun positionMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {},
): Mesh<CommonMeshGeometry> {
    return mesh(
        device,
        listOf(VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION)),
        size,
        generate,
    )
}

/**
 * Creates a new mesh using the list of [VertexAttribute].
 *
 * @see indexedMesh
 */
fun <T : ContextListener> T.mesh(
    attributes: List<VertexAttribute>,
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {},
): Mesh<CommonMeshGeometry> {
    return mesh(context.graphics.device, attributes, size, generate)
}

/**
 * Creates a mesh with [VertexAttrUsage.POSITION] and [VertexAttrUsage.COLOR] attributes.
 *
 * @see colorIndexedMesh
 */
fun <T : ContextListener> T.colorMesh(
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {},
): Mesh<CommonMeshGeometry> {
    return colorMesh(context.graphics.device, size, generate)
}

/**
 * Creates a mesh with [VertexAttrUsage.POSITION], [VertexAttrUsage.COLOR], and [VertexAttrUsage.UV]
 * attributes.
 *
 * @see textureIndexedMesh
 */
fun <T : ContextListener> T.textureMesh(
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {},
): Mesh<CommonMeshGeometry> {
    return textureMesh(context.graphics.device, size, generate)
}
