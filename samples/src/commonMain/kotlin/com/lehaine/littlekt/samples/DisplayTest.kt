package com.lehaine.littlekt.samples

import com.lehaine.littlekt.*
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.NinePatchDrawable
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graph.node.component.createDefaultTheme
import com.lehaine.littlekt.graph.node.node2d.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorVertexShader
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkWorld
import com.lehaine.littlekt.input.GameAxis
import com.lehaine.littlekt.input.GameButton
import com.lehaine.littlekt.input.InputMultiplexer
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.util.MutableTextureAtlas
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(context: Context) : Game<Scene>(context) {

    private val assetProvider = AssetProvider(context)
    val testLoad by assetProvider.load<Texture>(resourcesVfs["person.png"])

    private var x = 0f
    private var y = 0f

    private var xVel = 0f
    private var yVel = 0f
    private val controller = InputMultiplexer<GameInput>(input)
    val camera = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = ExtendViewport(960, 540)
    }

    val shader = createShader(SimpleColorVertexShader(), SimpleColorFragmentShader())
    val colorBits = Color.WHITE.toFloatBits()

    val mesh = colorMesh {
        maxVertices = 4
    }.apply {
        setVertex {
            x = 50f
            y = 50f
            colorPacked = colorBits
        }

        setVertex {
            x = 66f
            y = 50f
            colorPacked = colorBits
        }

        setVertex {
            x = 66f
            y = 66f
            colorPacked = colorBits
        }

        setVertex {
            x = 50f
            y = 66f
            colorPacked = colorBits
        }

        indicesAsQuad()
    }


    enum class GameInput {
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_UP,
        MOVE_DOWN,
        HORIZONTAL,
        VERTICAL,
        MOVEMENT,
        JUMP
    }

    init {
        Logger.setLevels(Logger.Level.DEBUG)
        input.addInputProcessor(controller)

        controller.addBinding(GameInput.MOVE_LEFT, listOf(Key.A, Key.ARROW_LEFT), axes = listOf(GameAxis.LX))
        controller.addBinding(GameInput.MOVE_RIGHT, listOf(Key.D, Key.ARROW_RIGHT), axes = listOf(GameAxis.LX))
        controller.addBinding(GameInput.MOVE_UP, listOf(Key.W, Key.ARROW_UP), axes = listOf(GameAxis.LY))
        controller.addBinding(GameInput.MOVE_DOWN, listOf(Key.S, Key.ARROW_DOWN), axes = listOf(GameAxis.LY))
        controller.addBinding(GameInput.JUMP, listOf(Key.SPACE), listOf(GameButton.XBOX_A))
        controller.addAxis(GameInput.HORIZONTAL, GameInput.MOVE_RIGHT, GameInput.MOVE_LEFT)
        controller.addAxis(GameInput.VERTICAL, GameInput.MOVE_DOWN, GameInput.MOVE_UP)
        controller.addVector(
            GameInput.MOVEMENT,
            GameInput.MOVE_RIGHT,
            GameInput.MOVE_DOWN,
            GameInput.MOVE_LEFT,
            GameInput.MOVE_UP
        )
    }


    override suspend fun Context.start() {
        super.setSceneCallbacks(this)
        val batch = SpriteBatch(context, 5000)
        val pixelFontTexture = resourcesVfs["m5x7_16_0.png"].readTexture()
        val texture by assetProvider.load<Texture>(resourcesVfs["atlas.png"])
        val tiles: TextureAtlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val atlas: TextureAtlas = MutableTextureAtlas(context).apply {
            add(tiles)
            add(pixelFontTexture.slice(), "pixelFont")
        }.toImmutable()

        val slices: Array<Array<TextureSlice>> by assetProvider.prepare { texture.slice(16, 16) }
        val person by assetProvider.prepare { slices[0][0] }
        val bossFrame by assetProvider.prepare { atlas.getByPrefix("bossAttack7") }
        val bossAttack by assetProvider.prepare { atlas.getAnimation("bossAttack") }
        val boss by assetProvider.prepare { AnimatedSprite(bossAttack.firstFrame) }
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont(preloadedTextures = listOf(atlas["pixelFont"].slice))
        val cache = BitmapFontCache(pixelFont).also {
            it.addText("Test cache", 200f, 50f, scaleX = 4f, scaleY = 4f)
            it.tint(Color.RED)
        }

        val ldtkWorld by assetProvider.load<LDtkWorld>(resourcesVfs["ldtk/sample.ldtk"])
        val ninepatchImg by assetProvider.load<Texture>(resourcesVfs["bg_9.png"])
        val ninepatch by assetProvider.prepare { NinePatch(ninepatchImg, 3, 3, 3, 4) }
        val greyButtonNinePatch = NinePatch(
            Textures.atlas.getByPrefix("grey_button").slice,
            5,
            5,
            5,
            4
        )

        val panelNinePatch = NinePatch(
            Textures.atlas.getByPrefix("grey_panel").slice,
            6,
            6,
            6,
            6
        )
        val theme = createDefaultTheme(
            extraDrawables = mapOf(
                "Button" to mapOf(
                    Button.themeVars.normal to NinePatchDrawable(greyButtonNinePatch)
                        .apply {
                            modulate = Color.GREEN
                            minHeight = 50f
                        }),
                "Panel" to mapOf(
                    Panel.themeVars.panel to NinePatchDrawable(panelNinePatch).apply {
                        modulate = Color.GREEN
                    }
                )
            )
        )

        lateinit var panel: Container
        lateinit var rootControl: Control
        lateinit var progressBar: ProgressBar

        val scene by assetProvider.prepare {
            sceneGraph(context, viewport = ExtendViewport(960, 540), batch = batch) {
                rootControl = control {
                    anchorRight = 1f
                    anchorBottom = 1f

                    paddedContainer {
                        padding(10)
                        hBoxContainer {
                            separation = 20
                            vBoxContainer {
                                separation = 20

                                button {
                                    text = "Top Left"
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(100.milliseconds)
                                    }
                                }
                                button {
                                    text = "Center Left"
                                    horizontalAlign = HAlign.RIGHT
                                    verticalAlign = VAlign.BOTTOM
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(200.milliseconds)
                                    }
                                }
                                button {
                                    text = "Bottom Left"
                                    horizontalAlign = HAlign.LEFT
                                    verticalAlign = VAlign.TOP
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(300.milliseconds)
                                    }
                                }
                            }
                            vBoxContainer {
                                separation = 20

                                button {
                                    text = "Top Center"
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(100.milliseconds)
                                    }
                                }
                                button {
                                    text = "Center Center"
                                    horizontalAlign = HAlign.RIGHT
                                    verticalAlign = VAlign.BOTTOM
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(200.milliseconds)
                                    }
                                }
                                button {
                                    text = "Bottom Center"
                                    horizontalAlign = HAlign.LEFT
                                    verticalAlign = VAlign.TOP
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(300.milliseconds)
                                    }
                                }
                            }
                            vBoxContainer {
                                separation = 20

                                button {
                                    text = "Top Right"
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(100.milliseconds)
                                    }
                                }
                                button {
                                    text = "Center Right"
                                    horizontalAlign = HAlign.RIGHT
                                    verticalAlign = VAlign.BOTTOM
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(200.milliseconds)
                                    }
                                }
                                button {
                                    text = "Bottom Right"
                                    horizontalAlign = HAlign.LEFT
                                    verticalAlign = VAlign.TOP
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(300.milliseconds)
                                    }
                                }
                            }
                        }
                    }

                    panelContainer {
                        x = 300f
                        y = 150f

                        width = 200f
                        height = 50f

                        panel = paddedContainer {
                            padding(10)
                            centerContainer {
                                vBoxContainer {
                                    separation = 50
                                    label {
                                        text = "Action"
                                        horizontalAlign = HAlign.CENTER
                                    }

                                    label {
                                        text = "E"
                                        horizontalAlign = HAlign.CENTER
                                    }
                                }
                            }
                        }
                    }

                    progressBar = progressBar {
                        anchorLeft = 1f
                        anchorRight = 1f
                        x = 200f
                        y = 300f
                        width = 200f

                        ratio = 0.27f
                        onReady += {
                            println(anchorLeft)
                            println(anchorRight)
                            println(marginLeft)
                            println(marginRight)
                        }
                    }

                    panel {
                        x = 100f
                        y = 150f
                        width = 50f
                        height = 50f
                    }

                    label {
                        text = "I am a label!"
                        x = 150f
                        y = 350f
                    }

                    ninePatchRect {
                        ninePatch = ninepatch
                        x = 650f
                        y = 10f
                        width = 200f
                        height = 50f
                    }
                }
            }.also { it.initialize() }
        }

        val music by assetProvider.load<AudioStream>(resourcesVfs["music_short.mp3"])
        assetProvider.prepare {
            KtScope.launch {
                music.play(0.05f, true)
            }
            KtScope.launch {
                delay(2500)
                music.pause()
                delay(2500)
                music.resume()
                delay(1000)
                music.stop()
                delay(2000)
                music.play(0.05f, true)
            }
        }

        assetProvider.prepare {
            boss.playLooped(bossAttack)
            boss.x = 450f
            boss.y = 250f
            boss.scaleX = 2f
            boss.scaleY = 2f
        }

        assetProvider.onFullyLoaded = {
            scene.resize(graphics.width, graphics.height)
        }
        input.inputProcessor {
            val temp = MutableVec2f()
            onTouchDown { screenX, screenY, pointer ->
                logger.info { "pointer down at $screenX,$screenY: $pointer" }
            }

            onMouseMoved { screenX, screenY ->
                camera.unProjectScreen(screenX, screenY, context, temp)
                x = temp.x
                y = temp.y
            }

            onTouchDragged { screenX, screenY, pointer ->
                camera.unProjectScreen(screenX, screenY, context, temp)
                x = temp.x
                y = temp.y
            }
            onKeyUp {
                logger.info { "key up: $it" }
            }
        }

        onResize { width, height ->
            println("resize $width,$height")
            camera.update(width, height, context)
            if (!assetProvider.fullyLoaded) return@onResize
            scene.resize(width, height)
        }

        var firstRender = true
        var done = false
        var firstLoaded = true
        onRender { dt ->
            if (!assetProvider.fullyLoaded) {
                assetProvider.update()
                return@onRender
            }
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            if (firstLoaded) {
                logger.info { "Finished loading!" }
                firstLoaded = false
            }

            if (!firstRender && !done) {
                done = true
                panel.run {
                    label {
                        text = "Added later!"
                        horizontalAlign = HAlign.CENTER
                        verticalAlign = VAlign.CENTER
                    }
                }
            } else {
                firstRender = false
            }
            xVel = 0f
            yVel = 0f

            val velocity = controller.vector(GameInput.MOVEMENT)
            xVel = velocity.x * 10f
            yVel = velocity.y * 10f

            if (controller.pressed(GameInput.JUMP)) {
                yVel -= 25f
            }
            if (input.isKeyJustPressed(Key.ENTER)) {
                if (rootControl.theme == null) {
                    rootControl.theme = theme
                } else {
                    rootControl.theme = null
                }
            }

            if (input.isKeyPressed(Key.Z)) {
                progressBar.value -= progressBar.step
            } else if (input.isKeyPressed(Key.X)) {
                progressBar.value += progressBar.step
            }

            camera.viewport.apply(context)
            camera.update()
            boss.update(dt)
            batch.use(camera.viewProjection) {
                ldtkWorld.render(it, camera)
                it.draw(
                    person,
                    x,
                    y,
                    scaleX = 10f,
                    scaleY = 10f,
                    originX = person.width / 2f,
                    originY = person.height / 2f
                )
                slices.forEachIndexed { rowIdx, row ->
                    row.forEachIndexed { colIdx, slice ->
                        it.draw(slice, 150f * (rowIdx * row.size + colIdx) + 50f, 50f, scaleX = 10f, scaleY = 10f)
                    }
                }
                boss.render(it)
                it.draw(bossFrame.slice, 450f, 350f)
                cache.draw(it)
                it.draw(Textures.white, 200f, 400f, scaleX = 5f, scaleY = 5f)
            }

            scene.update(dt)
            scene.render()

            shader.bind()
            shader.uProjTrans?.apply(shader, camera.viewProjection)
            mesh.render(shader)

            x += xVel
            y += yVel


            if (input.isKeyJustPressed(Key.P)) {
                logger.debug { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
        onDispose {
            scene.dispose()
            mesh.dispose()
            texture.dispose()
            shader.dispose()
            batch.dispose()
        }
    }
}