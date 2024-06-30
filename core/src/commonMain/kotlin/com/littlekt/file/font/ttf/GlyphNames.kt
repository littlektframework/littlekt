package com.littlekt.file.font.ttf

import com.littlekt.file.font.ttf.table.Post

/**
 * @author Colton Daily
 * @date 11/30/2021
 */
internal class GlyphNames(val post: Post) {
    val names: Array<String> =
        when (post.version) {
            1f -> Encoding.STANDARD_NAMES.copyOf()
            2f -> {
                Array(post.numberOfGlyphs) {
                    if (post.glyphNameIndex[it] < Encoding.STANDARD_NAMES.size) {
                        Encoding.STANDARD_NAMES[post.glyphNameIndex[it]]
                    } else {
                        post.names[post.glyphNameIndex[it] - Encoding.STANDARD_NAMES.size]
                    }
                }
            }
            2.5f -> {
                Array(post.numberOfGlyphs) { Encoding.STANDARD_NAMES[it + post.glyphNameIndex[it]] }
            }
            else -> arrayOf()
        }
}
