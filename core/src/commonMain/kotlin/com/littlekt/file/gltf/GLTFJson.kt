package com.littlekt.file.gltf

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val scene: Long,
    val scenes: List<GltfScene>,
    val skins: List<GltfSkin>? = null,
    val textures: List<GltfTexture>? = null,
    val extensionsUsed: List<String>? = null,
    val cameras: List<GltfCamera>? = null,
    val extensions: GLTFExtensions? = null
)

@Serializable
internal data class GltfAccessor(
    val bufferView: Long,
    val componentType: Long,
    val count: Long,
    val type: GltfAccessorType,
    val byteOffset: Long? = null,
    val min: List<Double>? = null,
    val max: List<Double>? = null,
    val name: String? = null
)

@Serializable
internal enum class GltfAccessorType(val value: String) {
    @SerialName("MAT4")
    Mat4("MAT4"),

    @SerialName("SCALAR")
    Scalar("SCALAR"),

    @SerialName("VEC2")
    Vec2("VEC2"),

    @SerialName("VEC3")
    Vec3("VEC3"),

    @SerialName("VEC4")
    Vec4("VEC4");
}

@Serializable
internal data class GltfAnimation(
    val channels: List<GltfChannel>,
    val samplers: List<GltfAnimationSampler>,
    val name: String? = null
)

@Serializable
internal data class GltfChannel(
    val sampler: Long,
    val target: GltfTarget
)

@Serializable
internal data class GltfTarget(
    val node: Long,
    val path: GltfPath
)

@Serializable
internal enum class GltfPath(val value: String) {
    @SerialName("rotation")
    Rotation("rotation"),

    @SerialName("scale")
    Scale("scale"),

    @SerialName("translation")
    Translation("translation");
}

@Serializable
internal data class GltfAnimationSampler(
    val input: Long,
    val output: Long,
    val interpolation: GltfInterpolation? = null
)

@Serializable
internal enum class GltfInterpolation(val value: String) {
    @SerialName("LINEAR")
    Linear("LINEAR");
}

@Serializable
internal data class GltfAsset(
    val copyright: String? = null,
    val version: String,
    val generator: String? = null,
    val extras: GltfExtras? = null
)

@Serializable
internal data class GltfExtras(
    val author: String,
    val license: String,
    val source: String,
    val title: String
)

@Serializable
internal data class GltfBufferView(
    val buffer: Long,
    val byteOffset: Long? = null,
    val byteLength: Long,
    val byteStride: Long? = null,
    val target: Long? = null,
    val name: String? = null
)

@Serializable
internal data class GltfBuffer(
    val uri: String,
    val byteLength: Long,
    val name: String? = null
)

@Serializable
internal data class GltfCamera(
    val perspective: GltfPerspective,
    val type: GltfCameraType,
    val name: String? = null
)

@Serializable
internal data class GltfPerspective(
    val yfov: Double,
    val zfar: Long,
    val znear: Double,
    val aspectRatio: Double? = null
)

@Serializable
internal enum class GltfCameraType(val value: String) {
    @SerialName("perspective")
    Perspective("perspective");
}

@Serializable
internal class GLTFExtensions()

@Serializable
internal data class GltfImage(
    val uri: String,
    val mimeType: GltfMIMEType? = null,
    val name: String? = null
)

@Serializable
internal enum class GltfMIMEType(val value: String) {
    @SerialName("image/jpeg")
    ImageJPEG("image/jpeg"),

    @SerialName("image/png")
    ImagePNG("image/png");
}

@Serializable
internal data class GltfMaterial(
    val name: String? = null,
    val pbrMetallicRoughness: GltfPbrMetallicRoughness,
    val normalTexture: GltfNormalTexture? = null,
    val occlusionTexture: GltfOcclusionTexture? = null,
    val doubleSided: Boolean? = null,
    val alphaMode: String? = null,
    val emissiveFactor: List<Double>? = null,
    val emissiveTexture: GltfEmissiveTextureClass? = null,
    val extensions: GltfMaterialExtensions? = null,
    val alphaCutoff: Double? = null
)

@Serializable
internal data class GltfEmissiveTextureClass(
    val index: Long,
    val texCoord: Long? = null
)

@Serializable
internal data class GltfMaterialExtensions(
    @SerialName("KHR_materials_transmission")
    val khrMaterialsTransmission: GltfKHRMaterialsTransmission? = null,

    @SerialName("KHR_materials_volume")
    val khrMaterialsVolume: GltfKHRMaterialsVolume? = null,

    @SerialName("KHR_materials_iridescence")
    val khrMaterialsIridescence: GltfKHRMaterialsIridescence? = null,

    @SerialName("KHR_materials_clearcoat")
    val khrMaterialsClearcoat: GltfKHRMaterialsClearcoat? = null,

    @SerialName("KHR_materials_sheen")
    val khrMaterialsSheen: GltfKHRMaterialsSheen? = null,

    @SerialName("KHR_materials_emissive_strength")
    val khrMaterialsEmissiveStrength: GltfKHRMaterialsEmissiveStrength? = null,

    @SerialName("KHR_materials_specular")
    val khrMaterialsSpecular: GltfKHRMaterialsSpecular? = null,

    @SerialName("KHR_materials_ior")
    val khrMaterialsIor: GltfKHRMaterialsIor? = null
)

@Serializable
internal data class GltfKHRMaterialsClearcoat(
    val clearcoatFactor: Long,
    val clearcoatTexture: GltfEmissiveTextureClass
)

@Serializable
internal data class GltfKHRMaterialsEmissiveStrength(
    val emissiveStrength: Long
)

@Serializable
internal data class GltfKHRMaterialsIor(
    val ior: Double
)

@Serializable
internal data class GltfKHRMaterialsIridescence(
    val iridescenceFactor: Long,
    val iridescenceIor: Double,
    val iridescenceThicknessMaximum: Long,
    val iridescenceThicknessMinimum: Long,
    val iridescenceThicknessTexture: GltfIridescenceThicknessTextureClass
)

@Serializable
internal data class GltfIridescenceThicknessTextureClass(
    val index: Long
)

@Serializable
internal data class GltfKHRMaterialsSheen(
    val sheenRoughnessFactor: Double,
    val sheenColorFactor: List<Long>
)

@Serializable
internal data class GltfKHRMaterialsSpecular(
    val specularFactor: Double? = null,
    val specularTexture: GltfIridescenceThicknessTextureClass? = null,
    val specularColorFactor: List<Double>? = null,
    val specularColorTexture: GltfIridescenceThicknessTextureClass? = null
)

@Serializable
internal data class GltfKHRMaterialsTransmission(
    val transmissionFactor: Double
)

@Serializable
internal data class GltfKHRMaterialsVolume(
    val thicknessFactor: Double,
    val attenuationColor: List<Double>? = null
)

@Serializable
internal data class GltfNormalTexture(
    val index: Long,
    val extensions: GltfNormalTextureExtensions? = null,
    val scale: Long? = null,
    val texCoord: Long? = null
)

@Serializable
internal data class GltfNormalTextureExtensions(
    @SerialName("KHR_texture_transform")
    val khrTextureTransform: GltfKHRTextureTransform
)

@Serializable
internal data class GltfKHRTextureTransform(
    val offset: List<Double>? = null,
    val scale: List<Double>? = null,
    val texCoord: Long? = null,
    val rotation: Double? = null
)

@Serializable
internal data class GltfOcclusionTexture(
    val index: Long,
    val extensions: GltfNormalTextureExtensions? = null
)

@Serializable
internal data class GltfPbrMetallicRoughness(
    val baseColorTexture: GltfBaseColorTexture? = null,
    val metallicFactor: Double? = null,
    val roughnessFactor: Double? = null,
    val metallicRoughnessTexture: GltfEmissiveTextureClass? = null,
    val baseColorFactor: List<Double>? = null
)

@Serializable
internal data class GltfBaseColorTexture(
    val index: Long,
    val extensions: GltfNormalTextureExtensions? = null,
    val texCoord: Long? = null
)

@Serializable
internal data class GltfMesh(
    val name: String? = null,
    val primitives: List<GltfPrimitive>
)

@Serializable
internal data class GltfPrimitive(
    val attributes: GltfAttributes,
    val material: Long? = null,
    val indices: Long? = null,
    val mode: Long? = null
)

@Serializable
internal data class GltfAttributes(
    @SerialName("POSITION")
    val position: Long,

    @SerialName("TEXCOORD_0")
    val texcoord0: Long? = null,

    @SerialName("JOINTS_0")
    val joints0: Long? = null,

    @SerialName("WEIGHTS_0")
    val weights0: Long? = null,

    @SerialName("TANGENT")
    val tangent: Long? = null,

    @SerialName("NORMAL")
    val normal: Long? = null,

    @SerialName("TEXCOORD_1")
    val texcoord1: Long? = null,

    @SerialName("TEXCOORD_2")
    val texcoord2: Long? = null
)

@Serializable
internal data class GltfNode(
    val children: List<Long>? = null,
    val name: String? = null,
    val mesh: Long? = null,
    val skin: Long? = null,
    val rotation: List<Double>? = null,
    val translation: List<Double>? = null,
    val matrix: List<Double>? = null,
    val scale: List<Double>? = null,
    val camera: Long? = null
)

@Serializable
internal data class GltfSampler(
    val magFilter: Long? = null,
    val minFilter: Long? = null,
    val wrapS: Long? = null,
    val wrapT: Long? = null
)

@Serializable
internal data class GltfScene(
    val nodes: List<Long>,
    val name: String? = null
)

@Serializable
internal data class GltfSkin(
    val inverseBindMatrices: Long,
    val joints: List<Long>,
    val skeleton: Long? = null,
    val name: String? = null
)

@Serializable
internal data class GltfTexture(
    val sampler: Long? = null,
    val source: Long,
    val name: String? = null
)
