package com.littlekt.file.gltf

import com.littlekt.file.vfs.VfsFile
import com.littlekt.graphics.*
import com.littlekt.graphics.g3d.*
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.material.UnlitMaterial
import com.littlekt.graphics.util.CommonIndexedMeshGeometry
import com.littlekt.graphics.util.IndexedMeshGeometry
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.graphics.webgpu.VertexFormat
import com.littlekt.graphics.webgpu.VertexStepMode
import com.littlekt.log.Logger
import com.littlekt.math.Mat4
import com.littlekt.math.Quaternion
import com.littlekt.math.Vec3f

/**
 * Converts a [GltfData] to a [Model] ready for rendering. This will load underlying buffers and
 * textures.
 *
 * @param config the configuration to use when generating the [Model]. Defaults to
 *   [GltfModelPbrConfig].
 * @param preferredFormat the preferred [TextureFormat] to be used when loading the model texture.
 */
suspend fun GltfData.toModel(
    config: GltfModelConfig = GltfModelPbrConfig(),
    preferredFormat: TextureFormat =
        if (root.vfs.context.graphics.preferredFormat.srgb) TextureFormat.RGBA8_UNORM_SRGB
        else TextureFormat.RGBA8_UNORM,
): Model {
    return GltfModelGenerator(this)
        .toModel(config, root.vfs.context.graphics.device, preferredFormat, scenes[scene])
}

private class GltfModelGenerator(val gltfFile: GltfData) {
    val root: VfsFile = gltfFile.root
    val modelNodes = mutableMapOf<GltfNode, Node3D>()
    val meshesByMaterial = mutableMapOf<Int, MutableSet<Mesh<*>>>()
    val meshMaterials = mutableMapOf<Mesh<*>, GltfMaterial?>()

    suspend fun toModel(
        config: GltfModelConfig,
        device: Device,
        preferredFormat: TextureFormat,
        scene: GltfScene,
    ): Model {
        val model = Model().apply { name = scene.name ?: "model_scene" }
        scene.nodeRefs.forEach { node -> model += node.toNode(model) }

        createSkins(model)
        modelNodes.forEach { (gltfNode, node) ->
            gltfNode.createMeshes(config, device, preferredFormat, model, node)
        }
        return model
    }

    private fun createSkins(model: Model) {
        gltfFile.skins.forEach { skin ->
            val modelSkin = Skin()
            val invBinMats = skin.inverseBindMatrixAccessorRef?.let { GltfMat4Accessor(it) }
            if (invBinMats != null) {
                // first create skin nodes for specified nodes / transform groups
                val skinNodes = mutableMapOf<GltfNode, Skin.SkinNode>()
                skin.jointRefs.forEach { joint ->
                    val jointNode = modelNodes[joint]!!
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
                model.skins += modelSkin
            }
        }
    }

    fun GltfNode.toNode(model: Model): Node3D {
        val nodeName = name ?: "node_${model.nodes.size}"
        val node = Node3D().apply { name = nodeName }
        modelNodes[this] = node
        model.nodes[nodeName] = node

        if (matrix.isNotEmpty()) {
            node.globalTransform = Mat4().set(matrix.map { it })
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

        childRefs.forEach { node += it.toNode(model) }
        return node
    }

    suspend fun GltfNode.createMeshes(
        config: GltfModelConfig,
        device: Device,
        preferredFormat: TextureFormat,
        model: Model,
        node: Node3D,
    ) {
        meshRef?.primitives?.forEachIndexed { index, prim ->
            val name = "${meshRef?.name ?: "${node.name}.mesh"}_$index"
            val geometry = prim.toGeometry(gltfFile.accessors)
            val mesh = IndexedMesh(device, geometry)

            meshesByMaterial.getOrPut(prim.material) { mutableSetOf() } += mesh
            meshMaterials[mesh] = prim.materialRef
            val material =
                prim.materialRef?.let { gltfMaterial ->
                    val baseColorFactor = gltfMaterial.pbrMetallicRoughness.baseColorFactor
                    if (config.pbr) {
                        PBRMaterial(
                            metallicFactor = gltfMaterial.pbrMetallicRoughness.metallicFactor,
                            roughnessFactor = gltfMaterial.pbrMetallicRoughness.roughnessFactor,
                            metallicRoughnessTexture =
                                gltfMaterial.pbrMetallicRoughness.metallicRoughnessTexture
                                    ?.loadTexture(device, preferredFormat),
                            normalTexture =
                                gltfMaterial.normalTexture?.loadTexture(device, preferredFormat),
                            emissiveFactor =
                                if (gltfMaterial.emissiveFactor.isNotEmpty()) {
                                    Vec3f(
                                        gltfMaterial.emissiveFactor[0],
                                        gltfMaterial.emissiveFactor[1],
                                        gltfMaterial.emissiveFactor[2],
                                    )
                                } else {
                                    Vec3f(0f)
                                },
                            emissiveTexture =
                                gltfMaterial.emissiveTexture.loadTexture(device, preferredFormat),
                            baseColorFactor =
                                Color(
                                    baseColorFactor[0],
                                    baseColorFactor[1],
                                    baseColorFactor[2],
                                    baseColorFactor[3],
                                ),
                            baseColorTexture =
                                gltfMaterial.pbrMetallicRoughness.baseColorTexture?.loadTexture(
                                    device,
                                    preferredFormat,
                                ) ?: EmptyTexture(device, preferredFormat, 0, 0),
                            transparent = gltfMaterial.alphaMode == GltfAlphaMode.Blend,
                            doubleSided = gltfMaterial.doubleSided,
                            alphaCutoff = gltfMaterial.alphaCutoff,
                            castShadows = config.castShadows,
                        )
                    } else {
                        UnlitMaterial(
                            baseColorFactor =
                                Color(
                                    baseColorFactor[0],
                                    baseColorFactor[1],
                                    baseColorFactor[2],
                                    baseColorFactor[3],
                                ),
                            baseColorTexture =
                                gltfMaterial.pbrMetallicRoughness.baseColorTexture?.loadTexture(
                                    device,
                                    preferredFormat,
                                ) ?: EmptyTexture(device, preferredFormat, 0, 0),
                        )
                    }
                } ?: UnlitMaterial(EmptyTexture(device, preferredFormat, 0, 0))
            val indexFormat =
                if (prim.indices >= 0)
                    gltfFile.accessors[prim.indices].componentType.toIndexFormat()
                else null
            val meshNode = MeshNode(mesh, material, prim.mode.toTopology(), indexFormat)
            node += meshNode
            // apply skin
            if (skin >= 0) {
                //  mesh.skin = model.skins[skin]
                val skeletonRoot = gltfFile.skins[skin].skeleton
                if (skeletonRoot > 0) {
                    //     node -= meshNode
                    //    modelNodes[gltfFile.nodes[skeletonRoot]]!! += mesh
                }
            }

            // apply morph weights
            if (prim.targets.isNotEmpty()) {
                //     mesh.morphWeights = FloatArray(prim.targets.sumOf { it.size })
            }

            model.meshes[name] = meshNode
        }
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

        //        if (colorGltfAccessor != null) {
        //            vertexAttributes +=
        //                VertexAttribute(VertexFormat.FLOAT32x4, offset, 2, VertexAttrUsage.COLOR)
        //            offset += 4L * Float.SIZE_BYTES
        //        }
        //        if (cfg.setVertexAttribsFromMaterial) {
        //            attribs += Attribute.EMISSIVE_COLOR
        //            attribs += Attribute.METAL_ROUGH
        //        }
        if (texCoordGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x2, offset, 2, VertexAttrUsage.TEX_COORDS)
            offset += VertexFormat.FLOAT32x2.bytes
        }
        //        if (tangentAcc != null) {
        //            attribs += Attribute.TANGENTS
        //        } else if (materialRef?.normalTexture != null) {
        //            attribs += Attribute.TANGENTS
        //            generateTangents = true
        //        }
        //        if (jointGltfAccessor != null) {
        //            vertexAttributes +=
        //                VertexAttribute(VertexFormat.SINT32x4, offset, 4, VertexAttrUsage.JOINT)
        //            offset += 4L * Int.SIZE_BYTES
        //        }
        //        if (weightGltfAccessor != null) {
        //            vertexAttributes +=
        //                VertexAttribute(VertexFormat.FLOAT32x4, offset, 5, VertexAttrUsage.WEIGHT)
        //            offset += 4L * Float.SIZE_BYTES
        //        }

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
                    )
            )

        repeat(positionGltfAccessor.count) { i ->
            geometry.addVertex {
                positionAccessor.next(position)
                normalAccessor?.next(normal)
                texCoordAccessor?.next(texCoords)
                //                colorAccessor?.next()?.let { col -> color.set(col) }
                //                jointAccessor?.next(joints)
                //                weightAccessor?.next(weights)
            }
        }
        if (indexGltfAccessor != null) {
            val indexAccessor = GltfIntAccessor(indexGltfAccessor)
            repeat(indexGltfAccessor.count) { geometry.addIndex(indexAccessor.next()) }
        } else {
            repeat(positionGltfAccessor.count) { i -> geometry.addIndex(i) }
        }

        return geometry
    }

    private suspend fun GltfTextureInfo?.loadTexture(
        device: Device,
        preferredFormat: TextureFormat,
    ): Texture? = this?.getTexture(gltfFile, root, device, preferredFormat)

    companion object {
        private val logger = Logger<GltfModelGenerator>()
    }
}
