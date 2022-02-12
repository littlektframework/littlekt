package com.lehaine.littlekt

import com.lehaine.littlekt.file.Vfs
import com.lehaine.littlekt.file.vfs.VfsFile
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.log.Logger

/**
 * @author Colton Daily
 * @date 2/11/2022
 */
class AndroidContext : Context() {
    override val stats: AppStats
        get() = TODO("Not yet implemented")
    override val configuration: ContextConfiguration
        get() = TODO("Not yet implemented")
    override val graphics: Graphics
        get() = TODO("Not yet implemented")
    override val input: Input
        get() = TODO("Not yet implemented")
    override val logger: Logger
        get() = TODO("Not yet implemented")
    override val vfs: Vfs
        get() = TODO("Not yet implemented")
    override val resourcesVfs: VfsFile
        get() = TODO("Not yet implemented")
    override val storageVfs: VfsFile
        get() = TODO("Not yet implemented")
    override val platform: Platform
        get() = TODO("Not yet implemented")

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