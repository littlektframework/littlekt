package com.littlekt.file.gltf

import com.littlekt.file.ByteBuffer
import com.littlekt.file.vfs.VfsFile
import com.littlekt.file.vfs.readPixmap
import com.littlekt.graphics.LazyPixmapTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class GltfData(
    val asset: GltfAsset,
    val accessors: List<GltfAccessor> = emptyList(),
    val animations: List<GltfAnimation> = emptyList(),
    val bufferViews: List<GltfBufferView> = emptyList(),
    val buffers: List<GltfBuffer> = emptyList(),
    val images: List<GltfImage> = emptyList(),
    val materials: List<GltfMaterial> = emptyList(),
    val meshes: List<GltfMesh> = emptyList(),
    val nodes: List<GltfNode> = emptyList(),
    val samplers: List<GltfSampler> = emptyList(),
    val scene: Int = 0,
    val scenes: List<GltfScene> = emptyList(),
    val skins: List<GltfSkin> = emptyList(),
    val textures: List<GltfTexture> = emptyList(),
    val extensionRequired: List<String> = emptyList(),
    val extensionsUsed: List<String> = emptyList(),
    val cameras: List<GltfCamera> = emptyList(),
) {
    @Transient lateinit var root: VfsFile

    init {
        updateReferences()
    }

    private fun updateReferences() {
        accessors.forEach {
            if (it.bufferView >= 0) {
                it.bufferViewRef = bufferViews[it.bufferView]
            }
            it.sparse?.let { sparse ->
                sparse.indices.bufferViewRef = bufferViews[sparse.indices.bufferView]
                sparse.values.bufferViewRef = bufferViews[sparse.values.bufferView]
            }
        }
        animations.forEach { anim ->
            anim.samplers.forEach {
                it.inputAccessorRef = accessors[it.input]
                it.outputAccessorRef = accessors[it.output]
            }
            anim.channels.forEach {
                it.samplerRef = anim.samplers[it.sampler]
                if (it.target.node >= 0) {
                    it.target.nodeRef = nodes[it.target.node]
                }
            }
        }
        bufferViews.forEach { it.bufferRef = buffers[it.buffer] }
        images
            .filter { it.bufferView >= 0 }
            .forEach { it.bufferViewRef = bufferViews[it.bufferView] }
        meshes.forEach { mesh ->
            mesh.primitives.forEach {
                if (it.material >= 0) {
                    it.materialRef = materials[it.material]
                }
            }
        }
        nodes.forEach {
            it.childRefs = it.children.map { iNd -> nodes[iNd] }
            if (it.mesh >= 0) {
                it.meshRef = meshes[it.mesh]
            }
            if (it.skin >= 0) {
                it.skinRef = skins[it.skin]
            }
        }
        scenes.forEach { it.nodeRefs = it.nodes.map { iNd -> nodes[iNd] } }
        skins.forEach {
            if (it.inverseBindMatrices >= 0) {
                it.inverseBindMatrixAccessorRef = accessors[it.inverseBindMatrices]
            }
            it.jointRefs = it.joints.map { iJt -> nodes[iJt] }
        }
        textures.forEach {
            it.imageRef = images[it.source]
            it.samplerRef = samplers[it.sampler]
        }
    }
}

@Serializable
data class GltfAccessor(
    val bufferView: Int = -1,
    val byteOffset: Int = 0,
    val componentType: GltfComponentType,
    val count: Int,
    val type: GltfAccessorType,
    val min: List<Float> = emptyList(),
    val max: List<Float> = emptyList(),
    val name: String? = null,
    val normalized: Boolean = false,
    val sparse: GltfAccessorSparse? = null,
) {
    @Transient var bufferViewRef: GltfBufferView? = null
}

@Serializable
enum class GltfComponentType(val value: String, val byteSize: kotlin.Int) {
    @SerialName("5120") Byte("5120", 1),
    @SerialName("5121") UnsignedByte("5121", 1),
    @SerialName("5122") Short("5122", 2),
    @SerialName("5123") UnsignedShort("5123", 2),
    @SerialName("5124") Int("5124", 4),
    @SerialName("5125") UnsignedInt("5125", 4),
    @SerialName("5126") Float("5126", 4),
    @SerialName("0") Unknown("0", 0);

    companion object {
        val IntTypes = setOf(Byte, UnsignedByte, Short, UnsignedShort, Int, UnsignedInt)
    }
}

/** @return an [IndexFormat] for this [GltfComponentType], if applicable; otherwise `null`. */
fun GltfComponentType.toIndexFormat(): IndexFormat? =
    when (this) {
        GltfComponentType.UnsignedShort -> IndexFormat.UINT16
        GltfComponentType.UnsignedInt -> IndexFormat.UINT32
        else -> null
    }

@Serializable
data class GltfAccessorSparse(
    val count: Int,
    val indices: GltfAccessorSparseIndices,
    val values: GltfAccessorSparseValues,
)

@Serializable
data class GltfAccessorSparseIndices(
    val bufferView: Int,
    val byteOffset: Int = 0,
    val componentType: GltfComponentType,
) {
    @Transient lateinit var bufferViewRef: GltfBufferView
}

@Serializable
data class GltfAccessorSparseValues(val bufferView: Int, val byteOffset: Int = 0) {

    @Transient lateinit var bufferViewRef: GltfBufferView
}

@Serializable
enum class GltfAccessorType(val value: String, val numComponents: Int) {
    @SerialName("SCALAR") Scalar("SCALAR", 1),
    @SerialName("VEC2") Vec2("VEC2", 2),
    @SerialName("VEC3") Vec3("VEC3", 3),
    @SerialName("VEC4") Vec4("VEC4", 4),
    @SerialName("MAT2") Mat2("MAT2", 4),
    @SerialName("MAT3") Mat3("MAT3", 9),
    @SerialName("MAT4") Mat4("MAT4", 16),
}

@Serializable
data class GltfAnimation(
    val channels: List<GltfChannel>,
    val samplers: List<GltfAnimationSampler>,
    val name: String? = null,
)

@Serializable
data class GltfChannel(val sampler: Int, val target: GltfTarget) {
    @Transient lateinit var samplerRef: GltfAnimationSampler
}

@Serializable
data class GltfTarget(val node: Int = -1, val path: GltfPath) {
    @Transient var nodeRef: GltfNode? = null
}

@Serializable
enum class GltfPath(val value: String) {
    @SerialName("rotation") Rotation("rotation"),
    @SerialName("scale") Scale("scale"),
    @SerialName("translation") Translation("translation"),
    @SerialName("weights") Weights("weights"),
}

@Serializable
data class GltfAnimationSampler(
    val input: Int,
    val output: Int,
    val interpolation: GltfInterpolation = GltfInterpolation.Linear,
) {
    @Transient lateinit var inputAccessorRef: GltfAccessor

    @Transient lateinit var outputAccessorRef: GltfAccessor
}

@Serializable
enum class GltfInterpolation(val value: String) {
    @SerialName("LINEAR") Linear("LINEAR"),
    @SerialName("STEP") Step("STEP"),
    @SerialName("CUBICSPLINE") CubicSpline("CUBICSPLINE"),
}

@Serializable
data class GltfAsset(
    val copyright: String? = null,
    val version: String,
    val generator: String? = null,
    val extras: GltfExtras? = null,
)

@Serializable
data class GltfExtras(
    val author: String,
    val license: String,
    val source: String,
    val title: String,
)

@Serializable
data class GltfBufferView(
    val buffer: Int,
    val byteOffset: Int = 0,
    val byteLength: Int,
    val byteStride: Int = 0,
    val target: Int = 0,
    val name: String? = null,
) {
    @Transient lateinit var bufferRef: GltfBuffer

    fun getData(): ByteBuffer {
        val data = ByteBuffer(byteLength)
        repeat(byteLength) { data[it] = bufferRef.data[byteOffset + it] }
        return data
    }
}

@Serializable
data class GltfBuffer(val uri: String? = null, val byteLength: Int, val name: String? = null) {
    @Transient lateinit var data: ByteBuffer
}

@Serializable
data class GltfCamera(
    val perspective: GltfPerspective,
    val type: GltfCameraType,
    val name: String? = null,
)

@Serializable
data class GltfPerspective(
    val yfov: Float,
    val zfar: Float,
    val znear: Float,
    val aspectRatio: Float? = null,
)

@Serializable
enum class GltfCameraType(val value: String) {
    @SerialName("perspective") Perspective("perspective")
}

@Serializable
data class GltfImage(
    val bufferView: Int = -1,
    val uri: String? = null,
    val mimeType: GltfMIMEType? = null,
    val name: String? = null,
) {
    @Transient var bufferViewRef: GltfBufferView? = null
}

@Serializable
enum class GltfMIMEType(val value: String) {
    @SerialName("image/jpeg") ImageJPEG("image/jpeg"),
    @SerialName("image/png") ImagePNG("image/png"),
    @SerialName("image/ktx2") ImageKTX2("image/ktx2"),
}

@Serializable
data class GltfMaterial(
    val alphaCutoff: Float = 0.5f,
    val alphaMode: GltfAlphaMode = GltfAlphaMode.Opaque,
    val name: String? = null,
    val pbrMetallicRoughness: GltfPbrMetallicRoughness =
        GltfPbrMetallicRoughness(baseColorFactor = listOf(0.5f, 0.5f, 0.5f, 1f)),
    val normalTexture: GltfTextureInfo? = null,
    val occlusionTexture: GltfTextureInfo? = null,
    val doubleSided: Boolean = false,
    val emissiveFactor: List<Float> = emptyList(),
    val emissiveTexture: GltfTextureInfo? = null,
    val extensions: List<GltfMaterialExtensions>? = null,
)

@Serializable
enum class GltfAlphaMode(val value: String) {
    @SerialName("BLEND") Blend("BLEND"),
    @SerialName("MASK") Mask("MASK"),
    @SerialName("OPAQUE") Opaque("OPAQUE"),
}

@Serializable
data class GltfTextureInfo(
    val index: Int,
    val texCoord: Int = 0,
    val strength: Float = 1f,
    val scale: Float = 1f,
    val extensions: List<GltfTextureExtensions>? = null,
) {
    fun getTexture(
        gltfData: GltfData,
        root: VfsFile,
        device: Device,
        preferredFormat: TextureFormat,
    ): Texture = gltfData.textures[index].toTexture(root, device, preferredFormat)
}

@Serializable
data class GltfMaterialExtensions(
    @SerialName("KHR_materials_transmission")
    val khrMaterialsTransmission: GltfKHRMaterialsTransmission? = null,
    @SerialName("KHR_materials_volume") val khrMaterialsVolume: GltfKHRMaterialsVolume? = null,
    @SerialName("KHR_materials_iridescence")
    val khrMaterialsIridescence: GltfKHRMaterialsIridescence? = null,
    @SerialName("KHR_materials_clearcoat")
    val khrMaterialsClearcoat: GltfKHRMaterialsClearcoat? = null,
    @SerialName("KHR_materials_sheen") val khrMaterialsSheen: GltfKHRMaterialsSheen? = null,
    @SerialName("KHR_materials_emissive_strength")
    val khrMaterialsEmissiveStrength: GltfKHRMaterialsEmissiveStrength? = null,
    @SerialName("KHR_materials_specular")
    val khrMaterialsSpecular: GltfKHRMaterialsSpecular? = null,
    @SerialName("KHR_materials_ior") val khrMaterialsIor: GltfKHRMaterialsIor? = null,
)

@Serializable
data class GltfKHRMaterialsClearcoat(
    val clearcoatFactor: Int,
    val clearcoatTexture: GltfTextureInfo,
)

@Serializable data class GltfKHRMaterialsEmissiveStrength(val emissiveStrength: Int)

@Serializable data class GltfKHRMaterialsIor(val ior: Float)

@Serializable
data class GltfKHRMaterialsIridescence(
    val iridescenceFactor: Int,
    val iridescenceIor: Float,
    val iridescenceThicknessMaximum: Int,
    val iridescenceThicknessMinimum: Int,
    val iridescenceThicknessTexture: GltfIridescenceThicknessTextureClass,
)

@Serializable data class GltfIridescenceThicknessTextureClass(val index: Int)

@Serializable
data class GltfKHRMaterialsSheen(val sheenRoughnessFactor: Float, val sheenColorFactor: List<Int>)

@Serializable
data class GltfKHRMaterialsSpecular(
    val specularFactor: Float? = null,
    val specularTexture: GltfIridescenceThicknessTextureClass? = null,
    val specularColorFactor: List<Float>? = null,
    val specularColorTexture: GltfIridescenceThicknessTextureClass? = null,
)

@Serializable data class GltfKHRMaterialsTransmission(val transmissionFactor: Float)

@Serializable
data class GltfKHRMaterialsVolume(
    val thicknessFactor: Float,
    val attenuationColor: List<Float>? = null,
)

@Serializable
data class GltfTextureExtensions(
    @SerialName("KHR_texture_transform") val khrTextureTransform: GltfKHRTextureTransform
)

@Serializable
data class GltfKHRTextureTransform(
    val offset: List<Float>? = null,
    val scale: List<Float>? = null,
    val texCoord: Int? = null,
    val rotation: Float? = null,
)

@Serializable
data class GltfPbrMetallicRoughness(
    val baseColorFactor: List<Float> = listOf(1f, 1f, 1f, 1f),
    val baseColorTexture: GltfTextureInfo? = null,
    val metallicFactor: Float = 1f,
    val metallicRoughnessTexture: GltfTextureInfo? = null,
    val roughnessFactor: Float = 1f,
)

@Serializable
data class GltfMesh(
    val primitives: List<GltfPrimitive>,
    val weights: List<Float> = emptyList(),
    val name: String? = null,
)

@Serializable
data class GltfPrimitive(
    val attributes: Map<GltfAttribute, Int>,
    val material: Int = -1,
    val indices: Int = -1,
    val mode: GltfRenderMode = GltfRenderMode.Triangles,
    val targets: List<Map<String, Int>> = emptyList(),
) {
    @Transient var materialRef: GltfMaterial? = null
}

@Serializable
enum class GltfAttribute(val value: String) {
    @SerialName("POSITION") Position("POSITION"),
    @SerialName("NORMAL") Normal("NORMAL"),
    @SerialName("TANGENT") Tangent("TANGENT"),
    @SerialName("TEXCOORD_0") TexCoord0("TEXCOORD_0"),
    @SerialName("TEXCOORD_1") TexCoord1("TEXCOORD_1"),
    @SerialName("TEXCOORD_2") TexCoord2("TEXCOORD_2"),
    @SerialName("TEXCOORD_3") TexCoord3("TEXCOORD_3"),
    @SerialName("TEXCOORD_4") TexCoord4("TEXCOORD_4"),
    @SerialName("COLOR_0") Color0("COLOR_0"),
    @SerialName("COLOR_1") Color1("COLOR_0"),
    @SerialName("COLOR_2") Color2("COLOR_0"),
    @SerialName("COLOR_3") Color3("COLOR_0"),
    @SerialName("COLOR_4") Color4("COLOR_0"),
    @SerialName("JOINTS_0") Joints0("JOINTS_0"),
    @SerialName("WEIGHTS_0") Weights0("WEIGHTS_0"),
}

@Serializable
enum class GltfRenderMode(val value: String) {
    @SerialName("0") Points("0"),
    @SerialName("1") Lines("1"),
    @SerialName("2") LineLoop("2"),
    @SerialName("3") LineStrip("3"),
    @SerialName("4") Triangles("4"),
    @SerialName("5") TriangleStrip("5"),
    @SerialName("6") TriangleFan("6"),
    @SerialName("7") Quads("7"),
    @SerialName("8") QuadStrip("8"),
    @SerialName("9") Polygon("9"),
}

/** Converts the [GltfRenderMode] to its respective [PrimitiveTopology]. */
fun GltfRenderMode.toTopology(): PrimitiveTopology =
    when (this) {
        GltfRenderMode.Points -> PrimitiveTopology.POINT_LIST
        GltfRenderMode.Lines -> PrimitiveTopology.LINE_LIST
        GltfRenderMode.LineLoop,
        GltfRenderMode.LineStrip -> PrimitiveTopology.LINE_STRIP
        GltfRenderMode.Triangles -> PrimitiveTopology.TRIANGLE_LIST
        GltfRenderMode.TriangleStrip -> PrimitiveTopology.TRIANGLE_STRIP
        else ->
            error(
                "Unsupported GLtf Render Mode: ${this.name}. Supported modes are: [Points, Lines, LineLoop, Triangles, TriangleStrip]."
            )
    }

@Serializable
data class GltfNode(
    val children: List<Int> = emptyList(),
    val name: String? = null,
    val mesh: Int = -1,
    val skin: Int = -1,
    val rotation: List<Float> = emptyList(),
    val translation: List<Float> = emptyList(),
    val matrix: List<Float> = emptyList(),
    val scale: List<Float> = emptyList(),
    val weights: List<Float> = emptyList(),
    val camera: Int = -1,
) {
    @Transient lateinit var childRefs: List<GltfNode>

    @Transient var meshRef: GltfMesh? = null

    @Transient var skinRef: GltfSkin? = null
}

@Serializable
data class GltfSampler(
    val magFilter: GltfTextureFilter? = null,
    val minFilter: GltfTextureFilter? = null,
    val wrapS: GltfTextureWrap? = null,
    val wrapT: GltfTextureWrap? = null,
)

@Serializable
enum class GltfTextureFilter(val value: String) {
    @SerialName("9728") Nearest("9728"),
    @SerialName("9729") Linear("9729"),
    @SerialName("9984") NearestMipMapNearest("9984"),
    @SerialName("9985") LinearMipMapNearest("9985"),
    @SerialName("9986") NearestMipMapLinear("9986"),
    @SerialName("9987") LinearMipMapLinear("9987"),
}

@Serializable
enum class GltfTextureWrap(val value: String) {
    @SerialName("10497") Repeat("10497"),
    @SerialName("33071") ClampToEdge("33071"),
    @SerialName("33648") MirroredRepeat("33648"),
}

/**
 * Maps this [GltfTextureWrap] to [AddressMode]. If `null`, it will default to [AddressMode.REPEAT].
 */
fun GltfTextureWrap?.toAddressMode(): AddressMode =
    when (this) {
        GltfTextureWrap.Repeat -> AddressMode.REPEAT
        GltfTextureWrap.ClampToEdge -> AddressMode.CLAMP_TO_EDGE
        GltfTextureWrap.MirroredRepeat -> AddressMode.MIRROR_REPEAT
        null -> AddressMode.REPEAT
    }

/**
 * Maps this [GltfTextureWrap] to a pair [FilterMode]. The first in the pair is for the respective
 * filter (`min`/`mag`) and the second in the pair is for the mipmap. For Example, `Gltf.Nearest`
 * will map to `Pair(FilterMode.NEAREST, FilterMode.NEAREST)`. If `null`, it will default to
 * `Pair(FilterMode.NEAREST, FilterMode.NEAREST)`.
 */
fun GltfTextureFilter?.toFilterMode(): Pair<FilterMode, FilterMode> =
    when (this) {
        GltfTextureFilter.Nearest,
        GltfTextureFilter.NearestMipMapNearest -> FilterMode.NEAREST to FilterMode.NEAREST
        GltfTextureFilter.Linear,
        GltfTextureFilter.LinearMipMapLinear -> FilterMode.LINEAR to FilterMode.LINEAR
        GltfTextureFilter.LinearMipMapNearest -> FilterMode.LINEAR to FilterMode.NEAREST
        GltfTextureFilter.NearestMipMapLinear -> FilterMode.NEAREST to FilterMode.LINEAR
        null -> FilterMode.LINEAR to FilterMode.LINEAR
    }

@Serializable
data class GltfScene(val nodes: List<Int>, val name: String? = null) {
    @Transient lateinit var nodeRefs: List<GltfNode>
}

@Serializable
data class GltfSkin(
    val inverseBindMatrices: Int = -1,
    val joints: List<Int>,
    val skeleton: Int = -1,
    val name: String? = null,
) {
    @Transient var inverseBindMatrixAccessorRef: GltfAccessor? = null

    @Transient lateinit var jointRefs: List<GltfNode>
}

@Serializable
data class GltfTexture(val sampler: Int = -1, val source: Int = 0, val name: String? = null) {
    @Transient lateinit var imageRef: GltfImage
    @Transient lateinit var samplerRef: GltfSampler

    @Transient private var texture: Texture? = null

    fun toTexture(root: VfsFile, device: Device, preferredFormat: TextureFormat): Texture {
        if (texture == null) {
            val uri = imageRef.uri

            val minFilters = samplerRef.minFilter.toFilterMode()
            val magFilters = samplerRef.magFilter.toFilterMode()
            texture =
                LazyPixmapTexture(
                        device,
                        samplerDescriptor =
                            SamplerDescriptor(
                                addressModeU = samplerRef.wrapS.toAddressMode(),
                                addressModeV = samplerRef.wrapT.toAddressMode(),
                                minFilter = minFilters.first,
                                magFilter = magFilters.first,
                                mipmapFilter = minFilters.second,
                            ),
                    )
                    .apply {
                        load(preferredFormat) {
                            if (uri != null) {
                                VfsFile(root.vfs, "${root.parent.path}/$uri").readPixmap()
                            } else {
                                imageRef.bufferViewRef?.getData()?.toArray()?.readPixmap()
                                    ?: error("Unable to read GltfTexture data!")
                            }
                        }
                    }
        }
        return texture ?: error("Unable to convert the GltfTexture to a Texture!")
    }
}
