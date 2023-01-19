package com.lehaine.littlekt.samples.s3d

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.file.vfs.readGltfModel
import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.addTo
import com.lehaine.littlekt.graph.node.node3d.Model
import com.lehaine.littlekt.graph.node.node3d.camera3d
import com.lehaine.littlekt.graph.node.node3d.directionalLight
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.shader.shaders.Albedo
import com.lehaine.littlekt.graphics.shader.shaders.ModelFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.ModelVertexShader
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.MutableVec4f
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.Vec4f
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.seconds
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/17/2022
 */
class GltfTest(context: Context) : ContextListener(context) {

    private val models = listOf(
        GltfModel("models/player.glb", "player.glb", Vec3f(100f)),
        GltfModel(
            "models/duck.glb",
            "duck.glb",
            rotation = MutableVec4f().setEuler(Angle.ZERO, (-90).degrees, Angle.ZERO)
        ),
        GltfModel("models/fox/Fox.gltf", "fox.gltf", animIdx = 1),
        GltfModel("models/flighthelmet/FlightHelmet.gltf", "FlightHelmet.gltf", Vec3f(200f))
    )

    private var modelIdx = 0
    private var loadingModel = false
    private lateinit var model: Model

    private data class GltfModel(
        val path: String,
        val name: String,
        val scale: Vec3f = Vec3f(1f),
        val rotation: Vec4f = Vec4f(0f, 0f, 0f, 1f),
        val animIdx: Int = -1,
    )

    override suspend fun Context.start() {
        println(ModelVertexShader(albedo = Albedo.VERTEX).generate(this))
        println()
        println(ModelFragmentShader(albedo = Albedo.VERTEX).generate(this))
        val scene = sceneGraph(this) {
            loadGltfModel(models[modelIdx], this)

            camera3d {
                active = true
                far = 1000f
                translate(0f, 250f, 250f)
                rotate((-45).degrees)

                onUpdate += {
                    val speed = (5f * if (input.isKeyPressed(Key.SHIFT_LEFT)) 10f else 1f)
                    if (input.isKeyPressed(Key.Q)) {
                        model.translate(0f, -speed, 0f)
                    }
                    if (input.isKeyPressed(Key.E)) {
                        model.translate(0f, speed, 0f)
                    }

                    if (input.isKeyPressed(Key.W)) {
                        model.rotate((-1).degrees)
                    }
                    if (input.isKeyPressed(Key.S)) {
                        model.rotate((1).degrees)
                    }

                    if (input.isKeyPressed(Key.A)) {
                        model.rotate(y = (-1).degrees)
                    }
                    if (input.isKeyPressed(Key.D)) {
                        model.rotate(y = 1.degrees)
                    }

                }
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
                anchorRight = 1f
                anchorBottom = 1f
                paddedContainer {
                    padding(10)
                    label {
                        text = "glTF Test with Scene Graph"
                    }
                }
                paddedContainer {
                    padding(10)
                    anchor(Control.AnchorLayout.TOP_RIGHT)
                    column {
                        separation = 10
                        label {
                            text = "Select Model"
                        }
                        row {
                            separation = 10
                            button {
                                text = "Prev"
                                onPressed += {
                                    if (!loadingModel) {
                                        model.destroy()
                                        modelIdx--
                                        if (modelIdx !in models.indices) modelIdx = models.size - 1
                                        loadGltfModel(models[modelIdx], this@sceneGraph)
                                    }
                                }
                            }
                            button {
                                text = "Next"
                                onPressed += {
                                    if (!loadingModel) {
                                        model.destroy()
                                        modelIdx++
                                        if (modelIdx !in models.indices) modelIdx = 0
                                        loadGltfModel(models[modelIdx], this@sceneGraph)
                                    }
                                }
                            }
                        }
                    }
                }

                label {
                    anchor(Control.AnchorLayout.CENTER)
                    text = "Loading..."

                    onUpdate += {
                        visible = loadingModel
                    }

                    onVisible += {
                        text = "Loading '${models[modelIdx].name}'..."
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

    private fun Context.loadGltfModel(gltfModel: GltfModel, sceneGraph: SceneGraph<*>) {
        if (loadingModel) return
        loadingModel = true
        KtScope.launch {
            model = resourcesVfs[models[modelIdx].path].readGltfModel().apply {
                rotation(gltfModel.rotation)
                scaling(gltfModel.scale)
                if (gltfModel.animIdx >= 0) {
                    enableAnimation(gltfModel.animIdx)
                    onUpdate += {
                        applyAnimation(it)
                    }
                }
            }.also { it.addTo(sceneGraph) }
            loadingModel = false
        }
    }
}