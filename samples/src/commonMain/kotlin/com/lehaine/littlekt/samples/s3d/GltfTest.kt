package com.lehaine.littlekt.samples.s3d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readGltfModel
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.PerspectiveCamera
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.ModelFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.ModelVertexShader
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.viewport.ScreenViewport

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class GltfTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        println("vertex:")
        println(ModelVertexShader().generate(this))
        println()
        println()
        println("fragment:")
        println(ModelFragmentShader().generate(this))
        val model = resourcesVfs["models/duck.glb"].readGltfModel()
        val shader: ShaderProgram<ModelVertexShader, ModelFragmentShader> =
            ShaderProgram(ModelVertexShader(), ModelFragmentShader())
                .also { it.prepare(this) }
                .apply {
                    bind()
                    fragmentShader.uLightColor.apply(this, Color.WHITE)
                    fragmentShader.uAmbientStrength.apply(this, 0.1f)
                    fragmentShader.uLightPosition.apply(this, Vec3f(1.2f, 1f, 2f))
                }
        val viewport = ScreenViewport(graphics.width, graphics.height, PerspectiveCamera())
        val camera = viewport.camera

        camera.position.z += 250
        //model.scale(100f)

        onResize { width, height ->
            viewport.update(width, height, context, false)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            model.update()

            shader.bind()
            shader.uProjTrans?.apply(shader, camera.viewProjection)
            shader.vertexShader.uModel.apply(shader, model.modelMat)

            model.render(shader)

            val speed = 5f * if (input.isKeyPressed(Key.SHIFT_LEFT)) 10f else 1f
            if (input.isKeyPressed(Key.W)) {
                camera.rotateAround(Vec3f.ZERO, Vec3f.X_AXIS, 1.degrees)
            }
            if (input.isKeyPressed(Key.S)) {
                camera.rotateAround(Vec3f.ZERO, Vec3f.X_AXIS, (-1).degrees)
            }

            if (input.isKeyPressed(Key.A)) {
                camera.rotateAround(Vec3f.ZERO, Vec3f.Y_AXIS, 1.degrees)
            }
            if (input.isKeyPressed(Key.D)) {
                camera.rotateAround(Vec3f.ZERO, Vec3f.Y_AXIS, (-1).degrees)
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