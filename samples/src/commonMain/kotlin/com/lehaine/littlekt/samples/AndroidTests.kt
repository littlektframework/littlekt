package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readAudioClip
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.combine
import com.lehaine.littlekt.util.viewport.ScreenViewport

/**
 * @author Colton Daily
 * @date 2/8/2022
 */
class AndroidTests(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val fntTexture = resourcesVfs["m5x7_16_0.png"].readTexture()
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas().combine(fntTexture, "font", this)
        val bossAttackAnim = atlas.getAnimation("bossAttack")
        val boss = AnimatedSprite(bossAttackAnim.firstFrame).apply {
            x = 450f
            y = 350f
            scaleX = 2f
            scaleY = 2f
            playLooped(bossAttackAnim)
        }

        val clip = resourcesVfs["random.wav"].readAudioClip()

        val camera = OrthographicCamera(graphics.width, graphics.height).apply {
            viewport = ScreenViewport(graphics.width, graphics.height)
        }
        val batch = SpriteBatch(this)

        clip.play()

        onResize { width, height ->
            camera.update(width, height, context)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            camera.update()
            boss.update(dt)
            batch.use(camera.viewProjection) {
                it.draw(atlas["font"].slice, 450f, 150f, scaleX = 4f, scaleY = 4f)
                boss.render(it)
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