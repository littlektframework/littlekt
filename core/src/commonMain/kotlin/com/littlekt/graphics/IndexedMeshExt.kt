package com.littlekt.graphics

import com.littlekt.ContextListener
import com.littlekt.graphics.util.CommonIndexedMeshBuilder
import com.littlekt.graphics.util.CommonIndexedMeshGeometry
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.VertexFormat
import io.ygdrasil.webgpu.VertexStepMode
import io.ygdrasil.webgpu.sizeInBytes

/** Creates a new indexed mesh using the list of [VertexAttributeView]. */
inline fun indexedMesh(
    device: Device,
    attributes: List<VertexAttributeView>,
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {},
): IndexedMesh<CommonIndexedMeshGeometry> {
    val geometry =
        CommonIndexedMeshGeometry(
            VertexBufferLayoutView(
                attributes.calculateStride().toLong(),
                VertexStepMode.Vertex,
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

/** Creates a new indexed mesh with [VertexAttrUsage.POSITION] attribute. */
fun positionIndexedMesh(
    device: Device,
    size: Int = 1000,
    generate: CommonIndexedMeshGeometry.() -> Unit = {}
): IndexedMesh<CommonIndexedMeshGeometry> {
    return indexedMesh(
        device,
        listOf(VertexAttributeView(VertexFormat.Float32x3, 0, 0, VertexAttrUsage.POSITION)),
        size,
        generate
    )
}

/** Creates a new indexed mesh using the list of [VertexAttributeView]. */
fun <T : ContextListener> T.indexedMesh(
    attributes: List<VertexAttributeView>,
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
