package com.lehaine.littlekt.file.gltf

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The root object for a glTF asset.
 */
@Serializable
internal data class GltfFile(
    /**
     * An array of accessors.
     */
    val accessors: List<GltfAccessor>? = null,

    /**
     * An array of keyframe animations.
     */
    val animations: List<GltfAnimation>? = null,

    /**
     * Metadata about the glTF asset.
     */
    val asset: GltfAsset,

    /**
     * An array of buffers.
     */
    val buffers: List<GltfBuffer>? = null,

    /**
     * An array of bufferViews.
     */
    val bufferViews: List<GltfBufferView>? = null,

    /**
     * An array of cameras.
     */
    val cameras: List<GltfCamera>? = null,

    val extensions: JsonObject? = null,

    /**
     * Names of glTF extensions required to properly load this asset.
     */
    val extensionsRequired: List<String>? = null,

    /**
     * Names of glTF extensions used in this asset.
     */
    val extensionsUsed: List<String>? = null,

    val extras: JsonObject? = null,

    /**
     * An array of images.
     */
    val images: List<GltfImage>? = null,

    /**
     * An array of materials.
     */
    val materials: List<GltfMaterial>? = null,

    /**
     * An array of meshes.
     */
    val meshes: List<GltfMesh>? = null,

    /**
     * An array of nodes.
     */
    val nodes: List<GltfNode>? = null,

    /**
     * An array of samplers.
     */
    val samplers: List<GltfSampler>? = null,

    /**
     * The index of the default scene.
     */
    val scene: Long? = null,

    /**
     * An array of scenes.
     */
    val scenes: List<GltfScene>? = null,

    /**
     * An array of skins.
     */
    val skins: List<GltfSkin>? = null,

    /**
     * An array of textures.
     */
    val textures: List<GltfTexture>? = null,
)

/**
 * A typed view into a buffer view that contains raw binary data.
 */
@Serializable
internal data class GltfAccessor(
    /**
     * The index of the bufferView.
     */
    val bufferView: Long? = null,

    /**
     * The offset relative to the start of the buffer view in bytes.
     */
    val byteOffset: Long? = null,

    /**
     * The datatype of the accessor's components.
     */
    val componentType: Long,

    /**
     * The number of elements referenced by this accessor.
     */
    val count: Long,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * Maximum value of each component in this accessor.
     */
    val max: List<Double>? = null,

    /**
     * Minimum value of each component in this accessor.
     */
    val min: List<Double>? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * Specifies whether integer data values are normalized before usage.
     */
    val normalized: Boolean? = null,

    /**
     * Sparse storage of elements that deviate from their initialization value.
     */
    val sparse: GltfAccessorSparse? = null,

    /**
     * Specifies if the accessor's elements are scalars, vectors, or matrices.
     */
    val type: JsonObject?,
)

/**
 * Sparse storage of elements that deviate from their initialization value.
 *
 * Sparse storage of accessor values that deviate from their initialization value.
 */
@Serializable
internal data class GltfAccessorSparse(
    /**
     * Number of deviating accessor values stored in the sparse array.
     */
    val count: Long,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * An object pointing to a buffer view containing the indices of deviating accessor values.
     * The number of indices is equal to `count`. Indices **MUST** strictly increase.
     */
    val indices: GltfAccessorSparseIndices,

    /**
     * An object pointing to a buffer view containing the deviating accessor values.
     */
    val values: GltfAccessorSparseValues,
)

/**
 * An object pointing to a buffer view containing the indices of deviating accessor values.
 * The number of indices is equal to `count`. Indices **MUST** strictly increase.
 *
 * An object pointing to a buffer view containing the indices of deviating accessor values.
 * The number of indices is equal to `accessor.sparse.count`. Indices **MUST** strictly
 * increase.
 */
@Serializable
internal data class GltfAccessorSparseIndices(
    /**
     * The index of the buffer view with sparse indices. The referenced buffer view **MUST NOT**
     * have its `target` or `byteStride` properties defined. The buffer view and the optional
     * `byteOffset` **MUST** be aligned to the `componentType` byte length.
     */
    val bufferView: Long,

    /**
     * The offset relative to the start of the buffer view in bytes.
     */
    val byteOffset: Long? = null,

    /**
     * The indices data type.
     */
    val componentType: Long,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,
)

/**
 * An object pointing to a buffer view containing the deviating accessor values.
 *
 * An object pointing to a buffer view containing the deviating accessor values. The number
 * of elements is equal to `accessor.sparse.count` times number of components. The elements
 * have the same component type as the base accessor. The elements are tightly packed. Data
 * **MUST** be aligned following the same rules as the base accessor.
 */
@Serializable
internal data class GltfAccessorSparseValues(
    /**
     * The index of the bufferView with sparse values. The referenced buffer view **MUST NOT**
     * have its `target` or `byteStride` properties defined.
     */
    val bufferView: Long,

    /**
     * The offset relative to the start of the bufferView in bytes.
     */
    val byteOffset: Long? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,
)

/**
 * A keyframe animation.
 */
@Serializable
internal data class GltfAnimation(
    /**
     * An array of animation channels. An animation channel combines an animation sampler with a
     * target property being animated. Different channels of the same animation **MUST NOT**
     * have the same targets.
     */
    val channels: List<GltfAnimationChannel>,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * An array of animation samplers. An animation sampler combines timestamps with a sequence
     * of output values and defines an interpolation algorithm.
     */
    val samplers: List<GltfAnimationSampler>,
)

/**
 * An animation channel combines an animation sampler with a target property being animated.
 */
@Serializable
internal data class GltfAnimationChannel(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of a sampler in this animation used to compute the value for the target.
     */
    val sampler: Long,

    /**
     * The descriptor of the animated property.
     */
    val target: GltfAnimationChannelTarget,
)

/**
 * The descriptor of the animated property.
 */
@Serializable
internal data class GltfAnimationChannelTarget(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of the node to animate. When undefined, the animated object **MAY** be defined
     * by an extension.
     */
    val node: Long? = null,

    /**
     * The name of the node's TRS property to animate, or the `"weights"` of the Morph Targets
     * it instantiates. For the `"translation"` property, the values that are provided by the
     * sampler are the translation along the X, Y, and Z axes. For the `"rotation"` property,
     * the values are a quaternion in the order (x, y, z, w), where w is the scalar. For the
     * `"scale"` property, the values are the scaling factors along the X, Y, and Z axes.
     */
    val path: JsonObject?,
)

/**
 * An animation sampler combines timestamps with a sequence of output values and defines an
 * interpolation algorithm.
 */
@Serializable
internal data class GltfAnimationSampler(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of an accessor containing keyframe timestamps.
     */
    val input: Long,

    /**
     * Interpolation algorithm.
     */
    val interpolation: JsonObject? = null,

    /**
     * The index of an accessor, containing keyframe output values.
     */
    val output: Long,
)

/**
 * Metadata about the glTF asset.
 */
@Serializable
internal data class GltfAsset(
    /**
     * A copyright message suitable for display to credit the content creator.
     */
    val copyright: String? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * Tool that generated this glTF model.  Useful for debugging.
     */
    val generator: String? = null,

    /**
     * The minimum glTF version in the form of `<major>.<minor>` that this asset targets. This
     * property **MUST NOT** be greater than the asset version.
     */
    val minVersion: String? = null,

    /**
     * The glTF version in the form of `<major>.<minor>` that this asset targets.
     */
    val version: String,
)

/**
 * A view into a buffer generally representing a subset of the buffer.
 */
@Serializable
internal data class GltfBufferView(
    /**
     * The index of the buffer.
     */
    val buffer: Long,

    /**
     * The length of the bufferView in bytes.
     */
    val byteLength: Long,

    /**
     * The offset into the buffer in bytes.
     */
    val byteOffset: Long? = null,

    /**
     * The stride, in bytes.
     */
    val byteStride: Long? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The hint representing the intended GPU buffer type to use with this buffer view.
     */
    val target: Long? = null,
)

/**
 * A buffer points to binary geometry, animation, or skins.
 */
@Serializable
internal data class GltfBuffer(
    /**
     * The length of the buffer in bytes.
     */
    val byteLength: Long,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The URI (or IRI) of the buffer.
     */
    val uri: String? = null,
)

/**
 * A camera's projection.  A node **MAY** reference a camera to apply a transform to place
 * the camera in the scene.
 */
@Serializable
internal data class GltfCamera(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * An orthographic camera containing properties to create an orthographic projection matrix.
     * This property **MUST NOT** be defined when `perspective` is defined.
     */
    val orthographic: GltfCameraOrthographic? = null,

    /**
     * A perspective camera containing properties to create a perspective projection matrix.
     * This property **MUST NOT** be defined when `orthographic` is defined.
     */
    val perspective: GltfCameraPerspective? = null,

    /**
     * Specifies if the camera uses a perspective or orthographic projection.
     */
    val type: JsonObject?,
)

/**
 * An orthographic camera containing properties to create an orthographic projection matrix.
 * This property **MUST NOT** be defined when `perspective` is defined.
 *
 * An orthographic camera containing properties to create an orthographic projection matrix.
 */
@Serializable
internal data class GltfCameraOrthographic(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The floating-point horizontal magnification of the view. This value **MUST NOT** be equal
     * to zero. This value **SHOULD NOT** be negative.
     */
    val xmag: Double,

    /**
     * The floating-point vertical magnification of the view. This value **MUST NOT** be equal
     * to zero. This value **SHOULD NOT** be negative.
     */
    val ymag: Double,

    /**
     * The floating-point distance to the far clipping plane. This value **MUST NOT** be equal
     * to zero. `zfar` **MUST** be greater than `znear`.
     */
    val zfar: Double,

    /**
     * The floating-point distance to the near clipping plane.
     */
    val znear: Double,
)

/**
 * A perspective camera containing properties to create a perspective projection matrix.
 * This property **MUST NOT** be defined when `orthographic` is defined.
 *
 * A perspective camera containing properties to create a perspective projection matrix.
 */
@Serializable
internal data class GltfCameraPerspective(
    /**
     * The floating-point aspect ratio of the field of view.
     */
    val aspectRatio: Double? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The floating-point vertical field of view in radians. This value **SHOULD** be less than
     * π.
     */
    val yfov: Double,

    /**
     * The floating-point distance to the far clipping plane.
     */
    val zfar: Double? = null,

    /**
     * The floating-point distance to the near clipping plane.
     */
    val znear: Double,
)

/**
 * Image data used to create a texture. Image **MAY** be referenced by an URI (or IRI) or a
 * buffer view index.
 */
@Serializable
internal data class GltfImage(
    /**
     * The index of the bufferView that contains the image. This field **MUST NOT** be defined
     * when `uri` is defined.
     */
    val bufferView: Long? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The image's media type. This field **MUST** be defined when `bufferView` is defined.
     */
    val mimeType: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The URI (or IRI) of the image.
     */
    val uri: String? = null,
)

/**
 * The material appearance of a primitive.
 */
@Serializable
internal data class GltfMaterial(
    /**
     * The alpha cutoff value of the material.
     */
    val alphaCutoff: Double? = null,

    /**
     * The alpha rendering mode of the material.
     */
    val alphaMode: JsonObject? = null,

    /**
     * Specifies whether the material is double sided.
     */
    val doubleSided: Boolean? = null,

    /**
     * The factors for the emissive color of the material.
     */
    val emissiveFactor: List<Double>? = null,

    /**
     * The emissive texture.
     */
    val emissiveTexture: GltfTextureInfo? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The tangent space normal texture.
     */
    val normalTexture: GltfMaterialNormalTextureInfoClass? = null,

    /**
     * The occlusion texture.
     */
    val occlusionTexture: GltfMaterialOcclusionTextureInfoClass? = null,

    /**
     * A set of parameter values that are used to define the metallic-roughness material model
     * from Physically Based Rendering (PBR) methodology. When undefined, all the default values
     * of `pbrMetallicRoughness` **MUST** apply.
     */
    val pbrMetallicRoughness: GltfMaterialPBRMetallicRoughness? = null,
)

/**
 * The emissive texture.
 *
 * The base color texture.
 *
 * The metallic-roughness texture.
 *
 * Reference to a texture.
 */
@Serializable
internal data class GltfTextureInfo(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of the texture.
     */
    val index: Long,

    /**
     * The set index of texture's TEXCOORD attribute used for texture coordinate mapping.
     */
    val texCoord: Long? = null,
)

/**
 * The tangent space normal texture.
 *
 * Reference to a texture.
 */
@Serializable
internal data class GltfMaterialNormalTextureInfoClass(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of the texture.
     */
    val index: Long,

    /**
     * The scalar parameter applied to each normal vector of the normal texture.
     */
    val scale: Double? = null,

    /**
     * The set index of texture's TEXCOORD attribute used for texture coordinate mapping.
     */
    val texCoord: Long? = null,
)

/**
 * The occlusion texture.
 *
 * Reference to a texture.
 */
@Serializable
internal data class GltfMaterialOcclusionTextureInfoClass(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of the texture.
     */
    val index: Long,

    /**
     * A scalar multiplier controlling the amount of occlusion applied.
     */
    val strength: Double? = null,

    /**
     * The set index of texture's TEXCOORD attribute used for texture coordinate mapping.
     */
    val texCoord: Long? = null,
)

/**
 * A set of parameter values that are used to define the metallic-roughness material model
 * from Physically Based Rendering (PBR) methodology. When undefined, all the default values
 * of `pbrMetallicRoughness` **MUST** apply.
 *
 * A set of parameter values that are used to define the metallic-roughness material model
 * from Physically-Based Rendering (PBR) methodology.
 */
@Serializable
internal data class GltfMaterialPBRMetallicRoughness(
    /**
     * The factors for the base color of the material.
     */
    val baseColorFactor: List<Double>? = null,

    /**
     * The base color texture.
     */
    val baseColorTexture: GltfTextureInfo? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The factor for the metalness of the material.
     */
    val metallicFactor: Double? = null,

    /**
     * The metallic-roughness texture.
     */
    val metallicRoughnessTexture: GltfTextureInfo? = null,

    /**
     * The factor for the roughness of the material.
     */
    val roughnessFactor: Double? = null,
)

/**
 * A set of primitives to be rendered.  Its global transform is defined by a node that
 * references it.
 */
@Serializable
internal data class GltfMesh(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * An array of primitives, each defining geometry to be rendered.
     */
    val primitives: List<GltfMeshPrimitive>,

    /**
     * Array of weights to be applied to the morph targets. The number of array elements
     * **MUST** match the number of morph targets.
     */
    val weights: List<Double>? = null,
)

/**
 * Geometry to be rendered with the given material.
 */
@Serializable
internal data class GltfMeshPrimitive(
    /**
     * A plain JSON object, where each key corresponds to a mesh attribute semantic and each
     * value is the index of the accessor containing attribute's data.
     */
    val attributes: Map<String, Long>,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of the accessor that contains the vertex indices.
     */
    val indices: Long? = null,

    /**
     * The index of the material to apply to this primitive when rendering.
     */
    val material: Long? = null,

    /**
     * The topology type of primitives to render.
     */
    val mode: Long? = null,

    /**
     * An array of morph targets.
     */
    val targets: List<Map<String, Long>>? = null,
)

/**
 * A node in the node hierarchy.  When the node contains `skin`, all `mesh.primitives`
 * **MUST** contain `JOINTS_0` and `WEIGHTS_0` attributes.  A node **MAY** have either a
 * `matrix` or any combination of `translation`/`rotation`/`scale` (TRS) properties. TRS
 * properties are converted to matrices and postmultiplied in the `T * R * S` order to
 * compose the transformation matrix; first the scale is applied to the vertices, then the
 * rotation, and then the translation. If none are provided, the transform is the identity.
 * When a node is targeted for animation (referenced by an animation.channel.target),
 * `matrix` **MUST NOT** be present.
 */
@Serializable
internal data class GltfNode(
    /**
     * The index of the camera referenced by this node.
     */
    val camera: Long? = null,

    /**
     * The indices of this node's children.
     */
    val children: List<Long>? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * A floating-point 4x4 transformation matrix stored in column-major order.
     */
    val matrix: List<Double>? = null,

    /**
     * The index of the mesh in this node.
     */
    val mesh: Long? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The node's unit quaternion rotation in the order (x, y, z, w), where w is the scalar.
     */
    val rotation: List<Double>? = null,

    /**
     * The node's non-uniform scale, given as the scaling factors along the x, y, and z axes.
     */
    val scale: List<Double>? = null,

    /**
     * The index of the skin referenced by this node.
     */
    val skin: Long? = null,

    /**
     * The node's translation along the x, y, and z axes.
     */
    val translation: List<Double>? = null,

    /**
     * The weights of the instantiated morph target. The number of array elements **MUST** match
     * the number of morph targets of the referenced mesh. When defined, `mesh` **MUST** also be
     * defined.
     */
    val weights: List<Double>? = null,
)

/**
 * Texture sampler properties for filtering and wrapping modes.
 */
@Serializable
internal data class GltfSampler(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * Magnification filter.
     */
    val magFilter: Long? = null,

    /**
     * Minification filter.
     */
    val minFilter: Long? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * S (U) wrapping mode.
     */
    val wrapS: Long? = null,

    /**
     * T (V) wrapping mode.
     */
    val wrapT: Long? = null,
)

/**
 * The root nodes of a scene.
 */
@Serializable
internal data class GltfScene(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The indices of each root node.
     */
    val nodes: List<Long>? = null,
)

/**
 * Joints and matrices defining a skin.
 */
@Serializable
data class GltfSkin(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of the accessor containing the floating-point 4x4 inverse-bind matrices.
     */
    val inverseBindMatrices: Long? = null,

    /**
     * Indices of skeleton nodes, used as joints in this skin.
     */
    val joints: List<Long>,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The index of the node used as a skeleton root.
     */
    val skeleton: Long? = null,
)

/**
 * A texture and its sampler.
 */
@Serializable
internal data class GltfTexture(
    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The index of the sampler used by this texture. When undefined, a sampler with repeat
     * wrapping and auto filtering **SHOULD** be used.
     */
    val sampler: Long? = null,

    /**
     * The index of the image used by this texture. When undefined, an extension or other
     * mechanism **SHOULD** supply an alternate texture source, otherwise behavior is undefined.
     */
    val source: Long? = null,
)
