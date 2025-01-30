package com.littlekt.file.vfs

import com.littlekt.graphics.webgpu.SamplerDescriptor
import com.littlekt.graphics.webgpu.TextureFormat

/**
 * @param format [TextureFormat] to load this texture as
 * @param generateMipMaps if `true`, mipmaps will be automatically generated when the texture is
 *   loaded
 * @param samplerDescriptor an optional [SamplerDescriptor] to update any sampler related
 *   parameters.
 * @author Colton Daily
 * @date 1/3/2025
 */
data class TextureOptions(
    val format: TextureFormat,
    val generateMipMaps: Boolean = true,
    val samplerDescriptor: SamplerDescriptor = SamplerDescriptor(),
)
