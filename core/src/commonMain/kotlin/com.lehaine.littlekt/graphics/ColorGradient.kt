package com.lehaine.littlekt.graphics

import com.lehaine.littlekt.graphics.Color.Companion.fromHex
import com.lehaine.littlekt.math.clamp
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 11/23/2021
 */
class ColorGradient(vararg colors: Pair<Float, Color>, n: Int = DEFAULT_N, toLinear: Boolean = false) {

    private val gradient = Array(n) { MutableColor() }

    constructor(vararg colors: Color, n: Int = DEFAULT_N) :
            this(*Array(colors.size) { i -> i.toFloat() to colors[i] }, n = n)

    init {
        if (colors.size < 2) {
            throw RuntimeException("ColorGradient requires at least two colors")
        }

        colors.sortBy { it.first }
        val mi = colors.first().first
        val mx = colors.last().first

        var pi = 0
        var p0 = colors[pi++]
        var p1 = colors[pi++]
        for (i in 0 until n) {
            val p = i / (n - 1f) * (mx - mi) + mi
            while (p > p1.first) {
                p0 = p1
                p1 = colors[min(pi++, colors.size)]
            }
            val w0 = 1f - (p - p0.first) / (p1.first - p0.first)
            gradient[i].set(p0.second).scale(w0).add(p1.second, 1f - w0)
            if (toLinear) {
                gradient[i] = gradient[i].toLinear()
            }
        }
    }

    fun getColor(value: Float, min: Float = 0f, max: Float = 1f): Color {
        val f = (value - min) / (max - min) * gradient.size
        val i = f.toInt()
        return gradient[i.clamp(0, gradient.size - 1)]
    }

    fun getColorInterpolated(value: Float, result: MutableColor, min: Float = 0f, max: Float = 1f): MutableColor {
        val fi = ((value - min) / (max - min) * gradient.size).clamp(0f, gradient.size - 1f)
        val iLower = fi.toInt().clamp(0, gradient.size - 1)
        val iUpper = (iLower + 1).clamp(0, gradient.size - 1)
        val wUpper = iUpper - fi
        val wLower = 1f - wUpper
        val cUpper = gradient[iUpper]
        result.set(gradient[iLower]).scale(wLower)
        result.r += cUpper.r * wUpper
        result.g += cUpper.g * wUpper
        result.b += cUpper.b * wUpper
        result.a += cUpper.a * wUpper
        return result
    }

    fun inverted(): ColorGradient {
        val invertedColors =
            Array<Pair<Float, Color>>(gradient.size) { i -> i.toFloat() to gradient[gradient.lastIndex - i] }
        return ColorGradient(*invertedColors, n = gradient.size)
    }

    companion object {
        const val DEFAULT_N = 256

        private val MD_BLUE = Color.fromHex("2196F3")
        private val MD_CYAN = Color.fromHex("00BCD4")
        private val MD_GREEN = Color.fromHex("4CAF50")
        private val MD_YELLOW = Color.fromHex("FFEB3B")
        private val MD_RED = Color.fromHex("F44336")
        private val MD_PURPLE = Color.fromHex("9C27B0")

        val JET = ColorGradient(Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA)

        val JET_MD = ColorGradient(MD_BLUE, MD_CYAN, MD_GREEN, MD_YELLOW, MD_RED, MD_PURPLE)

        val RED_YELLOW_GREEN = ColorGradient(Color.RED, Color.YELLOW, Color.GREEN)

        val RED_YELLOW_GREEN_MD = ColorGradient(MD_RED, MD_YELLOW, MD_GREEN)

        val RED_WHITE_BLUE = ColorGradient(
            0f to Color(0.35f, 0f, 0f, 1f),
            0.35f to Color(0.81f, 0.39f, 0f, 1f),
            0.5f to Color.WHITE,
            0.75f to Color(0f, 0.5f, 1f, 1f),
            1f to Color(0f, 0.18f, 0.47f, 1f)
        )

        val PLASMA = ColorGradient(
            Color(0.0504f, 0.0298f, 0.5280f, 1f),
            Color(0.1324f, 0.0223f, 0.5633f, 1f),
            Color(0.1934f, 0.0184f, 0.5903f, 1f),
            Color(0.2546f, 0.0139f, 0.6154f, 1f),
            Color(0.3062f, 0.0089f, 0.6337f, 1f),
            Color(0.3564f, 0.0038f, 0.6478f, 1f),
            Color(0.4055f, 0.0007f, 0.6570f, 1f),
            Color(0.4596f, 0.0036f, 0.6603f, 1f),
            Color(0.5065f, 0.0163f, 0.6562f, 1f),
            Color(0.5517f, 0.0431f, 0.6453f, 1f),
            Color(0.5950f, 0.0772f, 0.6279f, 1f),
            Color(0.6360f, 0.1121f, 0.6052f, 1f),
            Color(0.6792f, 0.1518f, 0.5752f, 1f),
            Color(0.7149f, 0.1873f, 0.5463f, 1f),
            Color(0.7483f, 0.2227f, 0.5168f, 1f),
            Color(0.7796f, 0.2581f, 0.4875f, 1f),
            Color(0.8126f, 0.2979f, 0.4553f, 1f),
            Color(0.8402f, 0.3336f, 0.4275f, 1f),
            Color(0.8661f, 0.3697f, 0.4001f, 1f),
            Color(0.8903f, 0.4064f, 0.3731f, 1f),
            Color(0.9155f, 0.4488f, 0.3429f, 1f),
            Color(0.9356f, 0.4877f, 0.3160f, 1f),
            Color(0.9534f, 0.5280f, 0.2889f, 1f),
            Color(0.9685f, 0.5697f, 0.2617f, 1f),
            Color(0.9806f, 0.6130f, 0.2346f, 1f),
            Color(0.9899f, 0.6638f, 0.2049f, 1f),
            Color(0.9941f, 0.7107f, 0.1801f, 1f),
            Color(0.9939f, 0.7593f, 0.1591f, 1f),
            Color(0.9886f, 0.8096f, 0.1454f, 1f),
            Color(0.9763f, 0.8680f, 0.1434f, 1f),
            Color(0.9593f, 0.9214f, 0.1516f, 1f),
            Color(0.9400f, 0.9752f, 0.1313f, 1f)
        )

        val VIRIDIS = ColorGradient(
            Color(0.2670f, 0.0049f, 0.3294f, 1f),
            Color(0.2770f, 0.0503f, 0.3757f, 1f),
            Color(0.2823f, 0.0950f, 0.4173f, 1f),
            Color(0.2826f, 0.1409f, 0.4575f, 1f),
            Color(0.2780f, 0.1804f, 0.4867f, 1f),
            Color(0.2693f, 0.2188f, 0.5096f, 1f),
            Color(0.2573f, 0.2561f, 0.5266f, 1f),
            Color(0.2412f, 0.2965f, 0.5397f, 1f),
            Color(0.2259f, 0.3308f, 0.5473f, 1f),
            Color(0.2105f, 0.3637f, 0.5522f, 1f),
            Color(0.1959f, 0.3954f, 0.5553f, 1f),
            Color(0.1823f, 0.4262f, 0.5571f, 1f),
            Color(0.1681f, 0.4600f, 0.5581f, 1f),
            Color(0.1563f, 0.4896f, 0.5579f, 1f),
            Color(0.1448f, 0.5191f, 0.5566f, 1f),
            Color(0.1337f, 0.5485f, 0.5535f, 1f),
            Color(0.1235f, 0.5817f, 0.5474f, 1f),
            Color(0.1194f, 0.6111f, 0.5390f, 1f),
            Color(0.1248f, 0.6405f, 0.5271f, 1f),
            Color(0.1433f, 0.6695f, 0.5112f, 1f),
            Color(0.1807f, 0.7014f, 0.4882f, 1f),
            Color(0.2264f, 0.7289f, 0.4628f, 1f),
            Color(0.2815f, 0.7552f, 0.4326f, 1f),
            Color(0.3441f, 0.7800f, 0.3974f, 1f),
            Color(0.4129f, 0.8030f, 0.3573f, 1f),
            Color(0.4966f, 0.8264f, 0.3064f, 1f),
            Color(0.5756f, 0.8446f, 0.2564f, 1f),
            Color(0.6576f, 0.8602f, 0.2031f, 1f),
            Color(0.7414f, 0.8734f, 0.1496f, 1f),
            Color(0.8353f, 0.8860f, 0.1026f, 1f),
            Color(0.9162f, 0.8961f, 0.1007f, 1f),
            Color(0.9932f, 0.9062f, 0.1439f, 1f)
        )

        val RED = ColorGradient(
            50f to fromHex("FFEBEE"),
            100f to fromHex("FFCDD2"),
            200f to fromHex("EF9A9A"),
            300f to fromHex("E57373"),
            400f to fromHex("EF5350"),
            500f to fromHex("F44336"),
            600f to fromHex("E53935"),
            700f to fromHex("D32F2F"),
            800f to fromHex("C62828"),
            900f to fromHex("B71C1C")
        )

        val PINK = ColorGradient(
            50f to fromHex("FCE4EC"),
            100f to fromHex("F8BBD0"),
            200f to fromHex("F48FB1"),
            300f to fromHex("F06292"),
            400f to fromHex("EC407A"),
            500f to fromHex("E91E63"),
            600f to fromHex("D81B60"),
            700f to fromHex("C2185B"),
            800f to fromHex("AD1457"),
            900f to fromHex("880E4F")
        )

        val PURPLE = ColorGradient(
            50f to fromHex("F3E5F5"),
            100f to fromHex("E1BEE7"),
            200f to fromHex("CE93D8"),
            300f to fromHex("BA68C8"),
            400f to fromHex("AB47BC"),
            500f to fromHex("9C27B0"),
            600f to fromHex("8E24AA"),
            700f to fromHex("7B1FA2"),
            800f to fromHex("6A1B9A"),
            900f to fromHex("4A148C")
        )

        val DEEP_PURPLE = ColorGradient(
            50f to fromHex("EDE7F6"),
            100f to fromHex("D1C4E9"),
            200f to fromHex("B39DDB"),
            300f to fromHex("9575CD"),
            400f to fromHex("7E57C2"),
            500f to fromHex("673AB7"),
            600f to fromHex("5E35B1"),
            700f to fromHex("512DA8"),
            800f to fromHex("4527A0"),
            900f to fromHex("311B92")
        )

        val INDIGO = ColorGradient(
            50f to fromHex("E8EAF6"),
            100f to fromHex("C5CAE9"),
            200f to fromHex("9FA8DA"),
            300f to fromHex("7986CB"),
            400f to fromHex("5C6BC0"),
            500f to fromHex("3F51B5"),
            600f to fromHex("3949AB"),
            700f to fromHex("303F9F"),
            800f to fromHex("283593"),
            900f to fromHex("1A237E"),
        )

        val BLUE = ColorGradient(
            50f to fromHex("E3F2FD"),
            100f to fromHex("BBDEFB"),
            200f to fromHex("90CAF9"),
            300f to fromHex("64B5F6"),
            400f to fromHex("42A5F5"),
            500f to fromHex("2196F3"),
            600f to fromHex("1E88E5"),
            700f to fromHex("1976D2"),
            800f to fromHex("1565C0"),
            900f to fromHex("0D47A1"),
        )

        val LIGHT_BLUE = ColorGradient(
            50f to fromHex("E1F5FE"),
            100f to fromHex("B3E5FC"),
            200f to fromHex("81D4FA"),
            300f to fromHex("4FC3F7"),
            400f to fromHex("29B6F6"),
            500f to fromHex("03A9F4"),
            600f to fromHex("039BE5"),
            700f to fromHex("0288D1"),
            800f to fromHex("0277BD"),
            900f to fromHex("01579B"),
        )

        val CYAN = ColorGradient(
            50f to fromHex("E0F7FA"),
            100f to fromHex("B2EBF2"),
            200f to fromHex("80DEEA"),
            300f to fromHex("4DD0E1"),
            400f to fromHex("26C6DA"),
            500f to fromHex("00BCD4"),
            600f to fromHex("00ACC1"),
            700f to fromHex("0097A7"),
            800f to fromHex("00838F"),
            900f to fromHex("006064"),
        )

        val TEAL = ColorGradient(
            50f to fromHex("E0F2F1"),
            100f to fromHex("B2DFDB"),
            200f to fromHex("80CBC4"),
            300f to fromHex("4DB6AC"),
            400f to fromHex("26A69A"),
            500f to fromHex("009688"),
            600f to fromHex("00897B"),
            700f to fromHex("00796B"),
            800f to fromHex("00695C"),
            900f to fromHex("004D40"),
        )

        val GREEN = ColorGradient(
            50f to fromHex("E8F5E9"),
            100f to fromHex("C8E6C9"),
            200f to fromHex("A5D6A7"),
            300f to fromHex("81C784"),
            400f to fromHex("66BB6A"),
            500f to fromHex("4CAF50"),
            600f to fromHex("43A047"),
            700f to fromHex("388E3C"),
            800f to fromHex("2E7D32"),
            900f to fromHex("1B5E20"),
        )

        val LIGHT_GREEN = ColorGradient(
            50f to fromHex("F1F8E9"),
            100f to fromHex("DCEDC8"),
            200f to fromHex("C5E1A5"),
            300f to fromHex("AED581"),
            400f to fromHex("9CCC65"),
            500f to fromHex("8BC34A"),
            600f to fromHex("7CB342"),
            700f to fromHex("689F38"),
            800f to fromHex("558B2F"),
            900f to fromHex("33691E"),
        )

        val LIME = ColorGradient(
            50f to fromHex("F9FBE7"),
            100f to fromHex("F0F4C3"),
            200f to fromHex("E6EE9C"),
            300f to fromHex("DCE775"),
            400f to fromHex("D4E157"),
            500f to fromHex("CDDC39"),
            600f to fromHex("C0CA33"),
            700f to fromHex("AFB42B"),
            800f to fromHex("9E9D24"),
            900f to fromHex("827717"),
        )

        val YELLOW = ColorGradient(
            50f to fromHex("FFFDE7"),
            100f to fromHex("FFF9C4"),
            200f to fromHex("FFF59D"),
            300f to fromHex("FFF176"),
            400f to fromHex("FFEE58"),
            500f to fromHex("FFEB3B"),
            600f to fromHex("FDD835"),
            700f to fromHex("FBC02D"),
            800f to fromHex("F9A825"),
            900f to fromHex("F57F17"),
        )

        val AMBER = ColorGradient(
            50f to fromHex("FFF8E1"),
            100f to fromHex("FFECB3"),
            200f to fromHex("FFE082"),
            300f to fromHex("FFD54F"),
            400f to fromHex("FFCA28"),
            500f to fromHex("FFC107"),
            600f to fromHex("FFB300"),
            700f to fromHex("FFA000"),
            800f to fromHex("FF8F00"),
            900f to fromHex("FF6F00"),
        )

        val ORANGE = ColorGradient(
            50f to fromHex("FFF3E0"),
            100f to fromHex("FFE0B2"),
            200f to fromHex("FFCC80"),
            300f to fromHex("FFB74D"),
            400f to fromHex("FFA726"),
            500f to fromHex("FF9800"),
            600f to fromHex("FB8C00"),
            700f to fromHex("F57C00"),
            800f to fromHex("EF6C00"),
            900f to fromHex("E65100"),
        )

        val DEEP_ORANGE = ColorGradient(
            50f to fromHex("FBE9E7"),
            100f to fromHex("FFCCBC"),
            200f to fromHex("FFAB91"),
            300f to fromHex("FF8A65"),
            400f to fromHex("FF7043"),
            500f to fromHex("FF5722"),
            600f to fromHex("F4511E"),
            700f to fromHex("E64A19"),
            800f to fromHex("D84315"),
            900f to fromHex("BF360C"),
        )

        val BROWN = ColorGradient(
            50f to fromHex("EFEBE9"),
            100f to fromHex("D7CCC8"),
            200f to fromHex("BCAAA4"),
            300f to fromHex("A1887F"),
            400f to fromHex("8D6E63"),
            500f to fromHex("795548"),
            600f to fromHex("6D4C41"),
            700f to fromHex("5D4037"),
            800f to fromHex("4E342E"),
            900f to fromHex("3E2723"),
        )

        val GREY = ColorGradient(
            50f to fromHex("FAFAFA"),
            100f to fromHex("F5F5F5"),
            200f to fromHex("EEEEEE"),
            300f to fromHex("E0E0E0"),
            400f to fromHex("BDBDBD"),
            500f to fromHex("9E9E9E"),
            600f to fromHex("757575"),
            700f to fromHex("616161"),
            800f to fromHex("424242"),
            900f to fromHex("212121"),
        )

        val BLUE_GREY = ColorGradient(
            50f to fromHex("ECEFF1"),
            100f to fromHex("CFD8DC"),
            200f to fromHex("B0BEC5"),
            300f to fromHex("90A4AE"),
            400f to fromHex("78909C"),
            500f to fromHex("607D8B"),
            600f to fromHex("546E7A"),
            700f to fromHex("455A64"),
            800f to fromHex("37474F"),
            900f to fromHex("263238"),
        )

        val PALETTE = listOf(
            RED, PINK, PURPLE, DEEP_PURPLE, INDIGO, BLUE, LIGHT_BLUE, CYAN,
            TEAL, GREEN, LIGHT_GREEN, LIME, YELLOW, AMBER, ORANGE, DEEP_ORANGE, BROWN,
            GREY, BLUE_GREY
        )
    }
}