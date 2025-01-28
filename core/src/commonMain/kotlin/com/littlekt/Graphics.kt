package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.Surface
import com.littlekt.graphics.SystemCursor
import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.Device
import io.ygdrasil.webgpu.TextureFormat
import io.ygdrasil.webgpu.TextureUsage

/**
 * Contains graphic related properties and methods.
 *
 * @author Colton Daily
 * @date 10/4/2021
 */
interface Graphics {

    /** @return the width of the client area in logical pixels. */
    val width: Int

    /** @return the height of the client area in logical pixels */
    val height: Int

    /** @return the width of the back framebuffer in physical pixels */
    val backBufferWidth: Int

    /** @return the height of the back framebuffer in physical pixels */
    val backBufferHeight: Int

    /** @return the underlying [Surface] of the graphics. */
    val surface: Surface

    /** @return the underlying [Adapter] of the graphics. */
    val adapter: Adapter

    /** @return the underlying [Device] */
    val device: Device

    /** @return the preferred [TextureFormat] of the underlying [surface]. */
    val preferredFormat: TextureFormat

    /**
     * Configures the underlying [surface], via [Surface.configure], which initializes the surface
     * for presentation.
     */
    fun configureSurface(
        usage: Set<TextureUsage> = setOf(TextureUsage.RenderAttachment),
        format: TextureFormat = preferredFormat,
        alphaMode: CompositeAlphaMode = surface.supportedAlphaMode.first()
    )

    /**
     * @param extension the extension name
     * @return whether the extension is supported
     */
    fun supportsExtension(extension: String): Boolean

    /** Overrides the current cursor with the specified [cursor]. */
    fun setCursor(cursor: Cursor)

    /** Overrides the current cursor with a default system cursor. See [SystemCursor] */
    fun setCursor(cursor: SystemCursor)

    /** Converts back buffer x-coordinate to logical screen coordinates. */
    fun toLogicalX(backBufferX: Int) = backBufferX.toLogicalX

    /** Converts back buffer y-coordinate to logical screen coordinates. */
    fun toLogicalY(backBufferY: Int) = backBufferY.toLogicalY

    /** Converts logical screen x-coordinate to back buffer coordinates. */
    fun toBackBufferX(logicalX: Int) = logicalX.toBackBufferX

    /** Converts logical screen y-coordinate to back buffer coordinates. */
    fun toBackBufferY(logicalY: Int) = logicalY.toBackBufferY

    /** Converts back buffer x-coordinate to logical screen coordinates. */
    val Int.toLogicalX
        get() = (this * width / backBufferWidth.toFloat()).toInt()

    /** Converts back buffer y-coordinate to logical screen coordinates. */
    val Int.toLogicalY
        get() = (this * height / backBufferHeight.toFloat()).toInt()

    /** Converts logical screen x-coordinate to back buffer coordinates. */
    val Int.toBackBufferX
        get() = (this * backBufferWidth / width.toFloat()).toInt()

    /** Converts logical screen y-coordinate to back buffer coordinates. */
    val Int.toBackBufferY
        get() = (this * backBufferHeight / height.toFloat()).toInt()
}
