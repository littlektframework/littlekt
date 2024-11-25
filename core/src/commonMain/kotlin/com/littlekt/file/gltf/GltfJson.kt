package com.littlekt.file.gltf

import com.littlekt.file.ByteBuffer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class GltfData(
    val asset: GltfAsset,
    val accessors: List<GltfAccessor>,
    val animations: List<GltfAnimation>? = null,
    val bufferViews: List<GltfBufferView>,
    val buffers: List<GltfBuffer>,
    val images: List<GltfImage>? = null,
    val materials: List<GltfMaterial>? = null,
    val meshes: List<GltfMesh>,
    val nodes: List<GltfNode>,
    val samplers: List<GltfSampler>? = null,
    val scene: Int,
    val scenes: List<GltfScene>,
    val skins: List<GltfSkin>? = null,
    val textures: List<GltfTexture>? = null,
    val extensionsUsed: List<String>? = null,
    val cameras: List<GltfCamera>? = null,
    val extensions: GLTFExtensions? = null,
)

@Serializable
internal data class GltfAccessor(
    val bufferView: Int,
    val componentType: Int,
    val count: Int,
    val type: GltfAccessorType,
    val byteOffset: Int = 0,
    val min: List<Float>? = null,
    val max: List<Float>? = null,
    val name: String? = null,
)

@Serializable
internal enum class GltfAccessorType(val value: String) {
    @SerialName("MAT4") Mat4("MAT4"),
    @SerialName("SCALAR") Scalar("SCALAR"),
    @SerialName("VEC2") Vec2("VEC2"),
    @SerialName("VEC3") Vec3("VEC3"),
    @SerialName("VEC4") Vec4("VEC4"),
}

@Serializable
internal data class GltfAnimation(
    val channels: List<GltfChannel>,
    val samplers: List<GltfAnimationSampler>,
    val name: String? = null,
)

@Serializable internal data class GltfChannel(val sampler: Int, val target: GltfTarget)

@Serializable internal data class GltfTarget(val node: Int, val path: GltfPath)

@Serializable
internal enum class GltfPath(val value: String) {
    @SerialName("rotation") Rotation("rotation"),
    @SerialName("scale") Scale("scale"),
    @SerialName("translation") Translation("translation"),
}

@Serializable
internal data class GltfAnimationSampler(
    val input: Int,
    val output: Int,
    val interpolation: GltfInterpolation? = null,
)

@Serializable
internal enum class GltfInterpolation(val value: String) {
    @SerialName("LINEAR") Linear("LINEAR")
}

@Serializable
internal data class GltfAsset(
    val copyright: String? = null,
    val version: String,
    val generator: String? = null,
    val extras: GltfExtras? = null,
)

@Serializable
internal data class GltfExtras(
    val author: String,
    val license: String,
    val source: String,
    val title: String,
)

@Serializable
internal data class GltfBufferView(
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
internal data class GltfBuffer(val uri: String, val byteLength: Int, val name: String? = null) {
    @Transient lateinit var data: ByteBuffer
}

@Serializable
internal data class GltfCamera(
    val perspective: GltfPerspective,
    val type: GltfCameraType,
    val name: String? = null,
)

@Serializable
internal data class GltfPerspective(
    val yfov: Float,
    val zfar: Int,
    val znear: Float,
    val aspectRatio: Float? = null,
)

@Serializable
internal enum class GltfCameraType(val value: String) {
    @SerialName("perspective") Perspective("perspective")
}

@Serializable internal class GLTFExtensions()

@Serializable
internal data class GltfImage(
    val uri: String,
    val mimeType: GltfMIMEType? = null,
    val name: String? = null,
)

@Serializable
internal enum class GltfMIMEType(val value: String) {
    @SerialName("image/jpeg") ImageJPEG("image/jpeg"),
    @SerialName("image/png") ImagePNG("image/png"),
}

@Serializable
internal data class GltfMaterial(
    val name: String? = null,
    val pbrMetallicRoughness: GltfPbrMetallicRoughness,
    val normalTexture: GltfNormalTexture? = null,
    val occlusionTexture: GltfOcclusionTexture? = null,
    val doubleSided: Boolean? = null,
    val alphaMode: String? = null,
    val emissiveFactor: List<Float>? = null,
    val emissiveTexture: GltfEmissiveTextureClass? = null,
    val extensions: GltfMaterialExtensions? = null,
    val alphaCutoff: Float? = null,
)

@Serializable
internal data class GltfEmissiveTextureClass(val index: Int, val texCoord: Int? = null)

@Serializable
internal data class GltfMaterialExtensions(
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
internal data class GltfKHRMaterialsClearcoat(
    val clearcoatFactor: Int,
    val clearcoatTexture: GltfEmissiveTextureClass,
)

@Serializable internal data class GltfKHRMaterialsEmissiveStrength(val emissiveStrength: Int)

@Serializable internal data class GltfKHRMaterialsIor(val ior: Float)

@Serializable
internal data class GltfKHRMaterialsIridescence(
    val iridescenceFactor: Int,
    val iridescenceIor: Float,
    val iridescenceThicknessMaximum: Int,
    val iridescenceThicknessMinimum: Int,
    val iridescenceThicknessTexture: GltfIridescenceThicknessTextureClass,
)

@Serializable internal data class GltfIridescenceThicknessTextureClass(val index: Int)

@Serializable
internal data class GltfKHRMaterialsSheen(
    val sheenRoughnessFactor: Float,
    val sheenColorFactor: List<Int>,
)

@Serializable
internal data class GltfKHRMaterialsSpecular(
    val specularFactor: Float? = null,
    val specularTexture: GltfIridescenceThicknessTextureClass? = null,
    val specularColorFactor: List<Float>? = null,
    val specularColorTexture: GltfIridescenceThicknessTextureClass? = null,
)

@Serializable internal data class GltfKHRMaterialsTransmission(val transmissionFactor: Float)

@Serializable
internal data class GltfKHRMaterialsVolume(
    val thicknessFactor: Float,
    val attenuationColor: List<Float>? = null,
)

@Serializable
internal data class GltfNormalTexture(
    val index: Int,
    val extensions: GltfNormalTextureExtensions? = null,
    val scale: Int? = null,
    val texCoord: Int? = null,
)

@Serializable
internal data class GltfNormalTextureExtensions(
    @SerialName("KHR_texture_transform") val khrTextureTransform: GltfKHRTextureTransform
)

@Serializable
internal data class GltfKHRTextureTransform(
    val offset: List<Float>? = null,
    val scale: List<Float>? = null,
    val texCoord: Int? = null,
    val rotation: Float? = null,
)

@Serializable
internal data class GltfOcclusionTexture(
    val index: Int,
    val extensions: GltfNormalTextureExtensions? = null,
)

@Serializable
internal data class GltfPbrMetallicRoughness(
    val baseColorTexture: GltfBaseColorTexture? = null,
    val metallicFactor: Float? = null,
    val roughnessFactor: Float? = null,
    val metallicRoughnessTexture: GltfEmissiveTextureClass? = null,
    val baseColorFactor: List<Float>? = null,
)

@Serializable
internal data class GltfBaseColorTexture(
    val index: Int,
    val extensions: GltfNormalTextureExtensions? = null,
    val texCoord: Int? = null,
)

@Serializable
internal data class GltfMesh(
    val primitives: List<GltfPrimitive>,
    val weights: List<Float>? = null,
    val name: String? = null,
)

@Serializable
internal data class GltfPrimitive(
    val attributes: Map<String, Int>,
    val material: Int = -1,
    val indices: Int = -1,
    val mode: Int = MODE_TRIANGLES,
) {
    @Transient var materialRef: GltfMaterial? = null

    internal companion object {
        const val MODE_POINTS = 0
        const val MODE_LINES = 1
        const val MODE_LINE_LOOP = 2
        const val MODE_LINE_STRIP = 3
        const val MODE_TRIANGLES = 4
        const val MODE_TRIANGLE_STRIP = 5
        const val MODE_TRIANGLE_FAN = 6
        const val MODE_QUADS = 7
        const val MODE_QUAD_STRIP = 8
        const val MODE_POLYGON = 9

        const val ATTRIBUTE_POSITION = "POSITION"
        const val ATTRIBUTE_NORMAL = "NORMAL"
        const val ATTRIBUTE_TANGENT = "TANGENT"
        const val ATTRIBUTE_TEXCOORD_0 = "TEXCOORD_0"
        const val ATTRIBUTE_TEXCOORD_1 = "TEXCOORD_1"
        const val ATTRIBUTE_COLOR_0 = "COLOR_0"
        const val ATTRIBUTE_JOINTS_0 = "JOINTS_0"
        const val ATTRIBUTE_WEIGHTS_0 = "WEIGHTS_0"
    }
}

@Serializable
internal data class GltfNode(
    val children: List<Int> = emptyList(),
    val name: String? = null,
    val mesh: Int = -1,
    val skin: Int = -1,
    val rotation: List<Float>? = null,
    val translation: List<Float>? = null,
    val matrix: List<Float>? = null,
    val scale: List<Float>? = null,
    val camera: Int = -1,
) {
    @Transient lateinit var childRefs: List<GltfNode>

    @Transient var meshRef: GltfMesh? = null

    @Transient var skinRef: GltfSkin? = null
}

@Serializable
internal data class GltfSampler(
    val magFilter: Int? = null,
    val minFilter: Int? = null,
    val wrapS: Int? = null,
    val wrapT: Int? = null,
)

@Serializable internal data class GltfScene(val nodes: List<Int>, val name: String? = null)

@Serializable
internal data class GltfSkin(
    val inverseBindMatrices: Int,
    val joints: List<Int>,
    val skeleton: Int? = null,
    val name: String? = null,
)

@Serializable
internal data class GltfTexture(
    val sampler: Int? = null,
    val source: Int,
    val name: String? = null,
)
