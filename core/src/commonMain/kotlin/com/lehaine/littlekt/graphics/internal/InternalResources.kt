package com.lehaine.littlekt.graphics.internal

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.file.createByteBuffer
import com.lehaine.littlekt.graphics.Pixmap
import com.lehaine.littlekt.graphics.Texture
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.font.VGrid
import com.lehaine.littlekt.graphics.font.VGridAtlas
import com.lehaine.littlekt.graphics.font.internal.GpuAtlas
import com.lehaine.littlekt.graphics.font.internal.GpuGlyph
import com.lehaine.littlekt.graphics.font.internal.GpuGlyphCompiler
import com.lehaine.littlekt.graphics.font.internal.GpuGlyphWriter
import com.lehaine.littlekt.graphics.gl.PixmapTextureData
import com.lehaine.littlekt.graphics.slice
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.internal.SingletonBase
import kotlin.time.measureTimedValue

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
internal class InternalResources private constructor(private val context: Context) {
    internal companion object : SingletonBase<InternalResources, Context>(::InternalResources)

    private val logger = Logger<InternalResources>()

    val default = Texture(
        PixmapTextureData(
            Pixmap(
                9, 6,
                createByteBuffer(
                    byteArrayOf(
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red

                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red

                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), // white
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // black
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red
                        0xFF.toByte(), 0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), // red

                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent

                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent

                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0xFF.toByte(), 0x00.toByte(), 0xFF.toByte(), // green
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xFF.toByte(), // blue
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // transparent
                    )
                )
            ),
            false
        )
    ).also { it.prepare(context) }

    private val defaultSlices = default.slice(1, 1)
    val white: TextureSlice = defaultSlices[1][1]
    val black: TextureSlice = defaultSlices[1][4]
    val red: TextureSlice = defaultSlices[1][7]
    val green: TextureSlice = defaultSlices[4][1]
    val blue: TextureSlice = defaultSlices[4][4]

    val transparent: TextureSlice = defaultSlices[4][7]

    /**
     * The width of the combined atlas that is generated for each gpu font cache.
     */
    var atlasWidth: Int = 256

    /**
     * The height of the combined atlast the is generated for gpu font cache. The bezier and the grid will each share
     * half of this total. The grid will take the bottom half while the beziers will take the top.
     *
     */
    var atlasHeight: Int = 512

    var gridSize = 10


    val gpuAtlas = GpuAtlas().apply {
        pixmap = Pixmap(atlasWidth, atlasHeight, createByteBuffer(atlasWidth * atlasHeight * 4))
    }.also {
        it.texture.prepare(context)
    }

    val compiledGlyphs = mutableMapOf<TtfFont, MutableMap<Int, GpuGlyph>>()


    /**
     * This will regenerate [gpuAtlas] to the specified size and copy over any existing data and dispose of the
     * previous texture.
     */
    fun setGpuFontAtlasSize(width: Int, height: Int) {
        atlasWidth = width
        atlasHeight = height
        val oldPixmap = gpuAtlas.textureData.pixmap
        val newPixmap = Pixmap(atlasWidth, atlasHeight, createByteBuffer(atlasWidth * atlasHeight * 4))
        newPixmap.draw(oldPixmap)
        gpuAtlas.pixmap = newPixmap
        gpuAtlas.texture.prepare(context)
    }

    private val compiler = GpuGlyphCompiler()

    /**
     * Compile and cache any glyphs from a specific [TtfFont].
     */
    fun compileGlyph(char: Char, font: TtfFont): GpuGlyph {
        // if already compiled -- return the glyph
        compiledGlyphs.getOrPut(font) { mutableMapOf() }[char.code]?.also { return it }

        val atlas = gpuAtlas
        val glyph = font.glyphs[char.code] ?: error("Glyph for $char doesn't exist!")
        val curves =
            measureTimedValue { compiler.compile(glyph) }.also { logger.debug { "Took ${it.duration} to compile $char (${char.code}) glyph." } }.value
        val grid = VGrid(curves, glyph.width, glyph.height, gridSize, gridSize)

        // Although the data is represented as a 32bit texture, it's actually
        // two 16bit ints per pixel, each with an x and y coordinate for
        // the bezier. Every six 16bit ints (3 pixels) is a full bezier
        // plus two pixels for grid position information
        val bezierPixelLength = 2 + curves.size * 3

        val tooManyCurves = bezierPixelLength > atlasWidth * atlasHeight

        if (curves.isEmpty() || tooManyCurves) {
            if (tooManyCurves) {
                logger.warn { "Glyph '$char' has too many curves!" }
            }
            // TODO do what then if its empty or too many curves?
            val gpuGlyph = GpuGlyph(
                glyph.width,
                glyph.height,
                glyph.leftSideBearing,
                glyph.yMax - glyph.height,
                0,
                0,
                -1,
                glyph.advanceWidth.toInt()
            )
            compiledGlyphs[font]?.put(char.code, gpuGlyph)
            return gpuGlyph
        }

        if (atlas.glyphDataBufOffset + bezierPixelLength > atlasWidth * atlasHeight) {
            throw IllegalStateException("The internal atlas cannot fit '${glyph.unicode.toChar()}' glyph! Try increasing the size of the atlas with 'setGpuFontAtlasSize()'")
        }

        if (atlas.gridX + gridSize > atlasWidth) {
            atlas.gridY += gridSize
            atlas.gridX = 0
            if (atlas.gridY >= atlasHeight) {
                atlas.full = true
                atlas.uploaded = false
                throw IllegalStateException("The internal atlas cannot fit '${glyph.unicode.toChar()}' glyph! Try increasing the size of the atlas with 'setGpuFontAtlasSize()'")
            }
        }

        val buffer = atlas.pixmap.pixels

        VGridAtlas.writeVGridAt(
            grid = grid,
            data = buffer,
            tx = atlas.gridX,
            ty = atlas.gridY,
            width = atlasWidth,
            height = atlasHeight,
            depth = 4
        )

        buffer.position = atlas.glyphDataBufOffset * 4 + atlasWidth * (atlasHeight / 2) * 4
        GpuGlyphWriter.writeGlyphToBuffer(
            buffer, curves, glyph.width, glyph.height, atlas.gridX.toShort(), atlas.gridY.toShort(),
            gridSize.toShort(), gridSize.toShort()
        )

        val gpuGlyph = GpuGlyph(
            glyph.width,
            glyph.height,
            glyph.leftSideBearing,
            glyph.yMax - glyph.height,
            atlas.glyphDataBufOffset % atlasWidth,
            atlas.glyphDataBufOffset / atlasWidth + atlasHeight / 2,
            0,
            glyph.advanceWidth.toInt()
        )

        compiledGlyphs[font]?.put(char.code, gpuGlyph)

        atlas.glyphDataBufOffset += bezierPixelLength
        atlas.gridX += gridSize
        atlas.uploaded = false


        return gpuGlyph
    }
}