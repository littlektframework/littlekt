package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readLDtkMap
import com.lehaine.littlekt.file.vfs.readTtfFont
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEnumValue
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkWorld
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/20/2021
 */
class LDtkMapTest(context: Context) : ContextListener(context) {

    private val batch = SpriteBatch(this)
    private val camera = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = ExtendViewport(480, 270)
    }
    private val uiCam = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = ExtendViewport(480, 270)
    }
    private lateinit var map: LDtkWorld
    private lateinit var atlas: TextureAtlas
    private lateinit var person: TextureSlice
    private lateinit var font: TtfFont
    private lateinit var gpuFontRenderer: GpuFont
    private var loading = true

    private val speed = 0.1f
    private var xVel = 0f
    private var yVel = 0f

    init {
        logger.level = Logger.Level.DEBUG
        Logger.defaultLevel = Logger.Level.DEBUG
        vfs.launch {
            map = resourcesVfs["ldtk/sample.ldtk"].readLDtkMap()
            atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
            person = atlas["heroIdle0.png"].slice
            font = resourcesVfs["LiberationSans-Regular.ttf"].readTtfFont()
            runOnMainThread {
                gpuFontRenderer = GpuFont(this@LDtkMapTest, font)
            }
            val level = map["West"]
            val player = level.entities("Player")[0]
            println(level.entities)
            println(player.field<LDtkEnumValue>("Equipped").value)
            println(player.fieldArray<LDtkEnumValue>("Backpack").values)
            loading = false
        }
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

        camera.translate(xVel * dt.milliseconds, yVel * dt.milliseconds, 0f)

        camera.update()
        camera.viewport.apply(this)
        batch.useDefaultShader()
        batch.use(camera.viewProjection) {
            val level = map["West"]
            map.render(it, camera)
            val player = level.entities("Player")[0]
            it.draw(
                person,
                player.x,
                player.y + level.worldY,
                player.pivotX * person.width,
                player.pivotY * person.height
            )
        }

        uiCam.update()
        uiCam.viewport.apply(this)
        gpuFontRenderer.setShaderTo(batch)
        batch.use(uiCam.viewProjection) {
            gpuFontRenderer.clear()
            gpuFontRenderer.addText("Vertices: ${stats.engineStats.vertices}", 0f, 15f, 16, color = Color.WHITE)
            gpuFontRenderer.draw(it)
        }

        if (input.isKeyPressed(Key.ESCAPE)) {
            close()
        }

    }

    override fun resize(width: Int, height: Int) {
        camera.update(width, height, this)
        uiCam.update(width, height, this)
    }
}