package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.shape.ShapeRenderer
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.gesture.GestureController
import com.lehaine.littlekt.util.combine
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 10/24/2022
 */
class GestureControllerTest(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val viewport = ExtendViewport(960, 540)
        val camera = viewport.camera

        var gestureStatus = "TBD"

        GestureController(input) {
            onTouchDown { screenX, screenY, pointer ->
                gestureStatus = "Pointer ${pointer.index} touch down at $screenX, $screenY"
            }

            onFling { velocityX, velocityY, pointer ->
                gestureStatus = "Pointer ${pointer.index} fling at a velocity of $velocityX, $velocityY"
            }

            onPinch { initialPos1, initialPos2, pos1, pos2 ->
                gestureStatus = "Pinching!"
            }

            onZoom { initialDistance, distance ->
                gestureStatus = "Zooming a distance of $distance"
            }

            onPinchStop {
                gestureStatus = "Pinch stopped!"
            }

            onLongPress { screenX, screenY ->
                gestureStatus = "Long pressing at $screenX, $screenY"
            }

            onPan { screenX, screenY, dx, dy ->
                gestureStatus = "Panning at $screenX,$screenY with a delta of $dx,$dy"
            }

            onPanStop { screenX, screenY, pointer ->
                gestureStatus = "Panning stopped at $screenX,$screenY"
            }
        }.also {
            input.addInputProcessor(it)
        }

        onResize { width, height ->
            viewport.update(width, height, this, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            camera.update()
            batch.use(camera.viewProjection) { batch ->
                Fonts.default.draw(batch, gestureStatus, 100f, 250f)
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