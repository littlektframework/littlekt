package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.font.use
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.random
import com.lehaine.littlekt.util.seconds
import com.lehaine.littlekt.util.toString
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.math.PI
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
class ParticlesTest(context: Context) : Game<Scene>(context) {
    val particleSimulator = ParticleSimulator(2000)
    val camera = OrthographicCamera(context.graphics.width, context.graphics.height).apply {
        viewport = ExtendViewport(480, 270)
    }
    val uiCam = OrthographicCamera(context.graphics.width, context.graphics.height).apply {
        viewport = ExtendViewport(480, 270)
    }
    val libSans: TtfFont by load(resourcesVfs["LiberationSans-Regular.ttf"])
    val gpuFont: GpuFont by prepare { GpuFont(this@ParticlesTest, libSans, maxVertices = 50000) }
    val batch = SpriteBatch(this)

    override fun update(dt: Duration) {
        if (input.isKeyJustPressed(Key.ENTER)) {
            dustExplosion(camera.virtualWidth / 2f, camera.virtualHeight / 2f)
        }

        camera.update()
        uiCam.update()
        particleSimulator.update(dt)

        camera.viewport.apply(this)
        batch.use(camera.viewProjection) {
            particleSimulator.draw(batch)
        }

        uiCam.viewport.apply(this)
        gpuFont.use(uiCam.viewProjection) {
            it.drawText("FPS: ${stats.fps.toString(1)}", 0f, 15f, pxSize = 16, color = Color.WHITE)
            it.drawText(
                "Total particles allocated: ${particleSimulator.numAlloc}",
                0f,
                40f,
                pxSize = 16,
                color = Color.WHITE
            )
            it.drawText(
                "Total particles alive: ${particleSimulator.totalAlive}",
                0f,
                65f,
                pxSize = 16,
                color = Color.WHITE
            )
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.update(width, height, context)
        uiCam.update(width, height, context)
    }

    private fun dustExplosion(x: Float, y: Float) {
        create(15) {
            val p = particleSimulator.alloc(Textures.white, ((x - 4)..(x + 4)).random(), y)
            p.scale((2f..5f).random())
            p.color.set(
                (198..218).random() / 255f,
                (157..177).random() / 255f,
                (136..156).random() / 255f,
                (225..255).random() / 255f
            ).also {
                p.colorBits = it.toFloatBits()
            }
            p.yDelta = (-1..1).random()
            p.xDelta = (1f..2f).random() * if (it % 2 == 0) 1 else -1
            p.gravityY = (0.07f..0.1f).random()
            p.friction = (0.92f..0.96f).random()
            p.rotationDelta = (0f..(PI.toFloat() * 2)).random()
            p.life = (0.5f..1.5f).random().seconds
        }
    }

    private fun create(num: Int, createParticle: (index: Int) -> Unit) {
        for (i in 0 until num) {
            createParticle(i)
        }
    }
}