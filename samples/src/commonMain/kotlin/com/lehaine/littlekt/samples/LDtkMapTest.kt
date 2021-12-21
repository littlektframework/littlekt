package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readLDtkMap
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
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
    private lateinit var atlas: TextureAtlas
    private lateinit var person: TextureSlice
    private var loading = true

    init {
        logger.level = Logger.Level.DEBUG
        Logger.defaultLevel = Logger.Level.DEBUG
        vfs.launch {
            map = resourcesVfs["ldtk/sample.ldtk"].readLDtkMap(true)
            atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
            person = atlas["heroIdle0.png"].slice
            loading = false
        }
        camera.translate(0f, -200f, 0f)
    }

    override fun render(dt: Duration) {
        if (loading) return
        batch.use {
            val lastIdx = map.levels[0].layers.lastIndex
            map.levels[0].levelBackgroundImage?.render(batch, 0f, 0f)
            map.levels[0].layers[lastIdx].render(batch, camera, viewport, 0f, 0f)
            map.levels[0].layers[lastIdx - 1].render(batch, camera, viewport, 0f, 0f)
            it.draw(person, 100f, 50f)
        }
    }
}