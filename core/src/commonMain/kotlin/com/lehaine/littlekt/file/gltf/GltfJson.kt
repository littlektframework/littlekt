package com.lehaine.littlekt.file.gltf

import com.lehaine.littlekt.file.ByteBuffer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

/**
 * The root object for a glTF asset.
 */
@Serializable
internal data class GltfFile(
    /**
     * An array of accessors.
     */
    val accessors: List<GltfAccessor> = emptyList(),

    /**
     * An array of keyframe animations.
     */
    val animations: List<GltfAnimation> = emptyList(),

    /**
     * Metadata about the glTF asset.
     */
    val asset: GltfAsset,

    /**
     * An array of buffers.
     */
    val buffers: List<GltfBuffer> = emptyList(),

    /**
     * An array of bufferViews.
     */
    val bufferViews: List<GltfBufferView> = emptyList(),

    /**
     * An array of cameras.
     */
    val cameras: List<GltfCamera> = emptyList(),

    val extensions: JsonObject? = null,

    /**
     * Names of glTF extensions required to properly load this asset.
     */
    val extensionsRequired: List<String> = emptyList(),

    /**
     * Names of glTF extensions used in this asset.
     */
    val extensionsUsed: List<String> = emptyList(),

    val extras: JsonObject? = null,

    /**
     * An array of images.
     */
    val images: List<GltfImage> = emptyList(),

    /**
     * An array of materials.
     */
    val materials: List<GltfMaterial> = emptyList(),

    /**
     * An array of meshes.
     */
    val meshes: List<GltfMesh> = emptyList(),

    /**
     * An array of nodes.
     */
    val nodes: List<GltfNode> = emptyList(),

    /**
     * An array of samplers.
     */
    val samplers: List<GltfSampler> = emptyList(),

    /**
     * The index of the default scene.
     */
    val scene: Long? = null,

    /**
     * An array of scenes.
     */
    val scenes: List<GltfScene> = emptyList(),

    /**
     * An array of skins.
     */
    val skins: List<GltfSkin> = emptyList(),

    /**
     * An array of textures.
     */
    val textures: List<GltfTexture> = emptyList(),
) {
    companion object {
        const val GLB_FILE_MAGIC = 0x46546C67
        const val GLB_CHUNK_MAGIC_JSON = 0x4E4F534A
        const val GLB_CHUNK_MAGIC_BIN = 0x004E4942
    }
}

/**
 * A typed view into a buffer view that contains raw binary data.
 */
@Serializable
internal data class GltfAccessor(
    /**
     * The index of the bufferView.
     */
    val bufferView: Int = -1,

    /**
     * The offset relative to the start of the buffer view in bytes.
     */
    val byteOffset: Int = 0,

    /**
     * The datatype of the accessor's components.
     */
    val componentType: Int,

    /**
     * The number of elements referenced by this accessor.
     */
    val count: Int,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * Maximum value of each component in this accessor.
     */
    val max: List<Float> = emptyList(),

    /**
     * Minimum value of each component in this accessor.
     */
    val min: List<Float> = emptyList(),

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * Specifies whether integer data values are normalized before usage.
     */
    val normalized: Boolean = false,

    /**
     * Sparse storage of elements that deviate from their initialization value.
     */
    val sparse: GltfAccessorSparse? = null,

    /**
     * Specifies if the accessor's elements are scalars, vectors, or matrices.
     */
    val type: String,
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
    val count: Int,

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
    val bufferView: Int,

    /**
     * The offset relative to the start of the buffer view in bytes.
     */
    val byteOffset: Int = 0,

    /**
     * The indices data type.
     */
    val componentType: Int,

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
    val bufferView: Int,

    /**
     * The offset relative to the start of the bufferView in bytes.
     */
    val byteOffset: Int = 0,

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
    val sampler: Int,

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
    val node: Int = -1,

    /**
     * The name of the node's TRS property to animate, or the `"weights"` of the Morph Targets
     * it instantiates. For the `"translation"` property, the values that are provided by the
     * sampler are the translation along the X, Y, and Z axes. For the `"rotation"` property,
     * the values are a quaternion in the order (x, y, z, w), where w is the scalar. For the
     * `"scale"` property, the values are the scaling factors along the X, Y, and Z axes.
     */
    val path: String,
) {

    companion object {
        const val PATH_TRANSLATION = "translation"
        const val PATH_ROTATION = "rotation"
        const val PATH_SCALE = "scale"
        const val PATH_WEIGHTS = "weights"
    }
}

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
    val input: Int,

    /**
     * Interpolation algorithm.
     */
    val interpolation: String = INTERPOLATION_LINEAR,

    /**
     * The index of an accessor, containing keyframe output values.
     */
    val output: Int,
) {
    companion object {
        const val INTERPOLATION_LINEAR = "LINEAR"
        const val INTERPOLATION_STEP = "STEP"
        const val INTERPOLATION_CUBICSPLINE = "CUBICSPLINE"
    }
}

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
    val buffer: Int,

    /**
     * The length of the bufferView in bytes.
     */
    val byteLength: Int,

    /**
     * The offset into the buffer in bytes.
     */
    val byteOffset: Int = 0,

    /**
     * The stride, in bytes.
     */
    val byteStride: Int = 0,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The hint representing the intended GPU buffer type to use with this buffer view.
     */
    val target: Int = 0,
)

/**
 * A buffer points to binary geometry, animation, or skins.
 */
@Serializable
internal data class GltfBuffer(
    /**
     * The length of the buffer in bytes.
     */
    val byteLength: Int,

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
) {
    @Transient
    lateinit var data: ByteBuffer
}

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
    val bufferView: Int = -1,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The image's media type. This field **MUST** be defined when `bufferView` is defined.
     */
    val mimeType: String? = null,

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
    val alphaCutoff: Float = 0.5f,

    /**
     * The alpha rendering mode of the material.
     */
    val alphaMode: String = ALPHA_MODE_OPAQUE,

    /**
     * Specifies whether the material is double sided.
     */
    val doubleSided: Boolean = false,

    /**
     * The factors for the emissive color of the material.
     */
    val emissiveFactor: List<Float> = emptyList(),

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
    val normalTexture: GltfTextureInfo? = null,

    /**
     * The occlusion texture.
     */
    val occlusionTexture: GltfTextureInfo? = null,

    /**
     * A set of parameter values that are used to define the metallic-roughness material model
     * from Physically Based Rendering (PBR) methodology. When undefined, all the default values
     * of `pbrMetallicRoughness` **MUST** apply.
     */
    val pbrMetallicRoughness: GltfMaterialPBRMetallicRoughness = GltfMaterialPBRMetallicRoughness(
        baseColorFactor = listOf(
            0.5f,
            0.5f,
            0.5f,
            1f
        )
    ),
) {
    companion object {
        const val ALPHA_MODE_BLEND = "BLEND"
        const val ALPHA_MODE_MASK = "MASK"
        const val ALPHA_MODE_OPAQUE = "OPAQUE"
    }
}

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
    val index: Int,

    /**
     * The set index of texture's TEXCOORD attribute used for texture coordinate mapping.
     */
    val texCoord: Int = 0,
    val strength: Float = 1f,
    val scale: Float = 1f,
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
    val baseColorFactor: List<Float> = listOf(1f, 1f, 1f, 1f),

    /**
     * The base color texture.
     */
    val baseColorTexture: GltfTextureInfo? = null,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The factor for the metalness of the material.
     */
    val metallicFactor: Float = 1f,

    /**
     * The metallic-roughness texture.
     */
    val metallicRoughnessTexture: GltfTextureInfo? = null,

    /**
     * The factor for the roughness of the material.
     */
    val roughnessFactor: Float = 1f,
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
    val weights: List<Float> = emptyList(),
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
    val attributes: Map<String, Int>,

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * The index of the accessor that contains the vertex indices.
     */
    val indices: Int = -1,

    /**
     * The index of the material to apply to this primitive when rendering.
     */
    val material: Int = -1,

    /**
     * The topology type of primitives to render.
     */
    val mode: Int = MODE_TRIANGLES,

    /**
     * An array of morph targets.
     */
    val targets: List<Map<String, Int>> = emptyList(),
) {
    companion object {
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
    val camera: Int = -1,

    /**
     * The indices of this node's children.
     */
    val children: List<Int> = emptyList(),

    val extensions: JsonObject? = null,
    val extras: JsonObject? = null,

    /**
     * A floating-point 4x4 transformation matrix stored in column-major order.
     */
    val matrix: List<Float> = emptyList(),

    /**
     * The index of the mesh in this node.
     */
    val mesh: Int = -1,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The node's unit quaternion rotation in the order (x, y, z, w), where w is the scalar.
     */
    val rotation: List<Float> = emptyList(),

    /**
     * The node's non-uniform scale, given as the scaling factors along the x, y, and z axes.
     */
    val scale: List<Float> = emptyList(),

    /**
     * The index of the skin referenced by this node.
     */
    val skin: Int = -1,

    /**
     * The node's translation along the x, y, and z axes.
     */
    val translation: List<Float> = emptyList(),

    /**
     * The weights of the instantiated morph target. The number of array elements **MUST** match
     * the number of morph targets of the referenced mesh. When defined, `mesh` **MUST** also be
     * defined.
     */
    val weights: List<Float> = emptyList(),
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
    val magFilter: Int? = null,

    /**
     * Minification filter.
     */
    val minFilter: Int? = null,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * S (U) wrapping mode.
     */
    val wrapS: Int? = null,

    /**
     * T (V) wrapping mode.
     */
    val wrapT: Int? = null,
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
    val nodes: List<Int> = emptyList(),
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
    val inverseBindMatrices: Int = -1,

    /**
     * Indices of skeleton nodes, used as joints in this skin.
     */
    val joints: List<Int>,

    /**
     * The user-defined name of this object.
     */
    val name: String? = null,

    /**
     * The index of the node used as a skeleton root.
     */
    val skeleton: Int = -1,
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
    val sampler: Int = -1,

    /**
     * The index of the image used by this texture. When undefined, an extension or other
     * mechanism **SHOULD** supply an alternate texture source, otherwise behavior is undefined.
     */
    val source: Int = 0,
)
