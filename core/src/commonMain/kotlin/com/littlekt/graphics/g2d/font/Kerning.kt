package com.littlekt.graphics.g2d.font

import com.littlekt.util.internal.insert

class Kerning(val first: Int, val second: Int, val amount: Int) {
    companion object {
        fun buildKey(f: Int, s: Int) = 0.insert(f, 0, 16).insert(s, 16, 16)
    }
}
