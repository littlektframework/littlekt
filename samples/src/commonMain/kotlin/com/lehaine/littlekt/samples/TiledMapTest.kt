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
import com.lehaine.littlekt.util.viewport.ScreenViewport

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class TiledMapTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val camera = OrthographicCamera().apply {
            viewport = ScreenViewport(context.graphics.width, context.graphics.height)
        }

        val batch = SpriteBatch(context)

        val orthoMap = resourcesVfs["tiled/ortho-tiled-world.tmj"].readTiledMap()
        val isoMap = resourcesVfs["tiled/iso-tiled-world.tmj"].readTiledMap()

        var visibleMap = orthoMap
        onResize { width, height ->
            camera.update(width, height, context)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()

            batch.use(camera.viewProjection) {
                visibleMap.render(it, camera)
            }

            if (input.isKeyJustPressed(Key.ENTER)) {
                visibleMap = if (visibleMap == orthoMap) isoMap else orthoMap
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