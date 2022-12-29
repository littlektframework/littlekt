package com.lehaine.littlekt.samples.s3d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readGltfModel
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.node3d.*
import com.lehaine.littlekt.graph.node.ui.control
import com.lehaine.littlekt.graph.node.ui.label
import com.lehaine.littlekt.graph.node.ui.paddedContainer
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.VertexAttribute
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.CompareFunction
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.mesh
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.seconds
import kotlin.math.sin
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class GltfTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {

        val camera: Camera3D

        val duckModel: Model
        val humanModel: Model
        val cube: MeshNode
        val scene = sceneGraph(this) {
            camera = camera3d {
                active = true
                far = 1000f
                translate(0f, 0f, 250f)
            }
            duckModel = resourcesVfs["models/duck.glb"].readGltfModel().also { it.addTo(this) }
            humanModel = resourcesVfs["models/player.glb"].readGltfModel().also { it.addTo(this) }
            cube = meshNode {
                mesh = mesh(
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
            }
            directionalLight {
                var time = Duration.ZERO
                translate(1.2f, 1f, 2f)
                onUpdate += {
                    transform.setToTranslate(1f + sin(time.seconds) * 2f, sin(time.seconds / 2f) * 1f, 0f)
                    time += it
                }
            }

            control {
                paddedContainer {
                    padding(10)
                    label {
                        text = "glTF Test with Scene Graph"
                    }
                }
            }
        }.also { it.initialize() }

        duckModel.translate(100f, 0f, 0f)
        duckModel.rotate(Vec3f.Y_AXIS, (-90).degrees)
        humanModel.translate(-100f, 0f, 0f)
        humanModel.scale(85f)
        cube.scale(50f)

        onResize { width, height ->
            scene.resize(width, height, false)
        }

        gl.enable(State.DEPTH_TEST)
        gl.depthMask(true)
        gl.depthFunc(CompareFunction.LESS)

        var time = Duration.ZERO
        onRender { dt ->
            time += dt
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_DEPTH_BUFFER_BIT)

            scene.update(dt)
            scene.render()

            val speed = 5f * if (input.isKeyPressed(Key.SHIFT_LEFT)) 10f else 1f
            if (input.isKeyPressed(Key.W)) {
                camera.rotate(Vec3f.X_AXIS, 1.degrees)
            }
            if (input.isKeyPressed(Key.S)) {
                camera.rotate(Vec3f.X_AXIS, (-1).degrees)
            }

            if (input.isKeyPressed(Key.A)) {
                camera.rotate(Vec3f.Y_AXIS, 1.degrees)
            }
            if (input.isKeyPressed(Key.D)) {
                camera.rotate(Vec3f.Y_AXIS, (-1).degrees)
            }

            if (input.isKeyPressed(Key.Q)) {
                camera.translate(0f, -speed, 0f)
            }
            if (input.isKeyPressed(Key.E)) {
                camera.translate(0f, speed, 0f)
            }

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.T)) {
                logger.info { scene.root.treeString() }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}