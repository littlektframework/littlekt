package com.littlekt.input

import com.littlekt.graphics.webgpu.navigator
import com.littlekt.log.Console
import com.littlekt.util.datastructure.fastForEach
import org.w3c.dom.HTMLCanvasElement

actual fun createTouchIdentifiers(): IntArray = IntArray(20) { -1 }
actual fun createGamepads(): Array<GamepadInfo> = Array(8) { GamepadInfo(it) }
actual fun nativeLockCursor(canvas: HTMLCanvasElement) {
    js("canvas.requestPointerLock()")
}

actual fun nativeReleaseCursor(canvas: HTMLCanvasElement) {
    js("canvas.exitPointerLock()")
}

actual fun nativeCheckForGamepads(gamepads: Array<GamepadInfo>) {
    try {
        if (navigator.getGamepads != null) {
            val jsGamepads = navigator.getGamepads()
            gamepads.fastForEach { it.connected = false }

            for (gamepadId in 0 until jsGamepads.length) {
                val controller = jsGamepads[gamepadId]
                val gamepad = gamepads.getOrNull(gamepadId) ?: continue
                gamepad.apply {
                    this.connected = controller.connected
                    this.name = controller.id
                }
            }
        }
    } catch (e: Throwable) {
        Console.error(e)
    }
}

actual fun nativeUpdateGamepads(gamepads: Array<GamepadInfo>, inputCache: InputCache) {
    try {
        if (navigator.getGamepads != null) {
            val jsGamepads = navigator.getGamepads()

            for (gamepadId in 0 until jsGamepads.length) {
                val controller = jsGamepads[gamepadId]
                val gamepad = gamepads.getOrNull(gamepadId) ?: continue
                gamepad.apply {
                    for (n in 0 until controller.buttons.length) {
                        val button = controller.buttons[n]
                        this.rawButtonsPressed[n] = button.value.toFloat()
                    }
                    inputCache.updateGamepadTrigger(GameButton.L2, gamepad)
                    inputCache.updateGamepadTrigger(GameButton.R2, gamepad)
                    inputCache.updateGamepadButtons(gamepad)

                    for (n in 0 until controller.axes.length) {
                        this.rawAxes[n] = controller.axes[n].toFloat()
                    }
                    inputCache.updateGamepadStick(GameStick.LEFT, gamepad)
                    inputCache.updateGamepadStick(GameStick.RIGHT, gamepad)
                }
            }
        }
    } catch (e: Throwable) {
        Console.error(e)
    }
}