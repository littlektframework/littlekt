package com.lehaine.littlekt.samples.s3d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readGltfModel
import com.lehaine.littlekt.graph.node.node3d.MeshNode
import com.lehaine.littlekt.graph.node.node3d.Model
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.PerspectiveCamera
import com.lehaine.littlekt.graphics.VertexAttribute
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.CompareFunction
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.mesh
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.shaders.ModelFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.ModelVertexShader
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.MutableVec3f
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.seconds
import com.lehaine.littlekt.util.viewport.ScreenViewport
import kotlin.math.sin
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class GltfTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val duckModel = resourcesVfs["models/duck.glb"].readGltfModel()
        val humanModel = resourcesVfs["models/player.glb"].readGltfModel()
        val lightPos = MutableVec3f(1.2f, 1f, 2f).toMutableVec3()
        val shader: ShaderProgram<ModelVertexShader, ModelFragmentShader> =
            ShaderProgram(ModelVertexShader(), ModelFragmentShader())
                .also { it.prepare(this) }
                .apply {
                    bind()
                    fragmentShader.uLightColor.apply(this, Color.WHITE)
                    fragmentShader.uAmbientStrength.apply(this, 0.1f)
                    fragmentShader.uLightPosition.apply(this, lightPos)
                    fragmentShader.uSpecularStrength.apply(this, 0.5f)
                }
        val viewport = ScreenViewport(graphics.width, graphics.height, PerspectiveCamera().apply { far = 1000f })
        val camera = viewport.camera
        val cube =
            Model().apply {
                val meshName = "cube_mesh"
                val meshNode = MeshNode(
                    mesh(
                        listOf(VertexAttribute.POSITION, VertexAttribute.NORMAL),
                        grow = true
                    ) {
                        generate {
                            cube {
                                colored()
                                centered()
                            }
                        }
                    }
                ).apply { name = meshName }
                addChild(meshNode)
                meshes[meshName] = meshNode
            }

        duckModel.printHierarchy()

        camera.position.z += 250f
        duckModel.translate(100f, 0f, 0f)
        duckModel.rotate(Vec3f.Y_AXIS, (-90).degrees)
        humanModel.translate(-100f, 0f, 0f)
        humanModel.scale(85f)
        cube.scale(50f)

        onResize { width, height ->
            viewport.update(width, height, context, false)
        }

        gl.enable(State.DEPTH_TEST)
        gl.depthMask(true)
        gl.depthFunc(CompareFunction.LESS)

        var time = Duration.ZERO
        onRender { dt ->
            time += dt
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_DEPTH_BUFFER_BIT)

            lightPos.x = 1f + sin(time.seconds) * 2f
            lightPos.y = sin(time.seconds / 2f) * 1f

            camera.update()
            duckModel.update()
            humanModel.update()
            cube.update()

            shader.bind()
            shader.uProjTrans?.apply(shader, camera.viewProjection)
            shader.fragmentShader.uLightPosition.apply(shader, lightPos)
            shader.vertexShader.uModel.apply(shader, duckModel.modelMat)
            shader.fragmentShader.uViewPosition.apply(shader, camera.position)

            duckModel.render(shader)

            shader.vertexShader.uModel.apply(shader, humanModel.modelMat)
            humanModel.render(shader)

            shader.vertexShader.uModel.apply(shader, cube.modelMat)
            cube.render(shader)

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