package com.lehaine.littlekt.samples.s3d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readGltfModel
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class GltfTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val model = resourcesVfs["models/player.glb"].readGltfModel()

        onResize { width, height ->

        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)


            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}