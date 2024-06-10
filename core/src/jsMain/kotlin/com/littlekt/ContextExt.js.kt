package com.littlekt

import com.littlekt.file.Vfs
import com.littlekt.file.vfs.VfsFile
import com.littlekt.input.Input
import com.littlekt.log.Logger
import com.littlekt.util.Clipboard

internal actual var ownedContext: Context =
    object : Context() {
        override val stats: AppStats
            get() = TODO("fucked")

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

        override val clipboard: Clipboard
            get() = TODO("Not yet implemented")

        override fun start(build: (app: Context) -> ContextListener) {
            TODO("Not yet implemented")
        }

        override fun close() {
            TODO("Not yet implemented")
        }
    }
