package com.littlekt.graph.node.resource

import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.g2d.TextureSlice

/** @return a new [NinePatchDrawable] from the ninepatch. */
fun NinePatch.toDrawable() = NinePatchDrawable(this)

/** @return a new [TextureSliceDrawable] from the texture. */
fun Texture.toDrawable() = TextureSliceDrawable(this)

/** @return a new [TextureSliceDrawable] from the texture slice */
fun TextureSlice.toDrawable() = TextureSliceDrawable(this)
