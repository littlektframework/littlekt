package com.littlekt.graphics

import com.littlekt.ContextListener
import com.littlekt.graphics.util.CommonMeshGeometry
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.VertexFormat
import io.ygdrasil.webgpu.VertexStepMode
import io.ygdrasil.webgpu.sizeInBytes

/**
 * Creates a new mesh using the list of [VertexAttributeView].
 *
 * @see indexedMesh
 */
inline fun mesh(
    device: Device,
    attributes: List<VertexAttributeView>,
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {},
): Mesh<CommonMeshGeometry> {
    val geometry =
        CommonMeshGeometry(
            VertexBufferLayoutView(
                attributes.calculateStride().toLong(),
                VertexStepMode.Vertex,
                attributes
            ),
            size
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
    generate: CommonMeshGeometry.() -> Unit
): Mesh<CommonMeshGeometry> {
    return mesh(
        device,
        listOf(
            VertexAttributeView(VertexFormat.Float32x3, 0, 0, VertexAttrUsage.POSITION),
            VertexAttributeView(
                VertexFormat.Float32x4,
                VertexFormat.Float32x3.sizeInBytes().toLong(),
                1,
                VertexAttrUsage.COLOR
            )
        ),
        size,
        generate
    )
}

/**
 * Creates a mesh with [VertexAttrUsage.POSITION], [VertexAttrUsage.COLOR], and
 * [VertexAttrUsage.TEX_COORDS] attributes.
 *
 * @see textureIndexedMesh
 */
fun textureMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {}
): Mesh<CommonMeshGeometry> {
    return mesh(
        device,
        listOf(
            VertexAttributeView(VertexFormat.Float32x3, 0, 0, VertexAttrUsage.POSITION),
            VertexAttributeView(
                VertexFormat.Float32x4,
                VertexFormat.Float32x3.sizeInBytes().toLong(),
                1,
                VertexAttrUsage.COLOR
            ),
            VertexAttributeView(
                VertexFormat.Float32x2,
                VertexFormat.Float32x4.sizeInBytes().toLong() + VertexFormat.Float32x3.sizeInBytes().toLong(),
                2,
                VertexAttrUsage.TEX_COORDS
            )
        ),
        size,
        generate
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
    generate: CommonMeshGeometry.() -> Unit = {}
): Mesh<CommonMeshGeometry> {
    return mesh(
        device,
        listOf(VertexAttributeView(VertexFormat.Float32x3, 0, 0, VertexAttrUsage.POSITION)),
        size,
        generate
    )
}

/**
 * Creates a new mesh using the list of [VertexAttributeView].
 *
 * @see indexedMesh
 */
fun <T : ContextListener> T.mesh(
    attributes: List<VertexAttributeView>,
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
    generate: CommonMeshGeometry.() -> Unit = {}
): Mesh<CommonMeshGeometry> {
    return colorMesh(context.graphics.device, size, generate)
}

/**
 * Creates a mesh with [VertexAttrUsage.POSITION], [VertexAttrUsage.COLOR], and
 * [VertexAttrUsage.TEX_COORDS] attributes.
 *
 * @see textureIndexedMesh
 */
fun <T : ContextListener> T.textureMesh(
    size: Int = 1000,
    generate: CommonMeshGeometry.() -> Unit = {}
): Mesh<CommonMeshGeometry> {
    return textureMesh(context.graphics.device, size, generate)
}
