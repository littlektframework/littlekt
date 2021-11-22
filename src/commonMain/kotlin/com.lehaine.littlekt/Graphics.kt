package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.graphics.GLVersion
import com.lehaine.littlekt.util.TimeSpan


/**
 * @author Colton Daily
 * @date 10/4/2021
 */
interface Graphics {
    /**
     * Enumeration describing different types of [Graphics] implementations.
     */
    enum class GraphicsType {
        AndroidGL, WebGL, iOSGL, Mock, LWJGL3
    }

    /**
     * Describe a fullscreen display mode
     */
    data class DisplayMode(
        /** the width in physical pixels  */
        val width: Int,
        /** the height in physical pixles  */
        val height: Int,
        /** the refresh rate in Hertz  */
        val refreshRate: Int,
        /** the number of bits per pixel, may exclude alpha  */
        val bitsPerPixel: Int
    )

    /**
     * Describes a monitor
     */
    data class Monitor(val virtualX: Int, val virtualY: Int, val name: String)

    /**
     * Class describing the bits per pixel, depth buffer precision, stencil precision and number of MSAA samples.
     */
    data class BufferFormat(/* number of bits per color channel */
        val r: Int, val g: Int, val b: Int, val a: Int, /* number of bits for depth and stencil buffer */
        val depth: Int, val stencil: Int,
        /** number of samples for multi-sample anti-aliasing (MSAA)  */
        val samples: Int,
        /** whether coverage sampling anti-aliasing is used. in that case you have to clear the coverage buffer as well!  */
        val coverageSampling: Boolean
    )

    val gl: GL

    /**
     * @return the width of the client area in logical pixels.
     */
    val width: Int

    /**
     *  @return the height of the client area in logical pixels
     */
    val height: Int

    /**
     * @return the width of the framebuffer in physical pixels
     */
    val backBufferWidth: Int

    /**
     * @return the height of the framebuffer in physical pixels
     */
    val backBufferHeight: Int

    /**
     * @return amount of pixels per logical pixel (point)
     */
    val backBufferScale: Float get() = backBufferWidth / width.toFloat()

    /**
     * @return the inset from the left which avoids display cutouts in logical pixels
     */
    val safeInsetLeft: Int

    /**
     * @return the inset from the top which avoids display cutouts in logical pixels
     */
    val safeInsetTop: Int

    /**
     * @return the inset from the bottom which avoids display cutouts or floating gesture bars, in logical pixels
     */
    val safeInsetBottom: Int

    /**
     * @return the inset from the right which avoids display cutouts in logical pixels
     */
    val safeInsetRight: Int

    /**
     * Returns the id of the current frame. The general contract of this method is that the id is incremented only when the
     * application is in the running state right before calling the [ApplicationListener.render] method. Also, the id of
     * the first frame is 0; the id of subsequent frames is guaranteed to take increasing values for 2<sup>63</sup>-1 rendering
     * cycles.
     * @return the id of the current frame
     */
    fun getFrameId(): Long

    /**
     * @return the time span between the current frame and the last frame in seconds.
     */
    fun getDeltaTime(): TimeSpan

    /**
     * @return the average number of frames per second
     */
    fun getFramesPerSecond(): Int

    /**
     * @return the [GraphicsType] of this Graphics instance
     */
    fun getType(): GraphicsType?

    /**
     * @return the [GLVersion] of this Graphics instance
     */
    fun getGLVersion(): GLVersion

    /**
     * @return if the current GL version is 3.2 or higher
     */
    fun isGL32() = getGLVersion() == GLVersion.GL_30

    /** @return the pixels per inch on the x-axis
     */
    fun getPpiX(): Float

    /** @return the pixels per inch on the y-axis
     */
    fun getPpiY(): Float

    /** @return the pixels per centimeter on the x-axis
     */
    fun getPpcX(): Float

    /** @return the pixels per centimeter on the y-axis.
     */
    fun getPpcY(): Float

    /** This is a scaling factor for the Density Independent Pixel unit, following the same conventions as
     * android.util.DisplayMetrics#density, where one DIP is one pixel on an approximately 160 dpi screen. Thus on a 160dpi screen
     * this density value will be 1; on a 120 dpi screen it would be .75; etc.
     *
     * If the density could not be determined, this returns a default value of 1.
     *
     * @return the Density Independent Pixel factor of the display.
     */
    fun getDensity(): Float {
        val ppiX = getPpiX()
        return if (ppiX > 0 && ppiX <= Float.MAX_VALUE) {
            ppiX / 160f
        } else {
            1f
        }

    }

    /** Whether the given backend supports a display mode change via calling [Graphics.setFullscreenMode]
     *
     * @return whether display mode changes are supported or not.
     */
    fun supportsDisplayModeChange(): Boolean

    /**
     * @return the primary monitor
     */
    fun getPrimaryMonitor(): Monitor

    /**
     * @return the monitor the application's window is located on
     */
    fun getMonitor(): Monitor

    /**
     * @return the currently connected [Monitor]s
     */
    fun getMonitors(): Array<Monitor?>

    /**
     * @return the supported fullscreen [DisplayMode](s) of the monitor the window is on
     */
    fun getDisplayModes(): Array<DisplayMode>

    /**
     * @return the supported fullscreen [DisplayMode]s of the given [Monitor]
     */
    fun getDisplayModes(monitor: Monitor?): Array<DisplayMode>

    /**
     * @return the current [DisplayMode] of the monitor the window is on.
     */
    fun getDisplayMode(): DisplayMode

    /**
     * @return the current [DisplayMode] of the given [Monitor]
     */
    fun getDisplayMode(monitor: Monitor?): DisplayMode

    /**
     * Sets the window to full-screen mode.
     *
     * @param displayMode the display mode.
     * @return whether the operation succeeded.
     */
    fun setFullscreenMode(displayMode: DisplayMode?): Boolean

    /**
     * Sets the window to windowed mode.
     *
     * @param width the width in pixels
     * @param height the height in pixels
     * @return whether the operation succeeded
     */
    fun setWindowedMode(width: Int, height: Int): Boolean

    /**
     * Sets the title of the window. Ignored on Android.
     *
     * @param title the title.
     */
    fun setTitle(title: String?)

    /**
     * Sets the window decoration as enabled or disabled. On Android, this will enable/disable the menu bar.
     *
     * Note that immediate behavior of this method may vary depending on the implementation. It may be necessary for the window to
     * be recreated in order for the changes to take effect. Consult the documentation for the backend in use for more information.
     *
     * Supported on all GDX desktop backends and on Android (to disable the menu bar).
     *
     * @param undecorated true if the window border or status bar should be hidden. false otherwise.
     */
    fun setUndecorated(undecorated: Boolean)

    /**
     * Sets whether or not the window should be resizable. Ignored on Android.
     *
     * Note that immediate behavior of this method may vary depending on the implementation. It may be necessary for the window to
     * be recreated in order for the changes to take effect. Consult the documentation for the backend in use for more information.
     *
     * Supported on all GDX desktop backends.
     *
     * @param resizable
     */
    fun setResizable(resizable: Boolean)

    /**
     * Enable/Disable vsynching. This is a best-effort attempt which might not work on all platforms.
     *
     * @param vsync vsync enabled or not.
     */
    fun setVSync(vsync: Boolean)

    /**
     * Sets the target framerate for the application when using continuous rendering. Might not work on all platforms. Is not
     * generally advised to be used on mobile platforms.
     *
     * @param fps the targeted fps; default differs by platform
     */
    fun setForegroundFPS(fps: Int)

    /**
     * @return the format of the color, depth and stencil buffer in a [BufferFormat] instance
     */
    fun getBufferFormat(): BufferFormat?

    /**
     * @param extension the extension name
     * @return whether the extension is supported
     */
    fun supportsExtension(extension: String): Boolean

    /**
     * Sets whether to render continuously. In case rendering is performed non-continuously, the following events will trigger a
     * redraw:
     *
     *
     *  * A call to [.requestRendering]
     *  * Input events from the touch screen/mouse or keyboard
     *  * A [Runnable] is posted to the rendering thread via [Application.postRunnable]. In the case of a
     * multi-window app, all windows will request rendering if a runnable is posted to the application. To avoid this, post a
     * runnable to the window instead.
     *
     *
     * Life-cycle events will also be reported as usual, see [ApplicationListener]. This method can be called from any
     * thread.
     *
     * @param isContinuous whether the rendering should be continuous or not.
     */
    fun setContinuousRendering(isContinuous: Boolean)

    /**
     * @return whether rendering is continuous.
     */
    fun isContinuousRendering(): Boolean

    /** Requests a new frame to be rendered if the rendering mode is non-continuous. This method can be called from any thread.  */
    fun requestRendering()

    /** Whether the app is fullscreen or not  */
    fun isFullscreen(): Boolean

//    /** Create a new cursor represented by the [com.badlogic.gdx.graphics.Pixmap]. The Pixmap must be in RGBA8888 format,
//     * width & height must be powers-of-two greater than zero (not necessarily equal) and of a certain minimum size (32x32 is a
//     * safe bet), and alpha transparency must be single-bit (i.e., 0x00 or 0xFF only). This function returns a Cursor object that
//     * can be set as the system cursor by calling [.setCursor] .
//     *
//     * @param pixmap the mouse cursor image as a [com.badlogic.gdx.graphics.Pixmap]
//     * @param xHotspot the x location of the hotspot pixel within the cursor image (origin top-left corner)
//     * @param yHotspot the y location of the hotspot pixel within the cursor image (origin top-left corner)
//     * @return a cursor object that can be used by calling [.setCursor] or null if not supported
//     */
//    fun newCursor(pixmap: Pixmap?, xHotspot: Int, yHotspot: Int): Cursor?
//
//    /** Only viable on the lwjgl-backend and on the gwt-backend. Browsers that support cursor:url() and support the png format (the
//     * pixmap is converted to a data-url of type image/png) should also support custom cursors. Will set the mouse cursor image to
//     * the image represented by the [com.badlogic.gdx.graphics.Cursor]. It is recommended to call this function in the main
//     * render thread, and maximum one time per frame.
//     *
//     * @param cursor the mouse cursor as a [com.badlogic.gdx.graphics.Cursor]
//     */
//    fun setCursor(cursor: Cursor?)
//
//    /** Sets one of the predefined [SystemCursor]s  */
//    fun setSystemCursor(systemCursor: SystemCursor?)
}