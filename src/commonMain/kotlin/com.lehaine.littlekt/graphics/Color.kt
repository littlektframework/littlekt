package com.lehaine.littlekt.graphics

class Color(val rgba8888: Int = 0) {

    var r: Float = ((rgba8888 and 0xff000000.toInt()) ushr 24) / 255f
    var g: Float = ((rgba8888 and 0x00ff0000) ushr 16) / 255f
    var b: Float = ((rgba8888 and 0x0000ff00) ushr 8) / 255f
    var a: Float = (rgba8888 and 0x000000ff) / 255f


    companion object {
        operator fun invoke(r: Float, g: Float, b: Float, a: Float): Color = Color(rgba8888(r, g, b, a))

        fun rgba8888(r: Float, g: Float, b: Float, a: Float) =
            ((r * 255).toInt() shl 24) or ((g * 255).toInt() shl 16) or ((b * 255).toInt() shl 8) or (a * 255).toInt()

        /** List of Colors **/
        val WHITE get() = Color(1f, 1f, 1f, 1f)
        val BLACK get() = Color(0f, 0f, 0f, 1f)
        val RED get() = Color(1f, 0f, 0f, 1f)
        val BLUE get() = Color(0f, 0f, 1f, 1f)
        val GREEN get() = Color(0f, 1f, 0f, 1f)
        val CLEAR get() = Color(0f, 0f, 0f, 0f)
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