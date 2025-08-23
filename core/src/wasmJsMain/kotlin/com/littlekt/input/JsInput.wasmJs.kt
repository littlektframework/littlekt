package com.littlekt.input

import org.w3c.dom.HTMLCanvasElement

actual fun createTouchIdentifiers(): IntArray = IntArray(20) { -1 }
actual fun createGamepads(): Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

@JsFun(code = "canvas => canvas.requestPointerLock()")
actual external fun nativeLockCursor(canvas: HTMLCanvasElement)
@JsFun("canvas => canvas.exitPointerLock()")
actual external fun nativeReleaseCursor(canvas: HTMLCanvasElement)
actual fun nativeCheckForGamepads(gamepads: Array<GamepadInfo>) {
//    TODO("Implement nativeCheckForGamepads")
}

actual fun nativeUpdateGamepads(
    gamepads: Array<GamepadInfo>,
    inputCache: InputCache
) {
//    TODO("Implement nativeUpdateGamepads")
}