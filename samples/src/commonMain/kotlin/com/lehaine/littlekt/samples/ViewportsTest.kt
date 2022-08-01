package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readPixmap
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.file.vfs.readTiledMap
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.addBorderToSlices
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.slice
import com.lehaine.littlekt.graphics.tilemap.tiled.TiledMap
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.MutableTextureAtlas
import com.lehaine.littlekt.util.viewport.*

/**
 * @author Colton Daily
 * @date 8/1/2022
 */
class ViewportsTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {

        val virtualWidth = 480
        val virtualHeight = 270
        var viewportIdx = 0
        val viewports = listOf(ScreenViewport(virtualWidth, virtualHeight),
            ExtendViewport(virtualWidth, virtualHeight),
            FitViewport(256, 256),
            StretchViewport(virtualWidth, virtualHeight),
            FillViewport(virtualWidth, virtualHeight))

        fun viewport() = viewports[viewportIdx]
        fun camera() = viewport().camera

        viewport().update(graphics.width, graphics.height, context, true)
        val batch = SpriteBatch(context, 8191)

        // load the textures manually for the ortho map
        val cavernasTexture =
            resourcesVfs["tiled/Cavernas_by_Adam_Saltsman.png"].readPixmap().addBorderToSlices(context, 8, 8, 2)
        val background = resourcesVfs["ldtk/N2D - SpaceWallpaper1280x448.png"].readTexture()
        val atlas = MutableTextureAtlas(this)
            .add(cavernasTexture.slice(), "Cavernas_by_Adam_Saltsman.png")
            .add(background.slice(), "N2D - SpaceWallpaper1280x448.png")
            .toImmutable()
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val cache = BitmapFontCache(pixelFont).also {
            it.setText(viewport()::class.simpleName ?: "N/A", viewport().virtualWidth * 0.5f,
                camera().virtualHeight * 0.5f)
        }

        // we need to dispose of them if we aren't using them since the atlas generates new textures
        cavernasTexture.dispose()
        background.dispose()

        val maps = mutableListOf<TiledMap>()
        resourcesVfs["tiled/ortho-tiled-world.tmj"].readTiledMap(atlas).also { maps += it }
        resourcesVfs["tiled/iso-tiled-world.tmj"].readTiledMap().also { maps += it }
        resourcesVfs["tiled/staggered-tiled-world.tmj"].readTiledMap().also { maps += it }

        var mapIdx = 0
        onResize { width, height ->
            viewport().update(width, height, context, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            val camera = camera()
            camera.update()

            batch.use(camera.viewProjection) {
                maps[mapIdx].render(it, camera, scale = 1f, displayObjects = true)
                cache.draw(it)
            }

            if (input.isKeyJustPressed(Key.ENTER)) {
                mapIdx++
                if (mapIdx >= maps.size) mapIdx = 0
            }

            if (input.isKeyJustPressed(Key.V)) {
                viewportIdx++
                if (viewportIdx >= viewports.size) {
                    viewportIdx = 0
                }
                viewport().update(graphics.width, graphics.height, context, true)
                cache.setText(viewport()::class.simpleName ?: "N/A",
                    viewport().virtualWidth * 0.5f,
                    viewport().virtualHeight * 0.5f)
            }
            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}