package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.HdpiMode
import com.littlekt.graphics.SystemCursor
import com.littlekt.log.Logger
import io.ygdrasil.wgpu.Adapter
import io.ygdrasil.wgpu.CompositeAlphaMode
import io.ygdrasil.wgpu.Device
import io.ygdrasil.wgpu.PowerPreference
import io.ygdrasil.wgpu.PresentMode
import io.ygdrasil.wgpu.Surface
import io.ygdrasil.wgpu.SurfaceConfiguration
import io.ygdrasil.wgpu.TextureFormat
import io.ygdrasil.wgpu.TextureUsage
import io.ygdrasil.wgpu.WGPU
import io.ygdrasil.wgpu.WGPU.Companion
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.lwjgl.system.JNI.*
import org.lwjgl.system.macosx.ObjCRuntime.*
import java.awt.AlphaComposite
import org.rococoa.ID
import org.rococoa.Rococoa
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import darwin.CAMetalLayer
import darwin.NSWindow

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class LwjglGraphics(private val context: LwjglContext) : Graphics, Releasable {
    private val systemCursors = mutableMapOf<SystemCursor, Long>()

    internal var _logicalWidth: Int = 0
    internal var _logicalHeight: Int = 0

    internal var _backBufferWidth: Int = 0
    internal var _backBufferHeight: Int = 0

    override val width: Int
        get() =
            if (context.configuration.hdpiMode == HdpiMode.PIXELS) backBufferWidth
            else _logicalWidth

    override val height: Int
        get() =
            if (context.configuration.hdpiMode == HdpiMode.PIXELS) backBufferHeight
            else _logicalHeight

    override val backBufferWidth: Int
        get() = _backBufferWidth

    override val backBufferHeight: Int
        get() = _backBufferHeight

    internal var instance = WGPU.createInstance() ?: error("fail to wgpu instance")

    override lateinit var surface: Surface
    override lateinit var adapter: Adapter
    override lateinit var device: Device


    override val preferredFormat by lazy { surface.preferredCanvasFormat  ?: surface.supportedFormats.first() }
    private var hasSurfaceCapabilities = false

    override fun configureSurface(
        usage: Set<TextureUsage>,
        format: TextureFormat,
        presentMode: PresentMode,
        alphaMode: CompositeAlphaMode
    ) {
        surface.configure(
            SurfaceConfiguration(device, format, usage,  alphaMode = alphaMode, presentMode = presentMode) //width, height
        )
    }

    internal suspend fun requestAdapterAndDevice(powerPreference: PowerPreference) {
        adapter = instance.requestAdapter(surface, powerPreference) ?: error("No adapter found.")
        surface.computeSurfaceCapabilities(adapter)
        requestDevice()
    }

    private suspend fun requestDevice() {
        device = adapter.requestDevice() ?: error("No device found.")
    }

    override fun supportsExtension(extension: String): Boolean {
        return GLFW.glfwExtensionSupported(extension)
    }

    override fun setCursor(cursor: Cursor) {
        GLFW.glfwSetCursor(context.windowHandle, cursor.cursorHandle)
    }

    override fun setCursor(cursor: SystemCursor) {
        var handle = systemCursors[cursor]
        if (handle == null) {
            handle =
                when (cursor) {
                    SystemCursor.ARROW -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)
                    SystemCursor.I_BEAM -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR)
                    SystemCursor.CROSSHAIR ->
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR)
                    SystemCursor.HAND -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR)
                    SystemCursor.HORIZONTAL_RESIZE ->
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR)
                    SystemCursor.VERTICAL_RESIZE ->
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR)
                }
            if (handle == 0L) return
            systemCursors[cursor] = handle
        }
        GLFW.glfwSetCursor(context.windowHandle, handle)
    }

    internal fun configureSurfaceToWindow(windowHandle: Long) {
        surface = instance.getSurface(windowHandle, { width to height })
    }

    private fun WGPU.getSurface(window: Long, sizeProvider: () -> Pair<Int, Int>): Surface = when (Platform.os) {
        Os.Linux -> {
            val display = glfwGetX11Display().let { MemorySegment.ofAddress(it) }
            val x11_window = glfwGetX11Window(window)
            getSurfaceFromX11Window(display, x11_window) ?: error("fail to get surface on Linux")
        }
        Os.Window -> {
            val hwnd = glfwGetWin32Window(window).let { MemorySegment.ofAddress(it) }
            val hinstance = Kernel32.INSTANCE.GetModuleHandle(null).pointer.toMemory()
            getSurfaceFromWindows(hinstance, hwnd) ?: error("fail to get surface on Windows")
        }
        Os.MacOs -> {
            val nsWindowPtr = glfwGetCocoaWindow(window)
            val nswindow = Rococoa.wrap(ID.fromLong(nsWindowPtr), NSWindow::class.java)
            nswindow.contentView()?.setWantsLayer(true)
            val layer = CAMetalLayer.layer()
            nswindow.contentView()?.setLayer(layer.id().toLong().toPointer())
            getSurfaceFromMetalLayer(MemorySegment.ofAddress(layer.id().toLong()))
        }
    }.also { if( it == MemorySegment.NULL) error("fail to get surface") }
        .let { Surface(it, sizeProvider) }

    override fun release() {  }
}

private enum class Os {
    Linux,
    Window,
    MacOs
}

private object Platform {
    val os: Os
        get() = System.getProperty("os.name").let { name ->
            when {
                arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> Os.Linux
                arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> Os.MacOs
                arrayOf("Windows").any { name.startsWith(it) } -> Os.Window
                else -> error("Unrecognized or unsupported operating system.")
            }
        }
}

private fun Pointer.toMemory() = MemorySegment.ofAddress(Pointer.nativeValue(this))
private fun Long.toPointer(): Pointer = Pointer(this)