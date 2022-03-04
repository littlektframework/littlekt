package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readLDtkMapLoader
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class LDtkMapTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val camera = OrthographicCamera().apply {
            viewport = ExtendViewport(480, 270)
        }

        val batch = SpriteBatch(context, 8191)

       val mapLoader =  resourcesVfs["ldtk/world.ldtk"].readLDtkMapLoader()
        val level = mapLoader.loadLevel(2)

        onResize { width, height ->
            camera.update(width, height, context)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (input.isKeyPressed(Key.W)) {
                camera.position.y -= 10f
            } else if (input.isKeyPressed(Key.S)) {
                camera.position.y += 10f
            }

            if (input.isKeyPressed(Key.D)) {
                camera.position.x += 10f
            } else if (input.isKeyPressed(Key.A)) {
                camera.position.x -= 10f
            }
            camera.update()

            batch.use(camera.viewProjection) {
                level.render(it, camera)
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