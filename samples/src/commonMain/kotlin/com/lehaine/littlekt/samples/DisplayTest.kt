package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.createShader
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readPixmap
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorVertexShader
import com.lehaine.littlekt.input.GameAxis
import com.lehaine.littlekt.input.GameButton
import com.lehaine.littlekt.input.InputMultiplexer
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import kotlin.math.roundToInt

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(context: Context) : Game<Scene>(context) {

    val testLoad by load<Texture>(resourcesVfs["person.png"])

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

    override suspend fun Context.run() {
        val batch = SpriteBatch(context)
        val texture = resourcesVfs["atlas.png"].readTexture()
        val atlas: TextureAtlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val slices: Array<Array<TextureSlice>> = texture.slice(16, 16)
        val person = slices[0][0]
        val bossAttack = atlas.getAnimation("bossAttack")
        val boss = AnimatedSprite(bossAttack.firstFrame)
        val cursorPixmap = resourcesVfs["cursor.png"].readPixmap()
        graphics.setCursor(
            Cursor(
                cursorPixmap,
                (cursorPixmap.width * 0.5f).roundToInt(),
                (cursorPixmap.height * 0.5f).roundToInt()
            )
        )

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

        onRender { dt ->
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
                it.draw(Textures.default, 150f, 450f, scaleX = 5f, scaleY = 5f)
                it.draw(Textures.white, 200f, 400f, scaleX = 5f, scaleY = 5f)
                it.draw(Textures.transparent, 220f, 400f, scaleX = 5f, scaleY = 5f)
                it.draw(Textures.red, 240f, 400f, scaleX = 5f, scaleY = 5f)
                it.draw(Textures.green, 260f, 400f, scaleX = 5f, scaleY = 5f)
                it.draw(Textures.blue, 280f, 400f, scaleX = 5f, scaleY = 5f)
                it.draw(Textures.black, 300f, 400f, scaleX = 5f, scaleY = 5f)
            }

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
