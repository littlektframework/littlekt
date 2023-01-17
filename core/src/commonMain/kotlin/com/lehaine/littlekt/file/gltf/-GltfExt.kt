package com.lehaine.littlekt.file.gltf

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.async.newSingleThreadAsyncContext
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_COLOR_0
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_JOINTS_0
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_NORMAL
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_POSITION
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_TANGENT
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_TEXCOORD_0
import com.lehaine.littlekt.file.gltf.GltfMeshPrimitive.Companion.ATTRIBUTE_WEIGHTS_0
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graph.node.node3d.MeshNode
import com.lehaine.littlekt.graph.node.node3d.Model
import com.lehaine.littlekt.graph.node.node3d.Node3D
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.Mesh
import com.lehaine.littlekt.graphics.VertexAttribute
import com.lehaine.littlekt.graphics.VertexAttributes
import com.lehaine.littlekt.graphics.g3d.model.*
import com.lehaine.littlekt.graphics.gl.Usage
import com.lehaine.littlekt.graphics.util.MeshGeometry
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Mat4
import com.lehaine.littlekt.math.Vec4f
import kotlinx.coroutines.launch
import kotlin.math.min


internal suspend fun GltfFile.toModel(
    context: Context,
    gl: GL,
    file: VfsFile,
    loadTextureAsynchronously: Boolean,
): Model {
    return GltfModelGenerator(context, this, gl, file).toModel(scenes[scene], loadTextureAsynchronously)
}


private class GltfModelGenerator(val context: Context, val gltfFile: GltfFile, val gl: GL, val root: VfsFile) {
    val modelAnimations = mutableListOf<Animation>()
    val modelNodes = mutableMapOf<GltfNode, Node3D>()
    val meshesByMaterial = mutableMapOf<Int, MutableSet<MeshNode>>()
    val meshMaterials = mutableMapOf<MeshNode, GltfMaterial?>()
    suspend fun toModel(scene: GltfScene, loadTextureAsynchronously: Boolean): Model {
        val model = Model().apply { name = scene.name ?: "model_scene" }
        scene.nodeRefs.forEach { nd -> model += nd.toNode(model) }

        createAnimations()
        createSkins(model)
        modelNodes.forEach { (gltfNode, node) ->
            gltfNode.createMeshes(model, node, loadTextureAsynchronously)
        }
        createMorphAnimations()

        modelAnimations.filter { it.channels.isNotEmpty() }.forEach { modelAnim ->
            modelAnim.prepareAnimation()
            model.animations += modelAnim
        }
        model.disableAllAnimations()

        // TODO apply transforms if animations are empty
        // TODO merge meshes by material
        return model
    }

    fun createAnimations() {
        gltfFile.animations.forEach { anim ->
            val modelAnim = Animation(anim.name)
            modelAnimations += modelAnim

            val animNodes = mutableMapOf<Node3D, AnimationNode>()
            anim.channels.forEach { channel ->
                val gltfNode = modelNodes[channel.target.nodeRef]
                if (gltfNode != null) {
                    val animationNode = animNodes.getOrPut(gltfNode) { AnimatedTransformGroup(gltfNode) }
                    when (channel.target.path) {
                        GltfAnimationChannelTarget.PATH_TRANSLATION -> createTranslationAnimation(
                            channel, animationNode, modelAnim
                        )

                        GltfAnimationChannelTarget.PATH_ROTATION -> createRotationAnimation(
                            channel, animationNode, modelAnim
                        )

                        GltfAnimationChannelTarget.PATH_SCALE -> createScaleAnimation(channel, animationNode, modelAnim)
                    }
                }

            }
        }
    }

    private fun createTranslationAnimation(
        channel: GltfAnimationChannel,
        animationNode: AnimationNode,
        modelAnim: Animation,
    ) {
        val inputAccessor = channel.samplerRef.inputAccessorRef
        val outputAccessor = channel.samplerRef.outputAccessorRef

        if (inputAccessor.type != GltfAccessor.TYPE_SCALAR || inputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported translation animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}." }
            return
        }

        if (outputAccessor.type != GltfAccessor.TYPE_VEC3 || outputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported translation animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}." }
            return
        }

        val transChannel = TranslationAnimationChannel("${modelAnim.name}_translation", animationNode)
        val interpolation = when (channel.samplerRef.interpolation) {
            GltfAnimationSampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
            GltfAnimationSampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
            else -> AnimationKey.Interpolation.LINEAR
        }

        modelAnim.channels += transChannel

        val inTime = FloatAccessor(inputAccessor)
        val outTranslation = Vec3fAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val transKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                val startTan = outTranslation.next()
                val point = outTranslation.next()
                val endTan = outTranslation.next()
                CubicTranslationKey(t, point, startTan, endTan)
            } else {
                TranslationKey(t, outTranslation.next())
            }
            transKey.interpolation = interpolation
            transChannel.keys[t] = transKey
        }
    }


    private fun createRotationAnimation(
        channel: GltfAnimationChannel,
        animationNode: AnimationNode,
        modelAnim: Animation,
    ) {
        val inputAccessor = channel.samplerRef.inputAccessorRef
        val outputAccessor = channel.samplerRef.outputAccessorRef

        if (inputAccessor.type != GltfAccessor.TYPE_SCALAR || inputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported rotation animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}." }
            return
        }

        if (outputAccessor.type != GltfAccessor.TYPE_VEC4 || outputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported rotation animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}." }
            return
        }

        val rotChannel = RotationAnimationChannel("${modelAnim.name}_rotation", animationNode)
        val interpolation = when (channel.samplerRef.interpolation) {
            GltfAnimationSampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
            GltfAnimationSampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
            else -> AnimationKey.Interpolation.LINEAR
        }

        modelAnim.channels += rotChannel

        val inTime = FloatAccessor(inputAccessor)
        val outRotation = Vec4fAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val rotKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                val startTan = outRotation.next()
                val point = outRotation.next()
                val endTan = outRotation.next()
                CubicRotationKey(t, point, startTan, endTan)
            } else {
                RotationKey(t, outRotation.next())
            }
            rotKey.interpolation = interpolation
            rotChannel.keys[t] = rotKey
        }
    }

    private fun createScaleAnimation(
        channel: GltfAnimationChannel,
        animationNode: AnimationNode,
        modelAnim: Animation,
    ) {
        val inputAccessor = channel.samplerRef.inputAccessorRef
        val outputAccessor = channel.samplerRef.outputAccessorRef

        if (inputAccessor.type != GltfAccessor.TYPE_SCALAR || inputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported scale animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}." }
            return
        }

        if (outputAccessor.type != GltfAccessor.TYPE_VEC3 || outputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported scale animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}." }
            return
        }

        val scaleChannel = ScaleAnimationChannel("${modelAnim.name}_scale", animationNode)
        val interpolation = when (channel.samplerRef.interpolation) {
            GltfAnimationSampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
            GltfAnimationSampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
            else -> AnimationKey.Interpolation.LINEAR
        }

        modelAnim.channels += scaleChannel

        val inTime = FloatAccessor(inputAccessor)
        val outScale = Vec3fAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val scaleKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                val startTan = outScale.next()
                val point = outScale.next()
                val endTan = outScale.next()
                CubicScaleKey(t, point, startTan, endTan)
            } else {
                ScaleKey(t, outScale.next())
            }
            scaleKey.interpolation = interpolation
            scaleChannel.keys[t] = scaleKey
        }
    }

    private fun createMorphAnimations() {
        gltfFile.animations.forEachIndexed { i, anim ->
            anim.channels.forEach { channel ->
                if (channel.target.path == GltfAnimationChannelTarget.PATH_WEIGHTS) {
                    val modelAnim = modelAnimations[i]
                    val mesh = channel.target.nodeRef?.meshRef
                    val node = modelNodes[channel.target.nodeRef]
                    node?.children?.filterIsInstance<MeshNode>()?.forEach {
                        createWeightAnimation(mesh!!, channel, MorphAnimatedMesh(it), modelAnim)
                    }
                }
            }
        }
    }

    private fun createWeightAnimation(
        mesh: GltfMesh,
        channel: GltfAnimationChannel,
        morphAnimatedMesh: MorphAnimatedMesh,
        modelAnim: Animation,
    ) {
        val inputAccessor = channel.samplerRef.inputAccessorRef
        val outputAccessor = channel.samplerRef.outputAccessorRef

        if (inputAccessor.type != GltfAccessor.TYPE_SCALAR || inputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported weight animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}." }
            return
        }

        if (outputAccessor.type != GltfAccessor.TYPE_SCALAR || outputAccessor.componentType != GltfAccessor.COMP_TYPE_FLOAT) {
            context.logger.warn { "Unsupported weight animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}." }
            return
        }

        val weightChannel = WeightAnimationChannel("${modelAnim.name}_weight", morphAnimatedMesh)
        val interpolation = when (channel.samplerRef.interpolation) {
            GltfAnimationSampler.INTERPOLATION_STEP -> AnimationKey.Interpolation.STEP
            GltfAnimationSampler.INTERPOLATION_CUBICSPLINE -> AnimationKey.Interpolation.CUBICSPLINE
            else -> AnimationKey.Interpolation.LINEAR
        }
        modelAnim.channels += weightChannel

        val morphTargets = mesh.primitives[0].targets
        val attributesSize = mesh.primitives[0].targets.sumOf { it.size }
        val inTime = FloatAccessor(inputAccessor)
        val outWeight = FloatAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val weightKey = if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                val startTan = FloatArray(attributesSize)
                val point = FloatArray(attributesSize)
                val endTan = FloatArray(attributesSize)

                var attribIndex = 0
                for (m in morphTargets.indices) {
                    val w = outWeight.next()
                    for (j in 0 until morphTargets[m].size) {
                        startTan[attribIndex++] = w
                    }
                }

                attribIndex = 0
                for (m in morphTargets.indices) {
                    val w = outWeight.next()
                    for (j in 0 until morphTargets[m].size) {
                        point[attribIndex++] = w
                    }
                }

                attribIndex = 0
                for (m in morphTargets.indices) {
                    val w = outWeight.next()
                    for (j in 0 until morphTargets[m].size) {
                        endTan[attribIndex++] = w
                    }
                }


                CubicWeightKey(t, point, startTan, endTan)
            } else {
                val weights = FloatArray(attributesSize)
                var attribIndex = 0
                for (m in morphTargets.indices) {
                    val w = outWeight.next()
                    for (j in 0 until morphTargets[m].size) {
                        weights[attribIndex++] = w
                    }
                }
                WeightKey(t, weights)
            }
            weightKey.interpolation = interpolation
            weightChannel.keys[t] = weightKey
        }
    }

    private fun createSkins(model: Model) {
        gltfFile.skins.forEach { skin ->
            val modelSkin = Skin()
            val invBinMats = skin.inverseBindMatrixAccessorRef?.let { Mat4Accessor(it) }
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
        val modelNdName = name ?: "node_${model.nodes3d.size}"
        val node = Node3D().apply { name = modelNdName }
        modelNodes[this] = node
        model.nodes3d[modelNdName] = node

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
                node.scaling(scale[0], scale[1], scale[2])
            }
        }

        childRefs.forEach {
            node += it.toNode(model)
        }
        return node
    }

    suspend fun GltfNode.createMeshes(model: Model, node: Node3D, loadTextureAsynchronously: Boolean) {
        meshRef?.primitives?.forEachIndexed { index, prim ->
            val name = "${meshRef?.name ?: "${node.name}.mesh"}_$index"
            val geometry = prim.toGeometry(gltfFile.accessors)
            val mesh = MeshNode().apply {
                this.mesh = Mesh(gl, geometry)
                this.name = name
            }
            node += mesh

            meshesByMaterial.getOrPut(prim.material) { mutableSetOf() } += mesh
            meshMaterials[mesh] = prim.materialRef

            // apply skin
            if (skin >= 0) {
                mesh.skin = model.skins[skin]
                val skeletonRoot = gltfFile.skins[skin].skeleton
                if (skeletonRoot > 0) {
                    node -= mesh
                    modelNodes[gltfFile.nodes[skeletonRoot]]!! += mesh
                }
            }

            // apply morph weights
            if (prim.targets.isNotEmpty()) {
                mesh.morphWeights = FloatArray(prim.targets.sumOf { it.size })
            }

            if (loadTextureAsynchronously) {
                KtScope.launch(newSingleThreadAsyncContext()) {
                    mesh.loadTextures(context, prim)
                }
            } else {
                mesh.loadTextures(context, prim)
            }
            model.meshes[name] = mesh
        }
    }

    suspend fun MeshNode.loadTextures(context: Context, prim: GltfMeshPrimitive) {
        prim.materialRef?.pbrMetallicRoughness?.baseColorTexture?.getTexture(context, gltfFile, root)?.also {
            textures["albedo"] = it
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
            return MeshGeometry(Usage.STATIC_DRAW, VertexAttributes(emptyList()), 0)
        }


        var generateTangents = false

        val attribs = mutableListOf<VertexAttribute>()

        // for PbrShader positions and normals are always required
        attribs += VertexAttribute.POSITION
        attribs += VertexAttribute.NORMAL

        if (colorAcc != null) {
            attribs += VertexAttribute.COLOR_UNPACKED
        }
//        if (cfg.setVertexAttribsFromMaterial) {
//            attribs += Attribute.EMISSIVE_COLOR
//            attribs += Attribute.METAL_ROUGH
//        }
        if (texCoordAcc != null) {
            attribs += VertexAttribute.TEX_COORDS(0)
        }
//        if (tangentAcc != null) {
//            attribs += Attribute.TANGENTS
//        } else if (materialRef?.normalTexture != null) {
//            attribs += Attribute.TANGENTS
//            generateTangents = true
//        }
        if (jointAcc != null) {
            attribs += VertexAttribute.JOINT
        }
        if (weightAcc != null) {
            attribs += VertexAttribute.WEIGHT
        }

//        val morphAccessors = makeMorphTargetAccessors(gltfAccessors)
//        attribs += morphAccessors.keys

        val poss = Vec3fAccessor(positionAcc)
        val nrms = if (normalAcc != null) Vec3fAccessor(normalAcc) else null
        val tans = if (tangentAcc != null) Vec4fAccessor(tangentAcc) else null
        val texs = if (texCoordAcc != null) Vec2fAccessor(texCoordAcc) else null
        val cols = if (colorAcc != null) Vec4fAccessor(colorAcc) else null
        val jnts = if (jointAcc != null) Vec4iAccessor(jointAcc) else null
        val wgts = if (weightAcc != null) Vec4fAccessor(weightAcc) else null

        val verts = MeshGeometry(Usage.DYNAMIC_DRAW, VertexAttributes(attribs), grow = true)

        for (i in 0 until positionAcc.count) {
            verts.addVertex {
                poss.next(position)
                nrms?.next(normal)
                //      tans?.next(tangent)
                texs?.next(texCoords)
                cols?.next()?.let { col ->
                    color.set(col)
                }
                jnts?.next(joints)
                wgts?.next(weights)

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
            }
        }

        if (indexAccessor != null) {
            val inds = IntAccessor(indexAccessor)
            for (i in 0 until indexAccessor.count) {
                verts.addIndex(inds.next())
            }
        } else {
            for (i in 0 until positionAcc.count) {
                verts.addIndex(i)
            }
        }

//        if (generateTangents) {
//            verts.generateTangents()
//        }
//        if (cfg.generateNormals || normalAcc == null) {
//            verts.generateNormals()
//        }
        return verts
    }


    companion object {
        private val logger = Logger<GltfModelGenerator>()
    }
}
