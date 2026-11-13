package com.littlekt.graphics.g2d.font

import com.littlekt.file.ByteBuffer
import com.littlekt.file.font.ttf.TtfFontReader
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.FilterMode
import com.littlekt.math.Rect
import kotlin.math.round

/**
 * A True-type font. It holds the parsed glyph outlines, metrics and kerning in font units. Use
 * [raster] to get a [RasterFont] that draws it at an exact pixel size.
 *
 * Each pixel size is rasterized and cached once. Request the size you will draw at, such as the
 * logical size times the screen scale, instead of scaling a smaller size up. The cache can be
 * limited with [maxRasterFonts] or cleared with [releaseRasterFonts].
 *
 * @author Colton Daily
 * @date 12/2/2021
 */
class TtfFont(val chars: CharSequence = CharacterSets.LATIN_ALL) : Font {

    private val glyphMetricsCache = mutableMapOf<Int, GlyphMetrics>()
    private val glyphCache = mutableMapOf<Int, TtfGlyph>()
    private var _metrics = FontMetrics()
    private lateinit var reader: TtfFontReader
    private val rasterFonts = mutableListOf<RasterFont>()
    private val msdfAtlases = mutableListOf<MsdfAtlas>()

    override val glyphMetrics: Map<Int, GlyphMetrics>
        get() = glyphMetricsCache

    val glyphs: Map<Int, TtfGlyph>
        get() = glyphCache

    override var wrapChars: CharSequence = ""

    var unitsPerEm: Int = 0
        private set

    /** The ascender in font units. */
    var ascender: Int = 0
        private set

    /** The descender in font units. This is usually negative. */
    var descender: Int = 0
        private set

    /** The line gap in font units. */
    var lineGap: Int = 0
        private set

    /** The cap height in font units. */
    var capHeight: Int = 0
        private set

    /**
     * Requested pixel sizes are rounded to a multiple of this before rasterizing, so small changes
     * in scale reuse a cached size. Use `0` to rasterize every exact size.
     */
    var rasterSizeStep: Float = 1f

    /**
     * The maximum number of cached pixel sizes, or `null` for no limit. Once over the limit, the
     * least recently requested size is released, so request a [RasterFont] with [raster] when
     * drawing instead of holding on to one.
     */
    var maxRasterFonts: Int? = null
        set(value) {
            require(value == null || value > 0) { "maxRasterFonts must be greater than 0!" }
            field = value
            trimRasterFonts()
        }

    /** The cached [RasterFont] sizes, least recently requested first. */
    val rasterFontSizes: List<Float>
        get() = rasterFonts.map { it.pixelSize }

    override val metrics: FontMetrics
        get() = _metrics

    override fun getKerning(first: Int, second: Int): Kerning? = null

    override fun getKerningAmount(scale: Float, first: Int, second: Int): Float =
        kerning(first, second) / unitsPerEm.toFloat() * scale

    /** Loads and parses the data into this [TtfFont] */
    fun load(data: ByteBuffer) {
        val buffer = ByteBuffer(data.toArray(), isBigEndian = true).also { it.flip() }
        reader = TtfFontReader().also { it.parse(buffer) }
        unitsPerEm = reader.unitsPerEm
        ascender = reader.ascender
        descender = reader.descender
        lineGap = reader.lineGap
        capHeight = if (reader.capHeight > 0) reader.capHeight else glyph('H').yMax
        val scale = 1f / unitsPerEm
        _metrics =
            FontMetrics(
                size = 1f,
                top = reader.yMax * scale,
                ascent = ascender * scale,
                baseline = 0f,
                lineHeight = (ascender - descender + lineGap) * scale,
                descent = descender * scale,
                bottom = descender * scale,
                leading = lineGap * scale,
                maxWidth = reader.advanceWidthMax * scale,
                capHeight = capHeight * scale,
            )
        chars.forEach { char -> glyphMetricsCache[char.code] = glyph(char).toGlyphMetric(scale) }
    }

    /** @return the glyph for [char], or the missing glyph if the font doesn't have it. */
    fun glyph(char: Char): TtfGlyph = glyphCache.getOrPut(char.code) { reader[char] }

    /** @return the glyph index of [char], or `0` for the missing glyph. */
    fun glyphIndex(char: Char): Int = reader.glyphIndex(char)

    /** @return the kerning between [first] and [second] in font units. */
    fun kerning(first: Int, second: Int): Int =
        reader.kerning(reader.glyphIndex(first.toChar()), reader.glyphIndex(second.toChar()))

    /** @return the kerning between glyph indices [left] and [right] in font units. */
    fun kerningByIndex(left: Int, right: Int): Int = reader.kerning(left, right)

    /**
     * Gets the [RasterFont] for [pixelSize], rasterizing it the first time a size is requested.
     *
     * @param pixelSize the em size in pixels, the same as a font size in most tools
     * @param gamma values above `1` make thin text heavier
     * @param filter the texture filter. [FilterMode.NEAREST] is the sharpest when drawn at 1:1.
     */
    fun raster(
        device: Device,
        pixelSize: Float,
        gamma: Float = 1f,
        filter: FilterMode = FilterMode.LINEAR,
    ): RasterFont {
        val size = snapSize(pixelSize)
        for (i in rasterFonts.indices) {
            val font = rasterFonts[i]
            if (
                font.pixelSize == size &&
                    font.device === device &&
                    font.gamma == gamma &&
                    font.filter == filter
            ) {
                if (i != rasterFonts.lastIndex) {
                    rasterFonts.removeAt(i)
                    rasterFonts += font
                }
                return font
            }
        }
        val font = RasterFont(this, device, size, gamma, filter)
        rasterFonts += font
        trimRasterFonts()
        return font
    }

    /**
     * Gets an [MsdfFont] that draws this font from distance fields, so it stays sharp at any size,
     * scale or rotation. Glyph fields are generated once per [fieldSize] and [distanceRange] and
     * shared by every size and style.
     *
     * @param size the em size in pixels that the font is laid out at
     * @param style the look of the glyphs, such as an outline or glow
     * @param fieldSize the em size in pixels that glyph fields are generated at. Larger keeps
     *   sharper corners when drawn large, at the cost of texture space.
     * @param distanceRange the width in field pixels of the distance range. Larger allows wider
     *   outlines, glows and weights.
     */
    fun msdf(
        device: Device,
        size: Float = 32f,
        style: MsdfStyle = MsdfStyle(),
        fieldSize: Float = 48f,
        distanceRange: Float = 8f,
    ): MsdfFont {
        require(size > 0f) { "size must be greater than 0!" }
        require(fieldSize > 0f) { "fieldSize must be greater than 0!" }
        require(distanceRange > 0f) { "distanceRange must be greater than 0!" }
        val atlas =
            msdfAtlases.firstOrNull {
                it.device === device && it.fieldSize == fieldSize && it.distanceRange == distanceRange
            } ?: MsdfAtlas(this, device, fieldSize, distanceRange).also { msdfAtlases += it }
        return MsdfFont(atlas, size, style)
    }

    /** Releases every [MsdfFont] glyph field. */
    fun releaseMsdfFonts() {
        msdfAtlases.toList().forEach { it.release() }
        msdfAtlases.clear()
    }

    /** Releases every cached [RasterFont]. */
    fun releaseRasterFonts() {
        rasterFonts.toList().forEach { it.release() }
        rasterFonts.clear()
    }

    override fun release() {
        releaseRasterFonts()
        releaseMsdfFonts()
    }

    internal fun onMsdfAtlasReleased(atlas: MsdfAtlas) {
        msdfAtlases.remove(atlas)
    }

    internal fun onRasterFontReleased(font: RasterFont) {
        rasterFonts.remove(font)
    }

    internal fun snapSize(pixelSize: Float): Float {
        require(pixelSize > 0f) { "pixelSize must be greater than 0!" }
        val step = rasterSizeStep
        if (step <= 0f) return pixelSize
        return maxOf(step, round(pixelSize / step) * step)
    }

    private fun trimRasterFonts() {
        val max = maxRasterFonts ?: return
        while (rasterFonts.size > max) rasterFonts.first().release()
    }

    private fun TtfGlyph.toGlyphMetric(scale: Float) =
        GlyphMetrics(
            1f,
            unicode,
            Rect.fromBounds(xMin * scale, yMin * scale, xMax * scale, yMax * scale),
            advanceWidth * scale,
        )
}
