package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readAudioClip
import com.lehaine.littlekt.file.vfs.readAudioStream
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.g2d.AnimatedSprite
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.getAnimation
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.combine
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            x = 150f
            y = 150f
            scaleX = 2f
            scaleY = 2f
            playLooped(bossAttackAnim)
        }

        val clip = resourcesVfs["random.wav"].readAudioClip()

        val music = resourcesVfs["music_short.mp3"].readAudioStream()

        music.play(1f, true)
        KtScope.launch {
            delay(2500)
            music.pause()
            delay(2500)
            music.resume()
            delay(1000)
            music.stop()
            delay(2000)
            music.play(1f, true)
        }

        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera
        val batch = SpriteBatch(this)

        clip.play()

        onResize { width, height ->
            println("$width,$height")
            viewport.update(width, height, context, false)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            camera.update()
            boss.update(dt)
            batch.use(camera.viewProjection) {
                it.draw(atlas["font"].slice, 5f, 5f)
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