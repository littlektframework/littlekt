package com.littlekt.file.gltf

import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_COLOR_0
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_JOINTS_0
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_NORMAL
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_POSITION
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_TANGENT
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_TEXCOORD_0
import com.littlekt.file.gltf.GltfPrimitive.Companion.ATTRIBUTE_WEIGHTS_0
import com.littlekt.file.vfs.VfsFile
import com.littlekt.graphics.*
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
import com.littlekt.math.Vec4f

internal suspend fun GltfData.toModel(device: Device, file: VfsFile): Model {
    return GltfModelGenerator(this, file).toModel(device, scenes[scene])
}

private class GltfModelGenerator(val gltfFile: GltfData, val root: VfsFile) {
    val modelNodes = mutableMapOf<GltfNode, Node3D>()
    val meshesByMaterial = mutableMapOf<Int, MutableSet<Mesh<*>>>()
    val meshMaterials = mutableMapOf<Mesh<*>, GltfMaterial?>()

    fun toModel(device: Device, scene: GltfScene): Model {
        val model = Model().apply { name = scene.name ?: "model_scene" }
        scene.nodeRefs.forEach { nd -> model += nd.toNode(model) }

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
        val modelNdName = name ?: "node_${model.nodes.size}"
        val node = Node3D().apply { name = modelNdName }
        modelNodes[this] = node
        model.nodes[modelNdName] = node

        if (matrix.isNotEmpty()) {
            node.transform.set(matrix.map { it })
        } else {
            if (translation.isNotEmpty()) {
                node.translate(translation[0], translation[1], translation[2])
            }
            if (rotation.isNotEmpty()) {
                val rotMat =
                    Mat4().setToRotation(Vec4f(rotation[0], rotation[1], rotation[2], rotation[3]))
                node.transform.mul(rotMat)
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

            // apply skin
            if (skin >= 0) {
                //  mesh.skin = model.skins[skin]
                val skeletonRoot = gltfFile.skins[skin].skeleton
                if (skeletonRoot > 0) {
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
        val indexAccessor = if (indices >= 0) gltfAccessors[indices] else null

        val positionAcc = attributes[ATTRIBUTE_POSITION]?.let { gltfAccessors[it] }
        val normalAcc = attributes[ATTRIBUTE_NORMAL]?.let { gltfAccessors[it] }
        val tangentAcc = attributes[ATTRIBUTE_TANGENT]?.let { gltfAccessors[it] }
        val texCoordAcc = attributes[ATTRIBUTE_TEXCOORD_0]?.let { gltfAccessors[it] }
        val colorAcc = attributes[ATTRIBUTE_COLOR_0]?.let { gltfAccessors[it] }
        val jointAcc = attributes[ATTRIBUTE_JOINTS_0]?.let { gltfAccessors[it] }
        val weightAcc = attributes[ATTRIBUTE_WEIGHTS_0]?.let { gltfAccessors[it] }

        if (positionAcc == null) {
            logger.warn { "GltfPrimitive without position attribute." }
            return IndexedMeshGeometry(VertexBufferLayout(0, VertexStepMode.VERTEX, emptyList()), 0)
        }

        var generateTangents = false

        val attribs = mutableListOf<VertexAttribute>()

        var offset: Long = 0
        attribs += VertexAttribute(VertexFormat.FLOAT32x3, offset, 0, VertexAttrUsage.POSITION)
        offset += 3L * Float.SIZE_BYTES
        attribs += VertexAttribute(VertexFormat.FLOAT32x3, offset, 1, VertexAttrUsage.NORMAL)
        offset += 3L * Float.SIZE_BYTES

        if (colorAcc != null) {
            attribs += VertexAttribute(VertexFormat.FLOAT32x4, offset, 2, VertexAttrUsage.COLOR)
            offset += 4L * Float.SIZE_BYTES
        }
        //        if (cfg.setVertexAttribsFromMaterial) {
        //            attribs += Attribute.EMISSIVE_COLOR
        //            attribs += Attribute.METAL_ROUGH
        //        }
        if (texCoordAcc != null) {
            attribs +=
                VertexAttribute(VertexFormat.FLOAT32x2, offset, 3, VertexAttrUsage.TEX_COORDS)
            offset += 2L * Float.SIZE_BYTES
        }
        //        if (tangentAcc != null) {
        //            attribs += Attribute.TANGENTS
        //        } else if (materialRef?.normalTexture != null) {
        //            attribs += Attribute.TANGENTS
        //            generateTangents = true
        //        }
        if (jointAcc != null) {
            attribs += VertexAttribute(VertexFormat.FLOAT32x3, offset, 4, VertexAttrUsage.JOINT)
            offset += 3L * Float.SIZE_BYTES
        }
        if (weightAcc != null) {
            attribs += VertexAttribute(VertexFormat.FLOAT32x3, offset, 5, VertexAttrUsage.WEIGHT)
            offset += 3L * Float.SIZE_BYTES
        }

        //        val morphAccessors = makeMorphTargetAccessors(gltfAccessors)
        //        attribs += morphAccessors.keys

        val poss = GltfVec3fAccessor(positionAcc)
        val nrms = if (normalAcc != null) GltfVec3fAccessor(normalAcc) else null
        val tans = if (tangentAcc != null) GltfVec4fAccessor(tangentAcc) else null
        val texs = if (texCoordAcc != null) GltfVec2fAccessor(texCoordAcc) else null
        val cols = if (colorAcc != null) GltfVec4fAccessor(colorAcc) else null
        val jnts = if (jointAcc != null) GltfVec4iAccessor(jointAcc) else null
        val wgts = if (weightAcc != null) GltfVec4fAccessor(weightAcc) else null

        val geometry =
            CommonIndexedMeshGeometry(
                VertexBufferLayout(
                    attribs.calculateStride().toLong(),
                    VertexStepMode.VERTEX,
                    attribs,
                )
            )

        repeat(positionAcc.count) { i ->
            geometry.addVertex {
                poss.next(position)
                nrms?.next(normal)
                texs?.next(texCoords)
                cols?.next()?.let { col -> color.set(col) }
                jnts?.next(joints)
                wgts?.next(weights)
            }
        }
        if (indexAccessor != null) {
            val inds = GltfIntAccessor(indexAccessor)
            repeat(indexAccessor.count) { geometry.addIndex(inds.next()) }
        } else {
            repeat(positionAcc.count) { i -> geometry.addIndex(i) }
        }

        return geometry
    }

    companion object {
        private val logger = Logger<GltfModelGenerator>()
    }
}
