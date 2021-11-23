package com.lehaine.littlekt.graphics

class Color(rgba8888: Int = 0) {

    var r: Float = ((rgba8888 and 0xff000000.toInt()) ushr 24) / 255f
    var g: Float = ((rgba8888 and 0x00ff0000) ushr 16) / 255f
    var b: Float = ((rgba8888 and 0x0000ff00) ushr 8) / 255f
    var a: Float = (rgba8888 and 0x000000ff) / 255f

    fun setRgba8888(rgba8888: Int) {
        r = ((rgba8888 and 0xff000000.toInt()) ushr 24) / 255f
        g = ((rgba8888 and 0x00ff0000) ushr 16) / 255f
        b = ((rgba8888 and 0x0000ff00) ushr 8) / 255f
        a = (rgba8888 and 0x000000ff) / 255f
    }

    companion object {
        operator fun invoke(r: Float, g: Float, b: Float, a: Float): Color = Color(rgba8888(r, g, b, a))

        fun rgba8888(r: Float, g: Float, b: Float, a: Float) =
            ((r * 255).toInt() shl 24) or ((g * 255).toInt() shl 16) or ((b * 255).toInt() shl 8) or (a * 255).toInt()

        val CLEAR = Color(0f, 0f, 0f, 0f)

        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
        val LIGHT_GRAY = Color(-0x40404001)
        val GRAY = Color(0x7f7f7fff)
        val DARK_GRAY = Color(0x3f3f3fff)

        // Blue Colors
        val BLUE = Color(0f, 0f, 1f, 1f)
        val NAVY = Color(0f, 0f, 0.5f, 1f)
        val ROYAL = Color(0x4169e1ff)
        val SLATE = Color(0x708090ff)
        val SKY = Color(-0x78311401)
        val CYAN = Color(0f, 1f, 1f, 1f)
        val TEAL = Color(0f, 0.5f, 0.5f, 1f)

        // Green Colors
        val GREEN = Color(0x00ff00ff)
        val CHARTREUSE = Color(0x7fff00ff)
        val LIME = Color(0x32cd32ff)
        val FOREST = Color(0x228b22ff)
        val OLIVE = Color(0x6b8e23ff)

        // Yellow Colors
        val YELLOW = Color(-0xff01)
        val GOLD = Color(-0x28ff01)
        val GOLDENROD = Color(-0x255adf01)
        val ORANGE = Color(-0x5aff01)

        // Brown Colors
        val BROWN = Color(-0x74baec01)
        val TAN = Color(-0x2d4b7301)
        val FIREBRICK = Color(-0x4ddddd01)

        // Red Colors
        val RED = Color(-0xffff01)
        val SCARLET = Color(-0xcbe301)
        val CORAL = Color(-0x80af01)
        val SALMON = Color(-0x57f8d01)
        val PINK = Color(-0x964b01)
        val MAGENTA = Color(1f, 0f, 1f, 1f)

        // Purple Colors
        val PURPLE = Color(-0x5fdf0f01)
        val VIOLET = Color(-0x117d1101)
    }
}

fun Color.abgr(r: Int, g: Int, b: Int, a: Int) = (a shl 24) or (b shl 16) or (g shl 8) or r
fun Color.rgba8888() = Color.rgba8888(r, g, b, a)
fun Color.rgba() = rgba8888()
fun Color.toFloatBits(): Float {
    val bits =
        (((255 * a).toInt() shl 24) or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()) and 0xfeffffff.toInt()
    return Float.fromBits(bits)
}