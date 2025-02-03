package com.littlekt.file.gltf

import com.littlekt.graphics.*
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.Model
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.skin.*
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
import com.littlekt.util.datastructure.internal.threadSafeMutableListOf
import com.littlekt.util.datastructure.internal.threadSafeMutableMapOf
import kotlin.math.min

/**
 * Converts a [GltfData] to a [MeshNode] ready for rendering. This will load underlying buffers and
 * textures.
 *
 * @param config the configuration to use when generating the [MeshNode]. Defaults to
 *   [GltfLoaderPbrConfig].
 * @param preferredFormat the preferred [TextureFormat] to be used when loading the model texture.
 */
fun GltfData.toModel(
    config: GltfLoaderConfig = GltfLoaderPbrConfig(),
    preferredFormat: TextureFormat =
        if (root.vfs.context.graphics.preferredFormat.srgb) TextureFormat.RGBA8_UNORM_SRGB
        else TextureFormat.RGBA8_UNORM,
    scene: Int = this.scene,
): Model {
    return GltfModelGenerator(this)
        .toModel(config, root.vfs.context.graphics.device, preferredFormat, scenes[scene])
}

private class GltfModelGenerator(val gltfFile: GltfData) {
    val modelAnimations = threadSafeMutableListOf<Animation>()

    val nodeCache = threadSafeMutableMapOf<GltfNode, Node3D>()

    // gltf material index to set of meshes using it
    val meshesByMaterial = threadSafeMutableMapOf<Int, MutableSet<Mesh<*>>>()

    // gltf material index to material
    val materialCache = threadSafeMutableMapOf<Int, Material>()
    val meshMaterials = threadSafeMutableMapOf<Mesh<*>, GltfMaterial?>()

    // gltf mesh index to primitives
    val meshPrimitivesCache = threadSafeMutableMapOf<Int, List<MeshPrimitive>>()

    // gltf skin index to Skin
    val skinCache = threadSafeMutableMapOf<Int, Skin>()

    // gltf node index that needs to be the parent of the given node3d. This is for skinning when
    // the skeleton root index is not 0.
    val nodesToReparent = threadSafeMutableListOf<Pair<Int, Node3D>>()

    fun toModel(
        config: GltfLoaderConfig,
        device: Device,
        preferredFormat: TextureFormat,
        gltfScene: GltfScene,
    ): Model {
        val scene = Model().apply { name = gltfScene.name ?: "glTF scene" }
        gltfScene.nodeRefs.map { node ->
            scene += node.toNode(config, device, preferredFormat, scene, scene)
        }
        applySkins()
        createMorphAnimations()
        createAnimations()
        modelAnimations
            .filter { it.channels.isNotEmpty() }
            .forEach { modelAnim ->
                modelAnim.prepareAnimation()
                scene.animations += modelAnim
            }
        scene.animations.forEach { it.prepareAnimation() }
        scene.disableAllAnimations()

        return scene
    }

    fun GltfNode.toNode(
        config: GltfLoaderConfig,
        device: Device,
        preferredFormat: TextureFormat,
        parent: Node3D,
        scene: Node3D,
    ): Node3D {
        val nodeName = name ?: "node_${parent.childCount}"
        val meshRef = meshRef
        val skinRef = skinRef
        val skin =
            if (skinRef != null) {
                skinCache.getOrPut(skin) { createSkin(device, skinRef) }
            } else {
                null
            }
        val model =
            if (meshRef != null) {
                meshPrimitivesCache.getOrPut(mesh) {
                    createMeshPrimitives(config, device, preferredFormat, meshRef, skin)
                }
            } else {
                null
            }
        val node = (if (model != null) MeshNode(model) else Node3D()).apply { name = nodeName }

        if (skinRef != null && skinRef.skeleton > 0) {
            // skeleton root node isn't the base. So we must mark this node to be moved to the
            // skeleton node index
            // after we finish.
            nodesToReparent += skinRef.skeleton to node
        }

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

    fun createMeshPrimitives(
        config: GltfLoaderConfig,
        device: Device,
        preferredFormat: TextureFormat,
        meshRef: GltfMesh,
        skin: Skin?,
    ): List<MeshPrimitive> {
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
                        }
                            ?: config.fallbackMaterialStrategy.createMaterial(
                                config.modelConfig,
                                device,
                            )
                    }
                val indexFormat =
                    if (prim.indices >= 0)
                        gltfFile.accessors[prim.indices].componentType.toIndexFormat()
                    else null
                val morphWeights =
                    if (prim.targets.isNotEmpty()) {
                        FloatArray(prim.targets.sumOf { it.size })
                    } else {
                        null
                    }
                val meshPrimitive =
                    MeshPrimitive(mesh, material, prim.mode.toTopology(), indexFormat, skin)

                meshPrimitive
            }
        return primitives
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

    private fun createSkin(device: Device, skin: GltfSkin): Skin {
        val nodes = mutableListOf<Skin.SkinNode>()
        val invBinMats = skin.inverseBindMatrixAccessorRef?.let { GltfMat4Accessor(it) }
        if (invBinMats != null) {
            val skinNodes = mutableMapOf<GltfNode, Skin.SkinNode>()
            skin.jointRefs.forEach { joint ->
                val jointNode = nodeCache[joint]!!
                val invBindMat = invBinMats.next()
                val skinNode = Skin.SkinNode(jointNode, invBindMat)
                nodes += skinNode
                skinNodes[joint] = skinNode
            }

            skin.jointRefs.forEach { joint ->
                val skinNode = skinNodes[joint]
                if (skinNode != null) {
                    joint.childRefs.forEach { child ->
                        val childNode = skinNodes[child]
                        childNode?.let { skinNode.addChild(it) }
                    }
                }
            }
        }
        return Skin(device, nodes)
    }

    private fun applySkins() {
        nodesToReparent.forEach { (gltfNodeIdx, node) ->
            node.parent =
                nodeCache[gltfFile.nodes[gltfNodeIdx]]
                    ?: error(
                        "Unable to find the correct glTF skeleton root node ($gltfNodeIdx) to reparent mesh to."
                    )
        }
    }

    private fun createAnimations() {
        gltfFile.animations.forEach { anim ->
            val modelAnim = Animation(anim.name)
            modelAnimations += modelAnim

            val animNodes = mutableMapOf<Node3D, AnimationNode>()
            anim.channels.forEach { channel ->
                val gltfNode = nodeCache[channel.target.nodeRef]
                if (gltfNode != null) {
                    val animationNode =
                        animNodes.getOrPut(gltfNode) { AnimatedTransformGroup(gltfNode) }
                    when (channel.target.path) {
                        GltfAnimationPath.Translation ->
                            createTranslationAnimation(channel, animationNode, modelAnim)

                        GltfAnimationPath.Rotation ->
                            createRotationAnimation(channel, animationNode, modelAnim)

                        GltfAnimationPath.Scale ->
                            createScaleAnimation(channel, animationNode, modelAnim)

                        GltfAnimationPath.Weights -> {
                            // TODO
                        }
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

        if (
            inputAccessor.type != GltfAccessorType.Scalar ||
                inputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported translation animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}."
            }
            return
        }

        if (
            outputAccessor.type != GltfAccessorType.Vec3 ||
                outputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported translation animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}."
            }
            return
        }

        val transChannel =
            TranslationAnimationChannel("${modelAnim.name}_translation", animationNode)
        val interpolation =
            when (channel.samplerRef.interpolation) {
                GltfInterpolation.Step -> AnimationKey.Interpolation.STEP
                GltfInterpolation.CubicSpline -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }

        modelAnim.channels += transChannel

        val inTime = GltfFloatAccessor(inputAccessor)
        val outTranslation = GltfVec3fAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val transKey =
                if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
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

        if (
            inputAccessor.type != GltfAccessorType.Scalar ||
                inputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported rotation animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}."
            }
            return
        }

        if (
            outputAccessor.type != GltfAccessorType.Vec4 ||
                outputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported rotation animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}."
            }
            return
        }

        val rotChannel = RotationAnimationChannel("${modelAnim.name}_rotation", animationNode)
        val interpolation =
            when (channel.samplerRef.interpolation) {
                GltfInterpolation.Step -> AnimationKey.Interpolation.STEP
                GltfInterpolation.CubicSpline -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }

        modelAnim.channels += rotChannel

        val inTime = GltfFloatAccessor(inputAccessor)
        val outRotation = GltfVec4fAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val rotKey =
                if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
                    val startTan = outRotation.next()
                    val point = outRotation.next()
                    val endTan = outRotation.next()
                    CubicRotationKey(t, Quaternion(point), startTan, endTan)
                } else {
                    RotationKey(t, Quaternion(outRotation.next()))
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

        if (
            inputAccessor.type != GltfAccessorType.Scalar ||
                inputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported scale animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}."
            }
            return
        }

        if (
            outputAccessor.type != GltfAccessorType.Vec3 ||
                outputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported scale animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}."
            }
            return
        }

        val scaleChannel = ScaleAnimationChannel("${modelAnim.name}_scale", animationNode)
        val interpolation =
            when (channel.samplerRef.interpolation) {
                GltfInterpolation.Step -> AnimationKey.Interpolation.STEP
                GltfInterpolation.CubicSpline -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }

        modelAnim.channels += scaleChannel

        val inTime = GltfFloatAccessor(inputAccessor)
        val outScale = GltfVec3fAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val scaleKey =
                if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
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
                if (channel.target.path == GltfAnimationPath.Weights) {
                    val modelAnim = modelAnimations[i]
                    val mesh = channel.target.nodeRef?.meshRef
                    val node = nodeCache[channel.target.nodeRef]
                    node?.children?.filterIsInstance<MeshPrimitive>()?.forEach {
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

        if (
            inputAccessor.type != GltfAccessorType.Scalar ||
                inputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported weight animation input accessor: type = ${inputAccessor.type}, component type = ${inputAccessor.componentType}."
            }
            return
        }

        if (
            outputAccessor.type != GltfAccessorType.Scalar ||
                outputAccessor.componentType != GltfComponentType.Float
        ) {
            logger.warn {
                "Unsupported weight animation output accessor: type = ${outputAccessor.type}, component type = ${outputAccessor.componentType}."
            }
            return
        }

        val weightChannel = WeightAnimationChannel("${modelAnim.name}_weight", morphAnimatedMesh)
        val interpolation =
            when (channel.samplerRef.interpolation) {
                GltfInterpolation.Step -> AnimationKey.Interpolation.STEP
                GltfInterpolation.CubicSpline -> AnimationKey.Interpolation.CUBICSPLINE
                else -> AnimationKey.Interpolation.LINEAR
            }
        modelAnim.channels += weightChannel

        val morphTargets = mesh.primitives[0].targets
        val attributesSize = mesh.primitives[0].targets.sumOf { it.size }
        val inTime = GltfFloatAccessor(inputAccessor)
        val outWeight = GltfFloatAccessor(outputAccessor)
        for (i in 0 until min(inputAccessor.count, outputAccessor.count)) {
            val t = inTime.next()
            val weightKey =
                if (interpolation == AnimationKey.Interpolation.CUBICSPLINE) {
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

    companion object {
        private val logger = Logger<GltfModelGenerator>()
    }
}
