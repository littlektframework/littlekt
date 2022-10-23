package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.Experimental
import com.lehaine.littlekt.file.vfs.readTtfFont
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.VectorFont
import com.lehaine.littlekt.graphics.font.VectorFont.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 10/21/2022
 */
class VectorFontTest(context: Context) : ContextListener(context) {

    @OptIn(Experimental::class)
    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val viewport = ExtendViewport(480, 270)
        val camera = viewport.camera
        val freeSerif = resourcesVfs["FreeSerif.ttf"].readTtfFont()
        val vectorFont = VectorFont(freeSerif).also { it.prepare(this) }
        val text = TextBlock(50f, 150f, mutableListOf(Text("You're not real man", 32, Color.RED)))
        val text2 = TextBlock(50f, 200f, mutableListOf(Text("Had a funeral for a bird", 32, Color.RED)))

        camera.position.x = 481f
        onResize { width, height ->
            viewport.update(width, height, this, true)
            vectorFont.resize(width, height, this)
        }

        onRender { dt ->
            gl.clearColor(Color.DARK_GRAY)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            camera.update()

            vectorFont.queue(text)
            vectorFont.queue(text2)
            vectorFont.flush(camera.viewProjection)

            if (input.isKeyPressed(Key.V)) {
                text.text[0].pxScale++
            }
            if (input.isKeyPressed(Key.C)) {
                text.text[0].pxScale--
            }
            if (input.isKeyPressed(Key.W)) {
                text.y -= 10f
            }
            if (input.isKeyPressed(Key.S)) {
                text.y += 10f
            }
            if (input.isKeyPressed(Key.D)) {
                text.x += 10f
            }
            if (input.isKeyPressed(Key.A)) {
                text.x -= 10f
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