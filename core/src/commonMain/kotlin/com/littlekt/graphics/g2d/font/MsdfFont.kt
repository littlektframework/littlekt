package com.littlekt.graphics.g2d.font

import com.littlekt.Releasable
import com.littlekt.graphics.EmptyTexture
import com.littlekt.graphics.Texture
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.Rect
import com.littlekt.math.nextPowerOfTwo
import com.littlekt.util.datastructure.IntMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

/**
 * A [TtfFont] drawn from multi-channel signed distance fields, so it stays sharp at any size,
 * rotation or scale, and can have an outline and glow with a [MsdfStyle]. Glyphs are generated the
 * first time they are used and shared by every size and style of the font.
 *
 * Get one with [TtfFont.msdf]. Use [withSize] and [withStyle] for other sizes and looks of the same
 * font, which share its glyphs. For small UI text at a fixed size, a [RasterFont] is sharper.
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
class MsdfFont
internal constructor(
    internal val atlas: MsdfAtlas,
    /** The size in pixels that [metrics] & glyphs are reported at. */
    val size: Float,
    /** The look of the glyphs. */
    val style: MsdfStyle,
) : TextureFont {
    private val scale = size / atlas.fieldSize
    private val glyphs = IntMap<GlyphMetrics>()
    private val _glyphMetrics = mutableMapOf<Int, GlyphMetrics>()

    /** The font this was generated from. */
    val typeface: TtfFont
        get() = atlas.typeface

    override val textures: List<Texture>
        get() = atlas.textures

    override val shader: Shader
        get() = atlas.shader(style)

    override val metrics: FontMetrics =
        typeface.let { ttf ->
            val unit = size / ttf.unitsPerEm
            val padding = round(atlas.padding * scale).toInt()
            FontMetrics(
                size = size,
                top = (ttf.ascender - ttf.capHeight) * unit,
                ascent = (ttf.ascender - ttf.capHeight) * unit,
                baseline = ttf.ascender * unit,
                lineHeight = (ttf.ascender - ttf.descender + ttf.lineGap) * unit,
                descent = ttf.descender * unit,
                bottom = ttf.descender * unit,
                leading = ttf.lineGap * unit,
                maxWidth = ttf.metrics.maxWidth * size,
                capHeight = ttf.capHeight * unit,
                padding = FontMetrics.Padding(padding, padding, padding, padding),
            )
        }

    override val glyphMetrics: Map<Int, GlyphMetrics>
        get() = _glyphMetrics

    override var wrapChars: CharSequence = typeface.wrapChars

    override fun get(code: Int): GlyphMetrics? {
        if (code < SPACE) return null
        glyphs[code]?.let {
            return it
        }
        val base = atlas.glyph(code)
        val metrics =
            GlyphMetrics(
                size = size,
                code = code,
                bounds =
                    Rect(
                        base.bounds.x * scale,
                        base.bounds.y * scale,
                        base.bounds.width * scale,
                        base.bounds.height * scale,
                    ),
                xAdvance = base.xAdvance * scale,
                u0 = base.u0,
                v0 = base.v0,
                u1 = base.u1,
                v1 = base.v1,
                page = base.page,
            )
        glyphs[code] = metrics
        _glyphMetrics[code] = metrics
        return metrics
    }

    override fun get(char: Char): GlyphMetrics? = get(char.code)

    override fun getKerning(first: Int, second: Int): Kerning? = null

    override fun getKerningAmount(scale: Float, first: Int, second: Int): Float {
        if (first < SPACE || second < SPACE) return 0f
        val amount = atlas.kerning(first, second)
        return if (amount == 0) 0f else amount * size / typeface.unitsPerEm * scale
    }

    /** @return this font at another [size], sharing its glyphs and [style]. */
    fun withSize(size: Float): MsdfFont = if (size == this.size) this else MsdfFont(atlas, size, style)

    /** @return this font with another [style], sharing its glyphs and [size]. */
    fun withStyle(style: MsdfStyle): MsdfFont =
        if (style == this.style) this else MsdfFont(atlas, size, style)

    /** Generates each glyph of [chars] now instead of when first drawn. */
    fun preload(chars: CharSequence) {
        for (i in chars.indices) atlas.glyph(chars[i].code)
    }

    /** Releases the glyphs and textures shared by every size and style of this font. */
    override fun release() {
        atlas.release()
    }

    override fun toString(): String = "MsdfFont(size=$size, style=$style)"

    private companion object {
        const val SPACE = 32
    }
}

/**
 * The glyph fields of a [TtfFont], shared by every size and style of an [MsdfFont].
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
internal class MsdfAtlas(
    val typeface: TtfFont,
    val device: Device,
    val fieldSize: Float,
    val distanceRange: Float,
) : Releasable {
    val padding = ceil(distanceRange / 2f).toInt() + 1
    private val scale = fieldSize / typeface.unitsPerEm
    private val base = typeface.ascender * scale
    private val pageSize =
        max(
            ceil(fieldSize * 12f).toInt().nextPowerOfTwo.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
            (ceil(fieldSize * 2f).toInt() + padding * 2).nextPowerOfTwo,
        )
    private val glyphs = IntMap<Glyph>()
    private val shaders = mutableMapOf<MsdfStyle, MsdfShader>()
    private val generator = MsdfGenerator()
    private var pixels = ByteArray(0)
    private var shelfX = 0
    private var shelfY = 0
    private var shelfHeight = 0
    val textures = mutableListOf<Texture>()

    var isReleased = false
        private set

    fun shader(style: MsdfStyle): MsdfShader =
        shaders.getOrPut(style) { MsdfShader(device, style, distanceRange, fieldSize / distanceRange) }

    fun glyph(code: Int): GlyphMetrics = (glyphs[code] ?: createGlyph(code).also { glyphs[code] = it }).metrics

    fun kerning(first: Int, second: Int): Int {
        val left = glyphs[first]?.index ?: typeface.glyphIndex(first.toChar())
        val right = glyphs[second]?.index ?: typeface.glyphIndex(second.toChar())
        return typeface.kerningByIndex(left, right)
    }

    private fun createGlyph(code: Int): Glyph {
        check(!isReleased) { "MsdfFont has been released!" }
        val char = code.toChar()
        val ttf = typeface.glyph(char)
        val index = typeface.glyphIndex(char)
        val advance = ttf.advanceWidth * scale
        if (ttf.path.isEmpty() || ttf.width <= 0 || ttf.height <= 0) {
            return Glyph(index, GlyphMetrics(fieldSize, code, Rect(), advance))
        }
        val left = floor(ttf.xMin * scale).toInt() - padding
        val right = ceil(ttf.xMax * scale).toInt() + padding
        val top = ceil(ttf.yMax * scale).toInt() + padding
        val bottom = floor(ttf.yMin * scale).toInt() - padding
        val width = right - left
        val height = top - bottom

        generator.generate(ttf.path, width, height, scale, -left.toFloat(), top.toFloat(), distanceRange)
        val count = width * height * 4
        if (pixels.size < count) pixels = ByteArray(count)
        generator.write(pixels)

        val page = place(width, height)
        device.queue.writeTexture(
            pixels,
            TextureCopyView(textures[page].gpuTexture, origin = Origin3D(shelfX, shelfY, 0)),
            TextureDataLayout(width * 4, height),
            Extent3D(width, height, 1),
            count.toLong(),
        )
        val inv = 1f / pageSize
        val metrics =
            GlyphMetrics(
                size = fieldSize,
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
        return Glyph(index, metrics)
    }

    private fun place(width: Int, height: Int): Int {
        if (textures.isEmpty()) addPage()
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
        return textures.lastIndex
    }

    private fun addPage() {
        textures +=
            EmptyTexture(
                device,
                TextureFormat.RGBA8_UNORM,
                pageSize,
                pageSize,
                SamplerDescriptor(magFilter = FilterMode.LINEAR, minFilter = FilterMode.LINEAR),
                usage = TextureUsage.TEXTURE or TextureUsage.COPY_DST,
            )
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        textures.forEach { it.release() }
        textures.clear()
        shaders.values.forEach { it.release() }
        shaders.clear()
        typeface.onMsdfAtlasReleased(this)
    }

    private class Glyph(val index: Int, val metrics: GlyphMetrics)

    private companion object {
        const val SPACING = 1
        const val MIN_PAGE_SIZE = 256
        const val MAX_PAGE_SIZE = 2048
    }
}
