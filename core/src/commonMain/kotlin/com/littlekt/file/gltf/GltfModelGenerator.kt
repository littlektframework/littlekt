package com.littlekt.file.gltf

import com.littlekt.file.vfs.VfsFile
import com.littlekt.graphics.*
import com.littlekt.graphics.g3d.*
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.util.CommonIndexedMeshGeometry
import com.littlekt.graphics.util.IndexedMeshGeometry
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.graphics.webgpu.VertexFormat
import com.littlekt.graphics.webgpu.VertexStepMode
import com.littlekt.log.Logger
import com.littlekt.math.Mat4
import com.littlekt.math.Quaternion
import com.littlekt.util.align
import com.littlekt.util.datastructure.internal.threadSafeMutableMapOf

/**
 * Converts a [GltfData] to a [Model] ready for rendering. This will load underlying buffers and
 * textures.
 *
 * @param config the configuration to use when generating the [Model]. Defaults to
 *   [GltfLoaderPbrConfig].
 * @param preferredFormat the preferred [TextureFormat] to be used when loading the model texture.
 */
fun GltfData.toModel(
    config: GltfLoaderConfig = GltfLoaderPbrConfig(),
    preferredFormat: TextureFormat =
        if (root.vfs.context.graphics.preferredFormat.srgb) TextureFormat.RGBA8_UNORM_SRGB
        else TextureFormat.RGBA8_UNORM,
): Scene {
    return GltfModelGenerator(this)
        .toModel(config, root.vfs.context.graphics.device, preferredFormat, scenes[scene])
}

private class GltfModelGenerator(val gltfFile: GltfData) {
    val root: VfsFile = gltfFile.root
    val nodeCache = threadSafeMutableMapOf<GltfNode, Node3D>()

    // gltf material index to set of meshes using it
    val meshesByMaterial = threadSafeMutableMapOf<Int, MutableSet<Mesh<*>>>()

    // gltf material index to material
    val materialCache = threadSafeMutableMapOf<Int, Material>()
    val meshMaterials = threadSafeMutableMapOf<Mesh<*>, GltfMaterial?>()

    // gltf mesh index to model
    val modelCache = threadSafeMutableMapOf<Int, Model>()

    fun toModel(
        config: GltfLoaderConfig,
        device: Device,
        preferredFormat: TextureFormat,
        gltfScene: GltfScene,
    ): Scene {
        val scene = Scene().apply { name = gltfScene.name ?: "glTF scene" }
        gltfScene.nodeRefs.map { node ->
            scene += node.toNode(config, device, preferredFormat, scene, scene)
        }
        // createSkins(scene)
        // applySkins(scene)

        // mergeMeshesByMaterial()
        return scene
    }

    private fun createSkins(scene: Scene) {
        gltfFile.skins.forEach { skin ->
            val modelSkin = Skin()
            val invBinMats = skin.inverseBindMatrixAccessorRef?.let { GltfMat4Accessor(it) }
            if (invBinMats != null) {
                // first create skin nodes for specified nodes / transform groups
                val skinNodes = mutableMapOf<GltfNode, Skin.SkinNode>()
                skin.jointRefs.forEach { joint ->
                    val jointNode = nodeCache[joint]!!
                    val invBindMat = invBinMats.next()
                    val skinNode = Skin.SkinNode(jointNode, invBindMat)
                    modelSkin.nodes += skinNode
                    skinNodes[joint] = skinNode
                }

                // second create skin nodes hierarchy
                skin.jointRefs.forEach { joint ->
                    val skinNode = skinNodes[joint]
                    if (skinNode != null) {
                        joint.childRefs.forEach { child ->
                            val childNode = skinNodes[child]
                            childNode?.let { skinNode.addChild(it) }
                        }
                    }
                }
                scene.skins += modelSkin
            }
        }
    }

    private fun applySkin(scene: Scene) {
        //        scene.modelInstances.forEach {
        //            // apply skin
        //            if (skin >= 0) {
        //                modelSkin = scene.skins[skin]
        //                val skeletonRoot = gltfFile.skins[skin].skeleton
        //                if (skeletonRoot > 0) {
        //                    //     node -= visualInstance
        //                    //    modelNodes[gltfFile.nodes[skeletonRoot]]!! += mesh
        //                }
        //            }
        //        }
    }

    fun GltfNode.toNode(
        config: GltfLoaderConfig,
        device: Device,
        preferredFormat: TextureFormat,
        parent: Node3D,
        scene: Scene,
    ): Node3D {
        val nodeName = name ?: "node_${parent.childCount}"
        val meshRef = meshRef
        val model =
            if (meshRef != null) {
                modelCache.getOrPut(mesh) { createModel(config, device, preferredFormat, meshRef) }
            } else {
                null
            }
        val node =
            (if (model != null)
                    ModelInstance(model).also {
                        it.createVisualInstances()
                        scene.modelInstances += it
                    }
                else Node3D())
                .apply { name = nodeName }
        nodeCache[this] = node

        if (matrix.isNotEmpty()) {
            node.globalTransform = Mat4().set(matrix)
        } else {
            if (translation.isNotEmpty()) {
                node.translate(translation[0], translation[1], translation[2])
            }
            if (rotation.isNotEmpty()) {
                node.rotate(Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]))
            }
            if (scale.isNotEmpty()) {
                node.scaling(scale[0], scale[1], scale[2])
            }
        }

        childRefs.forEach { node += it.toNode(config, device, preferredFormat, node, scene) }
        return node
    }

    fun createModel(
        config: GltfLoaderConfig,
        device: Device,
        preferredFormat: TextureFormat,
        meshRef: GltfMesh,
    ): Model {
        val primitives =
            meshRef.primitives.mapIndexed { index, prim ->
                val geometry = prim.toGeometry(gltfFile.accessors)
                val mesh = IndexedMesh(device, geometry)

                meshesByMaterial.getOrPut(prim.material) { mutableSetOf() } += mesh
                meshMaterials[mesh] = prim.materialRef
                val material =
                    materialCache.getOrPut(prim.material) {
                        prim.materialRef?.let { gltfMaterial ->
                            config.materialStrategy.createMaterial(
                                config.modelConfig,
                                device,
                                preferredFormat,
                                gltfMaterial,
                                gltfFile,
                            )
                        } ?: config.fallbackMaterialStrategy.createMaterial(device)
                    }
                val indexFormat =
                    if (prim.indices >= 0)
                        gltfFile.accessors[prim.indices].componentType.toIndexFormat()
                    else null
                val meshPrimitive =
                    MeshPrimitive(mesh, material, prim.mode.toTopology(), indexFormat)

                // apply morph weights
                if (prim.targets.isNotEmpty()) {
                    //     mesh.morphWeights = FloatArray(prim.targets.sumOf { it.size })
                }

                meshPrimitive
            }
        return Model(primitives)
    }

    private fun GltfPrimitive.toGeometry(gltfAccessors: List<GltfAccessor>): IndexedMeshGeometry {
        val indexGltfAccessor = if (indices >= 0) gltfAccessors[indices] else null

        val positionGltfAccessor = attributes[GltfAttribute.Position]?.let { gltfAccessors[it] }
        val normalGltfAccessor = attributes[GltfAttribute.Normal]?.let { gltfAccessors[it] }
        val tangentGltfAccessor = attributes[GltfAttribute.Tangent]?.let { gltfAccessors[it] }
        val texCoordGltfAccessor = attributes[GltfAttribute.TexCoord0]?.let { gltfAccessors[it] }
        val colorGltfAccessor = attributes[GltfAttribute.Color0]?.let { gltfAccessors[it] }
        val jointGltfAccessor = attributes[GltfAttribute.Joints0]?.let { gltfAccessors[it] }
        val weightGltfAccessor = attributes[GltfAttribute.Weights0]?.let { gltfAccessors[it] }

        if (positionGltfAccessor == null) {
            logger.warn { "GltfPrimitive without position attribute." }
            return IndexedMeshGeometry(VertexBufferLayout(0, VertexStepMode.VERTEX, emptyList()), 0)
        }

        var generateTangents = false

        val vertexAttributes = mutableListOf<VertexAttribute>()

        var offset: Long = 0
        vertexAttributes +=
            VertexAttribute(VertexFormat.FLOAT32x3, offset, 0, VertexAttrUsage.POSITION)
        offset += VertexFormat.FLOAT32x3.bytes
        vertexAttributes +=
            VertexAttribute(VertexFormat.FLOAT32x3, offset, 1, VertexAttrUsage.NORMAL)
        offset += VertexFormat.FLOAT32x3.bytes

        if (colorGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x4, offset, 2, VertexAttrUsage.COLOR)
            offset += VertexFormat.FLOAT32x4.bytes
        }

        if (texCoordGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x2, offset, 3, VertexAttrUsage.UV)
            offset += VertexFormat.FLOAT32x2.bytes
        }
        if (tangentGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x4, offset, 4, VertexAttrUsage.TANGENT)
            offset += VertexFormat.FLOAT32x4.bytes
        } else if (materialRef?.normalTexture != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x4, offset, 4, VertexAttrUsage.TANGENT)
            offset += VertexFormat.FLOAT32x4.bytes
            generateTangents = true
        }
        if (jointGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.SINT32x4, offset, 5, VertexAttrUsage.JOINT)
            offset += VertexFormat.SINT32x4.bytes
        }
        if (weightGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x4, offset, 6, VertexAttrUsage.WEIGHT)
            offset += VertexFormat.FLOAT32x4.bytes
        }

        //        val morphAccessors = makeMorphTargetAccessors(gltfAccessors)
        //        attribs += morphAccessors.keys

        val positionAccessor = GltfVec3fAccessor(positionGltfAccessor)
        val normalAccessor =
            if (normalGltfAccessor != null) GltfVec3fAccessor(normalGltfAccessor) else null
        val tangentAccessor =
            if (tangentGltfAccessor != null) GltfVec4fAccessor(tangentGltfAccessor) else null
        val texCoordAccessor =
            if (texCoordGltfAccessor != null) GltfVec2fAccessor(texCoordGltfAccessor) else null
        val colorAccessor =
            if (colorGltfAccessor != null) GltfVec4fAccessor(colorGltfAccessor) else null
        val jointAccessor =
            if (jointGltfAccessor != null) GltfVec4iAccessor(jointGltfAccessor) else null
        val weightAccessor =
            if (weightGltfAccessor != null) GltfVec4fAccessor(weightGltfAccessor) else null

        val geometry =
            CommonIndexedMeshGeometry(
                layout =
                    VertexBufferLayout(
                        arrayStride = vertexAttributes.calculateStride().toLong(),
                        stepMode = VertexStepMode.VERTEX,
                        attributes = vertexAttributes,
                    ),
                size = indexGltfAccessor?.count?.align(4) ?: positionGltfAccessor.count.align(4),
            )

        repeat(positionGltfAccessor.count) {
            geometry.addVertex {
                positionAccessor.next(position)
                normalAccessor?.next(normal)
                tangentAccessor?.next(tangent)
                texCoordAccessor?.next(uv)
                colorAccessor?.next()?.let { col -> color.set(col) }
                jointAccessor?.next(joints)
                weightAccessor?.next(weights)
            }
        }
        if (indexGltfAccessor != null) {
            val indexAccessor = GltfIntAccessor(indexGltfAccessor)
            repeat(indexGltfAccessor.count) { geometry.addIndex(indexAccessor.next()) }
        } else {
            repeat(positionGltfAccessor.count) { i -> geometry.addIndex(i) }
        }

        if (generateTangents) {
            geometry.generateTangents()
        }

        return geometry
    }

    private fun GltfTextureInfo?.loadTexture(
        device: Device,
        preferredFormat: TextureFormat,
    ): Texture? = this?.getTexture(gltfFile, root, device, preferredFormat)

    companion object {
        private val logger = Logger<GltfModelGenerator>()
    }
}
