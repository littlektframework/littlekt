package com.littlekt.graphics

import com.littlekt.ContextListener
import com.littlekt.graphics.util.CommonIndexedMeshBuilder
import com.littlekt.graphics.util.CommonIndexedMeshGeometry
import io.ygdrasil.wgpu.Device
import io.ygdrasil.wgpu.VertexStepMode

/** Creates a new indexed mesh using the list of [VertexAttribute]. */
inline fun indexedMesh(
    device: Device,
    attributes: List<VertexAttribute>,
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {},
): IndexedMesh<CommonIndexedMeshGeometry> {
    val geometry =
        CommonIndexedMeshGeometry(
            VertexBufferLayout(
                attributes.calculateStride().toLong(),
                VertexStepMode.vertex,
                attributes
            ),
            size
        )
    geometry.indicesAsTri()
    geometry.generate()
    return IndexedMesh(device, geometry)
}

/**
 * Creates a new indexed mesh with [VertexAttrUsage.POSITION] and [VertexAttrUsage.COLOR]
 * attributes.
 */
fun colorIndexedMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {}
): IndexedMesh<CommonIndexedMeshGeometry> {
    return indexedMesh(
        device,
        listOf(
            VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION),
            VertexAttribute(
                VertexFormat.FLOAT32x4,
                VertexFormat.FLOAT32x3.bytes.toLong(),
                1,
                VertexAttrUsage.COLOR
            )
        ),
        size,
        generate
    )
}

/**
 * Creates a new indexed mesh with [VertexAttrUsage.POSITION], [VertexAttrUsage.COLOR], and
 * [VertexAttrUsage.TEX_COORDS] attributes.
 */
fun textureIndexedMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {}
): IndexedMesh<CommonIndexedMeshGeometry> {
    return indexedMesh(
        device,
        listOf(
            VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION),
            VertexAttribute(
                VertexFormat.FLOAT32x4,
                VertexFormat.FLOAT32x3.bytes.toLong(),
                1,
                VertexAttrUsage.COLOR
            ),
            VertexAttribute(
                VertexFormat.FLOAT32x2,
                VertexFormat.FLOAT32x4.bytes.toLong() + VertexFormat.FLOAT32x3.bytes.toLong(),
                2,
                VertexAttrUsage.TEX_COORDS
            )
        ),
        size,
        generate
    )
}

/** Creates a new indexed mesh with [VertexAttrUsage.POSITION] attribute. */
fun positionIndexedMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {}
): IndexedMesh<CommonIndexedMeshGeometry> {
    return indexedMesh(
        device,
        listOf(VertexAttribute(VertexFormat.FLOAT32x3, 0, 0, VertexAttrUsage.POSITION)),
        size,
        generate
    )
}

/** Creates a new indexed mesh using the list of [VertexAttribute]. */
fun <T : ContextListener> T.indexedMesh(
    attributes: List<VertexAttribute>,
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {},
): IndexedMesh<CommonIndexedMeshGeometry> {
    return indexedMesh(context.graphics.device, attributes, size, generate)
}

/**
 * Creates a new indexed mesh with [VertexAttrUsage.POSITION] and [VertexAttrUsage.COLOR]
 * attributes.
 */
fun <T : ContextListener> T.colorIndexedMesh(
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {}
): IndexedMesh<CommonIndexedMeshGeometry> {
    return colorIndexedMesh(context.graphics.device, size, generate)
}

/**
 * Creates a new indexed mesh with [VertexAttrUsage.POSITION], [VertexAttrUsage.COLOR], and
 * [VertexAttrUsage.TEX_COORDS] attributes.
 */
fun <T : ContextListener> T.textureIndexedMesh(
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {}
): IndexedMesh<CommonIndexedMeshGeometry> {
    return textureIndexedMesh(context.graphics.device, size, generate)
}

/** Update the current [IndexedMesh] geometry in a batch update. */
fun IndexedMesh<CommonIndexedMeshGeometry>.generate(
    generator: CommonIndexedMeshBuilder.() -> Unit
) {
    geometry.batchUpdate {
        clearVertices()
        CommonIndexedMeshBuilder(geometry, false).generator()
    }
}
