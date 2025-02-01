package com.littlekt.file.gltf

import com.littlekt.graphics.Color
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.material.UnlitMaterial
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.math.Vec3f
import com.littlekt.resources.Textures

/**
 * A configuration class that is used when generating a GLtf model.
 *
 * @param castShadows if `true`, then designate the model to cast shadows.
 * @author Colton Daily
 * @date 12/8/2024
 */
data class GltfModelConfig(val castShadows: Boolean)

/**
 * @param modelConfig the basic model config
 * @param materialStrategy the material strategy for when a primitive has a glTF material
 * @param fallbackMaterialStrategy the material strategy for when a primitive doesn't have a glTF
 *   material.
 */
data class GltfLoaderConfig(
    val modelConfig: GltfModelConfig,
    val materialStrategy: GltfModelMaterialStrategy,
    val fallbackMaterialStrategy: GltfModelFallbackMaterialStrategy,
)

/** A strategy for creating a [Material] for when a glTF primitive does not have a glTF material. */
abstract class GltfModelFallbackMaterialStrategy {
    abstract fun createMaterial(device: Device): Material
}

/** Creates an [UnlitMaterial] that defaults to a white texture. */
class UnlitMaterialFallbackStrategy : GltfModelFallbackMaterialStrategy() {
    override fun createMaterial(device: Device): Material =
        UnlitMaterial(device, Textures.textureWhite)
}

/** A strategy for creating a [Material] for a Gltf primitive. */
abstract class GltfModelMaterialStrategy {

    /**
     * Create a material when converting a [GltfData] to a renderable model.
     *
     * @param config basic gltf model config to adhere to
     * @param device gpu context
     * @param preferredFormat preferred format of textures
     * @param gltfMaterial the gltf material that needs created
     * @param gltfFile the gltf file as a whole
     * @return a newly created [Material]
     */
    abstract fun createMaterial(
        config: GltfModelConfig,
        device: Device,
        preferredFormat: TextureFormat,
        gltfMaterial: GltfMaterial,
        gltfFile: GltfData,
    ): Material

    /** Helper method to load textures for materials. */
    protected fun GltfTextureInfo?.loadTexture(
        device: Device,
        preferredFormat: TextureFormat,
        gltfFile: GltfData,
    ): Texture? = this?.getTexture(gltfFile, gltfFile.root, device, preferredFormat)
}

/** A [GltfModelMaterialStrategy] for creating a [PBRMaterial] from glTF data. */
class PBRMaterialStrategy : GltfModelMaterialStrategy() {
    override fun createMaterial(
        config: GltfModelConfig,
        device: Device,
        preferredFormat: TextureFormat,
        gltfMaterial: GltfMaterial,
        gltfFile: GltfData,
    ): Material {
        val baseColorFactor = gltfMaterial.pbrMetallicRoughness.baseColorFactor
        return PBRMaterial(
            device = device,
            metallicFactor = gltfMaterial.pbrMetallicRoughness.metallicFactor,
            roughnessFactor = gltfMaterial.pbrMetallicRoughness.roughnessFactor,
            metallicRoughnessTexture =
                gltfMaterial.pbrMetallicRoughness.metallicRoughnessTexture?.loadTexture(
                    device,
                    preferredFormat,
                    gltfFile,
                ),
            normalTexture =
                gltfMaterial.normalTexture?.loadTexture(device, preferredFormat, gltfFile),
            emissiveFactor =
                if (gltfMaterial.emissiveFactor.isNotEmpty()) {
                    Vec3f(
                        gltfMaterial.emissiveFactor[0],
                        gltfMaterial.emissiveFactor[1],
                        gltfMaterial.emissiveFactor[2],
                    )
                } else {
                    Vec3f(0f)
                },
            emissiveTexture =
                gltfMaterial.emissiveTexture.loadTexture(device, preferredFormat, gltfFile),
            baseColorFactor =
                Color(
                    baseColorFactor[0],
                    baseColorFactor[1],
                    baseColorFactor[2],
                    baseColorFactor[3],
                ),
            baseColorTexture =
                gltfMaterial.pbrMetallicRoughness.baseColorTexture?.loadTexture(
                    device,
                    preferredFormat,
                    gltfFile,
                ) ?: EmptyTexture(device, preferredFormat, 0, 0),
            transparent = gltfMaterial.alphaMode == GltfAlphaMode.Blend,
            doubleSided = gltfMaterial.doubleSided,
            alphaCutoff = gltfMaterial.alphaCutoff,
            castShadows = config.castShadows,
        )
    }
}

class UnlitMaterialStrategy : GltfModelMaterialStrategy() {
    override fun createMaterial(
        config: GltfModelConfig,
        device: Device,
        preferredFormat: TextureFormat,
        gltfMaterial: GltfMaterial,
        gltfFile: GltfData,
    ): Material {
        val baseColorFactor = gltfMaterial.pbrMetallicRoughness.baseColorFactor
        return UnlitMaterial(
            device = device,
            baseColorFactor =
                Color(
                    baseColorFactor[0],
                    baseColorFactor[1],
                    baseColorFactor[2],
                    baseColorFactor[3],
                ),
            baseColorTexture =
                gltfMaterial.pbrMetallicRoughness.baseColorTexture?.loadTexture(
                    device,
                    preferredFormat,
                    gltfFile,
                ) ?: EmptyTexture(device, preferredFormat, 0, 0),
            transparent = gltfMaterial.alphaMode != GltfAlphaMode.Opaque,
            doubleSided = gltfMaterial.doubleSided,
        )
    }
}

/** Creates a [GltfLoaderConfig], specific for PBR usage. */
fun GltfLoaderPbrConfig(modelConfig: GltfModelConfig = GltfModelConfig(castShadows = true)) =
    GltfLoaderConfig(modelConfig, PBRMaterialStrategy(), UnlitMaterialFallbackStrategy())

/** Creates a [GltfLoaderConfig], specific for unlit usage. */
fun GltfLoaderUnlitConfig(modelConfig: GltfModelConfig = GltfModelConfig(castShadows = true)) =
    GltfLoaderConfig(modelConfig, UnlitMaterialStrategy(), UnlitMaterialFallbackStrategy())
