package com.lehaine.littlekt.graphics.font

import com.lehaine.littlekt.util.internal.insert

class Kerning(
    val first: Int,
    val second: Int,
    val amount: Int
) {
    companion object {
        fun buildKey(f: Int, s: Int) = 0.insert(f, 0, 16).insert(s, 16, 16)
    }
}