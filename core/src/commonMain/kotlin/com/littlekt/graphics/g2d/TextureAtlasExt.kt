package com.littlekt.graphics.g2d

import com.littlekt.Context
import com.littlekt.graphics.Texture
import com.littlekt.graphics.slice
import com.littlekt.util.MutableTextureAtlas

/**
 * Combine a [TextureSlice] with a [TextureAtlas] into a new [TextureAtlas].
 *
 * **This does not release of the [TextureAtlas] or the [Texture]!!**
 *
 * @param slice the texture slice to combine
 * @param name the name to give the texture slice in the [TextureAtlas]
 * @param context the current [Context]
 */
fun TextureAtlas.combine(slice: TextureSlice, name: String, context: Context) =
    MutableTextureAtlas(context)
        .apply {
            add(slice, name)
            add(this@combine)
        }
        .toImmutable()

/**
 * Combine a [Texture] with a [TextureAtlas] into a new [TextureAtlas].
 *
 * **This does not release of the [TextureAtlas] or the [Texture]!!**
 *
 * @param texture the texture to combine
 * @param name the name to give the texture in the [TextureAtlas]
 * @param context the current [Context]
 */
fun TextureAtlas.combine(texture: Texture, name: String, context: Context) =
    combine(texture.slice(), name, context)

/**
 * Combine another [TextureAtlas] with the current atlas to create a new [TextureAtlas].
 *
 * **This does not release of either [TextureAtlas]!!**
 *
 * @param atlas the texture atlas to combine
 * @param context the current [Context]
 */
fun TextureAtlas.combine(atlas: TextureAtlas, context: Context) =
    MutableTextureAtlas(context)
        .apply {
            add(atlas)
            add(this@combine)
        }
        .toImmutable()
