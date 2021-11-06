package com.lehaine.littlekt

import com.lehaine.littlekt.util.TimeSpan

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
actual class PlatformGraphics : Graphics {
    override val GL: GL
        get() = TODO("Not yet implemented")
    override val width: Int
        get() = TODO("Not yet implemented")
    override val height: Int
        get() = TODO("Not yet implemented")
    override val backBufferWidth: Int
        get() = TODO("Not yet implemented")
    override val backBufferHeight: Int
        get() = TODO("Not yet implemented")
    override val safeInsetLeft: Int
        get() = TODO("Not yet implemented")
    override val safeInsetTop: Int
        get() = TODO("Not yet implemented")
    override val safeInsetBottom: Int
        get() = TODO("Not yet implemented")
    override val safeInsetRight: Int
        get() = TODO("Not yet implemented")

    override fun getFrameId(): Long {
        TODO("Not yet implemented")
    }

    override fun getDeltaTime(): TimeSpan {
        TODO("Not yet implemented")
    }

    override fun getFramesPerSecond(): Int {
        TODO("Not yet implemented")
    }

    override fun getType(): Graphics.GraphicsType? {
        TODO("Not yet implemented")
    }

    override fun getPpiX(): Float {
        TODO("Not yet implemented")
    }

    override fun getPpiY(): Float {
        TODO("Not yet implemented")
    }

    override fun getPpcX(): Float {
        TODO("Not yet implemented")
    }

    override fun getPpcY(): Float {
        TODO("Not yet implemented")
    }

    override fun supportsDisplayModeChange(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPrimaryMonitor(): Graphics.Monitor {
        TODO("Not yet implemented")
    }

    override fun getMonitor(): Graphics.Monitor {
        TODO("Not yet implemented")
    }

    override fun getMonitors(): Array<Graphics.Monitor?> {
        TODO("Not yet implemented")
    }

    override fun getDisplayModes(): Array<Graphics.DisplayMode> {
        TODO("Not yet implemented")
    }

    override fun getDisplayModes(monitor: Graphics.Monitor?): Array<Graphics.DisplayMode> {
        TODO("Not yet implemented")
    }

    override fun getDisplayMode(): Graphics.DisplayMode {
        TODO("Not yet implemented")
    }

    override fun getDisplayMode(monitor: Graphics.Monitor?): Graphics.DisplayMode {
        TODO("Not yet implemented")
    }

    override fun setFullscreenMode(displayMode: Graphics.DisplayMode?): Boolean {
        TODO("Not yet implemented")
    }

    override fun setWindowedMode(width: Int, height: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun setTitle(title: String?) {
        TODO("Not yet implemented")
    }

    override fun setUndecorated(undecorated: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setResizable(resizable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setVSync(vsync: Boolean) {
        TODO("Not yet implemented")
    }

    override fun setForegroundFPS(fps: Int) {
        TODO("Not yet implemented")
    }

    override fun getBufferFormat(): Graphics.BufferFormat? {
        TODO("Not yet implemented")
    }

    override fun supportsExtension(extension: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun setContinuousRendering(isContinuous: Boolean) {
        TODO("Not yet implemented")
    }

    override fun isContinuousRendering(): Boolean {
        TODO("Not yet implemented")
    }

    override fun requestRendering() {
        TODO("Not yet implemented")
    }

    override fun isFullscreen(): Boolean {
        TODO("Not yet implemented")
    }

}