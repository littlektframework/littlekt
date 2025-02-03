package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.HdpiMode
import com.littlekt.graphics.Surface
import com.littlekt.graphics.SystemCursor
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import darwin.CAMetalLayer
import darwin.NSWindow
import ffi.NativeAddress
import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.NativeSurface
import io.ygdrasil.webgpu.PowerPreference
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.TextureFormat
import io.ygdrasil.webgpu.TextureUsage
import io.ygdrasil.webgpu.WGPU
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.rococoa.ID
import org.rococoa.Rococoa
import java.lang.foreign.MemorySegment

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
    lateinit var nativeSurface: NativeSurface
    override lateinit var adapter: Adapter
    override lateinit var device: Device


    override val preferredFormat by lazy { surface.preferredCanvasFormat  ?: surface.supportedFormats.first() }
    private var hasSurfaceCapabilities = false

    override fun configureSurface(
        usage: Set<TextureUsage>,
        format: TextureFormat,
        alphaMode: CompositeAlphaMode
    ) {
        surface.configure(
            SurfaceConfiguration(device, format, usage,  alphaMode = alphaMode)
        )
    }

    internal suspend fun requestAdapterAndDevice(powerPreference: PowerPreference) {
        adapter = instance.requestAdapter(nativeSurface, powerPreference) ?: error("No adapter found.")
        nativeSurface.computeSurfaceCapabilities(adapter)
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
        nativeSurface = instance.getSurface(windowHandle)
        surface = Surface(nativeSurface, windowHandle)
    }

    private fun WGPU.getSurface(window: Long): NativeSurface = when (Platform.os) {
        Os.Linux -> {
            val display = glfwGetX11Display().let { MemorySegment.ofAddress(it) }
            val x11_window = glfwGetX11Window(window)
            getSurfaceFromX11Window(display.let(::NativeAddress), x11_window.toULong()) ?: error("fail to get surface on Linux")
        }
        Os.Window -> {
            val hwnd = glfwGetWin32Window(window).let { MemorySegment.ofAddress(it) }
            val hinstance = Kernel32.INSTANCE.GetModuleHandle(null).pointer.toMemory()
            getSurfaceFromWindows(hinstance.let(::NativeAddress), hwnd.let(::NativeAddress)) ?: error("fail to get surface on Windows")
        }
        Os.MacOs -> {
            val nsWindowPtr = glfwGetCocoaWindow(window)
            val nswindow = Rococoa.wrap(ID.fromLong(nsWindowPtr), NSWindow::class.java)
            nswindow.contentView()?.setWantsLayer(true)
            val layer = CAMetalLayer.layer()
            nswindow.contentView()?.setLayer(layer.id().toLong().toPointer())
            getSurfaceFromMetalLayer(MemorySegment.ofAddress(layer.id().toLong()).let(::NativeAddress)) ?: error("fail to get surface on Mac")
        }
    }

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