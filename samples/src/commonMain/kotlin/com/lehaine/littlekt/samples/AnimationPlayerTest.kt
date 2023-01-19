package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 3/25/2022
 */
class AnimationPlayerTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera
        val animPlayer = AnimationPlayer<TextureSlice>()
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val heroSleep = atlas.getAnimation("heroSleeping")
        val heroSlingShot = atlas.getAnimation("heroSlingShot")
        val heroIdle = atlas.getAnimation("heroIdle", 250.milliseconds)
        val heroWakeup = atlas.getAnimation("heroWakeUp")
        val heroRun = atlas.getAnimation("heroRun")
        val heroRoll = atlas.getAnimation("heroRoll", 250.milliseconds)

        animPlayer.playLooped(heroSleep)

        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (input.isKeyJustPressed(Key.NUM1)) {
                animPlayer.playLooped(heroSleep)
            }

            if (input.isKeyJustPressed(Key.NUM2)) {
                animPlayer.playLooped(heroWakeup)
            }

            if (input.isKeyJustPressed(Key.NUM3)) {
                animPlayer.playLooped(heroRun)
            }

            if (input.isKeyJustPressed(Key.NUM4)) {
                animPlayer.playLooped(heroSlingShot)
            }

            if (input.isKeyJustPressed(Key.NUM5)) {
                animPlayer.playLooped(heroIdle)
            }

            if (input.isKeyJustPressed(Key.SPACE)) {
                animPlayer.play(heroIdle.firstFrame, 2.seconds)
            }

            if (input.isKeyJustPressed(Key.ENTER)) {
                animPlayer.play(heroRoll)
            }

            animPlayer.update(dt)
            camera.update()
            batch.use(camera.viewProjection) { batch ->
                animPlayer.currentKeyFrame?.let {
                    batch.draw(it, 50f, 50f, scaleX = 5f, scaleY = 5f)
                }
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