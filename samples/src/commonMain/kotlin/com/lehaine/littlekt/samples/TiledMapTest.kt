package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readTiledMap
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
class TiledMapTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val camera = OrthographicCamera().apply {
            viewport = ExtendViewport(480, 270)
        }

        val batch = SpriteBatch(context)

        val tiledMap = resourcesVfs["tiled/tiled-world.tmj"].readTiledMap()

        onResize { width, height ->
            camera.update(width, height, context)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()

            batch.use(camera.viewProjection) {
                tiledMap.render(it, camera)
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