package com.lehaine.littlekt

import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.log.Logger
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 10/5/2021
 */
abstract class Context {

    enum class Platform {
        DESKTOP,
        JS,
        ANDROID,
        IOS
    }

    abstract val stats: AppStats

    abstract val configuration: ContextConfiguration

    abstract val graphics: Graphics

    open val gl: GL get() = graphics.gl

    abstract val input: Input

    abstract val logger: Logger

    abstract val vfs: Vfs

    abstract val resourcesVfs: VfsFile

    abstract val storageVfs: VfsFile

    abstract val platform: Platform

    internal abstract val shouldClose: Boolean

    internal abstract suspend fun initialize(build: (app: Context) -> ContextListener)

    internal abstract suspend fun update(dt: Duration)

    abstract fun close()

    internal abstract suspend fun destroy()

    abstract fun onRender(action: suspend (dt: Duration) -> Unit)
    abstract fun onPostRender(action: suspend (dt: Duration) -> Unit)
    abstract fun onResize(action: suspend (width: Int, height: Int) -> Unit)
    abstract fun onDispose(action: suspend () -> Unit)
    abstract fun postRunnable(action: suspend () -> Unit)
}