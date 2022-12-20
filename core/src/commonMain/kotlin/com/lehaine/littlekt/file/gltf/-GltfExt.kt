package com.lehaine.littlekt.file.gltf

import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_COLOR_0
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_JOINTS_0
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_NORMAL
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_POSITION
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_TANGENT
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_TEXCOORD_0
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_WEIGHTS_0
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.g3d.model.MeshNode
import com.lehaine.littlekt.graphics.g3d.model.Model
import com.lehaine.littlekt.graphics.g3d.model.Node
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.Vec4f


internal fun GltfFile.toModel(gl: GL): Model {
    return GltfModelGenerator(this, gl).toModel(scenes[scene])
}


private class GltfModelGenerator(val gltfFile: GltfFile, val gl: GL) {
    val modelAnimations = mutableListOf<GltfAnimation>()
    val modelNodes = mutableMapOf<GltfNode, Node>()
    val meshesByMaterial = mutableMapOf<Int, MutableSet<Mesh>>()
    val meshMaterials = mutableMapOf<Mesh, GltfMaterial?>()
    fun toModel(scene: GltfScene): Model {
        val model = Model(scene.name ?: "model_scene")
        scene.nodeRefs.forEach { nd -> model += nd.toNode(model) }
        // TODO create transition animations
        // TODO create skins
        modelNodes.forEach { (gltfNode, node) -> gltfNode.createMeshes(model, node) }
        // TODO create morph animations

        // TODO apply transforms
        // TODO merge meshes by material
        // TODO sort nodes by alpha
        // TODO remove empty nodes
        return model
    }

    fun GltfNode.toNode(model: Model): Node {
        val modelNdName = name ?: "node_${model.nodes.size}"
        val node = Node(name)
        modelNodes[this] = node
        model.nodes[modelNdName] = node

        if (matrix.isNotEmpty()) {
            node.transform.set(matrix.map { it })
        } else {
            if (translation.isNotEmpty()) {
                node.translate(translation[0], translation[1], translation[2])
            }
            if (rotation.isNotEmpty()) {
                val rotMat = Mat4().setToRotation(Vec4f(rotation[0], rotation[1], rotation[2], rotation[3]))
                node.transform.mul(rotMat)
            }
            if (scale.isNotEmpty()) {
                node.scale(scale[0], scale[1], scale[2])
            }
        }

        childRefs.forEach {
            node += it.toNode(model)
        }
        return node
    }

    fun GltfNode.createMeshes(model: Model, node: Node) {
        meshRef?.primitives?.forEachIndexed { index, prim ->
            val name = "${meshRef?.name ?: "${node.name}.mesh"}_$index"
            val geometry = prim.toGeometry(gltfFile.accessors)
            val mesh = meshFromGeometry(gl, geometry)
            node += MeshNode(mesh)
            meshesByMaterial.getOrPut(prim.material) { mutableSetOf() } += mesh
            meshMaterials[mesh] = prim.materialRef
            model.meshes[name] = mesh
        }
    }

    fun GltfMeshPrimitive.toGeometry(gltfAccessors: List<GltfAccessor>): MeshGeometry {
        val indexAccessor = if (indices >= 0) gltfAccessors[indices] else null

        val positionAcc = attributes[ATTRIBUTE_POSITION]?.let { gltfAccessors[it] }
        val normalAcc = attributes[ATTRIBUTE_NORMAL]?.let { gltfAccessors[it] }
        val tangentAcc = attributes[ATTRIBUTE_TANGENT]?.let { gltfAccessors[it] }
        val texCoordAcc = attributes[ATTRIBUTE_TEXCOORD_0]?.let { gltfAccessors[it] }
        val colorAcc = attributes[ATTRIBUTE_COLOR_0]?.let { gltfAccessors[it] }
        val jointAcc = attributes[ATTRIBUTE_JOINTS_0]?.let { gltfAccessors[it] }
        val weightAcc = attributes[ATTRIBUTE_WEIGHTS_0]?.let { gltfAccessors[it] }


        if (positionAcc == null) {
            logger.warn { "GltfMeshPrimitive without position attribute" }
            // return IndexedVertexList()
            return MeshGeometry(0, VertexAttributes(emptyList()))
        }


        var generateTangents = false

        val attribs = mutableListOf<VertexAttribute>()

        // for PbrShader positions and normals are always required
        attribs += VertexAttribute.POSITION_VEC3
        attribs += VertexAttribute.NORMAL

        if (colorAcc != null) {
       //     attribs += VertexAttribute.COLOR_PACKED
        }
//        if (cfg.setVertexAttribsFromMaterial) {
//            attribs += Attribute.EMISSIVE_COLOR
//            attribs += Attribute.METAL_ROUGH
//        }
//        if (texCoordAcc != null) {
//            attribs += Attribute.TEXTURE_COORDS
//        }
//        if (tangentAcc != null) {
//            attribs += Attribute.TANGENTS
//        } else if (materialRef?.normalTexture != null) {
//            attribs += Attribute.TANGENTS
//            generateTangents = true
//        }
//        if (jointAcc != null) {
//            attribs += Attribute.JOINTS
//        }
//        if (weightAcc != null) {
//            attribs += Attribute.WEIGHTS
//        }

//        val morphAccessors = makeMorphTargetAccessors(gltfAccessors)
//        attribs += morphAccessors.keys

        val attributes = VertexAttributes(attribs)

//        val verts = IndexedVertexList(attribs)
        val poss = Vec3fAccessor(positionAcc)
        val nrms = if (normalAcc != null) Vec3fAccessor(normalAcc) else null
        val tans = if (tangentAcc != null) Vec4fAccessor(tangentAcc) else null
        val texs = if (texCoordAcc != null) Vec2fAccessor(texCoordAcc) else null
        val cols = if (colorAcc != null) Vec4fAccessor(colorAcc) else null
        val jnts = if (jointAcc != null) Vec4iAccessor(jointAcc) else null
        val wgts = if (weightAcc != null) Vec4fAccessor(weightAcc) else null

        val meshGeometry = MeshGeometry(positionAcc.count, attributes)
        for (i in 0 until positionAcc.count) {
            poss.next().let {
                meshGeometry.add(Vec3f(it.z, it.y, it.x))
            }
            cols?.next()?.let {
           //     meshGeometry.add(it)
            }
            nrms?.next()?.let {
                meshGeometry.add(it)
            }
//            texs?.next()?.let { batcher.add(it) }
//            cols?.next()?.let { batcher.add(it) }
        }

//        for (i in 0 until positionAcc.count) {
//            verts.addVertex {
//                poss.next(position)
//                nrms?.next(normal)
//                tans?.next(tangent)
//                texs?.next(texCoord)
//                cols?.next()?.let { col -> color.set(col) }
//                jnts?.next(joints)
//                wgts?.next(weights)
//
//                if (cfg.setVertexAttribsFromMaterial) {
//                    metallicRoughness.set(0f, 0.5f)
//                    materialRef?.let { mat ->
//                        val col = mat.pbrMetallicRoughness.baseColorFactor
//                        if (col.size == 4) {
//                            color.set(col[0], col[1], col[2], col[3])
//                        }
//                        metallicRoughness.set(
//                            mat.pbrMetallicRoughness.metallicFactor,
//                            mat.pbrMetallicRoughness.roughnessFactor
//                        )
//                        mat.emissiveFactor?.let { emissiveCol ->
//                            if (emissiveCol.size >= 3) {
//                                emissiveColor.set(emissiveCol[0], emissiveCol[1], emissiveCol[2])
//                            }
//                        }
//                    }
//                }
//
//                morphAccessors.forEach { (attrib, acc) ->
//                    getVec3fAttribute(attrib)?.let { acc.next(it) }
//                }
//            }
//        }
//
        if (indexAccessor != null) {
            val inds = IntAccessor(indexAccessor)
            for (i in 0 until indexAccessor.count) {
                meshGeometry.addIndex(inds.next())
            }
        } else {
            for (i in 0 until positionAcc.count) {
                meshGeometry.addIndex(i)
            }
        }

//        if (generateTangents) {
//            verts.generateTangents()
//        }
//        if (cfg.generateNormals || normalAcc == null) {
//            verts.generateNormals()
//        }
        return meshGeometry
    }

    companion object {
        private val logger = Logger<GltfModelGenerator>()
    }
}
