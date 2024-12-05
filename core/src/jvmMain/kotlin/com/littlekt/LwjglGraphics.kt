package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.HdpiMode
import com.littlekt.graphics.SystemCursor
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.wgpu.*
import com.littlekt.wgpu.WGPU.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.lwjgl.glfw.*
import org.lwjgl.system.JNI.*
import org.lwjgl.system.macosx.ObjCRuntime.*

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

    internal var instance: Instance = Instance(WGPU_NULL)

    override var surface: Surface = Surface(WGPU_NULL)
    override var adapter: Adapter = Adapter(WGPU_NULL)
    override var device: Device = Device(WGPU_NULL)

    override val preferredFormat by lazy { surface.getPreferredFormat(adapter) }
    private var hasSurfaceCapabilities = false
    override val surfaceCapabilities: SurfaceCapabilities by lazy {
        hasSurfaceCapabilities = true
        surface.getCapabilities(adapter)
    }

    override fun configureSurface(
        usage: TextureUsage,
        format: TextureFormat,
        presentMode: PresentMode,
        alphaMode: AlphaMode,
    ) {
        surface.configure(
            SurfaceConfiguration(device, usage, format, presentMode, alphaMode, width, height)
        )
    }

    internal suspend fun requestAdapterAndDevice(powerPreference: PowerPreference) {
        val output = atomic(WGPU_NULL)

        Arena.ofConfined().use { scope ->
            val options = WGPURequestAdapterOptions.allocate(scope)
            val callback =
                WGPUInstanceRequestAdapterCallback.allocate(
                    { status, adapter, message, _ ->
                        if (status == WGPURequestAdapterStatus_Success()) {
                            output.update { adapter }
                        } else {
                            logger.error {
                                "requestAdapter status=$status, message=${message.getString(0)}"
                            }
                        }
                    },
                    scope,
                )
            WGPURequestAdapterOptions.powerPreference(options, powerPreference.nativeVal)
            WGPURequestAdapterOptions.compatibleSurface(options, surface.segment)
            WGPURequestAdapterOptions.nextInChain(options, WGPU_NULL)
            wgpuInstanceRequestAdapter(instance.segment, options, callback, WGPU_NULL)
        }
        adapter = Adapter(output.value)
        requestDevice()
    }

    private suspend fun requestDevice() {
        device = adapter.requestDevice()
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

    internal fun createInstance(configuration: JvmConfiguration) {
        instance =
            Arena.ofConfined().use { scope ->
                val instanceDesc = WGPUInstanceDescriptor.allocate(scope)
                val extras = WGPUInstanceExtras.allocate(scope)
                if (configuration.preferredBackends.isInvalid()) {
                    logger.warn {
                        "Configuration.preferredBackends is invalid and will resort to the default backend. Specify at least one backend or remove the list to get rid of this warning."
                    }
                } else {
                    WGPUInstanceExtras.backends(extras, configuration.preferredBackends.flag)
                }
                WGPUChainedStruct.sType(
                    WGPUInstanceExtras.chain(extras),
                    WGPUSType_InstanceExtras(),
                )
                WGPUInstanceDescriptor.nextInChain(instanceDesc, extras)
                Instance(wgpuCreateInstance(instanceDesc))
            }
    }

    internal fun configureSurfaceToWindow(windowHandle: Long) {
        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val isLinux = System.getProperty("os.name").lowercase().contains("linux")
        surface =
            Surface(
                when {
                    isWindows -> {
                        val osHandle = GLFWNativeWin32.glfwGetWin32Window(windowHandle)
                        Arena.ofConfined().use { scope ->
                            val desc = WGPUSurfaceDescriptor.allocate(scope)
                            val windowsDesc = WGPUSurfaceDescriptorFromWindowsHWND.allocate(scope)
                            WGPUSurfaceDescriptorFromWindowsHWND.hwnd(
                                windowsDesc,
                                MemorySegment.ofAddress(osHandle),
                            )
                            WGPUSurfaceDescriptorFromWindowsHWND.hinstance(windowsDesc, WGPU_NULL)
                            WGPUChainedStruct.sType(
                                WGPUSurfaceDescriptorFromWindowsHWND.chain(windowsDesc),
                                WGPUSType_SurfaceDescriptorFromWindowsHWND(),
                            )
                            WGPUSurfaceDescriptor.label(desc, WGPU_NULL)
                            WGPUSurfaceDescriptor.nextInChain(desc, windowsDesc)
                            wgpuInstanceCreateSurface(instance.segment, desc)
                        }
                    }
                    isLinux -> {
                        val platform = GLFW.glfwGetPlatform()
                        when (platform) {
                            GLFW.GLFW_PLATFORM_X11 -> {
                                Arena.ofConfined().use { scope ->
                                    val display = GLFWNativeX11.glfwGetX11Display()
                                    val osHandle = GLFWNativeX11.glfwGetX11Window(windowHandle)
                                    val desc = WGPUSurfaceDescriptor.allocate(scope)
                                    val windowsDesc =
                                        WGPUSurfaceDescriptorFromXlibWindow.allocate(scope)
                                    WGPUSurfaceDescriptorFromXlibWindow.display(
                                        windowsDesc,
                                        MemorySegment.ofAddress(display),
                                    )
                                    WGPUSurfaceDescriptorFromXlibWindow.window(
                                        windowsDesc,
                                        osHandle,
                                    )
                                    WGPUChainedStruct.sType(
                                        WGPUSurfaceDescriptorFromXlibWindow.chain(windowsDesc),
                                        WGPUSType_SurfaceDescriptorFromXlibWindow(),
                                    )
                                    WGPUSurfaceDescriptor.label(desc, WGPU_NULL)
                                    WGPUSurfaceDescriptor.nextInChain(desc, windowsDesc)
                                    wgpuInstanceCreateSurface(instance.segment, desc)
                                }
                            }
                            GLFW.GLFW_PLATFORM_WAYLAND -> {
                                Arena.ofConfined().use { scope ->
                                    val display = GLFWNativeWayland.glfwGetWaylandDisplay()
                                    val osHandle =
                                        GLFWNativeWayland.glfwGetWaylandWindow(windowHandle)
                                    val desc = WGPUSurfaceDescriptor.allocate(scope)
                                    val windowsDesc =
                                        WGPUSurfaceDescriptorFromWaylandSurface.allocate(scope)
                                    WGPUSurfaceDescriptorFromWaylandSurface.display(
                                        windowsDesc,
                                        MemorySegment.ofAddress(display),
                                    )
                                    WGPUSurfaceDescriptorFromWaylandSurface.surface(
                                        windowsDesc,
                                        MemorySegment.ofAddress(osHandle),
                                    )
                                    WGPUChainedStruct.sType(
                                        WGPUSurfaceDescriptorFromWaylandSurface.chain(windowsDesc),
                                        WGPUSType_SurfaceDescriptorFromWaylandSurface(),
                                    )
                                    WGPUSurfaceDescriptor.label(desc, WGPU_NULL)
                                    WGPUSurfaceDescriptor.nextInChain(desc, windowsDesc)
                                    wgpuInstanceCreateSurface(instance.segment, desc)
                                }
                            }
                            else -> {
                                logger.log(Logger.Level.ERROR) {
                                    "Linux platform not supported. Supported backends: [X11, Wayland]"
                                }
                                WGPU_NULL
                            }
                        }
                    }
                    isMac -> {
                        val osHandle = GLFWNativeCocoa.glfwGetCocoaWindow(windowHandle)
                        Arena.ofConfined().use { scope ->
                            val objc_msgSend = getLibrary().getFunctionAddress("objc_msgSend")
                            val CAMetalLayer = objc_getClass("CAMetalLayer")
                            val contentView =
                                invokePPP(osHandle, sel_getUid("contentView"), objc_msgSend)
                            // [ns_window.contentView setWantsLayer:YES];
                            invokePPV(contentView, sel_getUid("setWantsLayer:"), true, objc_msgSend)
                            // metal_layer = [CAMetalLayer layer];
                            val metal_layer =
                                invokePPP(CAMetalLayer, sel_registerName("layer"), objc_msgSend)
                            // [ns_window.contentView setLayer:metal_layer];
                            invokePPPP(
                                contentView,
                                sel_getUid("setLayer:"),
                                metal_layer,
                                objc_msgSend,
                            )

                            val desc = WGPUSurfaceDescriptor.allocate(scope)
                            val metalDesc = WGPUSurfaceDescriptorFromMetalLayer.allocate(scope)
                            WGPUSurfaceDescriptorFromMetalLayer.layer(
                                metalDesc,
                                MemorySegment.ofAddress(metal_layer),
                            )
                            WGPUChainedStruct.sType(
                                WGPUSurfaceDescriptorFromMetalLayer.chain(metalDesc),
                                WGPUSType_SurfaceDescriptorFromMetalLayer(),
                            )
                            WGPUSurfaceDescriptor.label(desc, WGPU_NULL)
                            WGPUSurfaceDescriptor.nextInChain(desc, metalDesc)

                            wgpuInstanceCreateSurface(instance.segment, desc)
                        }
                    }
                    else -> {
                        logger.log(Logger.Level.ERROR) { "Platform not supported." }
                        WGPU_NULL
                    }
                }
            )
    }

    override fun release() {
        if (device.queue.segment != WGPU_NULL) {
            device.queue.release()
        }
        if (device.segment != WGPU_NULL) {
            device.release()
        }
        if (adapter.segment != WGPU_NULL) {
            adapter.release()
        }

        if (surface.segment != WGPU_NULL) {
            surface.release()
        }

        if (instance.segment != WGPU_NULL) {
            wgpuInstanceRelease(instance.segment)
        }
    }

    companion object {
        private val logger = Logger<LwjglGraphics>()
    }
}
