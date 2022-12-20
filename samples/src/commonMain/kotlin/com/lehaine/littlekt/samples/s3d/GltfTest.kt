package com.lehaine.littlekt.samples.s3d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.createShader
import com.lehaine.littlekt.file.vfs.readGltfModel
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.PerspectiveCamera
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColor3DFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColor3DVertexShader
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.util.viewport.ScreenViewport

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class GltfTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val model = resourcesVfs["models/player.glb"].readGltfModel()
        val shader = createShader(SimpleColor3DVertexShader(), SimpleColor3DFragmentShader())
        val viewport = ScreenViewport(graphics.width, graphics.height, PerspectiveCamera())
        val camera = viewport.camera

        onResize { width, height ->
            viewport.update(width, height, context, false)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            shader.bind()
            shader.uProjTrans?.apply(shader, camera.viewProjection)
            model.update()
            model.render(shader)

            val speed = 5f * if (input.isKeyPressed(Key.SHIFT_LEFT)) 10f else 1f
            if (input.isKeyPressed(Key.W)) {
                camera.position.z -= speed
            }
            if (input.isKeyPressed(Key.S)) {
                camera.position.z += speed
            }

            if (input.isKeyPressed(Key.A)) {
                camera.position.x -= speed
            }
            if (input.isKeyPressed(Key.D)) {
                camera.position.x += speed
            }

            if (input.isKeyPressed(Key.Q)) {
                camera.position.y -= speed
            }
            if (input.isKeyPressed(Key.E)) {
                camera.position.y += speed
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