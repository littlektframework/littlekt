package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.createShader
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorVertexShader
import com.lehaine.littlekt.input.GameButton
import com.lehaine.littlekt.input.GameStick
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import kotlin.math.absoluteValue
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(context: Context) : Game<Scene>(context), InputProcessor {

    val batch = SpriteBatch(context)

    val texture by load<Texture>(resourcesVfs["atlas.png"])
    val atlas: TextureAtlas by load(resourcesVfs["tiles.atlas.json"])
    val slices: Array<Array<TextureSlice>> by prepare { texture.slice(16, 16) }
    val person by prepare { slices[0][0] }
    val bossAttack by prepare { atlas.getAnimation("bossAttack") }
    val boss by prepare { AnimatedSprite(bossAttack.firstFrame) }

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

    val camera = OrthographicCamera(graphics.width, graphics.height)

    private var x = 0f
    private var y = 0f

    private var xVel = 0f
    private var yVel = 0f

    init {
        logger.level = Logger.Level.DEBUG
        input.inputProcessor = this
        camera.translate(graphics.width / 2f, graphics.height / 2f, 0f)
    }

    override fun create() {
        boss.playLooped(bossAttack)
        boss.x = 450f
        boss.y = 250f
        boss.scaleX = 2f
        boss.scaleY = 2f
    }

    override fun update(dt: Duration) {
        xVel = 0f
        yVel = 0f

        val deadZone = 0.3f
        val leftStickX = input.axisLeftX
        val leftStickY = input.axisLeftY

        if (leftStickX.absoluteValue > deadZone || leftStickY.absoluteValue > deadZone) {
            xVel = leftStickX * 10f
            yVel = leftStickY * 10f
        }

        if (input.isKeyPressed(Key.W)) {
            yVel -= 10f
        }
        if (input.isKeyPressed(Key.S)) {
            yVel += 10f
        }
        if (input.isKeyPressed(Key.A)) {
            xVel -= 10f
        }
        if (input.isKeyPressed(Key.D)) {
            xVel += 10f
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

    override fun gamepadButtonPressed(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        println("Gamepad $button pressed")
        return false
    }

    override fun gamepadButtonReleased(button: GameButton, gamepad: Int): Boolean {
        println("Gamepad $button released")
        return false
    }

    override fun gamepadJoystickMoved(stick: GameStick, xAxis: Float, yAxis: Float, gamepad: Int): Boolean {
     //   println("Gamepad joystick $stick moved to $xAxis,$yAxis")
        return false
    }

    override fun gamepadTriggerChanged(button: GameButton, pressure: Float, gamepad: Int): Boolean {
        println("Gamepad trigger $button changed to $")
        return false
    }

    override fun resize(width: Int, height: Int) {
        logger.debug { "Resize to $width,$height" }
    }

    override fun dispose() {
        mesh.dispose()
        texture.dispose()
        shader.dispose()
        batch.dispose()
    }
}
