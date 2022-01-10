package com.lehaine.littlekt

import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.graphics.GL
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 10/5/2021
 */
interface Context : CoroutineScope {

    enum class Platform {
        DESKTOP,
        JS,
        ANDROID,
        IOS
    }

    val stats: AppStats

    val configuration: ContextConfiguration

    val graphics: Graphics

    val gl: GL get() = graphics.gl

    val input: Input

    val logger: Logger

    val vfs: Vfs

    val resourcesVfs: VfsFile

    val storageVfs: VfsFile

    val platform: Platform

    suspend fun start(build: (app: Context) -> ContextListener)

    suspend fun close()

    suspend fun destroy()

    fun onRender(action: suspend (dt: Duration) -> Unit)
    fun onPostRender(action: suspend (dt: Duration) -> Unit)
    fun onResize(action: suspend (width: Int, height: Int) -> Unit)
    fun onDispose(action: suspend () -> Unit)
    fun postRunnable(action: suspend () -> Unit)
}