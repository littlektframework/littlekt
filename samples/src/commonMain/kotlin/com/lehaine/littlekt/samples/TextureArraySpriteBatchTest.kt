package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ScreenViewport

/**
 * @author Colton Daily
 * @date 2/9/2022
 */
class TextureArraySpriteBatchTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val font = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val person = resourcesVfs["person.png"].readTexture()
        val bossAttackAnim = atlas.getAnimation("bossAttack")
        val boss = AnimatedSprite(bossAttackAnim.firstFrame).apply {
            x = 450f
            y = 250f
            scaleX = 2f
            scaleY = 2f
            playLooped(bossAttackAnim)
        }

        val camera = OrthographicCamera(graphics.width, graphics.height).apply {
            viewport = ScreenViewport(graphics.width, graphics.height)
        }
        val batch =
            TextureArraySpriteBatch(this, maxTextureSlots = 3, maxTextureWidth = 256, maxTextureHeight = 256)

        onResize { width, height ->
            camera.update(width, height, context)
        }
        onRender { dt ->
            gl.clearColor(Color.DARK_GRAY)
            camera.update()
            boss.update(dt)
            batch.use(camera.viewProjection) {
                font.draw(it, "Boobies! --- TITTIES", 50f, 50f)
                boss.render(it)
                it.draw(person, 50f, 200f)
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