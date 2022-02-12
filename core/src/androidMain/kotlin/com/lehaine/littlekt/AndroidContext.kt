package com.lehaine.littlekt

import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 2/11/2022
 */
class AndroidContext(override val configuration: AndroidConfiguration) : Context() {
    override val stats: AppStats = AppStats()
    override val graphics: Graphics
        get() = TODO("Not yet implemented")
    override val input: Input
        get() = TODO("Not yet implemented")
    override val logger: Logger = Logger(configuration.title)
    override val vfs: Vfs
        get() = TODO("Not yet implemented")
    override val resourcesVfs: VfsFile
        get() = TODO("Not yet implemented")
    override val storageVfs: VfsFile
        get() = TODO("Not yet implemented")
    override val platform: Platform = Platform.ANDROID

    override fun start(build: (app: Context) -> ContextListener) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

}