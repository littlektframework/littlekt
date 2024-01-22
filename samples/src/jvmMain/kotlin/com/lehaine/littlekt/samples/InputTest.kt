package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Fonts
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.g2d.*
import com.lehaine.littlekt.graphics.g2d.shape.ShapeRenderer
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.toFloatBits
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.gesture.gestureController
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.util.combine
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration.Companion.milliseconds


class InputTest(context: Context) : ContextListener(context) {


    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera

        var inputStatus = "TBD"
        var gestureStatus = "TBD"

        input.inputProcessor {
            onMouseMoved { screenX, screenY ->
                inputStatus = "Moving at $screenX, $screenY"
            }

            onTouchDown { screenX, screenY, pointer ->
                inputStatus = "Pointer ${pointer.index} touch down at $screenX, $screenY"
            }

            onTouchDragged { screenX, screenY, pointer ->
                inputStatus = "Pointer ${pointer.index} touch dragged at $screenX, $screenY"
            }

            onTouchUp { screenX, screenY, pointer ->
                inputStatus = "Pointer ${pointer.index} touch up at $screenX, $screenY"
            }
        }

        input.gestureController {
            onTouchDown { screenX, screenY, pointer ->
                gestureStatus = "Pointer ${pointer.index} touch down at $screenX, $screenY"
            }

            onFling { velocityX, velocityY, pointer ->
                gestureStatus = "Pointer ${pointer.index} fling at a velocity of $velocityX, $velocityY"
            }

            onPinch { initialPos1, initialPos2, pos1, pos2 ->
                gestureStatus = "Pinching!"
            }

            onTap { screenX, screenY, count, pointer ->
                gestureStatus = "Pointer ${pointer.index} is tapping at $screenX,$screenY for a total of $count taps."
            }

            onZoom { initialDistance, distance ->
                gestureStatus = "Zooming a distance of $distance"
            }

            onPinchStop {
                gestureStatus = "Pinch stopped!"
            }

            onLongPress { screenX, screenY ->
                gestureStatus = "Long pressing at $screenX, $screenY"
                input.vibrate(100.milliseconds)
            }

            onPan { screenX, screenY, dx, dy ->
                gestureStatus = "Panning at $screenX,$screenY with a delta of $dx,$dy"
            }

            onPanStop { screenX, screenY, pointer ->
                gestureStatus = "Panning stopped at $screenX,$screenY"
            }
        }

        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            batch.use(camera.viewProjection) { batch ->
                Fonts.default.draw(batch, gestureStatus, 100f, 450f)
                Fonts.default.draw(batch, inputStatus, 100f, 250f)
                Fonts.default.draw(batch, "FPS: ${context.stats.fps.toInt()}", 35f, 50f)
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

/**
 * @author Colton Daily
 * @date 1/21/2024
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "JVM - Input Test"
        icons = listOf("icon_16x16.png", "icon_32x32.png", "icon_48x48.png")
        backgroundColor = Color.DARK_GRAY
    }.start {
        InputTest(it)
    }
}