package com.lehaine.littlekt.util.internal

import com.lehaine.littlekt.Application
import com.lehaine.littlekt.DevicePosition
import com.lehaine.littlekt.GamePosition

/**
 * Convert a device coordinate into a game screen coordinate
 *  * @author Colton Daily
 * @date 11/7/2021
 */
fun Application.convert(x: DevicePosition, y: DevicePosition): Pair<GamePosition, GamePosition>? {
//    val converted = this.viewport.convert(
//        x,
//        y,
//        this.deviceScreen.width,
//        this.deviceScreen.height,
//        this.gameScreen.width,
//        this.gameScreen.height
//    )
//    val (gameX, gameY) = converted
//    return if (
//    // x within the game screen
//        (0 <= gameX && gameX <= this.gameScreen.width) &&
//        // y within the game screen
//        (0 <= gameY && gameY <= this.gameScreen.height)
//    ) {
//        converted
//    } else {
//        null
//    }
    // TODO: 11/7/2021 impl
    return null
}