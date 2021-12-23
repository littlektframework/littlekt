package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readLDtkMap
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapTest(context: Context) : ContextListener(context) {

    private val batch = SpriteBatch(this)
    private val camera = OrthographicCamera(480, 270)
    private lateinit var map: LDtkTileMap
    private lateinit var atlas: TextureAtlas
    private lateinit var person: TextureSlice
    private var loading = true

    private val speed = 0.1f
    private var xVel = 0f
    private var yVel = 0f

    init {
        logger.level = Logger.Level.DEBUG
        Logger.defaultLevel = Logger.Level.DEBUG
        vfs.launch {
            map = resourcesVfs["ldtk/sample.ldtk"].readLDtkMap(true)
            atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
            person = atlas["heroIdle0.png"].slice
            loading = false
        }
        camera.translate(240f, 135f, 0f)
    }

    override fun render(dt: Duration) {
        if (loading) return

        xVel = 0f
        yVel = 0f

        if (input.isKeyPressed(Key.W)) {
            yVel -= speed
        }
        if (input.isKeyPressed(Key.S)) {
            yVel += speed
        }
        if (input.isKeyPressed(Key.A)) {
            xVel -= speed
        }
        if (input.isKeyPressed(Key.D)) {
            xVel += speed
        }

        camera.translate(xVel * dt.inWholeMilliseconds.toFloat(), yVel * dt.inWholeMilliseconds.toFloat(), 0f)

        camera.update()
        batch.use(camera.viewProjection) {
            val level = map.levels[0]
            level.render(it, camera, 0f, 0f)
//            level.levelBackgroundImage?.render(batch, 0f, 0f)
//            level.layers.forEachReversed { layer ->
//                layer.render(batch, camera, 0f, 0f)
//            }
            it.draw(person, 100f, 170f)
        }

        if (input.isKeyPressed(Key.ESCAPE)) {
            close()
        }

    }
}