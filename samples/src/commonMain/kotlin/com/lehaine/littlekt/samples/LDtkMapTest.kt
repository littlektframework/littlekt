package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readLDtkMapLoader
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 2/28/2022
 */
class LDtkMapTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val viewport = ExtendViewport(30, 16)
        val camera = viewport.camera

        val batch = SpriteBatch(context, 8191)

        val mapLoader = resourcesVfs["ldtk/world.ldtk"].readLDtkMapLoader()
        val level = mapLoader.loadLevel(2)
        val tempVec2f = MutableVec2f()

        onResize { width, height ->
            viewport.update(width, height, context)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            if (input.isKeyPressed(Key.W)) {
                camera.position.y -= 0.05f * dt.milliseconds
            } else if (input.isKeyPressed(Key.S)) {
                camera.position.y += 0.05f * dt.milliseconds
            }

            if (input.isKeyPressed(Key.D)) {
                camera.position.x += 0.05f * dt.milliseconds
            } else if (input.isKeyPressed(Key.A)) {
                camera.position.x -= 0.05f * dt.milliseconds
            }
            camera.update()

            batch.use(camera.viewProjection) {
                level.render(it, camera, scale = 1 / 8f)
            }

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.M)) {
                logger.info { "Screen coords: ${input.x},${input.y}" }
                logger.info { "cam pos: ${camera.position}" }
                tempVec2f.x = input.x.toFloat()
                tempVec2f.y = input.y.toFloat()
                camera.screenToWorld(context, tempVec2f, viewport, result = tempVec2f)
                logger.info { "world coords: $tempVec2f" }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}