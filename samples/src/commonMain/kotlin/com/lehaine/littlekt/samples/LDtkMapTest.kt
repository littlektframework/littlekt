package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readLDtkMap
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.Viewport
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.log.Logger
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapTest(context: Context) : ContextListener(context) {

    private val batch = SpriteBatch(this)
    val camera = OrthographicCamera().apply {
        left = 0f
        right = graphics.width.toFloat()
        bottom = 0f
        top = graphics.height.toFloat()
    }
    private val viewport = Viewport(0, 0, 480, 270)
    private lateinit var map: LDtkTileMap
    private var loading = true

    init {
        logger.level = Logger.Level.DEBUG
        Logger.defaultLevel = Logger.Level.DEBUG
        vfs.launch {
            map = resourcesVfs["ldtk/sample.ldtk"].readLDtkMap(true)
            loading = false
        }
        camera.translate(0f, -200f, 0f)
    }

    override fun render(dt: Duration) {
        if (loading) return
        batch.use {
            map.levels[0].render(it, camera, viewport, renderWithOffsets = false)
        }
    }
}