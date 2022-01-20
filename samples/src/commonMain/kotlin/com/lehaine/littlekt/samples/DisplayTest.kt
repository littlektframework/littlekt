package com.lehaine.littlekt.samples

import com.lehaine.littlekt.*
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readAudioStream
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graph.node.node2d.Node2D
import com.lehaine.littlekt.graph.node.node2d.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorVertexShader
import com.lehaine.littlekt.input.GameAxis
import com.lehaine.littlekt.input.GameButton
import com.lehaine.littlekt.input.InputMultiplexer
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger

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
    val camera = OrthographicCamera(graphics.width, graphics.height)

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
        camera.translate(graphics.width / 2f, graphics.height / 2f, 0f)

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

        val batch = SpriteBatch(context)
        val texture = resourcesVfs["atlas.png"].readTexture()
        val atlas: TextureAtlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val slices: Array<Array<TextureSlice>> = texture.slice(16, 16)
        val person = slices[0][0]
        val bossAttack = atlas.getAnimation("bossAttack")
        val boss = AnimatedSprite(bossAttack.firstFrame)
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        val cache = BitmapFontCache(pixelFont).also {
            it.addText("Test cache", 200f, 50f, scaleX = 4f, scaleY = 4f)
            it.tint(Color.RED)
        }
        val ninepatchImg = resourcesVfs["bg_9.png"].readTexture()
        val ninepatch = NinePatch(ninepatchImg, 3, 3, 3, 4)
        val secondNinePatch = NinePatch(
            Textures.atlas.getByPrefix("grey_button").slice,
            4,
            4,
            8,
            4
        )

        val scene = sceneGraph(context, batch = batch) {
            paddedContainer {
                padding(10)
                vBoxContainer {
                    separation = 20
                    button {
                        text = "Center Center"
                        minHeight = 50f
                        onPressed += {
                            logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                        }
                    }
                    button {
                        text = "Bottom Right"
                        horizontalAlign = HAlign.RIGHT
                        verticalAlign = VAlign.BOTTOM
                        minHeight = 50f
                        onPressed += {
                            logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                        }
                    }
                    button {
                        text = "Top Left"
                        horizontalAlign = HAlign.LEFT
                        verticalAlign = VAlign.TOP
                        minHeight = 50f
                        onPressed += {
                            logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                        }
                    }
                }
            }

            label {
                text = "I am a label!"
                marginLeft = 150f
                marginTop = 350f
            }

            ninePatchRect {
                ninePatch = ninepatch
                marginLeft = 250f
                marginTop = 10f
                minWidth = 200f
                minHeight = 50f
            }
        }.also { it.initialize() }

        val music = resourcesVfs["music_short.mp3"].readAudioStream()
//        music.play(0.05f, true)
//
//        launch {
//            delay(2500)
//            music.pause()
//            delay(2500)
//            music.resume()
//            delay(1000)
//            music.stop()
//            delay(2000)
//            music.play(0.05f, true)
//        }

        boss.playLooped(bossAttack)
        boss.x = 450f
        boss.y = 250f
        boss.scaleX = 2f
        boss.scaleY = 2f

        input.inputProcessor {
            onKeyUp {
                logger.info { "key up: $it" }
            }
        }

        onResize { width, height ->
            scene.resize(width, height)
        }

        onRender { dt ->
            if (!assetProvider.fullyLoaded) return@onRender

            xVel = 0f
            yVel = 0f

            val velocity = controller.vector(GameInput.MOVEMENT)
            xVel = velocity.x * 10f
            yVel = velocity.y * 10f

            if (controller.pressed(GameInput.JUMP)) {
                yVel -= 25f
            }

            gl.clearColor(Color.DARK_GRAY)
            camera.update()
            boss.update(dt)
            batch.use(camera.viewProjection) {
                it.draw(person, x, y, scaleX = 10f, scaleY = 10f)
                slices.forEachIndexed { rowIdx, row ->
                    row.forEachIndexed { colIdx, slice ->
                        it.draw(slice, 150f * (rowIdx * row.size + colIdx) + 50f, 50f, scaleX = 10f, scaleY = 10f)
                    }
                }
                boss.render(it)
                it.draw(Textures.white, 200f, 400f, scaleX = 5f, scaleY = 5f)
                ninepatch.draw(it, 200f, 200f, 25f, 20f, scaleX = 5f, scaleY = 5f)
                secondNinePatch.draw(it, 400f, 250f, 50f, 50f, scaleX = 3f, scaleY = 3f)
                cache.draw(it)
                it.draw(Textures.atlas.getByPrefix("grey_button").slice, 250f, 400f)
                secondNinePatch.draw(it, 250f, 250f, 200f, 50f)
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
            mesh.dispose()
            texture.dispose()
            shader.dispose()
            batch.dispose()
        }
    }
}

private class TextureNode(val slice: TextureSlice) : Node2D() {

    override fun render(batch: SpriteBatch, camera: Camera) {
        batch.draw(
            slice,
            globalX,
            globalY,
            scaleX = globalScaleX,
            scaleY = globalScaleY,
            rotation = globalRotation
        )
    }
}