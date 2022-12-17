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
class AnimationPlayerStateAnimTest(context: Context) : ContextListener(context) {

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

        var shouldSleep = false
        var shouldWakeup = false
        var shouldRun = false
        var shouldRoll = false

        animPlayer.apply {
            registerState(heroRoll, 11) { shouldRoll }
            registerState(heroWakeup, 10) { shouldWakeup }
            registerState(heroRun, 5) { shouldRun }
            registerState(heroSleep, 5) { shouldSleep }
            registerState(heroIdle, 0)
        }

        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (input.isKeyJustPressed(Key.NUM1)) {
                shouldSleep = !shouldSleep
            }

            if (input.isKeyJustPressed(Key.NUM2)) {
                shouldWakeup = !shouldWakeup
            }

            if (input.isKeyJustPressed(Key.NUM3)) {
                shouldRun = !shouldRun
            }

            if (input.isKeyJustPressed(Key.NUM4)) {
                shouldRoll = !shouldRoll
            }

            if (input.isKeyJustPressed(Key.ENTER)) {
                animPlayer.play(heroSlingShot)
            }

            if (input.isKeyJustPressed(Key.SPACE)) {
                animPlayer.play(heroRoll, 2.seconds)
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