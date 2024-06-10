package com.littlekt.graphics

import com.littlekt.math.Vec4f
import com.littlekt.math.clamp
import kotlin.math.pow
import kotlin.math.roundToInt

/** An immutable representation of Color based on RGBA channels. */
open class Color(r: Float, g: Float, b: Float, a: Float = 1f) : Vec4f(r, g, b, a) {

    constructor(other: Color) : this(other.r, other.g, other.b, other.a)

    /** This color's normalized `(0f <= r <= 1f)` 'red' channel. */
    open val r
        get() = this[0]

    /** This color's normalized `(0f <= g <= 1f)` 'green' channel. */
    open val g
        get() = this[1]

    /** This color's normalized `(0f <= b <= 1f)` 'blue' channel. */
    open val b
        get() = this[2]

    /** This color's normalized `(0f <= a <= 1f)` 'alpha' channel. */
    open val a
        get() = this[3]

    /** The perceived brightness */
    val brightness: Float
        get() = 0.299f * r + 0.587f * g + 0.114f * b

    /**
     * Linearly interpolate this color with the specified color.
     *
     * @return a new [MutableColor]
     */
    fun mix(other: Color, weight: Float, result: MutableColor = MutableColor()): MutableColor {
        result.r = other.r * weight + r * (1f - weight)
        result.g = other.g * weight + g * (1f - weight)
        result.b = other.b * weight + b * (1f - weight)
        result.a = other.a * weight + a * (1f - weight)
        return result
    }

    /**
     * @return a new [MutableColor] with this color's `rgb` channels for this color scaled by the
     *   specified factor.
     */
    fun scaleRgb(factor: Float, result: MutableColor = MutableColor()): MutableColor {
        return result.set(this).scaleRgb(factor)
    }

    /** @return a new [MutableColor] with this color's `rgb` channels and the specified [alpha] */
    fun withAlpha(alpha: Float): MutableColor {
        return MutableColor(r, g, b, alpha)
    }

    /**
     * An alias for `gamma(2.2f)`.
     *
     * @return a new [MutableColor] converted from sRGB to linear space.
     * @see [gamma]
     */
    fun toLinear(result: MutableColor = MutableColor()): MutableColor = gamma(2.2f, result)

    /**
     * An alias for `gamma(1f / 2.2f)`.
     *
     * @return a new [MutableColor] converted from linear to sRGB space.
     * @see [gamma]
     */
    fun toSrgb(result: MutableColor = MutableColor()): MutableColor = gamma(1f / 2.2f, result)

    /**
     * Applies a gamma to this color.
     *
     * @return a new [MutableColor] with the specified [gamma] applied.
     */
    fun gamma(gamma: Float, result: MutableColor = MutableColor()): MutableColor {
        return result.set(r.pow(gamma), g.pow(gamma), b.pow(gamma), a)
    }

    /**
     * Lighten the color by the specified amount.
     *
     * @return a new [MutableColor] with th specified [amount] applied
     */
    fun lighten(amount: Float = 0.1f, result: MutableColor = MutableColor()): MutableColor {
        return result.set(this).lighten(amount)
    }

    /** @return the hex string representation following the `rrggbbaa` structure. */
    fun toHexString(): String {
        val ir = (r * 255).roundToInt().clamp(0, 255)
        val ig = (g * 255).roundToInt().clamp(0, 255)
        val ib = (b * 255).roundToInt().clamp(0, 255)
        val ia = (a * 255).roundToInt().clamp(0, 255)
        return ir.toString(16) + ig.toString(16) + ib.toString(16) + ia.toString(16)
    }

    /** @return a new [MutableColor] using the color values of this color. */
    fun toMutableColor(result: MutableColor = MutableColor()) = result.set(r, g, b, a)

    companion object {
        val CLEAR = Color(0f, 0f, 0f, 0f)

        val BLACK = Color(0f, 0f, 0f, 1f)
        val DARK_GRAY = Color(0.25f, 0.25f, 0.25f, 1f)
        val GRAY = Color(0.5f, 0.5f, 0.5f, 1f)
        val LIGHT_GRAY = Color(0.75f, 0.75f, 0.75f, 1f)
        val WHITE = Color(1f, 1f, 1f, 1f)

        val RED = Color(1f, 0f, 0f, 1f)
        val GREEN = Color(0f, 1f, 0f, 1f)
        val BLUE = Color(0f, 0f, 1f, 1f)
        val YELLOW = Color(1f, 1f, 0f, 1f)
        val CYAN = Color(0f, 1f, 1f, 1f)
        val MAGENTA = Color(1f, 0f, 1f, 1f)
        val ORANGE = Color(1f, 0.5f, 0f, 1f)
        val LIME = Color(0.7f, 1f, 0f, 1f)

        val LIGHT_RED = Color(1f, 0.5f, 0.5f, 1f)
        val LIGHT_GREEN = Color(0.5f, 1f, 0.5f, 1f)
        val LIGHT_BLUE = Color(0.5f, 0.5f, 1f, 1f)
        val LIGHT_YELLOW = Color(1f, 1f, 0.5f, 1f)
        val LIGHT_CYAN = Color(0.5f, 1f, 1f, 1f)
        val LIGHT_MAGENTA = Color(1f, 0.5f, 1f, 1f)
        val LIGHT_ORANGE = Color(1f, 0.75f, 0.5f, 1f)

        val DARK_RED = Color(0.5f, 0f, 0f, 1f)
        val DARK_GREEN = Color(0f, 0.5f, 0f, 1f)
        val DARK_BLUE = Color(0f, 0f, 0.5f, 1f)
        val DARK_YELLOW = Color(0.5f, 0.5f, 0f, 1f)
        val DARK_CYAN = Color(0f, 0.5f, 0.5f, 1f)
        val DARK_MAGENTA = Color(0.5f, 0f, 0.5f, 1f)
        val DARK_ORANGE = Color(0.5f, 0.25f, 0f, 1f)

        fun fromHsv(h: Float, s: Float, v: Float, a: Float): Color {
            val color = MutableColor()
            return color.setHsv(h, s, v, a)
        }

        /**
         * Parse a hex string into a [Color]. Supported hex formats: `#FFF` `#FFFF `#FFFFFF`
         * `#FFFFFFFF`
         */
        fun fromHex(hex: String): Color {
            if (hex.isEmpty()) {
                return BLACK
            }

            var str = hex
            if (str[0] == '#') {
                str = str.substring(1)
            }

            var r = 0f
            var g = 0f
            var b = 0f
            var a = 1f
            when (str.length) {
                3 -> {
                    val r4 = str.substring(0, 1).toInt(16)
                    val g4 = str.substring(1, 2).toInt(16)
                    val b4 = str.substring(2, 3).toInt(16)
                    r = (r4 or (r4 shl 4)) / 255f
                    g = (g4 or (g4 shl 4)) / 255f
                    b = (b4 or (b4 shl 4)) / 255f
                }
                4 -> {
                    val r4 = str.substring(0, 1).toInt(16)
                    val g4 = str.substring(1, 2).toInt(16)
                    val b4 = str.substring(2, 3).toInt(16)
                    val a4 = str.substring(2, 3).toInt(16)
                    r = (r4 or (r4 shl 4)) / 255f
                    g = (g4 or (g4 shl 4)) / 255f
                    b = (b4 or (b4 shl 4)) / 255f
                    a = (a4 or (a4 shl 4)) / 255f
                }
                6 -> {
                    // parse rgb
                    r = str.substring(0, 2).toInt(16) / 255f
                    g = str.substring(2, 4).toInt(16) / 255f
                    b = str.substring(4, 6).toInt(16) / 255f
                }
                8 -> {
                    // parse rgba
                    r = str.substring(0, 2).toInt(16) / 255f
                    g = str.substring(2, 4).toInt(16) / 255f
                    b = str.substring(4, 6).toInt(16) / 255f
                    a = str.substring(6, 8).toInt(16) / 255f
                }
            }
            return Color(r, g, b, a)
        }

        /**
         * @return the converted the given normalized rgba channels to an RGBA8888 int
         *   representation.
         */
        fun toRgba8888(r: Float, g: Float, b: Float, a: Float) =
            ((r * 255).toInt() shl 24) or
                ((g * 255).toInt() shl 16) or
                ((b * 255).toInt() shl 8) or
                (a * 255).toInt()

        /**
         * @return the converted the given normalized rgba channels to an AGBR888 int
         *   representation.
         */
        fun toAbgr888(r: Float, g: Float, b: Float, a: Float) =
            ((a * 255).toInt() shl 24) or
                ((b * 255).toInt() shl 16) or
                ((g * 255).toInt() shl 8) or
                (r * 255).toInt()
    }
}

/** @return the ABGR888 int representation of this color */
fun Color.toAbgr888(): Int = Color.toAbgr888(r, g, b, a)

/**
 * An alias for [toAbgr888].
 *
 * @return the AGBR888 int representation of this color
 */
fun Color.abgr(): Int = toAbgr888()

/** @return the RGBA8888 int representation of this color */
fun Color.toRgba8888(): Int = Color.toRgba8888(r, g, b, a)

/**
 * An alias for [toRgba8888].
 *
 * @return the RGBA8888 int representation of this color
 */
fun Color.rgba(): Int = toRgba8888()

/**
 * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a
 * float. Alpha is compressed from 0-255 to use only even numbers between 0-254 to avoid using float
 * bits in the NaN range. Converting a color to a float and back can be lossy for alpha.
 *
 * @return the color as a 32-bit float
 */
fun Color.toFloatBits(): Float {
    val abgr =
        ((255 * a).toInt() shl 24) or
            ((255 * b).toInt() shl 16) or
            ((255 * g).toInt() shl 8) or
            (255 * r).toInt()
    return Float.fromBits(abgr and 0xfeffffff.toInt())
}

/** A mutable representation of [Color]. */
open class MutableColor(r: Float, g: Float, b: Float, a: Float) : Color(r, g, b, a) {

    override var r
        get() = this[0]
        set(value) {
            this[0] = value
        }

    override var g
        get() = this[1]
        set(value) {
            this[1] = value
        }

    override var b
        get() = this[2]
        set(value) {
            this[2] = value
        }

    override var a
        get() = this[3]
        set(value) {
            this[3] = value
        }

    constructor() : this(0f, 0f, 0f, 1f)

    constructor(color: Color) : this(color.r, color.g, color.b, color.a)

    /** Adds a [Vec4f] to this color's channels. */
    fun add(other: Vec4f): MutableColor {
        r += other.x
        g += other.y
        b += other.z
        a += other.w
        return this
    }

    /**
     * Scales the specified [Vec4f] by the specified [weight] and adds the result to this color's
     * channels.
     */
    fun add(other: Vec4f, weight: Float): MutableColor {
        r += other.x * weight
        g += other.y * weight
        b += other.z * weight
        a += other.w * weight
        return this
    }

    /** Subtract the rgba channels by the given vector. */
    fun subtract(other: Vec4f): MutableColor {
        r -= other.x
        g -= other.y
        b -= other.z
        a -= other.w
        return this
    }

    /** Scale the rgba channels by the given factor. */
    fun scale(factor: Float): MutableColor {
        r *= factor
        g *= factor
        b *= factor
        a *= factor
        return this
    }

    /** Scale the rgb channels by the given factor. */
    fun scaleRgb(factor: Float): MutableColor {
        r *= factor
        g *= factor
        b *= factor
        return this
    }

    /** Multiply the rgba channels by the given vector. */
    fun mul(other: Vec4f): MutableColor {
        r *= other.x
        g *= other.y
        b *= other.z
        a *= other.w
        return this
    }

    /** Set this color's channels to `0,0,0,0`. */
    fun clear(): MutableColor {
        set(0f, 0f, 0f, 0f)
        return this
    }

    /** Set the color using normalized values. `(0f <= value <= 1f)` */
    fun set(r: Float, g: Float, b: Float, a: Float): MutableColor {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
        return this
    }

    /** Set the color using normalized values. `(0f <= value <= 1f)` */
    fun set(other: Vec4f): MutableColor {
        r = other.x
        g = other.y
        b = other.z
        a = other.w
        return this
    }

    open operator fun set(i: Int, v: Float) {
        fields[i] = v
    }

    /** Sets this color from HSV channels. */
    fun setHsv(h: Float, s: Float, v: Float, a: Float): MutableColor {
        var hue = h % 360f
        if (hue < 0) {
            hue += 360f
        }
        val hi = (hue / 60f).toInt()
        val f = hue / 60f - hi
        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))

        when (hi) {
            1 -> set(q, v, p, a)
            2 -> set(p, v, t, a)
            3 -> set(p, q, v, a)
            4 -> set(t, p, v, a)
            5 -> set(v, p, q, a)
            else -> set(v, t, p, a)
        }
        return this
    }

    /** Set this color from an RGBA8888 int. */
    fun setRgba8888(rgba8888: Int): MutableColor {
        r = ((rgba8888 and 0xff000000.toInt()) ushr 24) / 255f
        g = ((rgba8888 and 0x00ff0000) ushr 16) / 255f
        b = ((rgba8888 and 0x0000ff00) ushr 8) / 255f
        a = (rgba8888 and 0x000000ff) / 255f
        return this
    }

    /**
     * Set this color from an ABGR888 int.
     *
     * @return this color
     */
    fun setAbgr888(abgr888: Int): MutableColor {
        a = ((abgr888 and 0xff000000.toInt()) ushr 24) / 255f
        b = ((abgr888 and 0x00ff0000) ushr 16) / 255f
        g = ((abgr888 and 0x0000ff00) ushr 8) / 255f
        r = (abgr888 and 0x000000ff) / 255f
        return this
    }

    /**
     * Lighten the color by the specified amount.
     *
     * @return this color
     */
    fun lighten(amount: Float = 0.1f): MutableColor {
        val oneMinus = 1f - amount
        r = amount + oneMinus * r
        g = amount + oneMinus * g
        b = amount + oneMinus * b
        return this
    }
}
