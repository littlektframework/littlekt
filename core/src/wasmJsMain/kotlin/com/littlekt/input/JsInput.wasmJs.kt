package com.littlekt.input

import com.littlekt.graphics.webgpu.navigator
import com.littlekt.log.Console
import com.littlekt.util.datastructure.fastForEach
import org.w3c.dom.HTMLCanvasElement

actual fun createTouchIdentifiers(): IntArray = IntArray(20) { -1 }
actual fun createGamepads(): Array<GamepadInfo> = Array(8) { GamepadInfo(it) }

@JsFun(code = "canvas => canvas.requestPointerLock()")
actual external fun nativeLockCursor(canvas: HTMLCanvasElement)

@JsFun("canvas => canvas.exitPointerLock()")
actual external fun nativeReleaseCursor(canvas: HTMLCanvasElement)
actual fun nativeCheckForGamepads(gamepads: Array<GamepadInfo>) {
    try {
        val jsGamepads = navigator.getGamepads()
        gamepads.fastForEach { it.connected = false }
        for (gamepadId in 0 until jsGamepads.length) {
            val controller = jsGamepads[gamepadId] ?: continue
            val gamepad = gamepads.getOrNull(gamepadId) ?: continue
            gamepad.apply {
                this.connected = controller.connected
                this.name = controller.id
            }
        }
    } catch (e: Throwable) {
        Console.error(e)
    }
}

actual fun nativeUpdateGamepads(gamepads: Array<GamepadInfo>, inputCache: InputCache) {
    try {
        val jsGamepads = navigator.getGamepads()
        for (gamepadId in 0 until jsGamepads.length) {
            val controller = jsGamepads[gamepadId] ?: continue
            val gamepad = gamepads.getOrNull(gamepadId) ?: continue
            gamepad.apply {
                for (n in 0 until controller.buttons.length) {
                    val button = controller.buttons[n] ?: continue
                    this.rawButtonsPressed[n] = button.value.toFloat()
                }
                inputCache.updateGamepadTrigger(GameButton.L2, gamepad)
                inputCache.updateGamepadTrigger(GameButton.R2, gamepad)
                inputCache.updateGamepadButtons(gamepad)

                for (n in 0 until controller.axes.length) {
                    val axes = controller.axes[n] ?: continue
                    this.rawAxes[n] = axes.toDouble().toFloat()
                }
                inputCache.updateGamepadStick(GameStick.LEFT, gamepad)
                inputCache.updateGamepadStick(GameStick.RIGHT, gamepad)
            }
        }
    } catch (e: Throwable) {
        Console.error(e)
    }
}