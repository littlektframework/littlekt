package com.lehaine.littlekt.samples.s3d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readGltfModel
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.node3d.camera3d
import com.lehaine.littlekt.graph.node.node3d.directionalLight
import com.lehaine.littlekt.graph.node.node3d.meshNode
import com.lehaine.littlekt.graph.node.ui.control
import com.lehaine.littlekt.graph.node.ui.label
import com.lehaine.littlekt.graph.node.ui.paddedContainer
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.VertexAttribute
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.mesh
import com.lehaine.littlekt.graphics.shader.shaders.ModelVertexShader
import com.lehaine.littlekt.input.Key
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

        println(ModelVertexShader().generate(this))
        val scene = sceneGraph(this) {

//            val duck = resourcesVfs["models/duck.glb"].readGltfModel().apply {
//                translate(100f, 0f, 0f)
//                rotate(y = (-90).degrees)
//            }.also { it.addTo(this) }

            resourcesVfs["models/fox/Fox.gltf"].readGltfModel(loadTexturesAsynchronously = true).apply {
                enableAnimation(0)
                onUpdate += {
                    applyAnimation(it)
                }
            }.also { it.addTo(this) }

//            resourcesVfs["models/flighthelmet/FlightHelmet.gltf"].readGltfModel(loadTexturesAsynchronously = true)
//                .apply {
//                    translate(-100f, 0f, 0f)
//                    scale(200f)
//                }.also { it.addTo(this) }
//

            camera3d {
                active = true
                far = 1000f
                translate(0f, 0f, 250f)

                onUpdate += {
                    val speed = 5f * if (input.isKeyPressed(Key.SHIFT_LEFT)) 10f else 1f
                    if (input.isKeyPressed(Key.W)) {
                        rotate(1.degrees)
                    }
                    if (input.isKeyPressed(Key.S)) {
                        rotate((-1).degrees)
                    }

                    if (input.isKeyPressed(Key.A)) {
                        rotate(y = 1.degrees)
                    }
                    if (input.isKeyPressed(Key.D)) {
                        rotate(y = (-1).degrees)
                    }

                    if (input.isKeyPressed(Key.Q)) {
                        translate(0f, -speed, 0f)
                    }
                    if (input.isKeyPressed(Key.E)) {
                        translate(0f, speed, 0f)
                    }

//                    if (input.isKeyPressed(Key.ARROW_UP)) {
//                        duck.z += speed
//                    }
//
//                    if (input.isKeyPressed(Key.ARROW_DOWN)) {
//                        duck.z -= speed
//                    }

                }
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
                    x = 1f + sin(time.seconds) * 2f
                    y = sin(time.seconds / 2f) * 1f
                    z = 0f
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