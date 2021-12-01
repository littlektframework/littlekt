package com.lehaine.littlekt.file.atlas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @author Colton Daily
 * @date 11/27/2021
 */

internal data class AtlasInfo(val meta: AtlasPage.Meta, val pages: List<AtlasPage>) {
    val frames = pages.flatMap { it.frames }
    val framesMap = frames.associateBy { it.name }
}

@Serializable
internal data class AtlasPage(
    val meta: Meta,
    var filterMin: Boolean = false,
    var filterMag: Boolean = false,
    var repeatX: Boolean = false,
    var repeatY: Boolean = false,
    val frames: List<Frame>
) {
    @Serializable
    internal data class Frame(
        @SerialName("filename")
        val name: String,
        val frame: Rect,
        val rotated: Boolean,
        val trimmed: Boolean,
        val spriteSourceSize: Rect,
        val sourceSize: Size
    ) {
        fun applyRotation() = if (rotated) {
            copy(
                frame = frame.copy(w = frame.h, h = frame.w),
                spriteSourceSize = spriteSourceSize.copy(
                    x = spriteSourceSize.y,
                    y = spriteSourceSize.x,
                    w = spriteSourceSize.h,
                    h = spriteSourceSize.w
                )
            )
        } else {
            this
        }
    }

    @Serializable
    internal data class Rect(val x: Int, val y: Int, val w: Int, val h: Int)

    @Serializable
    internal data class Size(val w: Int, val h: Int)

    @Serializable
    internal data class Meta(
        val app: String = "",
        val version: String = "1.0.0",
        val image: String = "",
        val format: String = "",
        val scale: Float = 1f,
        val size: Size = Size(0, 0),
        val frameTags: List<FrameTag> = listOf(),
        val layers: List<Layer> = listOf(),
        val slices: List<Slice> = listOf()
    )

    @Serializable
    internal data class FrameTag(
        val name: String,
        val from: Int,
        val to: Int,
        val direction: String
    )

    @Serializable
    internal data class Layer(
        val name: String,
        val opacity: Int,
        val blendMode: String
    )

    @Serializable
    internal data class Slice(
        val name: String,
        val color: String,
        val key: List<Key>
    )

    @Serializable
    internal data class Key(
        val frame: Int,
        val bound: Rect
    )
}