package com.lehaine.littlekt

import android.app.Activity
import com.lehaine.littlekt.file.AndroidVfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.input.AndroidInput
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 2/11/2022
 */
class AndroidContext(private val activity: Activity, override val configuration: AndroidConfiguration) : Context() {
    override val stats: AppStats = AppStats()
    override val graphics: AndroidGraphics = AndroidGraphics(stats.engineStats)
    override val input: AndroidInput = AndroidInput()
    override val logger: Logger = Logger(configuration.title)
    override val vfs: AndroidVfs =
        AndroidVfs(
            activity,
            activity.getPreferences(android.content.Context.MODE_PRIVATE),
            this, logger, "./.storage", "."
        )
    override val resourcesVfs: VfsFile get() = vfs.root
    override val storageVfs: VfsFile get() = VfsFile(vfs, "./.storage")
    override val platform: Platform = Platform.ANDROID

    override fun start(build: (app: Context) -> ContextListener) {
        build.invoke(this)
    }

    override fun close() {

    }

    override fun destroy() {

    }

}