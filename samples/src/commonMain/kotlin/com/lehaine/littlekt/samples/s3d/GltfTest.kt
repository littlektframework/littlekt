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

        val scene = sceneGraph(this) {
            camera3d {
                active = true
                far = 1000f
                translate(0f, 0f, 250f)

                onUpdate += {
                    val speed = 5f * if (input.isKeyPressed(Key.SHIFT_LEFT)) 10f else 1f
                    if (input.isKeyPressed(Key.W)) {
                        rotate(Vec3f.X_AXIS, 1.degrees)
                    }
                    if (input.isKeyPressed(Key.S)) {
                        rotate(Vec3f.X_AXIS, (-1).degrees)
                    }

                    if (input.isKeyPressed(Key.A)) {
                        rotate(Vec3f.Y_AXIS, 1.degrees)
                    }
                    if (input.isKeyPressed(Key.D)) {
                        rotate(Vec3f.Y_AXIS, (-1).degrees)
                    }

                    if (input.isKeyPressed(Key.Q)) {
                        translate(0f, -speed, 0f)
                    }
                    if (input.isKeyPressed(Key.E)) {
                        translate(0f, speed, 0f)
                    }
                }
            }
            resourcesVfs["models/duck.glb"].readGltfModel().apply {
                translate(100f, 0f, 0f)
                rotate(Vec3f.Y_AXIS, (-90).degrees)
            }.also { it.addTo(this) }

            resourcesVfs["models/flighthelmet/FlightHelmet.gltf"].readGltfModel().apply {
                translate(-100f, 0f, 0f)
                scale(200f)
            }.also { it.addTo(this) }

            meshNode {
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
                scale(50f)
            }

            meshNode {
                mesh = mesh(
                    listOf(VertexAttribute.POSITION, VertexAttribute.NORMAL),
                    grow = true
                ) {
                    generate {
                        grid { }
                    }
                }
                translate(0f, -50f, 0f)
                scale(50f)
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

        onResize { width, height ->
            scene.resize(width, height, false)
        }

        var time = Duration.ZERO
        onRender { dt ->
            time += dt
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_DEPTH_BUFFER_BIT)

            scene.update(dt)
            scene.render()

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