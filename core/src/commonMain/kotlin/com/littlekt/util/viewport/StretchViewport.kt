package com.littlekt.util.viewport

import com.littlekt.graphics.Camera
import com.littlekt.graphics.OrthographicCamera
import com.littlekt.util.Scaler

/**
 * A viewport that supports using a virtual size. The virtual viewport is stretched to fit the
 * screen. There are no black bars and the aspect ratio can change after scaling.
 */
open class StretchViewport(
    virtualWidth: Int,
    virtualHeight: Int,
    camera: Camera = OrthographicCamera()
) : ScalingViewport(Scaler.Stretch(), virtualWidth, virtualHeight, camera)
