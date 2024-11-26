package com.littlekt.file.gltf

import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_COLOR_0
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_JOINTS_0
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_NORMAL
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_POSITION
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_TANGENT
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_TEXCOORD_0
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_WEIGHTS_0
import com.littlekt.graphics.*
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.Model
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.Skin
import com.littlekt.graphics.util.CommonIndexedMeshGeometry
import com.littlekt.graphics.util.IndexedMeshGeometry
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.VertexFormat
import com.littlekt.graphics.webgpu.VertexStepMode
import com.littlekt.log.Logger
import com.littlekt.math.Mat4
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec4f

suspend fun GltfData.toModel(device: Device): Model {
    return GltfModelGenerator(this).toModel(device, scenes[scene])
}

private class GltfModelGenerator(val gltfFile: GltfData) {
    val modelNodes = mutableMapOf<GltfNode, Node3D>()
    val meshesByMaterial = mutableMapOf<Int, MutableSet<Mesh<*>>>()
    val meshMaterials = mutableMapOf<Mesh<*>, GltfMaterial?>()

    fun toModel(device: Device, scene: GltfScene): Model {
        val model = Model().apply { name = scene.name ?: "model_scene" }
        scene.nodeRefs.forEach { node -> model += node.toNode(model) }

        createSkins(model)
        modelNodes.forEach { (gltfNode, node) -> gltfNode.createMeshes(device, model, node) }
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
            node.globalTransform =
                Mat4().set(matrix.map { it }).also {
                    it.getTranslation(MutableVec3f()).let { println(it) }
                }
            println(node.globalPosition)
        } else {
            if (translation.isNotEmpty()) {
                node.translate(translation[0], translation[1], translation[2])
            }
            if (rotation.isNotEmpty()) {
                node.rotate(Vec4f(rotation[0], rotation[1], rotation[2], rotation[3]))
            }
            if (scale.isNotEmpty()) {
                node.scaling(scale[0], scale[1], scale[2])
            }
        }

        childRefs.forEach { node += it.toNode(model) }
        return node
    }

    fun GltfNode.createMeshes(device: Device, model: Model, node: Node3D) {
        meshRef?.primitives?.forEachIndexed { index, prim ->
            val name = "${meshRef?.name ?: "${node.name}.mesh"}_$index"
            val geometry = prim.toGeometry(gltfFile.accessors)
            val mesh = IndexedMesh(device, geometry)

            meshesByMaterial.getOrPut(prim.material) { mutableSetOf() } += mesh
            meshMaterials[mesh] = prim.materialRef
            val meshNode = MeshNode(mesh)
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

            val useVertexColor = prim.attributes.containsKey(ATTRIBUTE_COLOR_0)

            model.meshes[name] = mesh
        }
    }

    private fun GltfPrimitive.toGeometry(gltfAccessors: List<GltfAccessor>): IndexedMeshGeometry {
        val indexGltfAccessor = if (indices >= 0) gltfAccessors[indices] else null

        val positionGltfAccessor = attributes[ATTRIBUTE_POSITION]?.let { gltfAccessors[it] }
        val normalGltfAccessor = attributes[ATTRIBUTE_NORMAL]?.let { gltfAccessors[it] }
        val tangentGltfAccessor = attributes[ATTRIBUTE_TANGENT]?.let { gltfAccessors[it] }
        val texCoordGltfAccessor = attributes[ATTRIBUTE_TEXCOORD_0]?.let { gltfAccessors[it] }
        val colorGltfAccessor = attributes[ATTRIBUTE_COLOR_0]?.let { gltfAccessors[it] }
        val jointGltfAccessor = attributes[ATTRIBUTE_JOINTS_0]?.let { gltfAccessors[it] }
        val weightGltfAccessor = attributes[ATTRIBUTE_WEIGHTS_0]?.let { gltfAccessors[it] }

        if (positionGltfAccessor == null) {
            logger.warn { "GltfPrimitive without position attribute." }
            return IndexedMeshGeometry(VertexBufferLayout(0, VertexStepMode.VERTEX, emptyList()), 0)
        }

        var generateTangents = false

        val vertexAttributes = mutableListOf<VertexAttribute>()

        var offset: Long = 0
        vertexAttributes +=
            VertexAttribute(VertexFormat.FLOAT32x3, offset, 0, VertexAttrUsage.POSITION)
        offset += 3L * Float.SIZE_BYTES
        vertexAttributes +=
            VertexAttribute(VertexFormat.FLOAT32x3, offset, 1, VertexAttrUsage.NORMAL)
        offset += 3L * Float.SIZE_BYTES

        if (colorGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x4, offset, 2, VertexAttrUsage.COLOR)
            offset += 4L * Float.SIZE_BYTES
        }
        //        if (cfg.setVertexAttribsFromMaterial) {
        //            attribs += Attribute.EMISSIVE_COLOR
        //            attribs += Attribute.METAL_ROUGH
        //        }
        if (texCoordGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x2, offset, 3, VertexAttrUsage.TEX_COORDS)
            offset += 2L * Float.SIZE_BYTES
        }
        //        if (tangentAcc != null) {
        //            attribs += Attribute.TANGENTS
        //        } else if (materialRef?.normalTexture != null) {
        //            attribs += Attribute.TANGENTS
        //            generateTangents = true
        //        }
        if (jointGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.SINT32x4, offset, 4, VertexAttrUsage.JOINT)
            offset += 4L * Int.SIZE_BYTES
        }
        if (weightGltfAccessor != null) {
            vertexAttributes +=
                VertexAttribute(VertexFormat.FLOAT32x4, offset, 5, VertexAttrUsage.WEIGHT)
            offset += 4L * Float.SIZE_BYTES
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
                VertexBufferLayout(
                    vertexAttributes.calculateStride().toLong(),
                    VertexStepMode.VERTEX,
                    vertexAttributes,
                )
            )

        repeat(positionGltfAccessor.count) { i ->
            geometry.addVertex {
                positionAccessor.next(position)
                normalAccessor?.next(normal)
                texCoordAccessor?.next(texCoords)
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

        return geometry
    }

    companion object {
        private val logger = Logger<GltfModelGenerator>()
    }
}
