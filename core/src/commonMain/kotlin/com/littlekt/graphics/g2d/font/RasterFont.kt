package com.littlekt.graphics.g2d.font

import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Rect
import com.littlekt.math.nextPowerOfTwo
import com.littlekt.util.datastructure.IntMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

/**
 * A [TtfFont] rasterized at an exact [pixelSize]. Glyphs are rasterized the first time they are
 * used and packed into texture pages, which are added as they fill up. Advances and kerning are
 * rounded to whole pixels, so text drawn at a whole pixel position stays sharp.
 *
 * Get one with [TtfFont.raster], which caches each size.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
class RasterFont
internal constructor(
    /** The font this was rasterized from. */
    val typeface: TtfFont,
    val device: Device,
    /** The size in pixels. */
    val pixelSize: Float,
    /** The coverage gamma. */
    val gamma: Float,
    /** The texture filter of each page. */
    val filter: FilterMode,
) : TextureFont {
    private val scale = pixelSize / typeface.unitsPerEm
    private val base = round(typeface.ascender * scale)
    private val pageSize =
        max(
            ceil(pixelSize * 12f).toInt().nextPowerOfTwo.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
            (ceil(pixelSize * 2f).toInt() + PADDING * 2).nextPowerOfTwo,
        )
    private val glyphs = IntMap<Glyph>()
    private val _glyphMetrics = mutableMapOf<Int, GlyphMetrics>()
    private val _textures = mutableListOf<Texture>()
    private val rasterizer = GlyphRasterizer()
    private var coverage = ByteArray(0)
    private var pixels = ByteArray(0)
    private var shelfX = 0
    private var shelfY = 0
    private var shelfHeight = 0

    /** `true` once released. A released font can't be drawn. */
    var isReleased: Boolean = false
        private set

    override val textures: List<Texture>
        get() = _textures

    /** The number of texture pages. */
    val pages: Int
        get() = _textures.size

    override val metrics: FontMetrics =
        FontMetrics(
            size = pixelSize,
            top = base - round(typeface.capHeight * scale),
            ascent = base - round(typeface.capHeight * scale),
            baseline = base,
            lineHeight =
                round((typeface.ascender - typeface.descender + typeface.lineGap) * scale),
            descent = round(typeface.descender * scale),
            bottom = round(typeface.descender * scale),
            leading = round(typeface.lineGap * scale),
            maxWidth = round(typeface.metrics.maxWidth * pixelSize),
            capHeight = round(typeface.capHeight * scale),
            padding = FontMetrics.Padding(PADDING, PADDING, PADDING, PADDING),
        )

    override val glyphMetrics: Map<Int, GlyphMetrics>
        get() = _glyphMetrics

    override var wrapChars: CharSequence = typeface.wrapChars

    override fun get(code: Int): GlyphMetrics? = glyph(code).metrics

    override fun get(char: Char): GlyphMetrics? = get(char.code)

    override fun getKerning(first: Int, second: Int): Kerning? {
        val amount = kerningPixels(first, second)
        return if (amount == 0) null else Kerning(first, second, amount)
    }

    override fun getKerningAmount(scale: Float, first: Int, second: Int): Float =
        kerningPixels(first, second) * scale

    override fun forScale(scale: Float): TextureFont =
        if (scale == 1f) this else typeface.raster(device, pixelSize * scale, gamma, filter)

    /** Rasterizes each glyph of [chars] now instead of when first drawn. */
    fun preload(chars: CharSequence) {
        for (i in chars.indices) glyph(chars[i].code)
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        _textures.forEach { it.release() }
        _textures.clear()
        typeface.onRasterFontReleased(this)
    }

    private fun kerningPixels(first: Int, second: Int): Int {
        val left = glyph(first).index
        val right = glyph(second).index
        return round(typeface.kerningByIndex(left, right) * scale).toInt()
    }

    private fun glyph(code: Int): Glyph = glyphs[code] ?: createGlyph(code).also { glyphs[code] = it }

    private fun createGlyph(code: Int): Glyph {
        check(!isReleased) { "RasterFont has been released!" }
        val char = code.toChar()
        val ttf = typeface.glyph(char)
        val index = typeface.glyphIndex(char)
        val advance = round(ttf.advanceWidth * scale)
        if (ttf.path.isEmpty() || ttf.width <= 0 || ttf.height <= 0) {
            val metrics = GlyphMetrics(pixelSize, code, Rect(), advance)
            _glyphMetrics[code] = metrics
            return Glyph(index, metrics)
        }
        val left = floor(ttf.xMin * scale).toInt() - PADDING
        val right = ceil(ttf.xMax * scale).toInt() + PADDING
        val top = ceil(ttf.yMax * scale).toInt() + PADDING
        val bottom = floor(ttf.yMin * scale).toInt() - PADDING
        val width = right - left
        val height = top - bottom

        rasterizer.rasterize(ttf.path, width, height, scale, -left.toFloat(), top.toFloat())
        val count = width * height
        if (coverage.size < count) coverage = ByteArray(count)
        rasterizer.coverage(coverage, gamma = gamma)
        if (pixels.size < count * 4) pixels = ByteArray(count * 4)
        for (i in 0 until count) {
            val p = i * 4
            pixels[p] = WHITE
            pixels[p + 1] = WHITE
            pixels[p + 2] = WHITE
            pixels[p + 3] = coverage[i]
        }

        val page = place(width, height)
        val texture = _textures[page]
        device.queue.writeTexture(
            pixels,
            TextureCopyView(texture.gpuTexture, origin = Origin3D(shelfX, shelfY, 0)),
            TextureDataLayout(width * 4, height),
            Extent3D(width, height, 1),
            (count * 4).toLong(),
        )
        val inv = 1f / pageSize
        val metrics =
            GlyphMetrics(
                size = pixelSize,
                code = code,
                bounds = Rect(left.toFloat(), bottom - base, width.toFloat(), height.toFloat()),
                xAdvance = advance,
                u0 = shelfX * inv,
                v0 = shelfY * inv,
                u1 = (shelfX + width) * inv,
                v1 = (shelfY + height) * inv,
                page = page,
            )
        shelfX += width + SPACING
        _glyphMetrics[code] = metrics
        return Glyph(index, metrics)
    }

    private fun place(width: Int, height: Int): Int {
        if (_textures.isEmpty()) addPage()
        if (shelfX + width > pageSize) {
            shelfX = 0
            shelfY += shelfHeight + SPACING
            shelfHeight = 0
        }
        if (shelfY + height > pageSize) {
            addPage()
            shelfX = 0
            shelfY = 0
            shelfHeight = 0
        }
        shelfHeight = max(shelfHeight, height)
        return _textures.lastIndex
    }

    private fun addPage() {
        _textures +=
            EmptyTexture(
                device,
                TextureFormat.RGBA8_UNORM,
                pageSize,
                pageSize,
                SamplerDescriptor(magFilter = filter, minFilter = filter),
                usage = TextureUsage.TEXTURE or TextureUsage.COPY_DST,
            )
    }

    override fun toString(): String = "RasterFont(pixelSize=$pixelSize, pages=$pages)"

    private class Glyph(val index: Int, val metrics: GlyphMetrics)

    private companion object {
        const val PADDING = 1
        const val SPACING = 1
        const val MIN_PAGE_SIZE = 256
        const val MAX_PAGE_SIZE = 2048
        const val WHITE = 0xFF.toByte()
    }
}
